package com.soulmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.soulmate.app.ui.journal.editor.MultimediaEditor
import com.soulmate.app.ui.setting.SettingScreen
import com.soulmate.app.ui.theme.SoulMateTheme
import dagger.hilt.android.AndroidEntryPoint
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import com.soulmate.app.ui.theme.SoulMateTheme
import com.soulmate.app.ui.journal.editor.MultimediaEditor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoulMateTheme {
                val navController = rememberNavController()

                // Danh sách các màn hình xuất hiện trên Bottom Bar
                val items = listOf(
                    Screen.Diary,
                    Screen.Home,
                    Screen.Setting
                )

                Scaffold(
                    bottomBar = {
                        BottomNavigation(
                            backgroundColor = Color.White, // Bạn có thể đổi màu tùy thích
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
                                    selectedContentColor = Color(0xffef9b38), // Màu khi chọn
                                    unselectedContentColor = Color.Gray,      // Màu khi không chọn
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            // Tránh chồng chất nhiều instance của cùng một màn hình
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            // Tránh mở lại màn hình đó nếu đang ở chính nó
                                            launchSingleTop = true
                                            // Giữ lại trạng thái của màn hình khi quay lại
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
                        composable(Screen.Home.route) { HomeScreen() }
                        composable(Screen.Setting.route) { SettingScreen() }
                    }
                }
            }
            // val context = LocalContext.current

            // MultimediaEditor(
            //    onSaveClick = { draftData ->
            //        val message = "Title: ${draftData.title}\nContent: ${draftData.contentHtml}"

            //        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            //    }
            //)
        }
    }
}