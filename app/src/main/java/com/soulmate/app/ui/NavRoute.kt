package com.soulmate.app.ui

import com.soulmate.app.R

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Diary : Screen("diary", "Diary", R.drawable.ic_diary)
    object History : Screen("history", "History", R.drawable.ic_history)
    object Setting : Screen("setting", "Setting", R.drawable.ic_setting)
}