package com.soulmate.app.data.remote.dto

data class PredictMoodRequestDto(
    val text: String
)

data class PredictMoodResponseDto(
    val mood: String = "Neutral",
    val raw: String? = null,
    val model: String? = null
)
