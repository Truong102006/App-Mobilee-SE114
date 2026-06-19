package com.soulmate.backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.soulmate.backend.config.BackendProperties;
import com.soulmate.backend.dto.payment.CreatePremiumOrderResponse;
import com.soulmate.backend.dto.payment.PaymentOrderStatusResponse;
import com.soulmate.backend.dto.payment.PremiumOfferResponse;
import com.soulmate.backend.dto.payment.ReconcilePaymentOrderResponse;
import com.soulmate.backend.dto.payment.SepayWebhookPayload;
import com.soulmate.backend.dto.payment.WebhookAckResponse;
import com.soulmate.backend.exception.ApiException;
import com.soulmate.backend.exception.FirestoreApiExceptionMapper;
import com.soulmate.backend.service.payment.PaymentCodeUtils;
import com.soulmate.backend.service.payment.PremiumTimeCalculator;
import com.soulmate.backend.service.payment.SepayWebhookVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final ZoneId SEPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SEPAY_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USERS_COLLECTION = "users";
    private static final String ORDERS_COLLECTION = "payment_orders";
    private static final String EVENTS_COLLECTION = "payment_events";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final Firestore firestore;
    private final BackendProperties backendProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final SepayWebhookVerifier sepayWebhookVerifier;

    public PaymentService(
        Firestore firestore,
        BackendProperties backendProperties,
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        SepayWebhookVerifier sepayWebhookVerifier
    ) {
        this.firestore = firestore;
        this.backendProperties = backendProperties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.sepayWebhookVerifier = sepayWebhookVerifier;
    }

    public PremiumOfferResponse getPremiumOffer(String uid) {
        BackendProperties.Sepay sepay = checkoutSepayConfig();
        BackendProperties.Premium premium = premiumConfig();
        return new PremiumOfferResponse(
            premium.getPlanCode(),
            premium.getPriceVnd(),
            premium.getDurationDays(),
            premium.getOrderExpireMinutes(),
            sepay.getBankCode().trim(),
            sepay.getBankAccount().trim(),
            sepay.getAccountHolder().trim(),
            loadCurrentPremiumUntil(uid)
        );
    }

    public CreatePremiumOrderResponse createOrResumePremiumOrder(String uid) {
        checkoutSepayConfig();
        long now = System.currentTimeMillis();
        expireStaleOrders(uid, now);

        PaymentOrderRecord activeOrder = findActivePendingOrder(uid, now);
        if (activeOrder == null) {
            activeOrder = createPendingOrder(uid, now);
        }

        return toCreateOrderResponse(activeOrder);
    }

    public PaymentOrderStatusResponse getOrderStatus(String uid, String orderId) {
        PaymentOrderRecord order = loadOwnedOrder(uid, orderId);
        if (STATUS_PENDING.equals(order.status()) && order.expiresAt() <= System.currentTimeMillis()) {
            order = markOrderExpired(order);
        }
        return toStatusResponse(order);
    }

    public ReconcilePaymentOrderResponse reconcilePaymentOrder(String uid, String orderId) {
        SepayConfigSnapshot sepay = reconcileSepayConfig();
        long now = System.currentTimeMillis();
        PaymentOrderRecord order = loadOwnedOrder(uid, orderId);

        if (STATUS_PENDING.equals(order.status()) && order.expiresAt() <= now) {
            order = markOrderExpired(order);
        }
        if (!STATUS_PENDING.equals(order.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending orders can be reconciled.");
        }
        if (order.lastReconcileAttemptAt() != null) {
            long elapsedMillis = now - order.lastReconcileAttemptAt();
            long cooldownMillis = Duration.ofSeconds(sepay.reconcileCooldownSeconds()).toMillis();
            if (elapsedMillis < cooldownMillis) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Please wait a few seconds before checking again.");
            }
        }

        updateOrderFields(order.orderId(), Map.of(
            "lastReconcileAttemptAt", now,
            "updatedAt", now
        ));

        SepayWebhookPayload matchedTransaction = findMatchingSepayTransaction(order, sepay);
        if (matchedTransaction == null) {
            PaymentOrderStatusResponse latest = getOrderStatus(uid, orderId);
            return new ReconcilePaymentOrderResponse(false, "Payment has not been detected yet.", latest);
        }

        TransactionProcessResult result = processIncomingPayment(matchedTransaction, serializeSafely(matchedTransaction), "RECONCILE");
        PaymentOrderStatusResponse latest = getOrderStatus(uid, orderId);
        return new ReconcilePaymentOrderResponse(
            result.matched(),
            result.message(),
            latest
        );
    }

    public WebhookAckResponse handleSepayWebhook(String rawBody, String signatureHeader, String timestampHeader) {
        BackendProperties.Sepay sepay = webhookSepayConfig();
        sepayWebhookVerifier.verify(
            rawBody,
            signatureHeader,
            timestampHeader,
            sepay.getWebhookSecret(),
            sepay.getWebhookMaxSkewSeconds()
        );

        SepayWebhookPayload payload = parseWebhookPayload(rawBody);
        TransactionProcessResult result = processIncomingPayment(payload, rawBody, "WEBHOOK");
        return new WebhookAckResponse(true, result.message());
    }

    private TransactionProcessResult processIncomingPayment(SepayWebhookPayload payload, String rawPayload, String source) {
        if (!"in".equalsIgnoreCase(payload.transferType())) {
            recordPaymentEvent(payload.id(), source, rawPayload, null, null, "IGNORED", "Ignoring outbound SePay transaction.");
            return TransactionProcessResult.ignored("Ignoring outbound SePay transaction.");
        }

        String paymentCode = PaymentCodeUtils.extract(payload.code(), payload.content(), payload.description());
        if (!StringUtils.hasText(paymentCode)) {
            recordPaymentEvent(payload.id(), source, rawPayload, null, null, "IGNORED", "No premium payment code found in SePay content.");
            return TransactionProcessResult.ignored("No payment code found in transfer content.");
        }

        PaymentOrderRecord order = findOrderByPaymentCode(paymentCode);
        if (order == null) {
            recordPaymentEvent(payload.id(), source, rawPayload, null, paymentCode, "IGNORED", "No premium order matches the SePay payment code.");
            return TransactionProcessResult.ignored("No matching premium order was found.");
        }

        long paymentTime = parseSepayDate(payload.transactionDate());
        if (!Objects.equals(normalizeText(payload.accountNumber()), normalizeText(order.bankAccount()))) {
            recordPaymentEvent(payload.id(), source, rawPayload, order.orderId(), paymentCode, "IGNORED", "SePay account number does not match configured destination account.");
            return TransactionProcessResult.ignored("Payment account number does not match.");
        }
        if (payload.transferAmount() != order.amountVnd()) {
            recordPaymentEvent(payload.id(), source, rawPayload, order.orderId(), paymentCode, "IGNORED", "Transfer amount does not match premium order amount.");
            return TransactionProcessResult.ignored("Transfer amount does not match the order amount.");
        }
        if (paymentTime < order.createdAt() - Duration.ofMinutes(5).toMillis()) {
            recordPaymentEvent(payload.id(), source, rawPayload, order.orderId(), paymentCode, "IGNORED", "Transaction time is earlier than the premium order creation window.");
            return TransactionProcessResult.ignored("Transaction timestamp is outside the valid window.");
        }
        if (!PremiumTimeCalculator.isWithinLateWindow(order.expiresAt(), paymentTime, premiumConfig().getReconcileWindowHours())) {
            recordPaymentEvent(payload.id(), source, rawPayload, order.orderId(), paymentCode, "IGNORED", "Transaction arrived after the premium reconciliation window.");
            return TransactionProcessResult.ignored("Payment arrived after the reconciliation window.");
        }

        try {
            TransactionProcessResult result = firestore.runTransaction(transaction -> {
                DocumentReference orderRef = firestore.collection(ORDERS_COLLECTION).document(order.orderId());
                DocumentSnapshot orderSnapshot = transaction.get(orderRef).get();
                if (!orderSnapshot.exists()) {
                    return TransactionProcessResult.ignored("Premium order no longer exists.");
                }

                PaymentOrderRecord currentOrder = PaymentOrderRecord.fromSnapshot(orderSnapshot);
                if (STATUS_PAID.equals(currentOrder.status())) {
                    return TransactionProcessResult.alreadyPaid("This premium order was already confirmed.");
                }

                long now = System.currentTimeMillis();
                long grantedUntil = PremiumTimeCalculator.computeGrantedUntil(
                    loadSnapshotPremiumUntil(transaction.get(firestore.collection(USERS_COLLECTION).document(currentOrder.userId())).get()),
                    paymentTime,
                    currentOrder.durationDays()
                );
                boolean latePayment = paymentTime > currentOrder.expiresAt();

                transaction.update(orderRef, Map.of(
                    "status", STATUS_PAID,
                    "paidAt", paymentTime,
                    "premiumGrantedUntil", grantedUntil,
                    "sepayTransactionId", payload.id(),
                    "latePayment", latePayment,
                    "updatedAt", now
                ));

                DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(currentOrder.userId());
                transaction.set(userRef, Map.of(
                    "premiumUntil", grantedUntil,
                    "premiumUpdatedAt", now
                ), SetOptions.merge());

                return TransactionProcessResult.paid(
                    currentOrder.orderId(),
                    grantedUntil,
                    latePayment,
                    latePayment
                        ? "Late payment accepted and premium was activated."
                        : "Payment confirmed and premium was activated."
                );
            }).get();

            recordPaymentEvent(
                payload.id(),
                source,
                rawPayload,
                order.orderId(),
                paymentCode,
                result.eventStatus(),
                result.message()
            );
            return result;
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to process SePay payment order={} txId={} source={}", order.orderId(), payload.id(), source, exception);
            recordPaymentEvent(
                payload.id(),
                source,
                rawPayload,
                order.orderId(),
                paymentCode,
                "ERROR",
                "Failed to persist premium payment result."
            );
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to process premium payment.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private PaymentOrderRecord createPendingOrder(String uid, long now) {
        BackendProperties.Sepay sepay = checkoutSepayConfig();
        BackendProperties.Premium premium = premiumConfig();
        String orderId = UUID.randomUUID().toString();
        String paymentCode = generateUniquePaymentCode();
        long expiresAt = now + Duration.ofMinutes(premium.getOrderExpireMinutes()).toMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("userId", uid);
        data.put("planCode", premium.getPlanCode());
        data.put("priceVnd", premium.getPriceVnd());
        data.put("durationDays", premium.getDurationDays());
        data.put("paymentCode", paymentCode);
        data.put("status", STATUS_PENDING);
        data.put("createdAt", now);
        data.put("updatedAt", now);
        data.put("expiresAt", expiresAt);
        data.put("paidAt", null);
        data.put("premiumGrantedUntil", null);
        data.put("sepayTransactionId", null);
        data.put("latePayment", false);
        data.put("lastReconcileAttemptAt", null);
        data.put("bankCode", sepay.getBankCode().trim());
        data.put("bankAccount", sepay.getBankAccount().trim());
        data.put("accountHolder", sepay.getAccountHolder().trim());

        try {
            firestore.collection(ORDERS_COLLECTION).document(orderId).set(data).get();
            return PaymentOrderRecord.fromData(orderId, data);
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to create premium payment order for uid={}", uid, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to create premium payment order.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private PaymentOrderRecord loadOwnedOrder(String uid, String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "orderId is required.");
        }

        try {
            DocumentSnapshot snapshot = firestore.collection(ORDERS_COLLECTION).document(orderId.trim()).get().get();
            if (!snapshot.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Premium payment order was not found.");
            }

            PaymentOrderRecord order = PaymentOrderRecord.fromSnapshot(snapshot);
            if (!Objects.equals(uid, order.userId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this premium payment order.");
            }
            return order;
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to load premium payment order={} for uid={}", orderId, uid, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to load premium payment order.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private PaymentOrderRecord findOrderByPaymentCode(String paymentCode) {
        try {
            QuerySnapshot snapshot = firestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("paymentCode", paymentCode)
                .limit(1)
                .get()
                .get();
            if (snapshot.isEmpty()) {
                return null;
            }
            return PaymentOrderRecord.fromSnapshot(snapshot.getDocuments().get(0));
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to find premium order by paymentCode={}", paymentCode, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to match premium payment order.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private List<PaymentOrderRecord> listOrdersForUser(String uid) {
        try {
            QuerySnapshot snapshot = firestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("userId", uid)
                .get()
                .get();
            List<PaymentOrderRecord> result = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                result.add(PaymentOrderRecord.fromSnapshot(document));
            }
            result.sort(Comparator.comparingLong(PaymentOrderRecord::createdAt).reversed());
            return result;
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to list premium payment orders for uid={}", uid, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to list premium payment orders.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private PaymentOrderRecord findActivePendingOrder(String uid, long now) {
        String planCode = premiumConfig().getPlanCode();
        return listOrdersForUser(uid).stream()
            .filter(order -> STATUS_PENDING.equals(order.status()))
            .filter(order -> Objects.equals(planCode, order.planCode()))
            .filter(order -> order.expiresAt() > now)
            .findFirst()
            .orElse(null);
    }

    private void expireStaleOrders(String uid, long now) {
        for (PaymentOrderRecord order : listOrdersForUser(uid)) {
            if (STATUS_PENDING.equals(order.status()) && order.expiresAt() <= now) {
                updateOrderFields(order.orderId(), Map.of(
                    "status", STATUS_EXPIRED,
                    "updatedAt", now
                ));
            }
        }
    }

    private PaymentOrderRecord markOrderExpired(PaymentOrderRecord order) {
        updateOrderFields(order.orderId(), Map.of(
            "status", STATUS_EXPIRED,
            "updatedAt", System.currentTimeMillis()
        ));
        return new PaymentOrderRecord(
            order.orderId(),
            order.userId(),
            order.planCode(),
            order.amountVnd(),
            order.durationDays(),
            order.paymentCode(),
            STATUS_EXPIRED,
            order.createdAt(),
            order.expiresAt(),
            order.paidAt(),
            order.premiumGrantedUntil(),
            order.sepayTransactionId(),
            order.latePayment(),
            order.lastReconcileAttemptAt(),
            order.bankCode(),
            order.bankAccount(),
            order.accountHolder()
        );
    }

    private void updateOrderFields(String orderId, Map<String, Object> fields) {
        try {
            firestore.collection(ORDERS_COLLECTION).document(orderId).update(fields).get();
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to update premium payment order={}", orderId, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to update premium payment order.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private String generateUniquePaymentCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = PaymentCodeUtils.generate();
            if (findOrderByPaymentCode(candidate) == null) {
                return candidate;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a unique premium payment code.");
    }

    private SepayWebhookPayload findMatchingSepayTransaction(PaymentOrderRecord order, SepayConfigSnapshot sepay) {
        URI uri = UriComponentsBuilder
            .fromUriString(sepay.apiUrl())
            .pathSegment("transactions", "list")
            .queryParam("account_number", sepay.bankAccount())
            .queryParam("amount_in", order.amountVnd())
            .queryParam("transaction_date_min", formatSepayDate(order.createdAt()))
            .queryParam("transaction_date_max", formatSepayDate(order.expiresAt() + Duration.ofHours(premiumConfig().getReconcileWindowHours()).toMillis()))
            .queryParam("limit", 50)
            .build()
            .encode()
            .toUri();

        try {
            SepayTransactionsApiResponse response = restClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + sepay.apiToken())
                .retrieve()
                .body(SepayTransactionsApiResponse.class);

            if (response == null || response.transactions() == null) {
                return null;
            }

            for (SepayTransactionItem item : response.transactions()) {
                long amountIn = parseMoney(item.amountIn());
                String content = item.transactionContent();
                if (amountIn == order.amountVnd()
                    && normalizeText(item.accountNumber()).equals(normalizeText(sepay.bankAccount()))
                    && PaymentCodeUtils.extract(content).equals(order.paymentCode())) {
                    return new SepayWebhookPayload(
                        parseLong(item.id()),
                        item.bankBrandName(),
                        item.transactionDate(),
                        item.accountNumber(),
                        item.subAccount(),
                        null,
                        content,
                        amountIn > 0 ? "in" : "out",
                        content,
                        amountIn,
                        parseNullableLong(item.accumulated()),
                        item.referenceNumber()
                    );
                }
            }

            return null;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SePay reconciliation is being rate-limited. Please try again shortly.");
            }
            log.error("SePay API returned an error while reconciling premium order={}", order.orderId(), exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to reconcile payment with SePay.");
        } catch (RestClientException exception) {
            log.error("SePay API call failed while reconciling premium order={}", order.orderId(), exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to connect to SePay reconciliation API.");
        }
    }

    private void recordPaymentEvent(long transactionId, String source, String rawPayload, String orderId, String paymentCode, String resultStatus, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", String.valueOf(transactionId));
        event.put("provider", "SEPAY");
        event.put("source", source);
        event.put("orderId", orderId);
        event.put("paymentCode", paymentCode);
        event.put("resultStatus", resultStatus);
        event.put("message", message);
        event.put("rawPayload", rawPayload);
        event.put("updatedAt", System.currentTimeMillis());

        try {
            firestore.collection(EVENTS_COLLECTION)
                .document(String.valueOf(transactionId))
                .set(event, SetOptions.merge())
                .get();
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to record SePay payment event txId={} source={}", transactionId, source, exception);
        }
    }

    private Long loadCurrentPremiumUntil(String uid) {
        try {
            DocumentSnapshot snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().get();
            return loadSnapshotPremiumUntil(snapshot);
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to load premium profile for uid={}", uid, exception);
            throw FirestoreApiExceptionMapper.map(
                exception,
                "Failed to load premium profile.",
                "Firestore quota exceeded. Please try again later."
            );
        }
    }

    private Long loadSnapshotPremiumUntil(DocumentSnapshot snapshot) {
        return asLong(snapshot.get("premiumUntil"));
    }

    private CreatePremiumOrderResponse toCreateOrderResponse(PaymentOrderRecord order) {
        return new CreatePremiumOrderResponse(
            order.orderId(),
            order.status(),
            order.planCode(),
            order.amountVnd(),
            order.durationDays(),
            order.paymentCode(),
            order.expiresAt(),
            buildQrImageUrl(order),
            order.bankCode(),
            order.bankAccount(),
            order.accountHolder()
        );
    }

    private PaymentOrderStatusResponse toStatusResponse(PaymentOrderRecord order) {
        return new PaymentOrderStatusResponse(
            order.orderId(),
            order.status(),
            order.planCode(),
            order.amountVnd(),
            order.durationDays(),
            order.paymentCode(),
            order.createdAt(),
            order.expiresAt(),
            order.paidAt(),
            order.premiumGrantedUntil(),
            order.latePayment()
        );
    }

    private String buildQrImageUrl(PaymentOrderRecord order) {
        URI uri = UriComponentsBuilder
            .fromUriString(checkoutSepayConfig().getQrBaseUrl())
            .queryParam("acc", order.bankAccount())
            .queryParam("bank", order.bankCode())
            .queryParam("amount", order.amountVnd())
            .queryParam("des", order.paymentCode())
            .queryParam("template", "compact")
            .build()
            .encode()
            .toUri();
        return uri.toString();
    }

    private SepayWebhookPayload parseWebhookPayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, SepayWebhookPayload.class);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid SePay webhook payload.");
        }
    }

    private String serializeSafely(SepayWebhookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "{\"id\":" + payload.id() + "}";
        }
    }

    private String formatSepayDate(long millis) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), SEPAY_ZONE).format(SEPAY_DATETIME_FORMAT);
    }

    private long parseSepayDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return System.currentTimeMillis();
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawDate.trim(), SEPAY_DATETIME_FORMAT);
            return dateTime.atZone(SEPAY_ZONE).toInstant().toEpochMilli();
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SePay returned an invalid transaction timestamp.");
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SePay returned an invalid transaction identifier.");
        }
    }

    private long parseMoney(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Math.round(parsed);
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SePay returned an invalid transaction amount.");
        }
    }

    private Long parseNullableLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseMoney(value);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private BackendProperties.Sepay checkoutSepayConfig() {
        BackendProperties.Sepay sepay = backendProperties.getPayment().getSepay();
        if (!StringUtils.hasText(sepay.getBankCode())
            || !StringUtils.hasText(sepay.getBankAccount())
            || !StringUtils.hasText(sepay.getAccountHolder())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SePay checkout is not configured on the backend yet.");
        }
        return sepay;
    }

    private BackendProperties.Sepay webhookSepayConfig() {
        BackendProperties.Sepay sepay = checkoutSepayConfig();
        if (!StringUtils.hasText(sepay.getWebhookSecret())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SePay webhook secret is not configured on the backend yet.");
        }
        return sepay;
    }

    private SepayConfigSnapshot reconcileSepayConfig() {
        BackendProperties.Sepay sepay = checkoutSepayConfig();
        if (!StringUtils.hasText(sepay.getApiToken())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SePay API token is not configured on the backend yet.");
        }
        return new SepayConfigSnapshot(
            sepay.getApiToken().trim(),
            sepay.getBankAccount().trim(),
            sepay.getApiUrl().trim(),
            sepay.getReconcileCooldownSeconds()
        );
    }

    private BackendProperties.Premium premiumConfig() {
        return backendProperties.getPayment().getPremium();
    }

    private record SepayConfigSnapshot(
        String apiToken,
        String bankAccount,
        String apiUrl,
        long reconcileCooldownSeconds
    ) {
    }

    private record PaymentOrderRecord(
        String orderId,
        String userId,
        String planCode,
        long amountVnd,
        int durationDays,
        String paymentCode,
        String status,
        long createdAt,
        long expiresAt,
        Long paidAt,
        Long premiumGrantedUntil,
        Long sepayTransactionId,
        boolean latePayment,
        Long lastReconcileAttemptAt,
        String bankCode,
        String bankAccount,
        String accountHolder
    ) {
        static PaymentOrderRecord fromSnapshot(DocumentSnapshot snapshot) {
            return fromData(snapshot.getId(), snapshot.getData());
        }

        static PaymentOrderRecord fromData(String orderId, Map<String, Object> data) {
            Map<String, Object> safe = data == null ? Map.of() : data;
            return new PaymentOrderRecord(
                orderId,
                asString(safe.get("userId")),
                asString(safe.get("planCode")),
                asRequiredLong(safe.get("priceVnd")),
                (int) asRequiredLong(safe.get("durationDays")),
                PaymentCodeUtils.normalize(asString(safe.get("paymentCode"))),
                asString(safe.getOrDefault("status", STATUS_PENDING)),
                asRequiredLong(safe.get("createdAt")),
                asRequiredLong(safe.get("expiresAt")),
                asNullableLong(safe.get("paidAt")),
                asNullableLong(safe.get("premiumGrantedUntil")),
                asNullableLong(safe.get("sepayTransactionId")),
                asBoolean(safe.get("latePayment")),
                asNullableLong(safe.get("lastReconcileAttemptAt")),
                asString(safe.get("bankCode")),
                asString(safe.get("bankAccount")),
                asString(safe.get("accountHolder"))
            );
        }

        private static String asString(Object value) {
            return value instanceof String text ? text : "";
        }

        private static long asRequiredLong(Object value) {
            return value instanceof Number number ? number.longValue() : 0L;
        }

        private static Long asNullableLong(Object value) {
            return value instanceof Number number ? number.longValue() : null;
        }

        private static boolean asBoolean(Object value) {
            return value instanceof Boolean bool && bool;
        }
    }

    private record TransactionProcessResult(
        boolean matched,
        String eventStatus,
        String message,
        String orderId,
        Long premiumGrantedUntil,
        boolean latePayment
    ) {
        static TransactionProcessResult ignored(String message) {
            return new TransactionProcessResult(false, "IGNORED", message, null, null, false);
        }

        static TransactionProcessResult alreadyPaid(String message) {
            return new TransactionProcessResult(true, "DUPLICATE", message, null, null, false);
        }

        static TransactionProcessResult paid(String orderId, Long premiumGrantedUntil, boolean latePayment, String message) {
            return new TransactionProcessResult(true, "PAID", message, orderId, premiumGrantedUntil, latePayment);
        }
    }

    private record SepayTransactionsApiResponse(
        int status,
        List<SepayTransactionItem> transactions
    ) {
    }

    private record SepayTransactionItem(
        String id,
        @JsonProperty("transaction_date") String transactionDate,
        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("sub_account") String subAccount,
        @JsonProperty("amount_in") String amountIn,
        String accumulated,
        @JsonProperty("transaction_content") String transactionContent,
        @JsonProperty("reference_number") String referenceNumber,
        @JsonProperty("bank_brand_name") String bankBrandName
    ) {
    }
}
