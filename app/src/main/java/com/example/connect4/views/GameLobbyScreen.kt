package com.example.connect4.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect4.viewmodels.OnlineConnect4ViewModel
import com.example.connect4.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    onStartOfflineGame: () -> Unit, // Callback para iniciar partida offline
    onStartOnlineGame: (String) -> Unit, // Callback para iniciar partida online, con gameId
    onLogout: () -> Unit,
    onlineConnect4ViewModel: OnlineConnect4ViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var gameIdInput by remember { mutableStateOf("") }
    val isLoading by onlineConnect4ViewModel.isLoading.collectAsState()
    val message by onlineConnect4ViewModel.message.collectAsState()
    val onlineGame by onlineConnect4ViewModel.onlineGame.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Cerrar",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(onlineGame) {
        onlineGame?.let {
            if (it.status == "playing" && it.gameId.isNotBlank()) {
                onStartOnlineGame(it.gameId) // Pasa el gameId cuando la partida empieza
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF2C3E50))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lobby de Conecta 4", // Título general
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Sección de Jugar Offline
            Button(
                onClick = {
                    // No necesitas resetear connect4ViewModel.resetGame() aquí,
                    // el OfflineConnect4ViewModel se encarga de su propio estado al ser creado/reseteado
                    onStartOfflineGame()
                },
                enabled = !isLoading, // Los botones no se habilitan mientras carga una operación online
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Jugar Offline", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp)) // Espacio entre offline y online

            Text(
                text = "Partida Online", // Título para la sección online
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Botón para Crear Partida Online
            Button(
                onClick = { onlineConnect4ViewModel.createOnlineGame() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading && onlineGame == null) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Crear Partida", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo y botón para Unirse a Partida
            OutlinedTextField(
                value = gameIdInput,
                onValueChange = { gameIdInput = it },
                label = { Text("ID de partida", color = Color.White) }, // Label más corto
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9B59B6),
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = Color(0xFF9B59B6),
                    focusedLabelColor = Color(0xFF9B59B6),
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF34495E),
                    unfocusedContainerColor = Color(0xFF34495E)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = { onlineConnect4ViewModel.joinOnlineGame(gameIdInput) },
                enabled = !isLoading && gameIdInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading && onlineGame != null) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Unirse a Partida", fontSize = 18.sp, color = Color.White)
                }
            }

            onlineGame?.let { game ->
                if (game.status == "waiting") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Tu ID de partida: ${game.gameId}",
                        color = Color(0xFFF1C40F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Comparte este ID con tu oponente.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón para Cerrar Sesión
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar Sesión", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
