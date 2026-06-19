package com.soulmate.app.domain.model

data class PremiumOffer(
    val planCode: String = "",
    val priceVnd: Long = 0L,
    val durationDays: Int = 0,
    val orderExpireMinutes: Int = 15,
    val bankCode: String = "",
    val bankAccount: String = "",
    val accountHolder: String = "",
    val currentPremiumUntil: Long? = null
)
