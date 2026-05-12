package com.soulmate.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.soulmate.app.ui.components.Screen
import com.soulmate.app.ui.components.CustomBottomNav
import com.soulmate.app.ui.home.HomeScreen
import com.soulmate.app.ui.home.MusicViewModel
import com.soulmate.app.ui.journal.editor.MultimediaEditor
import com.soulmate.app.ui.journal.history.DiaryDetailScreen
import com.soulmate.app.ui.journal.history.HistoryScreen
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.login.LoginScreen
import com.soulmate.app.ui.login.RegisterScreen
import com.soulmate.app.ui.setting.SettingScreen
import com.soulmate.app.ui.setting.ThemeViewModel
import com.soulmate.app.ui.social.CommunityViewModel
import com.soulmate.app.ui.stats.StatsScreen
import com.soulmate.app.ui.theme.SoulMateTheme
import com.soulmate.app.ui.chat.ChatListScreen
import com.soulmate.app.ui.chat.ChatDetailScreen
import com.soulmate.app.ui.chat.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val communityViewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            SoulMateTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val isAuthScreen = currentDestination?.hierarchy?.any {
                    it.route == Screen.Login.route || it.route == Screen.Register.route
                } == true

                val auth = FirebaseAuth.getInstance()
                val startDest = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        val route = currentDestination?.route
                        val isChatDetail = route?.startsWith(Screen.ChatDetail.route) == true
                        if (!isAuthScreen && route != Screen.ChatList.route && !isChatDetail) {
                            CustomBottomNav(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    val bottomPadding = if (isAuthScreen) {
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    } else {
                        innerPadding.calculateBottomPadding()
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(bottom = bottomPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                            )
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(
                                onRegisterSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                            )
                        }
                        composable(Screen.Home.route) { 
                            HomeScreen(
                                musicViewModel = musicViewModel, 
                                historyViewModel = hiltViewModel(),
                                communityViewModel = communityViewModel,
                                onChatBubbleClick = { navController.navigate(Screen.ChatList.route) },
                                onNavigateToChat = { userId, name, avatarUrl ->
                                    val encodedName = Uri.encode(name)
                                    val encodedUrl = if (avatarUrl != null) Uri.encode(avatarUrl) else "none"
                                    navController.navigate("${Screen.ChatDetail.route}?userId=$userId&userName=$encodedName&avatarUrl=$encodedUrl")
                                }
                            ) 
                        }
                        composable(
                            route = Screen.Diary.route + "?diaryId={diaryId}",
                            arguments = listOf(navArgument("diaryId") { type = NavType.StringType; nullable = true; defaultValue = null })
                        ) { backStackEntry ->
                            val diaryId = backStackEntry.arguments?.getString("diaryId")
                            MultimediaEditor(diaryId = diaryId, historyViewModel = hiltViewModel(), onBackClick = { navController.popBackStack() })
                        }
                        composable(Screen.History.route) { 
                            HistoryScreen(
                                viewModel = hiltViewModel(),
                                onNavigateToEdit = { id -> navController.navigate(Screen.Diary.route + "?diaryId=$id") },
                                onNavigateToDetail = { id -> navController.navigate(Screen.DiaryDetail.route + "/$id") }
                            )
                        }
                        composable(
                            route = Screen.DiaryDetail.route + "/{diaryId}",
                            arguments = listOf(navArgument("diaryId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val diaryId = backStackEntry.arguments?.getString("diaryId") ?: ""
                            DiaryDetailScreen(
                                diaryId = diaryId,
                                viewModel = hiltViewModel(),
                                communityViewModel = communityViewModel,
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { id -> navController.navigate(Screen.Diary.route + "?diaryId=$id") },
                                onShareSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                            )
                        }

                        // Chat Routes
                        composable(Screen.ChatList.route) {
                            ChatListScreen(
                                communityViewModel = communityViewModel,
                                onChatClick = { userId, name, avatarUrl ->
                                    val encodedName = Uri.encode(name)
                                    val encodedUrl = if (avatarUrl != null) Uri.encode(avatarUrl) else "none"
                                    navController.navigate("${Screen.ChatDetail.route}?userId=$userId&userName=$encodedName&avatarUrl=$encodedUrl")
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.ChatDetail.route + "?userId={userId}&userName={userName}&avatarUrl={avatarUrl}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType; defaultValue = "" },
                                navArgument("userName") { type = NavType.StringType; defaultValue = "" },
                                navArgument("avatarUrl") { type = NavType.StringType; defaultValue = "none" }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            val userName = backStackEntry.arguments?.getString("userName") ?: ""
                            val rawUrl = backStackEntry.arguments?.getString("avatarUrl")
                            val avatarUrl = if (rawUrl == "none" || rawUrl.isNullOrEmpty()) null else rawUrl

                            ChatDetailScreen(
                                userId = userId,
                                userName = userName,
                                userAvatarUrl = avatarUrl,
                                chatViewModel = hiltViewModel(),
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Stats.route) { StatsScreen() }
                        composable(Screen.Setting.route) {
                            val context = LocalContext.current
                            SettingScreen(themeViewModel = themeViewModel, onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut()
                                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                            })
                        }
                    }
                }
            }
        }
    }
}
