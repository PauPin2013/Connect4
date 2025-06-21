package com.example.connect4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connect4.viewmodels.AuthViewModel
import com.example.connect4.views.Connect4GameScreen
import com.example.connect4.views.GameLobbyScreen
import com.example.connect4.views.LoginScreen
import com.example.connect4.views.RegisterScreen
import com.google.firebase.FirebaseApp

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOBBY = "lobby"
    const val GAME = "game"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Connect4App()
                }
            }
        }
    }
}

@Composable
fun Connect4App(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val startDestination = if (currentUser != null) Routes.LOBBY else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.LOBBY) {
            GameLobbyScreen(
                onStartGame = { navController.navigate(Routes.GAME) }
            )
        }
        composable(Routes.GAME) {
            Connect4GameScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
            )
        }
    }
}