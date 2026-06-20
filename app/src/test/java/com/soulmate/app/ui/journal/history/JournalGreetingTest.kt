package com.soulmate.app.ui.journal.history

import org.junit.Assert.assertEquals
import org.junit.Test

class JournalGreetingTest {

    @Test
    fun `returns morning greeting from 5 to 10`() {
        assertEquals("Chào buổi sáng!", journalGreetingForHour(5).title)
        assertEquals("Chào buổi sáng!", journalGreetingForHour(10).title)
    }

    @Test
    fun `returns noon greeting from 11 to 17`() {
        assertEquals("Đến giờ nghỉ trưa rồi!", journalGreetingForHour(11).title)
        assertEquals("Đến giờ nghỉ trưa rồi!", journalGreetingForHour(17).title)
    }

    @Test
    fun `returns evening greeting from 18 to 22`() {
        assertEquals("Buổi tối bình yên.", journalGreetingForHour(18).title)
        assertEquals("Buổi tối bình yên.", journalGreetingForHour(22).title)
    }

    @Test
    fun `returns late night greeting outside daytime ranges`() {
        assertEquals("Muộn rồi đó!", journalGreetingForHour(23).title)
        assertEquals("Muộn rồi đó!", journalGreetingForHour(4).title)
    }

    @Test
    fun `normalizes out of range hours`() {
        assertEquals("Chào buổi sáng!", journalGreetingForHour(29).title)
        assertEquals("Muộn rồi đó!", journalGreetingForHour(-1).title)
    }
}
