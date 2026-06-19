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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.soulmate.app.notifications.AppNotificationManager
import com.soulmate.app.notifications.NotificationDestination
import com.soulmate.app.ui.admin.AdminDashboardScreen
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
import com.soulmate.app.ui.login.AuthViewModel
import com.soulmate.app.ui.setting.PremiumUpgradeScreen
import com.soulmate.app.ui.setting.SettingScreen
import com.soulmate.app.ui.setting.ThemeViewModel
import com.soulmate.app.ui.social.CommunityViewModel
import com.soulmate.app.ui.stats.StatsScreen
import com.soulmate.app.ui.theme.SoulMateTheme
import com.soulmate.app.ui.chat.ChatListScreen
import com.soulmate.app.ui.chat.ChatDetailScreen
import com.soulmate.app.ui.chat.ChatViewModel
import com.soulmate.app.ui.pet.PetScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()

    @Inject
    lateinit var notificationManager: AppNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()
            SoulMateTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val pendingNotification by notificationManager.pendingNavigation.collectAsState()

                val isAuthScreen = currentDestination?.hierarchy?.any {
                    it.route == Screen.Login.route || it.route == Screen.Register.route
                } == true

                val auth = FirebaseAuth.getInstance()
                val startDest = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

                LaunchedEffect(pendingNotification) {
                    val target = pendingNotification ?: return@LaunchedEffect
                    if (auth.currentUser == null) {
                        notificationManager.consumePendingNavigation()
                        return@LaunchedEffect
                    }

                    when (target) {
                        NotificationDestination.Home -> {
                            navController.navigate(Screen.Home.route) {
                                launchSingleTop = true
                            }
                        }

                        is NotificationDestination.Chat -> {
                            val encodedName = Uri.encode(target.userName)
                            val encodedUrl = target.avatarUrl?.let(Uri::encode) ?: "none"
                            navController.navigate(
                                "${Screen.ChatDetail.route}?userId=${target.userId}&userName=$encodedName&avatarUrl=$encodedUrl"
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }

                    notificationManager.consumePendingNavigation()
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        val route = currentDestination?.route
                        val isChatDetail = route?.startsWith(Screen.ChatDetail.route) == true
                        if (!isAuthScreen && route != Screen.ChatList.route && route != Screen.Premium.route && !isChatDetail) {
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
                                communityViewModel = hiltViewModel(),
                                onChatBubbleClick = { navController.navigate(Screen.ChatList.route) },
                                onNavigateToChat = { userId, name, avatarUrl ->
                                    val encodedName = Uri.encode(name)
                                    val encodedUrl = if (avatarUrl != null) Uri.encode(avatarUrl) else "none"
                                    navController.navigate("${Screen.ChatDetail.route}?userId=$userId&userName=$encodedName&avatarUrl=$encodedUrl")
                                },
                                onNavigateToDiary = { content ->
                                    val encodedContent = Uri.encode(content)
                                    navController.navigate(Screen.Diary.route + "?content=$encodedContent")
                                },
                                onNavigateToPet = {
                                    navController.navigate(Screen.Pet.route)
                                }
                            ) 
                        }
                        composable(Screen.Pet.route) {
                            PetScreen(
                                viewModel = hiltViewModel(),
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(
                            route = Screen.Diary.route + "?diaryId={diaryId}&content={content}",
                            arguments = listOf(
                                navArgument("diaryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("content") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStackEntry ->
                            val diaryId = backStackEntry.arguments?.getString("diaryId")
                            val content = backStackEntry.arguments?.getString("content")
                            MultimediaEditor(
                                diaryId = diaryId,
                                prefilledContent = content,
                                historyViewModel = hiltViewModel(),
                                onBackClick = { navController.popBackStack() }
                            )
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
                                communityViewModel = hiltViewModel(),
                                onBackClick = { navController.popBackStack() },
                                onEditClick = { id -> navController.navigate(Screen.Diary.route + "?diaryId=$id") },
                                onShareSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                            )
                        }
                        composable(Screen.ChatList.route) {
                            ChatListScreen(
                                communityViewModel = hiltViewModel(),
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
                            SettingScreen(
                                themeViewModel = themeViewModel,
                                navController = navController,
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut()
                                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }
                        composable(Screen.Premium.route) {
                            val parentEntry = remember(navController) {
                                navController.getBackStackEntry(Screen.Setting.route)
                            }
                            val authViewModel: AuthViewModel = hiltViewModel(parentEntry)
                            PremiumUpgradeScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.AdminDashboard.route) {
                            AdminDashboardScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
