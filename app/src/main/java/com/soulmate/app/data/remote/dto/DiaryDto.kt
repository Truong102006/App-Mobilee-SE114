package com.soulmate.app.data.remote.dto

import com.google.firebase.Timestamp

/**
 * Data Transfer Object cho bảng Diaries trên Firestore
 */
data class DiaryDto(
    val diary_id: String = "",
    val user_id: String = "",
    val text: String = "",
    val image_urls: List<String> = emptyList(),
    val audio_url: String? = null,
    val mood_tag: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    // Hàm chuyển đổi sang Map để lưu vào Firestore dễ dàng hơn
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "diary_id" to diary_id,
            "user_id" to user_id,
            "text" to text,
            "image_urls" to image_urls,
            "audio_url" to audio_url,
            "mood_tag" to mood_tag,
            "timestamp" to timestamp
        )
    }
}
