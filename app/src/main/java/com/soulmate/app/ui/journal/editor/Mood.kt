package com.soulmate.app.ui.journal.editor

import com.soulmate.app.R

enum class Mood(val label: String, val iconRes: Int) {
    Angry("Angry", R.drawable.ic_mood_angry),
    Sad("Sad", R.drawable.ic_mood_sad),
    Neutral("Neutral", R.drawable.ic_mood_neutral),
    Satisfied("Satisfied", R.drawable.ic_mood_satisfied),
    Happy("Happy", R.drawable.ic_mood_happy)
}