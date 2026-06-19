package com.soulmate.app.domain.repository

import com.soulmate.app.domain.model.PremiumOffer
import com.soulmate.app.domain.model.PremiumPaymentOrder

interface IPaymentRepository {
    suspend fun getPremiumOffer(): Result<PremiumOffer>
    suspend fun createOrResumePremiumOrder(): Result<PremiumPaymentOrder>
    suspend fun getPaymentOrderStatus(orderId: String): Result<PremiumPaymentOrder>
    suspend fun reconcilePaymentOrder(orderId: String): Result<PremiumPaymentOrder>
}
