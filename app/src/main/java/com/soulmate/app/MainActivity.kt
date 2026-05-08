package com.soulmate.app

import CommunityScreen
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
import com.soulmate.app.ui.journal.history.HistoryScreen
import com.soulmate.app.ui.journal.history.HistoryViewModel
import com.soulmate.app.ui.login.LoginScreen
import com.soulmate.app.ui.login.RegisterScreen
import com.soulmate.app.ui.setting.SettingScreen
import com.soulmate.app.ui.setting.ThemeViewModel
import com.soulmate.app.ui.stats.StatsScreen
import com.soulmate.app.ui.theme.SoulMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SoulMateTheme(darkTheme = themeViewModel.isDarkMode.value) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val isAuthScreen = currentDestination?.hierarchy?.any {
                    it.route == Screen.Login.route || it.route == Screen.Register.route
                } == true

                val auth = FirebaseAuth.getInstance()
                val startDest = if (auth.currentUser != null) {
                    Screen.Home.route
                } else {
                    Screen.Login.route
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!isAuthScreen) {
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
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
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
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate(Screen.Login.route)
                                }
                            )
                        }
                        composable(
                            route = Screen.Diary.route + "?diaryId={diaryId}",
                            arguments = listOf(
                                navArgument("diaryId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val diaryId = backStackEntry.arguments?.getString("diaryId")
                            val hvm: HistoryViewModel = hiltViewModel() // Lấy ViewModel riêng cho Editor

                            MultimediaEditor(
                                diaryId = diaryId,
                                historyViewModel = hvm,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Home.route) { 
                            val hvm: HistoryViewModel = hiltViewModel() // Lấy ViewModel riêng cho Home
                            HomeScreen(musicViewModel, hvm) 
                        }
                        composable(Screen.History.route) { 
                            val hvm: HistoryViewModel = hiltViewModel() // Lấy ViewModel riêng cho History
                            HistoryScreen(
                                viewModel = hvm,
                                onNavigateToEdit = { diaryId -> 
                                    navController.navigate(Screen.Diary.route + "?diaryId=$diaryId")
                                }
                            )
                        }

                        composable(Screen.Stats.route) { StatsScreen() }

                        composable(Screen.Community.route) { CommunityScreen() }

                        composable(Screen.Setting.route) {
                            val context = LocalContext.current
                            SettingScreen(
                                themeViewModel = themeViewModel,
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)

                                    googleSignInClient.signOut().addOnCompleteListener {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
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
