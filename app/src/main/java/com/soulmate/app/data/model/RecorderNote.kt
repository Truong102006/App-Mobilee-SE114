package com.soulmate.app.data.model

data class RecordedNote(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val dateTime: String
)