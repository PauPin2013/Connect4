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
import com.example.connect4.views.shared.BoardView // Import the shared BoardView.
import com.example.connect4.views.shared.PlayerInfo // Import the shared PlayerInfo.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineConnect4GameScreen(
    onNavigateBackToLobby: () -> Unit,
    offlineConnect4ViewModel: OfflineConnect4ViewModel = viewModel()
) {
    // Collect states from the OfflineConnect4ViewModel.
    val board by offlineConnect4ViewModel.board.collectAsState()
    val currentPlayer by offlineConnect4ViewModel.currentPlayer.collectAsState()
    val gameState by offlineConnect4ViewModel.gameState.collectAsState()
    val message by offlineConnect4ViewModel.message.collectAsState()

    // Determine the message to display based on the game state.
    val displayMessage = when (gameState) {
        is GameState.Winner -> "Player ${(gameState as GameState.Winner).player} has won!"
        GameState.Draw -> "It's a draw!"
        GameState.Playing -> "Player $currentPlayer's turn"
        else -> "Preparing game..."
    }

    // Determine the text color for the message.
    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect 4 (Offline)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLobby) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Lobby", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50)) // Dark blue top app bar.
            )
        },
        containerColor = Color(0xFF2C3E50) // Dark blue screen background.
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
                // Display game status message.
                Text(
                    text = displayMessage,
                    color = textColor,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Player information for offline mode (simplified).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerInfo(
                        playerName = "Player 1",
                        isCurrentPlayer = currentPlayer == 1,
                        isMe = true, // Human player is "You" in offline mode.
                        playerColor = Color.Red
                    )
                    Text("vs", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    PlayerInfo(
                        playerName = "AI",
                        isCurrentPlayer = currentPlayer == 2,
                        isMe = false,
                        playerColor = Color.Yellow
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Display the Connect4 game board.
            BoardView(
                board = board.cells,
                onColumnClick = { col ->
                    // Only allow Player 1 to click when the game is playing.
                    if (gameState == GameState.Playing && currentPlayer == 1) {
                        offlineConnect4ViewModel.dropPiece(col)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons for game actions based on game state.
            when (gameState) {
                is GameState.Winner, GameState.Draw -> {
                    Button(
                        onClick = { offlineConnect4ViewModel.resetGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)), // Green button for "Play Again".
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Play Again", fontSize = 18.sp, color = Color.White)
                    }
                }
                else -> { // GameState.Playing or WaitingToStart.
                    Button(
                        onClick = { offlineConnect4ViewModel.resetGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), // Red button for "Restart Game".
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Restart Game", fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    }
}