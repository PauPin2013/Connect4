package com.example.connect4.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.Connect4Board
import com.example.connect4.models.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

// ViewModel for handling offline Connect4 game logic.
class OfflineConnect4ViewModel : ViewModel() {
    // Board dimensions.
    private val boardRows = 6
    private val boardColumns = 7
    // Initial state of the board cells.
    private val initialBoardCells = List(boardRows) { List(boardColumns) { 0 } }

    // MutableStateFlow for the game board.
    private val _board = MutableStateFlow(Connect4Board(initialBoardCells))
    // Public StateFlow for observing the game board.
    val board: StateFlow<Connect4Board> = _board.asStateFlow()

    // MutableStateFlow for the current player.
    private val _currentPlayer = MutableStateFlow(1)
    // Public StateFlow for observing the current player.
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    // MutableStateFlow for the current game state.
    private val _gameState = MutableStateFlow<GameState>(GameState.WaitingToStart)
    // Public StateFlow for observing the game state.
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // MutableStateFlow for displaying messages to the user.
    private val _message = MutableStateFlow<String?>(null)
    // Public StateFlow for observing messages.
    val message: StateFlow<String?> = _message.asStateFlow()

    // Initialize the game when the ViewModel is created.
    init {
        resetGame()
    }

    // Resets the game to its initial state.
    fun resetGame() {
        _board.value = Connect4Board(initialBoardCells)
        _currentPlayer.value = 1
        _gameState.value = GameState.Playing
        _message.value = null
        Log.d("OfflineConnect4ViewModel", "resetGame() called. State: Playing")
    }

    /**
     * Drops a piece into the specified column.
     * Handles game state transitions, win conditions, and AI moves.
     * @param column The column where the piece will be dropped.
     */
    fun dropPiece(column: Int) {
        viewModelScope.launch {
            // Prevent moves if game is not in Playing state.
            if (_gameState.value != GameState.Playing) {
                _message.value = "Game is not in playing state. Please restart."
                Log.d("OfflineConnect4ViewModel", "DropPiece: GameState is not Playing. Current: ${_gameState.value}")
                return@launch
            }

            // Check if the column is full.
            if (_board.value.getCell(0, column) != 0) {
                _message.value = "Column is full. Choose another one."
                return@launch
            }

            // Drop the piece and update the board.
            val currentBoard = _board.value
            val newBoard = currentBoard.dropPiece(column, _currentPlayer.value)
            _board.value = newBoard

            // Check for a winner after the move.
            val winner = checkWinner(newBoard)
            if (winner != 0) {
                _gameState.value = GameState.Winner(winner)
                _message.value = "Player $winner has won!"
                Log.d("OfflineConnect4ViewModel", "Offline winner: Player $winner")
            } else if (isBoardFull(newBoard)) {
                // Check for a draw if no winner.
                _gameState.value = GameState.Draw
                _message.value = "It's a draw!"
                Log.d("OfflineConnect4ViewModel", "Offline draw.")
            } else {
                // Switch to the next player's turn.
                _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
                _message.value = "Player ${_currentPlayer.value}'s turn"
                Log.d("OfflineConnect4ViewModel", "Turn changed to Player ${_currentPlayer.value}")

                // AI logic for Player 2.
                if (_gameState.value == GameState.Playing && _currentPlayer.value == 2) {
                    delay(500) // Simulate AI thinking time. [Image of a thinking robot]
                    val validColumns = (0 until boardColumns).filter { _board.value.getCell(0, it) == 0 }

                    if (validColumns.isNotEmpty()) {
                        // Step 1: Can AI win?
                        val winColumn = validColumns.firstOrNull { col ->
                            val simulatedBoard = _board.value.dropPiece(col, 2)
                            checkWinner(simulatedBoard) == 2
                        }

                        // Step 2: Must AI block the opponent?
                        val blockColumn = validColumns.firstOrNull { col ->
                            val simulatedBoard = _board.value.dropPiece(col, 1)
                            checkWinner(simulatedBoard) == 1
                        }

                        // Select winning move, blocking move, or a random valid column.
                        val selectedColumn = winColumn ?: blockColumn ?: validColumns.random()
                        val aiBoard = _board.value.dropPiece(selectedColumn, 2)
                        _board.value = aiBoard

                        // Check for winner or draw after AI's move.
                        val winnerAI = checkWinner(aiBoard)
                        if (winnerAI != 0) {
                            _gameState.value = GameState.Winner(winnerAI)
                            _message.value = "Player $winnerAI has won!"
                        } else if (isBoardFull(aiBoard)) {
                            _gameState.value = GameState.Draw
                            _message.value = "It's a draw!"
                        } else {
                            // Switch back to Player 1's turn.
                            _currentPlayer.value = 1
                            _message.value = "Player 1's turn"
                        }
                    } else {
                        // If no valid columns, it's a draw (should be covered by isBoardFull).
                        _gameState.value = GameState.Draw
                        _message.value = "It's a draw!"
                    }
                }
            }
        }
    }

    // Checks if the board is completely full.
    private fun isBoardFull(board: Connect4Board): Boolean {
        return board.cells.all { row -> row.all { it != 0 } }
    }

    /**
     * Checks if there is a winner on the given board.
     * @param board The Connect4Board to check.
     * @return The player number (1 or 2) if a winner is found, otherwise 0.
     */
    private fun checkWinner(board: Connect4Board): Int {
        val rows = board.rows
        val columns = board.columns

        // Check horizontal wins.
        for (r in 0 until rows) {
            for (c in 0..columns - 4) {
                val value = board.getCell(r, c)
                if (value != 0 &&
                    value == board.getCell(r, c + 1) &&
                    value == board.getCell(r, c + 2) &&
                    value == board.getCell(r, c + 3)
                ) return value
            }
        }

        // Check vertical wins.
        for (c in 0 until columns) {
            for (r in 0..rows - 4) {
                val value = board.getCell(r, c)
                if (value != 0 &&
                    value == board.getCell(r + 1, c) &&
                    value == board.getCell(r + 2, c) &&
                    value == board.getCell(r + 3, c)
                ) return value
            }
        }

        // Check diagonal wins (top-left to bottom-right).
        for (r in 0..rows - 4) {
            for (c in 0..columns - 4) {
                val value = board.getCell(r, c)
                if (value != 0 &&
                    value == board.getCell(r + 1, c + 1) &&
                    value == board.getCell(r + 2, c + 2) &&
                    value == board.getCell(r + 3, c + 3)
                ) return value
            }
        }

        // Check diagonal wins (bottom-left to top-right).
        for (r in 3 until rows) {
            for (c in 0..columns - 4) {
                val value = board.getCell(r, c)
                if (value != 0 &&
                    value == board.getCell(r - 1, c + 1) &&
                    value == board.getCell(r - 2, c + 2) &&
                    value == board.getCell(r - 3, c + 3)
                ) return value
            }
        }
        return 0 // No winner found.
    }
}