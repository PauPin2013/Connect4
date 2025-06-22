package com.example.connect4.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.Connect4Board
import com.example.connect4.models.GameState
import com.example.connect4.models.OnlineGame
import com.example.connect4.models.VocabularyWord // Mantener el import si no lo borras completamente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import android.util.Log // Importar para logs

class Connect4ViewModel : ViewModel() {
    private val boardRows = 6
    private val boardColumns = 7
    private val initialBoardCells = List(boardRows) { List(boardColumns) { 0 } }
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var gameListener: ListenerRegistration? = null
    private val _board = MutableStateFlow(Connect4Board(initialBoardCells))
    val board: StateFlow<Connect4Board> = _board.asStateFlow()
    private val _currentPlayer = MutableStateFlow(1)
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()
    private val _gameState = MutableStateFlow<GameState>(GameState.WaitingToStart)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    private val _onlineGame = MutableStateFlow<OnlineGame?>(null)
    val onlineGame: StateFlow<OnlineGame?> = _onlineGame.asStateFlow()
    private val _isMyTurn = MutableStateFlow(false)
    val isMyTurn: StateFlow<Boolean> = _isMyTurn.asStateFlow()
    // Eliminamos _questionAnsweredCorrectly ya que no habrá preguntas
    // private val _questionAnsweredCorrectly = MutableStateFlow<Boolean?>(null)
    // val questionAnsweredCorrectly: StateFlow<Boolean?> = _questionAnsweredCorrectly.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val gson = Gson()
    // Eliminamos las palabras de vocabulario ya que no se usarán
    /*
    private val vocabularyWords = listOf(
        VocabularyWord(word = "Hello", translation = "Hola"),
        VocabularyWord(word = "Goodbye", translation = "Adiós"),
        VocabularyWord(word = "Thank you", translation = "Gracias"),
        VocabularyWord(word = "Please", translation = "Por favor"),
        VocabularyWord(word = "Yes", translation = "Sí"),
        VocabularyWord(word = "No"),
        VocabularyWord(word = "Cat", translation = "Gato"),
        VocabularyWord(word = "Dog", translation = "Perro"),
        VocabularyWord(word = "House", translation = "Casa"),
        VocabularyWord(word = "Car", translation = "Coche")
    )
    */

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserId.value = firebaseAuth.currentUser?.uid
            if (firebaseAuth.currentUser == null) {
                resetGame()
                stopListeningForGameUpdates()
                _onlineGame.value = null
            }
        }
    }

    fun resetGame() {
        _board.value = Connect4Board(initialBoardCells)
        _currentPlayer.value = 1
        _gameState.value = GameState.Playing // Aseguramos que el estado sea Playing para offline
        // _questionAnsweredCorrectly.value = null // Ya no es necesario
        _message.value = null
        _onlineGame.value = null // Fundamental: asegurar que onlineGame sea null para modo offline
        _isMyTurn.value = false
        stopListeningForGameUpdates()
        Log.d("Connect4ViewModel", "resetGame() llamado. Estado: Playing, onlineGame: null")
    }

    fun resetOnlineGame() {
        val game = _onlineGame.value ?: return
        val currentUid = _currentUserId.value ?: return

        if (game.player1Id == currentUid) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val updatedGame = game.copy(
                        boardCellsJson = gson.toJson(initialBoardCells),
                        currentPlayerId = game.player1Id,
                        status = "playing",
                        winnerId = null,
                        lastQuestion = null, // Limpiar
                        isQuestionCorrect = null // Limpiar
                    )
                    db.collection("onlineGames").document(game.gameId).set(updatedGame).await()
                    _message.value = "Partida reiniciada."
                    Log.d("Connect4ViewModel", "Partida online ${game.gameId} reiniciada por ${currentUid}")
                } catch (e: Exception) {
                    _message.value = "Error al reiniciar la partida: ${e.localizedMessage}"
                    Log.e("Connect4ViewModel", "Error al reiniciar online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Solo el creador de la partida puede reiniciarla."
        }
    }

    // dropPiece ahora solo contendrá la lógica principal de soltar la ficha.
    // La lógica de la pregunta se ha movido o eliminado.
    fun dropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value

            if (game != null && currentUid != null) {
                // Lógica para partida ONLINE
                if (game.currentPlayerId != currentUid || game.status != "playing" /*|| _gameState.value != GameState.Playing*/) {
                    _message.value = "No es tu turno o la partida no está activa."
                    Log.d("Connect4ViewModel", "DropPiece (Online): No es turno o no activa. GameState: ${_gameState.value}")
                    return@launch
                }

                // Eliminamos la lógica de preguntar antes de soltar la pieza.
                // Simplemente procedemos directamente con el movimiento.
                proceedWithDropPiece(column)

            } else { // Lógica para partida OFFLINE
                if (_gameState.value != GameState.Playing) {
                    _message.value = "El juego no está en estado de juego. Reinicia."
                    Log.d("Connect4ViewModel", "DropPiece (Offline): GameState no es Playing. Actual: ${_gameState.value}")
                    return@launch
                }

                if (_board.value.getCell(0, column) != 0) {
                    _message.value = "Columna llena. Elige otra."
                    return@launch
                }

                // Eliminamos la lógica de la pregunta para el modo offline
                val currentBoard = _board.value
                val newBoard = currentBoard.dropPiece(column, _currentPlayer.value)
                _board.value = newBoard // Actualización del tablero

                val winner = checkWinner(newBoard)
                if (winner != 0) {
                    _gameState.value = GameState.Winner(winner)
                    _message.value = "¡El Jugador $winner ha ganado!"
                    Log.d("Connect4ViewModel", "Ganador en modo offline: Jugador $winner")
                } else if (isBoardFull(newBoard)) {
                    _gameState.value = GameState.Draw
                    _message.value = "¡Es un empate!"
                    Log.d("Connect4ViewModel", "Empate en modo offline.")
                } else {
                    _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
                    _message.value = "Turno del Jugador ${_currentPlayer.value}"
                    Log.d("Connect4ViewModel", "Turno cambiado a Jugador ${_currentPlayer.value} (Offline)")
                }
            }
        }
    }

    // checkQuestionAnswer ya no es necesario si no hay preguntas. Podrías eliminarla.
    // La dejaré vacía o con un log por si otras partes del código la llaman por error.
    fun checkQuestionAnswer(userAnswer: String) {
        Log.w("Connect4ViewModel", "checkQuestionAnswer() llamado pero las preguntas están deshabilitadas.")
        // Si aún así se llama, simplemente vuelve al estado de juego
        _gameState.value = GameState.Playing
        _message.value = "Preguntas deshabilitadas. Puedes hacer tu movimiento."
    }

    fun proceedWithDropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value

            // En el modo sin preguntas, esta comprobación solo necesita el estado Playing y el turno
            if (game == null) {
                // Esta función no debería ser llamada para juegos offline en este modo.
                // La lógica offline se maneja directamente en dropPiece.
                Log.e("Connect4ViewModel", "proceedWithDropPiece llamado con onlineGame null. Esto es un error en la lógica de llamadas.")
                return@launch
            }

            if (game.currentPlayerId != currentUid) {
                _message.value = "No es tu turno."
                Log.d("Connect4ViewModel", "ProceedDropPiece: No es tu turno. CurrentPlayerId: ${game.currentPlayerId}, CurrentUid: $currentUid")
                return@launch
            }

            val currentBoard = Connect4Board(game.boardCells)
            if (currentBoard.getCell(0, column) != 0) {
                _message.value = "Columna llena. Elige otra."
                return@launch
            }

            val playerNum = if (game.player1Id == currentUid) 1 else 2
            val newBoard = currentBoard.dropPiece(column, playerNum)
            val winner = checkWinner(newBoard)
            val isFull = isBoardFull(newBoard)

            val nextPlayerId = if (game.player1Id == currentUid) game.player2Id else game.player1Id
            val newStatus = when {
                winner != 0 -> "finished"
                isFull -> "draw"
                else -> "playing"
            }
            val newWinnerId = if (winner != 0) currentUid else null

            val updatedGame = game.copy(
                boardCellsJson = gson.toJson(newBoard.cells),
                currentPlayerId = if (newStatus == "playing") nextPlayerId else null,
                status = newStatus,
                winnerId = newWinnerId,
                lastMoveColumn = column,
                lastQuestion = null, // Limpiar la pregunta
                isQuestionCorrect = null // Limpiar el estado de la respuesta
            )

            db.collection("onlineGames").document(game.gameId).set(updatedGame).await()
            Log.d("Connect4ViewModel", "Pieza soltada en online game ${game.gameId} por $currentUid en columna $column. Nuevo estado: $newStatus")
            // No restablecer _questionAnsweredCorrectly aquí, ya que no estamos usando preguntas.
            // _questionAnsweredCorrectly.value = null // Originalmente se restablecía aquí
        }
    }

    private fun isBoardFull(board: Connect4Board): Boolean {
        return board.cells.all { row -> row.all { it != 0 } }
    }

    private fun checkWinner(board: Connect4Board): Int {
        val rows = board.rows
        val columns = board.columns

        // ... (Tu lógica existente para checkWinner, no necesita cambios)
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

    fun createOnlineGame() {
        val currentUid = _currentUserId.value
        if (currentUid == null) {
            _message.value = "Debes iniciar sesión para crear una partida."
            return
        }

        _isLoading.value = true
        _message.value = "Creando partida online..."
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUid).get().await()
                val username = userDoc.toObject(com.example.connect4.models.User::class.java)?.username ?: "Player 1"

                val newGame = OnlineGame(
                    player1Id = currentUid,
                    player1Name = username,
                    currentPlayerId = currentUid,
                    status = "waiting",
                    boardCellsJson = gson.toJson(initialBoardCells)
                )
                val docRef = db.collection("onlineGames").add(newGame).await()
                val gameId = docRef.id

                db.collection("onlineGames").document(gameId).update("gameId", gameId).await()

                _onlineGame.value = newGame.copy(gameId = gameId)
                startListeningForGameUpdates(gameId)
                _message.value = "Partida creada con ID: $gameId. Esperando oponente..."
                _gameState.value = GameState.WaitingToStart
                Log.d("Connect4ViewModel", "Partida online ${gameId} creada por ${currentUid}")
            } catch (e: Exception) {
                _message.value = "Error al crear la partida: ${e.localizedMessage}"
                Log.e("Connect4ViewModel", "Error al crear online game: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinOnlineGame(gameId: String) {
        val currentUid = _currentUserId.value
        if (currentUid == null) {
            _message.value = "Debes iniciar sesión para unirte a una partida."
            return
        }

        _isLoading.value = true
        _message.value = "Uniéndose a la partida $gameId..."
        viewModelScope.launch {
            try {
                val gameDoc = db.collection("onlineGames").document(gameId).get().await()
                val game = gameDoc.toObject(OnlineGame::class.java)

                if (game == null) {
                    _message.value = "Partida no encontrada."
                    return@launch
                }

                if (game.player1Id == currentUid) {
                    _message.value = "Ya eres el jugador 1 en esta partida."
                    _onlineGame.value = game
                    startListeningForGameUpdates(gameId)
                    _gameState.value = GameState.WaitingToStart
                    return@launch
                }

                if (game.player2Id != null) {
                    _message.value = "La partida ya está llena."
                    return@launch
                }

                val userDoc = db.collection("users").document(currentUid).get().await()
                val username = userDoc.toObject(com.example.connect4.models.User::class.java)?.username ?: "Player 2"

                val updatedGame = game.copy(
                    player2Id = currentUid,
                    player2Name = username,
                    status = "playing"
                )
                db.collection("onlineGames").document(gameId).set(updatedGame).await()

                _onlineGame.value = updatedGame
                startListeningForGameUpdates(gameId)
                _message.value = "Te has unido a la partida $gameId. ¡Que empiece el juego!"
                _gameState.value = GameState.Playing
                Log.d("Connect4ViewModel", "Usuario ${currentUid} se unió a partida online ${gameId}. Estado: Playing")
            } catch (e: Exception) {
                _message.value = "Error al unirse a la partida: ${e.localizedMessage}"
                Log.e("Connect4ViewModel", "Error al unirse a online game: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startListeningForGameUpdates(gameId: String) {
        stopListeningForGameUpdates()
        gameListener = db.collection("onlineGames").document(gameId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _message.value = "Error al escuchar la partida: ${e.localizedMessage}"
                    Log.e("Connect4ViewModel", "Error en listener de partida: ${e.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val game = snapshot.toObject(OnlineGame::class.java)
                    game?.let {
                        _onlineGame.value = it
                        _board.value = Connect4Board(it.boardCells)
                        _currentPlayer.value = if (it.currentPlayerId == it.player1Id) 1 else 2

                        val currentUid = _currentUserId.value
                        _isMyTurn.value = it.currentPlayerId == currentUid && it.status == "playing"

                        // Lógica de estado más robusta para online
                        _gameState.value = when (it.status) {
                            "waiting" -> GameState.WaitingToStart
                            "playing" -> {
                                // Ya no hay preguntas, así que siempre es Playing si el estado del juego lo es.
                                GameState.Playing
                            }
                            "finished" -> GameState.Winner(if (it.winnerId == it.player1Id) 1 else 2)
                            "draw" -> GameState.Draw
                            else -> GameState.Playing // Fallback
                        }

                        // Mensajes de UI para online
                        when (it.status) {
                            "waiting" -> {
                                if (it.player1Id == currentUid) _message.value = "Esperando oponente para la partida ${it.gameId}..."
                                else _message.value = "Esperando que el jugador 1 inicie la partida..."
                            }
                            "playing" -> {
                                if (it.currentPlayerId == currentUid) {
                                    _message.value = "¡Es tu turno!"
                                } else {
                                    val opponentName = if (currentUid == it.player1Id) it.player2Name else it.player1Name
                                    _message.value = "Turno de ${opponentName ?: "oponente"}..."
                                }
                            }
                            "finished" -> {
                                if (it.winnerId == currentUid) _message.value = "¡Has ganado la partida!"
                                else _message.value = "Has perdido. Ganador: ${it.winnerId}"
                            }
                            "draw" -> _message.value = "La partida ha terminado en empate."
                        }
                        // _questionAnsweredCorrectly.value = it.isQuestionCorrect // Ya no es necesario
                    }
                } else {
                    _message.value = "La partida ya no existe."
                    _onlineGame.value = null
                    _gameState.value = GameState.Playing // Vuelve a un estado jugable si la partida online desaparece
                    Log.d("Connect4ViewModel", "Partida online no existe o fue eliminada. Volviendo a modo offline Playing.")
                }
            }
    }

    fun stopListeningForGameUpdates() {
        gameListener?.remove()
        gameListener = null
        Log.d("Connect4ViewModel", "Dejando de escuchar actualizaciones de partida.")
    }

    fun deleteOnlineGame() {
        val game = _onlineGame.value ?: return
        val currentUid = _currentUserId.value ?: return

        if (game.player1Id == currentUid) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    db.collection("onlineGames").document(game.gameId).delete().await()
                    _message.value = "Partida eliminada."
                    resetGame() // Llama a resetGame para limpiar el estado y volver a Playing (offline)
                    Log.d("Connect4ViewModel", "Partida ${game.gameId} eliminada por ${currentUid}.")
                } catch (e: Exception) {
                    _message.value = "Error al eliminar la partida: ${e.localizedMessage}"
                    Log.e("Connect4ViewModel", "Error al eliminar online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Solo el creador de la partida puede eliminarla."
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForGameUpdates()
        Log.d("Connect4ViewModel", "ViewModel onCleared. Listener detenido.")
    }
}