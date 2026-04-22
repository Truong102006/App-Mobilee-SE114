package com.soulmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soulmate.app.ui.Screen
import com.soulmate.app.ui.home.HomeScreen
import com.soulmate.app.ui.home.MusicViewModel
import com.soulmate.app.ui.journal.editor.MultimediaEditor
import com.soulmate.app.ui.journal.history.HistoryScreen
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.setting.SettingScreen
import com.soulmate.app.ui.setting.ThemeViewModel
import com.soulmate.app.ui.theme.SoulMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoulMateTheme(darkTheme = themeViewModel.isDarkMode.value) {
                val navController = rememberNavController()

                // Danh sách các màn hình xuất hiện trên Bottom Bar
                val items = listOf(
                    Screen.Diary,
                    Screen.Home,
                    Screen.History,
                    Screen.Setting
                )

                Scaffold(
                    bottomBar = {
                        BottomNavigation(
                            backgroundColor = MaterialTheme.colors.surface,
                            elevation = 8.dp
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination

                            items.forEach { screen ->
                                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                                BottomNavigationItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = screen.iconRes),
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = isSelected,
                                    selectedContentColor = MaterialTheme.colors.primary,
                                    unselectedContentColor = Color.Gray,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Diary.route) { MultimediaEditor() }
                        composable(Screen.Home.route) { HomeScreen(musicViewModel, historyViewModel) }
                        composable(Screen.History.route) { HistoryScreen(historyViewModel) }
                        composable(Screen.Setting.route) { SettingScreen(themeViewModel) }
                    }
                }
            }
        }
    }
}