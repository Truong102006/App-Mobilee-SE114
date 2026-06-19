package com.soulmate.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend")
public class BackendProperties {

    private final Security security = new Security();
    private final Gemini gemini = new Gemini();
    private final Cloudinary cloudinary = new Cloudinary();
    private final OneSignal oneSignal = new OneSignal();
    private final Payment payment = new Payment();

    public Security getSecurity() {
        return security;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public Cloudinary getCloudinary() {
        return cloudinary;
    }

    public OneSignal getOneSignal() {
        return oneSignal;
    }

    public Payment getPayment() {
        return payment;
    }

    public static class Security {
        private boolean requireAppCheck = true;
        private String firebaseProjectNumber;

        public boolean isRequireAppCheck() {
            return requireAppCheck;
        }

        public void setRequireAppCheck(boolean requireAppCheck) {
            this.requireAppCheck = requireAppCheck;
        }

        public String getFirebaseProjectNumber() {
            return firebaseProjectNumber;
        }

        public void setFirebaseProjectNumber(String firebaseProjectNumber) {
            this.firebaseProjectNumber = firebaseProjectNumber;
        }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-1.5-flash";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String uploadFolder = "soulmate_uploads";

        public String getCloudName() {
            return cloudName;
        }

        public void setCloudName(String cloudName) {
            this.cloudName = cloudName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public String getUploadFolder() {
            return uploadFolder;
        }

        public void setUploadFolder(String uploadFolder) {
            this.uploadFolder = uploadFolder;
        }
    }

    public static class OneSignal {
        private boolean enabled;
        private String appId;
        private String apiKey;
        private String apiUrl = "https://api.onesignal.com";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }
    }

    public static class Payment {
        private final Sepay sepay = new Sepay();
        private final Premium premium = new Premium();

        public Sepay getSepay() {
            return sepay;
        }

        public Premium getPremium() {
            return premium;
        }
    }

    public static class Sepay {
        private String apiToken;
        private String webhookSecret;
        private String bankCode;
        private String bankAccount;
        private String accountHolder;
        private String apiUrl = "https://my.sepay.vn/userapi";
        private String qrBaseUrl = "https://qr.sepay.vn/img";
        private long webhookMaxSkewSeconds = 300L;
        private long reconcileCooldownSeconds = 15L;

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getBankCode() {
            return bankCode;
        }

        public void setBankCode(String bankCode) {
            this.bankCode = bankCode;
        }

        public String getBankAccount() {
            return bankAccount;
        }

        public void setBankAccount(String bankAccount) {
            this.bankAccount = bankAccount;
        }

        public String getAccountHolder() {
            return accountHolder;
        }

        public void setAccountHolder(String accountHolder) {
            this.accountHolder = accountHolder;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getQrBaseUrl() {
            return qrBaseUrl;
        }

        public void setQrBaseUrl(String qrBaseUrl) {
            this.qrBaseUrl = qrBaseUrl;
        }

        public long getWebhookMaxSkewSeconds() {
            return webhookMaxSkewSeconds;
        }

        public void setWebhookMaxSkewSeconds(long webhookMaxSkewSeconds) {
            this.webhookMaxSkewSeconds = webhookMaxSkewSeconds;
        }

        public long getReconcileCooldownSeconds() {
            return reconcileCooldownSeconds;
        }

        public void setReconcileCooldownSeconds(long reconcileCooldownSeconds) {
            this.reconcileCooldownSeconds = reconcileCooldownSeconds;
        }
    }

    public static class Premium {
        private String planCode = "PREMIUM_30D_V1";
        private long priceVnd = 49_000L;
        private int durationDays = 30;
        private int orderExpireMinutes = 15;
        private int reconcileWindowHours = 24;

        public String getPlanCode() {
            return planCode;
        }

        public void setPlanCode(String planCode) {
            this.planCode = planCode;
        }

        public long getPriceVnd() {
            return priceVnd;
        }

        public void setPriceVnd(long priceVnd) {
            this.priceVnd = priceVnd;
        }

        public int getDurationDays() {
            return durationDays;
        }

        public void setDurationDays(int durationDays) {
            this.durationDays = durationDays;
        }

        public int getOrderExpireMinutes() {
            return orderExpireMinutes;
        }

        public void setOrderExpireMinutes(int orderExpireMinutes) {
            this.orderExpireMinutes = orderExpireMinutes;
        }

        public int getReconcileWindowHours() {
            return reconcileWindowHours;
        }

        public void setReconcileWindowHours(int reconcileWindowHours) {
            this.reconcileWindowHours = reconcileWindowHours;
        }
    }
}
