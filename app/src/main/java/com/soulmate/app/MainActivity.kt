package com.soulmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import com.soulmate.app.ui.login.LoginScreen
import com.soulmate.app.ui.login.RegisterScreen
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
        // Bật tính năng tràn viền (edge-to-edge)
        enableEdgeToEdge()
        
        setContent {
            SoulMateTheme(darkTheme = themeViewModel.isDarkMode.value) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Ẩn Bottom Bar khi ở màn hình Login hoặc Register
                val isAuthScreen = currentDestination?.hierarchy?.any { 
                    it.route == Screen.Login.route || it.route == Screen.Register.route 
                } == true

                Scaffold(
                    // Không sử dụng insets mặc định của Scaffold để có thể tự kiểm soát các phần
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!isAuthScreen) {
                            BottomNavigation(
                                backgroundColor = MaterialTheme.colors.surface,
                                elevation = 8.dp,
                                // Thêm padding ở dưới cùng cho BottomNavigation để không bị che bởi thanh điều hướng nút bấm
                                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                            ) {
                                val items = listOf(
                                    Screen.Diary,
                                    Screen.Home,
                                    Screen.History,
                                    Screen.Setting
                                )

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
                    }
                ) { innerPadding ->
                    // Tính toán padding dưới: bao gồm cả BottomBar và thanh điều hướng hệ thống
                    val bottomPadding = if (isAuthScreen) {
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    } else {
                        innerPadding.calculateBottomPadding()
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route,
                        modifier = Modifier.padding(bottom = bottomPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate(Screen.Register.route)
                                }
                            )
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate(Screen.Login.route)
                                }
                            )
                        }
                        composable(Screen.Diary.route) { 
                            MultimediaEditor(
                                historyViewModel = historyViewModel,
                                onBackClick = {
                                    navController.navigate(Screen.History.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ) 
                        }
                        composable(Screen.Home.route) { HomeScreen(musicViewModel, historyViewModel) }
                        composable(Screen.History.route) { HistoryScreen(historyViewModel) }
                        composable(Screen.Setting.route) { 
                            SettingScreen(
                                themeViewModel = themeViewModel,
                                onLogout = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            ) 
                        }
                    }
                }
            }
        }
    }
}