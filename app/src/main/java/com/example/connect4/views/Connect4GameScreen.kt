package com.example.connect4.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // Esta importación es crucial
import com.example.connect4.models.GameState
import com.example.connect4.viewmodels.Connect4ViewModel

@Composable
fun Connect4GameScreen(
    // ViewModel is provided by the system, no need to pass it as a parameter
    viewModel: Connect4ViewModel = viewModel()
) {
    // Collect game state from the ViewModel
    val board by viewModel.board.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val gameState by viewModel.gameState.collectAsState()

    // Determine the message to display based on the game state
    val message = when (gameState) {
        is GameState.Winner -> "Player ${(gameState as GameState.Winner).player} wins!"
        GameState.Draw -> "It's a Draw!"
        GameState.Playing -> "Player $currentPlayer's turn"
    }

    // Determine the color for the message and player pieces
    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        GameState.Playing -> if (currentPlayer == 1) Color.Red else Color.Yellow
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = textColor,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Draw the game board
        board.cells.forEachIndexed { rowIdx, row ->
            Row {
                row.forEachIndexed { colIdx, cell ->
                    val isClickable = gameState == GameState.Playing && board.getCell(0, colIdx) == 0

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(50.dp)
                            .background(Color.DarkGray, CircleShape)
                            .clickable(enabled = isClickable) {
                                viewModel.dropPiece(colIdx) // Call ViewModel to drop the piece
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner circle representing the piece
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (cell) {
                                        1 -> Color.Red
                                        2 -> Color.Yellow
                                        else -> Color.LightGray
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }

        // Reset button visible when game is over (win or draw)
        if (gameState is GameState.Winner || gameState == GameState.Draw) {
            Button(
                onClick = { viewModel.resetGame() }, // Call ViewModel to reset the game
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Reset Game")
            }
        }
    }
}
