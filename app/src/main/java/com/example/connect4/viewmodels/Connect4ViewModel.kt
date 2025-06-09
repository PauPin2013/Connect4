package com.example.connect4.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.Connect4Board
import com.example.connect4.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Connect4ViewModel : ViewModel() {

    // --- Game Board Configuration ---
    private val boardRows = 6
    private val boardColumns = 7

    // --- MutableStateFlows for Game State ---
    // Represents the current state of the Connect 4 board
    private val _board = MutableStateFlow(Connect4Board(List(boardRows) { List(boardColumns) { 0 } }))
    val board: StateFlow<Connect4Board> = _board.asStateFlow()

    // Represents the current player's turn (1 or 2)
    private val _currentPlayer = MutableStateFlow(1)
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    // Represents the overall game state (Playing, Winner, Draw)
    private val _gameState = MutableStateFlow<GameState>(GameState.Playing)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    /**
     * Initializes a new game, resetting all game states.
     */
    init {
        resetGame()
    }

    /**
     * Handles dropping a piece in a specified column.
     * Only allows a move if the game is in a 'Playing' state and the column is not full.
     * @param column The column index where the piece is to be dropped.
     */
    fun dropPiece(column: Int) {
        viewModelScope.launch {
            // Only allow moves if the game is currently playing
            if (_gameState.value != GameState.Playing) {
                return@launch
            }

            // Check if the column is full (top-most cell is not empty)
            if (_board.value.getCell(0, column) != 0) {
                // Column is full, no move allowed
                return@launch
            }

            // Create a new board state with the piece dropped
            _board.value = _board.value.dropPiece(column, _currentPlayer.value)

            // After dropping, check for a winner or a draw
            val currentBoard = _board.value
            val winner = checkWinner(currentBoard)

            if (winner != 0) {
                _gameState.value = GameState.Winner(winner)
            } else if (isBoardFull(currentBoard)) {
                _gameState.value = GameState.Draw
            } else {
                // If no winner and not a draw, switch to the next player
                _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
            }
        }
    }

    /**
     * Resets the game to its initial state.
     */
    fun resetGame() {
        _board.value = Connect4Board(List(boardRows) { List(boardColumns) { 0 } })
        _currentPlayer.value = 1
        _gameState.value = GameState.Playing
    }

    /**
     * Checks if the current board is full.
     * @param board The Connect4Board instance to check.
     * @return True if all cells are filled, false otherwise.
     */
    private fun isBoardFull(board: Connect4Board): Boolean {
        return board.cells.all { row -> row.all { it != 0 } }
    }

    /**
     * Checks if there's a winner on the given board.
     * Checks horizontal, vertical, and both diagonal directions.
     * @param board The Connect4Board instance to check.
     * @return The player number (1 or 2) if there's a winner, otherwise 0.
     */
    private fun checkWinner(board: Connect4Board): Int {
        val rows = board.rows
        val columns = board.columns

        // Check horizontal lines
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

        // Check vertical lines
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

        // Check diagonal (top-left to bottom-right)
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

        // Check diagonal (bottom-left to top-right)
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

        return 0 // No winner yet
    }
}
