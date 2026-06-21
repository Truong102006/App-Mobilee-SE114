package com.soulmate.app.ui.journal.history

import com.soulmate.app.ui.home.components.RecordingNote
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryViewModelTest {

    @Test
    fun `recording note maps to diary payload for quick save`() {
        val note = RecordingNote(
            id = 1L,
            diaryId = "diary-1",
            dateTime = "21/06/2026 13:14",
            text = "  Hom nay minh thay on hon  ",
            moodTag = "Happy",
            imageUrls = listOf("file://mood.png")
        )
        val expectedCreatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply {
            isLenient = false
        }.parse(note.dateTime)!!.time

        val diary = recordingNoteToDiary(
            note = note,
            userId = "user-123",
            fallbackTimeMillis = 1L,
            locale = Locale.US
        )

        assertEquals("diary-1", diary.diaryId)
        assertEquals("user-123", diary.userId)
        assertEquals("Hom nay minh thay on hon", diary.title)
        assertEquals("Hom nay minh thay on hon", diary.content)
        assertEquals("Happy", diary.moodTag)
        assertEquals(listOf("file://mood.png"), diary.imageUrls)
        assertEquals(expectedCreatedAt, diary.createdAt)
        assertEquals(expectedCreatedAt, diary.updatedAt)
    }

    @Test
    fun `quick save mapping falls back to current time when date cannot be parsed`() {
        val fallbackTimeMillis = 123456789L
        val note = RecordingNote(
            dateTime = "not-a-date",
            text = "Noi dung test"
        )

        val diary = recordingNoteToDiary(
            note = note,
            userId = "user-123",
            fallbackTimeMillis = fallbackTimeMillis,
            locale = Locale.US
        )

        assertEquals(fallbackTimeMillis, diary.createdAt)
        assertEquals(fallbackTimeMillis, diary.updatedAt)
        assertEquals("Noi dung test", diary.title)
        assertEquals("Noi dung test", diary.content)
    }
}
