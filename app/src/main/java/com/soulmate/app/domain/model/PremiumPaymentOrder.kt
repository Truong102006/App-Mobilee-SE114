package com.soulmate.app.domain.model

data class PremiumPaymentOrder(
    val orderId: String = "",
    val status: String = "",
    val planCode: String = "",
    val amountVnd: Long = 0L,
    val durationDays: Int = 0,
    val paymentCode: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val paidAt: Long? = null,
    val premiumGrantedUntil: Long? = null,
    val latePayment: Boolean = false,
    val qrImageUrl: String? = null,
    val bankCode: String = "",
    val bankAccount: String = "",
    val accountHolder: String = ""
) {
    fun isPending(): Boolean = status.equals("PENDING", ignoreCase = true)
    fun isPaid(): Boolean = status.equals("PAID", ignoreCase = true)
    fun isExpired(): Boolean = status.equals("EXPIRED", ignoreCase = true)
}
