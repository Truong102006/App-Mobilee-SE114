package com.soulmate.app.ui.journal.history

import java.util.Calendar

internal data class JournalGreetingContent(
    val title: String,
    val message: String,
    val emoji: String
)

internal fun journalGreetingForCurrentTime(
    calendar: Calendar = Calendar.getInstance()
): JournalGreetingContent {
    return journalGreetingForHour(calendar.get(Calendar.HOUR_OF_DAY))
}

internal fun journalGreetingForHour(hour: Int): JournalGreetingContent {
    val normalizedHour = ((hour % 24) + 24) % 24

    return when (normalizedHour) {
        in 5..10 -> JournalGreetingContent(
            title = "Chào buổi sáng!",
            message = "Chúc bạn một ngày mới ngập tràn năng lượng tích cực nhé.",
            emoji = "🌸"
        )

        in 11..17 -> JournalGreetingContent(
            title = "Đến giờ nghỉ trưa rồi!",
            message = "Nửa ngày trôi qua rồi, bạn có mệt không? Nghỉ ngơi một chút đi nào.",
            emoji = "☀️"
        )

        in 18..22 -> JournalGreetingContent(
            title = "Buổi tối bình yên.",
            message = "Hôm nay có chuyện gì đặc biệt muốn tâm sự với tớ không?",
            emoji = "🌙"
        )

        else -> JournalGreetingContent(
            title = "Muộn rồi đó!",
            message = "Đừng thức khuya quá nhé. Hãy ngủ một giấc thật ngon nha.",
            emoji = "😴"
        )
    }
}
