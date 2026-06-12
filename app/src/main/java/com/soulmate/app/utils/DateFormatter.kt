package com.soulmate.app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    fun timeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Vừa xong"
            diff < 3_600_000 -> "${diff / 60_000} phút trước"
            diff < 86_400_000 -> "${diff / 3_600_000} giờ trước"
            diff < 604_800_000 -> "${diff / 86_400_000} ngày trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
