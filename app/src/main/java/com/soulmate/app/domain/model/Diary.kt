package com.soulmate.app.domain.model

data class Diary(
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val moodTag: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
