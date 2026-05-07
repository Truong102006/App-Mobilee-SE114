package com.soulmate.app.ui.journal.editor

import androidx.compose.ui.graphics.Color
import com.soulmate.app.R
import com.soulmate.app.ui.theme.MoodColors

enum class Mood(val label: String, val iconRes: Int, val displayColor: Color) {
    Happy("Happy", R.drawable.ic_mood_happy, MoodColors[0]),
    Satisfied("Satisfied", R.drawable.ic_mood_satisfied, MoodColors[1]),
    Neutral("Neutral", R.drawable.ic_mood_neutral, MoodColors[2]),
    Sad("Sad", R.drawable.ic_mood_sad, MoodColors[3]),
    Angry("Angry", R.drawable.ic_mood_angry, MoodColors[4]),
}