package com.soulmate.app.ui

import com.soulmate.app.R

sealed class Screen(val route: String, val title: String, val iconRes: Int) {
    object Login : Screen("login", "Login", 0)
    object Register : Screen("register", "Register", 0)
    object Home : Screen("home", "Home", R.drawable.ic_home)
    object Diary : Screen("diary", "Diary", R.drawable.ic_diary)
    object History : Screen("history", "History", R.drawable.ic_history)
    object Stats: Screen("stats", "Stats", R.drawable.ic_stats)
    object Setting : Screen("setting", "Setting", R.drawable.ic_setting)
}