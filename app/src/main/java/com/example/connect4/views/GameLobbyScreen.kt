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
import com.example.connect4.viewmodels.Connect4ViewModel
import com.example.connect4.viewmodels.AuthViewModel // IMPORTAR ESTO
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    onStartGame: () -> Unit,
    onLogout: () -> Unit, // AÑADIDO: Callback para cerrar sesión
    connect4ViewModel: Connect4ViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel() // AÑADIDO: Instancia de AuthViewModel
) {
    var gameIdInput by remember { mutableStateOf("") }

    val isLoading by connect4ViewModel.isLoading.collectAsState()
    val message by connect4ViewModel.message.collectAsState()
    val onlineGame by connect4ViewModel.onlineGame.collectAsState()

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
            if (it.status == "playing") {
                onStartGame()
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
                text = "Lobby de Conecta 4 Online",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    connect4ViewModel.resetGame()
                    onStartGame()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Jugar Offline", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "O únete/crea una partida online",
                color = Color.LightGray,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = { connect4ViewModel.createOnlineGame() },
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
                    Text("Crear Partida Online", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = gameIdInput,
                onValueChange = { gameIdInput = it },
                label = { Text("ID de partida para unirte", color = Color.White) },
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
                onClick = { connect4ViewModel.joinOnlineGame(gameIdInput) },
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
                        text = "Comparte este ID con tu oponente para que se una.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp)) // Espacio para el nuevo botón de logout

            // Nuevo botón para cerrar sesión
            Button(
                onClick = {
                    authViewModel.logout() // Llama a la función de logout del AuthViewModel
                    onLogout() // Llama al callback para navegar a la pantalla de autenticación
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