package com.example.connect4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*

// MainActivity sets up the app's UI and starts the game
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Connect4Game()
        }
    }
}

@Composable
fun Connect4Game() {
    val columns = 6
    val rows = 4

    // 2D grid representing the game board: 0 = empty, 1 = player 1, 2 = player 2
    var board by remember {
        mutableStateOf(List(rows) { MutableList(columns) { 0 } })
    }

    // Current player turn (1 or 2)
    var currentPlayer by remember { mutableStateOf(1) }

    // Winner of the game: 0 = no winner yet, 1 = player 1, 2 = player 2
    var winner by remember { mutableStateOf(0) }

    // Draw flag: true if the board is full and there is no winner
    val isDraw = board.all { row -> row.all { it != 0 } } && winner == 0

    // Main layout of the game
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display turn, winner or draw message
        Text(
            text = when {
                winner != 0 -> "Win of player $winner!"
                isDraw -> "It's a draw!"
                else -> "Turn: player $currentPlayer"
            },
            color = when {
                winner == 1 -> Color.Red
                winner == 2 -> Color.Yellow
                currentPlayer == 1 -> Color.Red
                else -> Color.Yellow
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Draw the board with clickable cells
        for (row in 0 until rows) {
            Row {
                for (col in 0 until columns) {
                    val cell = board[row][col]

                    // Each cell is a colored circle that can be clicked
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(50.dp)
                            .background(Color.DarkGray, CircleShape)
                            .clickable(enabled = winner == 0 && !isDraw && board[0][col] == 0) {
                                // Create a new board with the piece dropped in the selected column
                                val newBoard = board.map { it.toMutableList() }.toMutableList()
                                for (r in rows - 1 downTo 0) {
                                    if (newBoard[r][col] == 0) {
                                        newBoard[r][col] = currentPlayer
                                        break
                                    }
                                }
                                board = newBoard
                                winner = checkWinner(newBoard)
                                if (winner == 0) {
                                    currentPlayer = if (currentPlayer == 1) 2 else 1
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner circle showing the player's color
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

        // Reset button: appears when someone wins or it's a draw
        if (winner != 0 || isDraw) {
            Button(
                onClick = {
                    board = List(rows) { MutableList(columns) { 0 } }
                    currentPlayer = 1
                    winner = 0
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Reset game")
            }
        }
    }
}

// Function to check for a winner (horizontal or vertical 4-in-a-row)
fun checkWinner(board: List<List<Int>>): Int {
    val rows = board.size
    val columns = board[0].size

    // Check horizontal lines
    for (r in 0 until rows) {
        for (c in 0..columns - 4) {
            val value = board[r][c]
            if (value != 0 &&
                value == board[r][c + 1] &&
                value == board[r][c + 2] &&
                value == board[r][c + 3]
            ) return value
        }
    }

    // Check vertical lines
    for (c in 0 until columns) {
        for (r in 0..rows - 4) {
            val value = board[r][c]
            if (value != 0 &&
                value == board[r + 1][c] &&
                value == board[r + 2][c] &&
                value == board[r + 3][c]
            ) return value
        }
    }

    return 0 // No winner yet
}