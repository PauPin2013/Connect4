package com.example.connect4.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect4.models.GameState
import com.example.connect4.viewmodels.OfflineConnect4ViewModel
import com.example.connect4.views.shared.BoardView // Importar el BoardView compartido
import com.example.connect4.views.shared.PlayerInfo // Importar el PlayerInfo compartido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineConnect4GameScreen(
    onNavigateBackToLobby: () -> Unit,
    offlineConnect4ViewModel: OfflineConnect4ViewModel = viewModel()
) {
    val board by offlineConnect4ViewModel.board.collectAsState()
    val currentPlayer by offlineConnect4ViewModel.currentPlayer.collectAsState()
    val gameState by offlineConnect4ViewModel.gameState.collectAsState()
    val message by offlineConnect4ViewModel.message.collectAsState()

    val displayMessage = when (gameState) {
        is GameState.Winner -> "¡El Jugador ${(gameState as GameState.Winner).player} ha ganado!"
        GameState.Draw -> "¡Es un empate!"
        GameState.Playing -> "Turno del Jugador $currentPlayer"
        else -> "Preparando partida..."
    }

    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conecta 4 (Offline)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLobby) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Lobby", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50))
            )
        },
        containerColor = Color(0xFF2C3E50) // Fondo de la pantalla
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayMessage,
                    color = textColor,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Información de jugadores para el modo offline (simplificada)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerInfo(
                        playerName = "Jugador 1",
                        isCurrentPlayer = currentPlayer == 1,
                        isMe = true, // Consideramos al jugador humano como "Tú" en offline
                        playerColor = Color.Red
                    )
                    Text("vs", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    PlayerInfo(
                        playerName = "IA",
                        isCurrentPlayer = currentPlayer == 2,
                        isMe = false,
                        playerColor = Color.Yellow
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            BoardView(
                board = board.cells,
                onColumnClick = { col ->
                    if (gameState == GameState.Playing && currentPlayer == 1) { // Solo Jugador 1 puede clickear
                        offlineConnect4ViewModel.dropPiece(col)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (gameState) {
                is GameState.Winner, GameState.Draw -> {
                    Button(
                        onClick = { offlineConnect4ViewModel.resetGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Jugar de Nuevo", fontSize = 18.sp, color = Color.White)
                    }
                }
                else -> { // GameState.Playing or WaitingToStart
                    Button(
                        onClick = { offlineConnect4ViewModel.resetGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reiniciar Partida", fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    }
}