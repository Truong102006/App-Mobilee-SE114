package com.soulmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.data.remote.api.BackendApiService
import com.soulmate.app.data.remote.dto.CreatePremiumOrderResponseDto
import com.soulmate.app.data.remote.dto.PaymentOrderStatusResponseDto
import com.soulmate.app.data.remote.dto.PremiumOfferResponseDto
import com.soulmate.app.domain.model.PremiumOffer
import com.soulmate.app.domain.model.PremiumPaymentOrder
import com.soulmate.app.domain.repository.IPaymentRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val auth: FirebaseAuth
) : IPaymentRepository {

    override suspend fun getPremiumOffer(): Result<PremiumOffer> = runCatching {
        val idToken = requireIdToken()
        backendApiService.getPremiumOffer("Bearer $idToken").toDomain()
    }

    override suspend fun createOrResumePremiumOrder(): Result<PremiumPaymentOrder> = runCatching {
        val idToken = requireIdToken()
        backendApiService.createOrResumePremiumOrder("Bearer $idToken").toDomain()
    }

    override suspend fun getPaymentOrderStatus(orderId: String): Result<PremiumPaymentOrder> = runCatching {
        val idToken = requireIdToken()
        backendApiService.getPaymentOrderStatus("Bearer $idToken", orderId).toDomain()
    }

    override suspend fun reconcilePaymentOrder(orderId: String): Result<PremiumPaymentOrder> = runCatching {
        val idToken = requireIdToken()
        backendApiService.reconcilePaymentOrder("Bearer $idToken", orderId).order.toDomain()
    }

    private suspend fun requireIdToken(): String {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
        return currentUser.getIdToken(false).await().token
            ?: throw IllegalStateException("Cannot get Firebase ID token")
    }

    private fun PremiumOfferResponseDto.toDomain(): PremiumOffer {
        return PremiumOffer(
            planCode = planCode,
            priceVnd = priceVnd,
            durationDays = durationDays,
            orderExpireMinutes = orderExpireMinutes,
            bankCode = bankCode,
            bankAccount = bankAccount,
            accountHolder = accountHolder,
            currentPremiumUntil = currentPremiumUntil
        )
    }

    private fun CreatePremiumOrderResponseDto.toDomain(): PremiumPaymentOrder {
        return PremiumPaymentOrder(
            orderId = orderId,
            status = status,
            planCode = planCode,
            amountVnd = amountVnd,
            durationDays = durationDays,
            paymentCode = paymentCode,
            expiresAt = expiresAt,
            qrImageUrl = qrImageUrl,
            bankCode = bankCode,
            bankAccount = bankAccount,
            accountHolder = accountHolder
        )
    }

    private fun PaymentOrderStatusResponseDto.toDomain(): PremiumPaymentOrder {
        return PremiumPaymentOrder(
            orderId = orderId,
            status = status,
            planCode = planCode,
            amountVnd = amountVnd,
            durationDays = durationDays,
            paymentCode = paymentCode,
            createdAt = createdAt,
            expiresAt = expiresAt,
            paidAt = paidAt,
            premiumGrantedUntil = premiumGrantedUntil,
            latePayment = latePayment
        )
    }
}
