package com.soulmate.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormatterTest {

    @Test
    fun testTimeAgoJustNow() {
        val now = System.currentTimeMillis()
        // 10 seconds ago -> "Vừa xong"
        val result = DateFormatter.timeAgo(now - 10_000)
        assertEquals("Vừa xong", result)
    }

    @Test
    fun testTimeAgoMinutes() {
        val now = System.currentTimeMillis()
        // 5 minutes ago -> "5 phút trước"
        val result = DateFormatter.timeAgo(now - 5 * 60_000)
        assertEquals("5 phút trước", result)
    }

    @Test
    fun testTimeAgoHours() {
        val now = System.currentTimeMillis()
        // 3 hours ago -> "3 giờ trước"
        val result = DateFormatter.timeAgo(now - 3 * 3600_000)
        assertEquals("3 giờ trước", result)
    }

    @Test
    fun testTimeAgoDays() {
        val now = System.currentTimeMillis()
        // 4 days ago -> "4 ngày trước"
        val result = DateFormatter.timeAgo(now - 4 * 86_400_000)
        assertEquals("4 ngày trước", result)
    }

    @Test
    fun testTimeAgoFormatDatePattern() {
        val now = System.currentTimeMillis()
        // 10 days ago -> Should format with simpleDateFormat "dd/MM/yyyy"
        val result = DateFormatter.timeAgo(now - 10 * 86_400_000)
        assertTrue(result.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun testFormatDateTime() {
        val now = System.currentTimeMillis()
        val result = DateFormatter.formatDateTime(now)
        // Match pattern "HH:mm - dd/MM/yyyy"
        assertTrue(result.matches(Regex("\\d{2}:\\d{2} - \\d{2}/\\d{2}/\\d{4}")))
    }
}
