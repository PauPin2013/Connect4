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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connect4.viewmodels.AuthViewModel
import com.example.connect4.viewmodels.OfflineConnect4ViewModel
import com.example.connect4.viewmodels.OnlineConnect4ViewModel
import com.example.connect4.views.GameLobbyScreen
import com.example.connect4.views.LoginScreen
import com.example.connect4.views.OfflineConnect4GameScreen
import com.example.connect4.views.OnlineConnect4GameScreen
import com.example.connect4.views.RegisterScreen
import com.google.firebase.FirebaseApp

// Defines the routes for navigation within the app.
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LOBBY = "lobby"
    const val OFFLINE_GAME = "offline_game"
    const val ONLINE_GAME = "online_game/{gameId}" // Route for online game with a gameId argument.
    const val ONLINE_GAME_BASE = "online_game" // Base route for online game, used for navigation.
}

// Main activity of the application.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Initialize Firebase.
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Connect4App() // Composable for the entire application UI.
                }
            }
        }
    }
}

// Main Composable function that sets up the navigation for the Connect4 app.
@Composable
fun Connect4App(
    authViewModel: AuthViewModel = viewModel(), // ViewModel for authentication.
    offlineConnect4ViewModel: OfflineConnect4ViewModel = viewModel(), // ViewModel for offline game logic.
    onlineConnect4ViewModel: OnlineConnect4ViewModel = viewModel() // ViewModel for online game logic.
) {
    val navController = rememberNavController() // Controller for navigation.
    val currentUser by authViewModel.currentUser.collectAsState() // Observe current user from AuthViewModel.

    // Determine the starting destination based on whether a user is logged in.
    val startDestination = remember(currentUser) {
        if (currentUser != null) Routes.LOBBY else Routes.LOGIN
    }

    // NavHost sets up the navigation graph.
    NavHost(navController = navController, startDestination = startDestination) {
        // Login Screen.
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to lobby on successful login, popping up to login to clear back stack.
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) } // Navigate to register screen.
            )
        }
        // Register Screen.
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Navigate to lobby on successful registration, popping up to register.
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) } // Navigate back to login screen.
            )
        }
        // Game Lobby Screen.
        composable(Routes.LOBBY) {
            GameLobbyScreen(
                onStartOfflineGame = { navController.navigate(Routes.OFFLINE_GAME) }, // Navigate to offline game.
                onStartOnlineGame = { gameId ->
                    // Navigate to online game with the provided gameId.
                    navController.navigate("${Routes.ONLINE_GAME_BASE}/$gameId")
                },
                onLogout = {
                    authViewModel.logout() // Log out the user.
                    // Navigate to login screen, clearing the entire back stack.
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onlineConnect4ViewModel = onlineConnect4ViewModel,
                authViewModel = authViewModel
            )
        }
        // Offline Connect4 Game Screen.
        composable(Routes.OFFLINE_GAME) {
            OfflineConnect4GameScreen(
                onNavigateBackToLobby = {
                    // Navigate back to lobby, popping up to lobby to clear current game.
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.LOBBY) { inclusive = true }
                    }
                },
                offlineConnect4ViewModel = offlineConnect4ViewModel
            )
        }
        // Online Connect4 Game Screen.
        composable(
            route = Routes.ONLINE_GAME, // Route with a placeholder for gameId.
            arguments = listOf(androidx.navigation.navArgument("gameId") { defaultValue = "" }) // Define the gameId argument.
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") // Extract gameId from arguments.
            OnlineConnect4GameScreen(
                gameId = gameId,
                onNavigateBackToLobby = {
                    onlineConnect4ViewModel.stopListeningForGameUpdates() // Stop real-time updates.
                    // Navigate back to lobby, popping up to lobby to clear current game.
                    navController.navigate(Routes.LOBBY) {
                        popUpTo(Routes.LOBBY) { inclusive = true }
                    }
                },
                onlineConnect4ViewModel = onlineConnect4ViewModel
            )
        }
    }
}