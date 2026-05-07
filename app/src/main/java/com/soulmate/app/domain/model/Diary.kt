package com.soulmate.app.domain.model

import com.google.firebase.firestore.PropertyName

data class Diary(
    @get:PropertyName("diary_id") @set:PropertyName("diary_id")
    var diaryId: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    var title: String = "",

    @get:PropertyName("text") @set:PropertyName("text")
    var content: String = "",

    @get:PropertyName("image_urls") @set:PropertyName("image_urls")
    var imageUrls: List<String> = emptyList(),

    @get:PropertyName("audio_url") @set:PropertyName("audio_url")
    var audioUrl: String? = null,

    @get:PropertyName("mood_tag") @set:PropertyName("mood_tag")
    var moodTag: String? = null,

    // createdAt và updatedAt sẽ được xử lý thủ công từ Timestamp trong Repository
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
