package com.example.connect4.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.Connect4Board
import com.example.connect4.models.GameState
import com.example.connect4.models.OnlineGame
import com.example.connect4.models.VocabularyWord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class OnlineConnect4ViewModel : ViewModel() {
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

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _answerAttempt = MutableStateFlow("") // Para el intento de respuesta del usuario en la UI
    val answerAttempt: StateFlow<String> = _answerAttempt.asStateFlow()

    private val gson = Gson()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserId.value = firebaseAuth.currentUser?.uid
            if (firebaseAuth.currentUser == null) {
                stopListeningForGameUpdates()
                _onlineGame.value = null
                _gameState.value = GameState.WaitingToStart // Set to a sensible state when logged out
                Log.d("OnlineConnect4ViewModel", "User logged out. State reset.")
            }
        }
    }

    // This reset is specifically for the online game state
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
                        currentQuestionWord = null, // Reset question
                        currentQuestionTranslation = null, // Reset translation
                        questionAnsweredCorrectly = null, // Reset answer status
                        questionAskedToPlayerId = null // Reset who was asked
                    )
                    db.collection("onlineGames").document(game.gameId).set(updatedGame).await()
                    _message.value = "Partida reiniciada."
                    Log.d("OnlineConnect4ViewModel", "Partida online ${game.gameId} reiniciada por ${currentUid}")
                } catch (e: Exception) {
                    _message.value = "Error al reiniciar la partida: ${e.localizedMessage}"
                    Log.e("OnlineConnect4ViewModel", "Error al reiniciar online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Solo el creador de la partida puede reiniciarla."
        }
    }

    fun dropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value

            if (game == null || currentUid == null) {
                _message.value = "No estás en una partida online activa."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): No hay partida online activa.")
                return@launch
            }

            if (game.currentPlayerId != currentUid || game.status != "playing") {
                _message.value = "No es tu turno o la partida no está activa."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): No es turno o no activa. GameState: ${_gameState.value}")
                return@launch
            }

            // AÑADIR: Comprobar si la pregunta ha sido respondida correctamente
            if (game.currentQuestionWord != null && game.questionAnsweredCorrectly != true) {
                _message.value = "Debes responder la pregunta de vocabulario para poder mover."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): Pregunta no respondida correctamente. questionAnsweredCorrectly=${game.questionAnsweredCorrectly}")
                return@launch
            }

            // Si la pregunta ha sido respondida correctamente o no hay pregunta, procede con el movimiento
            proceedWithDropPiece(column)
        }
    }

    fun proceedWithDropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value
            if (game == null || currentUid == null) {
                Log.e("OnlineConnect4ViewModel", "proceedWithDropPiece llamado con onlineGame o currentUid null. Esto es un error en la lógica de llamadas.")
                return@launch
            }
            if (game.currentPlayerId != currentUid) {
                _message.value = "No es tu turno."
                Log.d("OnlineConnect4ViewModel", "ProceedDropPiece: No es tu turno. CurrentPlayerId: ${game.currentPlayerId}, CurrentUid: $currentUid")
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

            // Update the game state in Firestore after the move
            val updatedGame = game.copy(
                boardCellsJson = gson.toJson(newBoard.cells),
                currentPlayerId = if (newStatus == "playing") nextPlayerId else null,
                status = newStatus,
                winnerId = newWinnerId,
                lastMoveColumn = column,
                currentQuestionWord = null, // Clear question after move
                currentQuestionTranslation = null, // Clear translation after move
                questionAnsweredCorrectly = null, // Reset answer status after move
                questionAskedToPlayerId = null // Reset who was asked
            )
            db.collection("onlineGames").document(game.gameId).set(updatedGame).await()
            Log.d("OnlineConnect4ViewModel", "Pieza soltada en online game ${game.gameId} por $currentUid en columna $column. Nuevo estado: $newStatus")

            // If the game is still playing, ask a new question to the next player
            if (newStatus == "playing") {
                Log.d("OnlineConnect4ViewModel", "Juego sigue jugando. Preguntando nueva palabra al siguiente jugador: ${nextPlayerId}")
                nextPlayerId?.let { fetchRandomVocabularyWord(game.gameId, it) }
            } else {
                Log.d("OnlineConnect4ViewModel", "Juego ha terminado o está en empate. No se preguntará nueva palabra.")
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
                Log.d("OnlineConnect4ViewModel", "Partida online ${gameId} creada por ${currentUid}")
            } catch (e: Exception) {
                _message.value = "Error al crear la partida: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error al crear online game: ${e.localizedMessage}")
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
                _gameState.value = GameState.Playing // Game starts now

                // AÑADIR: Al unirse el segundo jugador y la partida pasa a "playing", se le hace la primera pregunta al player1
                Log.d("OnlineConnect4ViewModel", "Player 2 joined. Asking first question to Player 1: ${game.player1Id}")
                fetchRandomVocabularyWord(gameId, game.player1Id)

            } catch (e: Exception) {
                _message.value = "Error al unirse a la partida: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error al unirse a online game: ${e.localizedMessage}")
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
                    Log.e("OnlineConnect4ViewModel", "Error en listener de partida: ${e.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val game = snapshot.toObject(OnlineGame::class.java)
                    game?.let {
                        Log.d("OnlineConnect4ViewModel", "Actualización de partida recibida: GameId=${it.gameId}, CurrentPlayerId=${it.currentPlayerId}, QuestionWord=${it.currentQuestionWord}, QuestionTranslation=${it.currentQuestionTranslation}, QuestionAnsweredCorrectly=${it.questionAnsweredCorrectly}, QuestionAskedToPlayerId=${it.questionAskedToPlayerId}, Status=${it.status}")

                        _onlineGame.value = it
                        _board.value = Connect4Board(it.boardCells)
                        _currentPlayer.value = if (it.currentPlayerId == it.player1Id) 1 else 2
                        val currentUid = _currentUserId.value

                        // Determine if it's YOUR turn and if you need to answer a question
                        val isCurrentPlayerMyTurn = it.currentPlayerId == currentUid && it.status == "playing"
                        _isMyTurn.value = isCurrentPlayerMyTurn

                        // Lógica para el estado del juego con preguntas
                        _gameState.value = when (it.status) {
                            "waiting" -> GameState.WaitingToStart
                            "playing" -> {
                                // Si hay una palabra, es tu turno y no ha sido respondida, se pregunta.
                                // O si es el turno del oponente y se le acaba de hacer una pregunta.
                                if (it.currentQuestionWord != null && it.questionAskedToPlayerId == currentUid && (it.questionAnsweredCorrectly == null || it.questionAnsweredCorrectly == false)) {
                                    Log.d("OnlineConnect4ViewModel", "GameState changed to AskingQuestion for current user.")
                                    GameState.AskingQuestion(it.currentQuestionWord)
                                } else {
                                    Log.d("OnlineConnect4ViewModel", "GameState changed to Playing.")
                                    GameState.Playing
                                }
                            }
                            "finished" -> {
                                Log.d("OnlineConnect4ViewModel", "GameState changed to Winner.")
                                GameState.Winner(if (it.winnerId == it.player1Id) 1 else 2)
                            }
                            "draw" -> {
                                Log.d("OnlineConnect4ViewModel", "GameState changed to Draw.")
                                GameState.Draw
                            }
                            else -> GameState.Playing // Fallback
                        }

                        // UI Messages based on current state
                        when (it.status) {
                            "waiting" -> {
                                if (it.player1Id == currentUid) _message.value = "Esperando oponente para la partida ${it.gameId}..."
                                else _message.value = "Esperando que el jugador 1 inicie la partida..."
                            }
                            "playing" -> {
                                if (it.currentPlayerId == currentUid) {
                                    if (it.currentQuestionWord != null && (it.questionAnsweredCorrectly == null || it.questionAnsweredCorrectly == false)) {
                                        _message.value = "¡Es tu turno! Responde la pregunta para mover."
                                    } else if (it.currentQuestionWord != null && it.questionAnsweredCorrectly == true) {
                                        _message.value = "Respuesta correcta. ¡Ahora haz tu movimiento!"
                                    }
                                    else {
                                        _message.value = "¡Es tu turno! Haz tu movimiento."
                                    }
                                } else {
                                    val opponentName = if (currentUid == it.player1Id) it.player2Name else it.player1Name
                                    if (it.currentQuestionWord != null && (it.questionAnsweredCorrectly == null || it.questionAnsweredCorrectly == false) && it.questionAskedToPlayerId == it.currentPlayerId) {
                                        _message.value = "Turno de ${opponentName ?: "oponente"}. Esperando respuesta a la pregunta..."
                                    } else {
                                        _message.value = "Turno de ${opponentName ?: "oponente"}..."
                                    }
                                }
                            }
                            "finished" -> {
                                if (it.winnerId == currentUid) _message.value = "¡Has ganado la partida!"
                                else _message.value = "Has perdido. Ganador: ${it.winnerId}"
                            }
                            "draw" -> _message.value = "La partida ha terminado en empate."
                        }
                    }
                } else {
                    _message.value = "La partida ya no existe."
                    _onlineGame.value = null
                    _gameState.value = GameState.WaitingToStart // Back to a state where a new game can be started or joined
                    Log.d("OnlineConnect4ViewModel", "Partida online no existe o fue eliminada. Volviendo a estado de espera.")
                }
            }
    }

    fun stopListeningForGameUpdates() {
        gameListener?.remove()
        gameListener = null
        Log.d("OnlineConnect4ViewModel", "Dejando de escuchar actualizaciones de partida.")
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
                    // Do not call resetGame() from the offline viewmodel here.
                    // Instead, explicitly reset online-specific state
                    _onlineGame.value = null
                    _board.value = Connect4Board(initialBoardCells)
                    _currentPlayer.value = 1
                    _gameState.value = GameState.WaitingToStart // Return to initial online state
                    Log.d("OnlineConnect4ViewModel", "Partida ${game.gameId} eliminada por ${currentUid}.")
                } catch (e: Exception) {
                    _message.value = "Error al eliminar la partida: ${e.localizedMessage}"
                    Log.e("OnlineConnect4ViewModel", "Error al eliminar online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Solo el creador de la partida puede eliminarla."
        }
    }

    // NUEVAS FUNCIONES PARA EL SISTEMA DE VOCABULARIO

    fun setAnswerAttempt(answer: String) {
        _answerAttempt.value = answer
    }

    private suspend fun fetchRandomVocabularyWord(gameId: String, playerIdAsking: String) {
        _isLoading.value = true
        Log.d("OnlineConnect4ViewModel", "Intentando obtener palabra de vocabulario para playerId: $playerIdAsking")
        viewModelScope.launch {
            try {
                val vocabularyCollection = db.collection("vocabulary")
                val querySnapshot = vocabularyCollection.get().await()
                val words = querySnapshot.documents.mapNotNull { it.toObject(VocabularyWord::class.java) }

                if (words.isNotEmpty()) {
                    val randomWord = words[Random.nextInt(words.size)]
                    val gameRef = db.collection("onlineGames").document(gameId)
                    gameRef.update(
                        "currentQuestionWord", randomWord.word,
                        "currentQuestionTranslation", randomWord.translation,
                        "questionAnsweredCorrectly", null, // Reset status for new question
                        "questionAskedToPlayerId", playerIdAsking // Store who was asked
                    ).await()
                    Log.d("OnlineConnect4ViewModel", "Palabra obtenida y actualizada en Firestore para $playerIdAsking: ${randomWord.word} -> ${randomWord.translation}")
                    // No update _message here. The message will be set by the listener
                } else {
                    _message.value = "No se encontraron palabras de vocabulario. El juego continuará sin preguntas."
                    Log.w("OnlineConnect4ViewModel", "No vocabulary words found in Firestore. Proceeding without question for $playerIdAsking.")
                    // If no words, allow the game to proceed without a question
                    val gameRef = db.collection("onlineGames").document(gameId)
                    gameRef.update(
                        "questionAnsweredCorrectly", true, // Assume correct if no question
                        "questionAskedToPlayerId", playerIdAsking
                    ).await()
                }
            } catch (e: Exception) {
                _message.value = "Error al obtener palabra de vocabulario: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error fetching vocabulary word for $playerIdAsking: ${e.localizedMessage}")
                // In case of error, assume correct to avoid blocking the game
                val gameRef = db.collection("onlineGames").document(gameId)
                gameRef.update(
                    "questionAnsweredCorrectly", true,
                    "questionAskedToPlayerId", playerIdAsking
                ).await()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitAnswer() {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value
            if (game == null || currentUid == null || game.currentQuestionWord == null || game.currentQuestionTranslation == null) {
                _message.value = "No hay una pregunta activa para responder."
                Log.d("OnlineConnect4ViewModel", "SubmitAnswer: No active question or game state invalid.")
                return@launch
            }
            if (game.currentPlayerId != currentUid) {
                _message.value = "No es tu turno para responder."
                Log.d("OnlineConnect4ViewModel", "SubmitAnswer: Not current player's turn to answer. CurrentPlayerId: ${game.currentPlayerId}, CurrentUid: $currentUid")
                return@launch
            }

            _isLoading.value = true
            try {
                val isCorrect = _answerAttempt.value.trim().lowercase() == game.currentQuestionTranslation.trim().lowercase()
                val gameRef = db.collection("onlineGames").document(game.gameId)

                if (isCorrect) {
                    gameRef.update(
                        "questionAnsweredCorrectly", true
                        // Keep currentQuestionWord and Translation until move is made for display
                        // Don't change currentPlayerId yet
                    ).await()
                    Log.d("OnlineConnect4ViewModel", "Respuesta CORRECTA de $currentUid. Ahora puede mover.")
                    _message.value = "¡Respuesta correcta! Ahora puedes hacer tu movimiento."
                    _answerAttempt.value = "" // Clear the input field
                } else {
                    val nextPlayerId = if (game.player1Id == currentUid) game.player2Id else game.player1Id
                    gameRef.update(
                        "questionAnsweredCorrectly", false,
                        "currentPlayerId", nextPlayerId, // Pass turn
                        "currentQuestionWord", null, // Clear question
                        "currentQuestionTranslation", null, // Clear translation
                        "questionAskedToPlayerId", null // Clear who was asked
                    ).await()
                    Log.d("OnlineConnect4ViewModel", "Respuesta INCORRECTA de $currentUid. Turno pasado a $nextPlayerId.")
                    _message.value = "Respuesta incorrecta. Turno pasado al oponente."
                    _answerAttempt.value = "" // Clear the input field
                    // Automatically fetch new question for the next player
                    nextPlayerId?.let { fetchRandomVocabularyWord(game.gameId, it) }
                }
            } catch (e: Exception) {
                _message.value = "Error al enviar respuesta: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error submitting answer by $currentUid: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForGameUpdates()
        Log.d("OnlineConnect4ViewModel", "ViewModel onCleared. Listener detenido.")
    }
}