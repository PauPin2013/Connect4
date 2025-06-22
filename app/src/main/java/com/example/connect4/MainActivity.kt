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
import androidx.compose.runtime.remember // Importar remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connect4.viewmodels.AuthViewModel
import com.example.connect4.viewmodels.Connect4ViewModel // Importar Connect4ViewModel
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
fun Connect4App(
    authViewModel: AuthViewModel = viewModel(),
    connect4ViewModel: Connect4ViewModel = viewModel() // Instancia Connect4ViewModel aquí
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Usar remember para que startDestination no cambie en cada recomposición innecesariamente.
    val startDestination = remember(currentUser) {
        if (currentUser != null) Routes.LOBBY else Routes.LOGIN
    }

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
                onStartGame = { navController.navigate(Routes.GAME) },
                onLogout = { // Implementación del callback de logout para GameLobbyScreen
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true } // Limpia toda la pila de navegación
                    }
                },
                connect4ViewModel = connect4ViewModel, // Pasa la instancia del ViewModel
                authViewModel = authViewModel // Pasa la instancia del AuthViewModel
            )
        }
        composable(Routes.GAME) {
            Connect4GameScreen(
                onNavigateBackToLobby = { // CAMBIO: Callback para volver al lobby
                    // Al volver del juego, queremos limpiar el estado del juego para evitar problemas
                    // Puedes decidir si resetear connect4ViewModel.resetGame() aquí o en el GameLobbyScreen
                    // al entrar de nuevo. Por ahora, asumimos que resetGame() ya se llama al iniciar partida offline
                    // o que el estado online se maneja adecuadamente.
                    connect4ViewModel.stopListeningForGameUpdates() // Detener escucha de la partida online
                    connect4ViewModel.resetGame() // Opcional: resetear el estado del juego al salir
                    navController.navigate(Routes.LOBBY) {
                        // popUpTo(Routes.LOBBY) { inclusive = true } // Esto puede recrear el lobby.
                        // Considera si quieres recrear el lobby cada vez o simplemente volver.
                        // Para este caso, solo navegamos de regreso. Si necesitas limpiar, usa popUpTo.
                        // Para una experiencia de "volver", a menudo no se limpia la pila.
                    }
                },
                connect4ViewModel = connect4ViewModel // Pasa la instancia del ViewModel
            )
        }
    }
}