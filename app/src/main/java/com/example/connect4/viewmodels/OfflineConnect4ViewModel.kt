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

class OfflineConnect4ViewModel : ViewModel() {
    private val boardRows = 6
    private val boardColumns = 7
    private val initialBoardCells = List(boardRows) { List(boardColumns) { 0 } }

    private val _board = MutableStateFlow(Connect4Board(initialBoardCells))
    val board: StateFlow<Connect4Board> = _board.asStateFlow()

    private val _currentPlayer = MutableStateFlow(1)
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.WaitingToStart)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        resetGame() // Start in playing mode for offline by default
    }

    fun resetGame() {
        _board.value = Connect4Board(initialBoardCells)
        _currentPlayer.value = 1
        _gameState.value = GameState.Playing
        _message.value = null
        Log.d("OfflineConnect4ViewModel", "resetGame() llamado. Estado: Playing")
    }

    fun dropPiece(column: Int) {
        viewModelScope.launch {
            if (_gameState.value != GameState.Playing) {
                _message.value = "El juego no está en estado de juego. Reinicia."
                Log.d("OfflineConnect4ViewModel", "DropPiece: GameState no es Playing. Actual: ${_gameState.value}")
                return@launch
            }

            if (_board.value.getCell(0, column) != 0) {
                _message.value = "Columna llena. Elige otra."
                return@launch
            }

            val currentBoard = _board.value
            val newBoard = currentBoard.dropPiece(column, _currentPlayer.value)
            _board.value = newBoard // Actualización del tablero

            val winner = checkWinner(newBoard)
            if (winner != 0) {
                _gameState.value = GameState.Winner(winner)
                _message.value = "¡El Jugador $winner ha ganado!"
                Log.d("OfflineConnect4ViewModel", "Ganador en modo offline: Jugador $winner")
            } else if (isBoardFull(newBoard)) {
                _gameState.value = GameState.Draw
                _message.value = "¡Es un empate!"
                Log.d("OfflineConnect4ViewModel", "Empate en modo offline.")
            } else {
                _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
                _message.value = "Turno del Jugador ${_currentPlayer.value}"
                Log.d("OfflineConnect4ViewModel", "Turno cambiado a Jugador ${_currentPlayer.value}")

                // Lógica de la IA (Jugador 2)
                if (_gameState.value == GameState.Playing && _currentPlayer.value == 2) {
                    delay(500) // Pausa para simular pensamiento de la IA
                    val validColumns = (0 until boardColumns).filter { _board.value.getCell(0, it) == 0 }

                    if (validColumns.isNotEmpty()) {
                        // Paso 1: ¿Puede ganar la IA?
                        val winColumn = validColumns.firstOrNull { col ->
                            val simulatedBoard = _board.value.dropPiece(col, 2)
                            checkWinner(simulatedBoard) == 2
                        }

                        // Paso 2: ¿Debe bloquear al jugador?
                        val blockColumn = validColumns.firstOrNull { col ->
                            val simulatedBoard = _board.value.dropPiece(col, 1)
                            checkWinner(simulatedBoard) == 1
                        }

                        val selectedColumn = winColumn ?: blockColumn ?: validColumns.random()
                        val aiBoard = _board.value.dropPiece(selectedColumn, 2)
                        _board.value = aiBoard

                        val winnerAI = checkWinner(aiBoard)
                        if (winnerAI != 0) {
                            _gameState.value = GameState.Winner(winnerAI)
                            _message.value = "¡El Jugador $winnerAI ha ganado!"
                        } else if (isBoardFull(aiBoard)) {
                            _gameState.value = GameState.Draw
                            _message.value = "¡Es un empate!"
                        } else {
                            _currentPlayer.value = 1
                            _message.value = "Turno del Jugador 1"
                        }
                    } else {
                        // Si no hay columnas válidas, es un empate (esto ya debería estar cubierto por isBoardFull)
                        _gameState.value = GameState.Draw
                        _message.value = "¡Es un empate!"
                    }
                }
            }
        }
    }

    private fun isBoardFull(board: Connect4Board): Boolean {
        return board.cells.all { row -> row.all { it != 0 } }
    }

    private fun checkWinner(board: Connect4Board): Int {
        val rows = board.rows
        val columns = board.columns

        // Check horizontal
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

        // Check vertical
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
        return 0
    }
}