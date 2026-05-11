package com.soulmate.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun CustomBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Home,
        Screen.History,
        null,
        Screen.Stats,
        Screen.Setting
    )

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = navBarPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        BottomNavigation(
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp,
            modifier = Modifier.height(64.dp)
        ) {
            items.forEach { screen ->
                if (screen == null) {
                    // Placeholder for the center FAB space
                    BottomNavigationItem(
                        icon = { Spacer(Modifier.size(24.dp)) },
                        label = { Text("") },
                        selected = false,
                        onClick = {},
                        enabled = false
                    )
                } else {
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    BottomNavigationItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.iconRes),
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.caption
                            ) 
                        },
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

        FloatingActionButton(
            onClick = {
                navController.navigate(Screen.Diary.route) {
                    launchSingleTop = true
                }
            },
            shape = CircleShape,
            backgroundColor = Color(0xFF5B9DFF), // A blue color matching the design
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Diary",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
