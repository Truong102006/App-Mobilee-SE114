package com.soulmate.app.data.model

import com.soulmate.app.R

data class RecordedNote(
    val id: Long = System.currentTimeMillis(),
    val dateTime: String,
    val text: String,
    val userName: String = "Dmanhz",
    val avatarRes: Int = R.drawable.ava1 // Đảm bảo có ảnh này trong res/drawable
)