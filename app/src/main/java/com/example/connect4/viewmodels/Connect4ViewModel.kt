package com.example.connect4.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.Connect4Board
import com.example.connect4.models.GameState
import com.example.connect4.models.OnlineGame
import com.example.connect4.models.VocabularyWord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

class Connect4ViewModel : ViewModel() {

    private val boardRows = 6
    private val boardColumns = 7

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var gameListener: ListenerRegistration? = null

    private val _board = MutableStateFlow(Connect4Board(List(boardRows) { List(boardColumns) { 0 } }))
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

    private val _questionAnsweredCorrectly = MutableStateFlow<Boolean?>(null)
    val questionAnsweredCorrectly: StateFlow<Boolean?> = _questionAnsweredCorrectly.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val vocabularyWords = listOf(
        VocabularyWord(word = "Hello", translation = "Hola"),
        VocabularyWord(word = "Goodbye", translation = "Adiós"),
        VocabularyWord(word = "Thank you", translation = "Gracias"),
        VocabularyWord(word = "Please", translation = "Por favor"),
        VocabularyWord(word = "Yes", translation = "Sí"),
        VocabularyWord(word = "No", translation = "No"),
        VocabularyWord(word = "Cat", translation = "Gato"),
        VocabularyWord(word = "Dog", translation = "Perro"),
        VocabularyWord(word = "House", translation = "Casa"),
        VocabularyWord(word = "Car", translation = "Coche")
    )

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
        _board.value = Connect4Board(List(boardRows) { List(boardColumns) { 0 } })
        _currentPlayer.value = 1
        _gameState.value = GameState.Playing
        _questionAnsweredCorrectly.value = null
        _message.value = null
        _onlineGame.value = null
        _isMyTurn.value = false
        stopListeningForGameUpdates()
    }

    fun resetOnlineGame() {
        val game = _onlineGame.value ?: return
        val currentUid = _currentUserId.value ?: return

        if (game.player1Id == currentUid) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val updatedGame = game.copy(
                        boardCells = List(boardRows) { List(boardColumns) { 0 } },
                        currentPlayerId = game.player1Id,
                        status = "playing",
                        winnerId = null,
                        lastQuestion = null,
                        isQuestionCorrect = null
                    )
                    db.collection("onlineGames").document(game.gameId).set(updatedGame).await()
                    _message.value = "Partida reiniciada."
                } catch (e: Exception) {
                    _message.value = "Error al reiniciar la partida: ${e.localizedMessage}"
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

            if (game != null && currentUid != null) {
                if (game.currentPlayerId != currentUid || game.status != "playing" || _gameState.value != GameState.Playing) {
                    _message.value = "No es tu turno o la partida no está activa."
                    return@launch
                }

                if (_gameState.value is GameState.AskingQuestion) {
                    _message.value = "Primero responde la pregunta de vocabulario."
                    return@launch
                }

                if (Connect4Board(game.boardCells).getCell(0, column) != 0) {
                    _message.value = "Columna llena. Elige otra."
                    return@launch
                }

                val randomWord = vocabularyWords.random()
                val updatedGameBeforeQuestion = game.copy(
                    lastQuestion = randomWord,
                    isQuestionCorrect = null,
                    lastMoveColumn = column
                )
                db.collection("onlineGames").document(game.gameId).set(updatedGameBeforeQuestion).await()

                _gameState.value = GameState.AskingQuestion(randomWord)
                _message.value = "Responde la pregunta antes de tu turno!"

            } else {
                if (_gameState.value != GameState.Playing) {
                    return@launch
                }

                if (_board.value.getCell(0, column) != 0) {
                    return@launch
                }

                val randomWord = vocabularyWords.random()
                _gameState.value = GameState.AskingQuestion(randomWord)
                _message.value = "Jugador ${_currentPlayer.value}: Responde a '${randomWord.word}' antes de tu turno!"
            }
        }
    }

    fun checkQuestionAnswer(userAnswer: String) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentQuestion = ( _gameState.value as? GameState.AskingQuestion)?.word

            if (currentQuestion == null) {
                _message.value = "No hay pregunta activa."
                _gameState.value = GameState.Playing
                return@launch
            }

            val isCorrect = userAnswer.trim().equals(currentQuestion.translation, ignoreCase = true)
            _questionAnsweredCorrectly.value = isCorrect
            _message.value = if (isCorrect) "¡Correcto! Es tu turno." else "Incorrecto. Pierdes el turno."

            if (game != null) {
                val updatedGame = game.copy(isQuestionCorrect = isCorrect)
                db.collection("onlineGames").document(game.gameId).set(updatedGame).await()

                if (!isCorrect) {
                    val nextPlayerId = if (game.player1Id == game.currentPlayerId) game.player2Id else game.player1Id
                    val updatedGameTurn = game.copy(
                        currentPlayerId = nextPlayerId,
                        status = "playing",
                        lastQuestion = null,
                        isQuestionCorrect = null,
                        lastMoveColumn = null
                    )
                    db.collection("onlineGames").document(game.gameId).set(updatedGameTurn).await()
                    _gameState.value = GameState.Playing
                }
            } else {
                if (isCorrect) {
                    _gameState.value = GameState.Playing
                } else {
                    _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
                    _gameState.value = GameState.Playing
                }
            }
        }
    }

    fun proceedWithDropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value

            if (_gameState.value != GameState.Playing || _questionAnsweredCorrectly.value != true) {
                _message.value = "No puedes hacer un movimiento ahora. Responde correctamente la pregunta o espera tu turno."
                return@launch
            }

            if (game != null && currentUid != null) {
                if (game.currentPlayerId != currentUid) {
                    _message.value = "No es tu turno."
                    return@launch
                }

                val currentBoard = Connect4Board(game.boardCells)
                if (currentBoard.getCell(0, column) != 0) {
                    _message.value = "Columna llena. Elige otra."
                    return@launch
                }

                val newBoard = currentBoard.dropPiece(column, if (game.player1Id == currentUid) 1 else 2)
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
                    boardCells = newBoard.cells,
                    currentPlayerId = if (newStatus == "playing") nextPlayerId else null,
                    status = newStatus,
                    winnerId = newWinnerId,
                    lastMoveColumn = column,
                    lastQuestion = null,
                    isQuestionCorrect = null
                )

                db.collection("onlineGames").document(game.gameId).set(updatedGame).await()

            } else {
                val newBoard = _board.value.dropPiece(column, _currentPlayer.value)
                _board.value = newBoard

                val winner = checkWinner(newBoard)
                if (winner != 0) {
                    _gameState.value = GameState.Winner(winner)
                } else if (isBoardFull(newBoard)) {
                    _gameState.value = GameState.Draw
                } else {
                    _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1
                }
            }
            _questionAnsweredCorrectly.value = null
        }
    }


    private fun isBoardFull(board: Connect4Board): Boolean {
        return board.cells.all { row -> row.all { it != 0 } }
    }

    private fun checkWinner(board: Connect4Board): Int {
        val rows = board.rows
        val columns = board.columns

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
                    status = "waiting"
                )
                val docRef = db.collection("onlineGames").add(newGame).await()
                val gameId = docRef.id

                db.collection("onlineGames").document(gameId).update("gameId", gameId).await()

                _onlineGame.value = newGame.copy(gameId = gameId)
                startListeningForGameUpdates(gameId)
                _message.value = "Partida creada con ID: $gameId. Esperando oponente..."
                _gameState.value = GameState.WaitingToStart
            } catch (e: Exception) {
                _message.value = "Error al crear la partida: ${e.localizedMessage}"
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
            } catch (e: Exception) {
                _message.value = "Error al unirse a la partida: ${e.localizedMessage}"
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

                        _gameState.value = when (it.status) {
                            "waiting" -> GameState.WaitingToStart
                            "playing" -> {
                                if (it.lastQuestion != null && it.isQuestionCorrect == null && it.currentPlayerId == currentUid) {
                                    GameState.AskingQuestion(it.lastQuestion)
                                } else {
                                    GameState.Playing
                                }
                            }
                            "finished" -> GameState.Winner(if (it.winnerId == it.player1Id) 1 else 2)
                            "draw" -> GameState.Draw
                            else -> GameState.Playing
                        }

                        when (it.status) {
                            "waiting" -> {
                                if (it.player1Id == currentUid) _message.value = "Esperando oponente para la partida ${it.gameId}..."
                                else _message.value = "Esperando que el jugador 1 inicie la partida..."
                            }
                            "playing" -> {
                                if (it.lastQuestion != null && it.isQuestionCorrect == null && it.currentPlayerId == currentUid) {
                                    _message.value = "Responde a '${it.lastQuestion.word}' antes de tu turno!"
                                } else if (it.currentPlayerId == currentUid) {
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
                        _questionAnsweredCorrectly.value = it.isQuestionCorrect
                    }
                } else {
                    _message.value = "La partida ya no existe."
                    _onlineGame.value = null
                    _gameState.value = GameState.WaitingToStart
                }
            }
    }

    fun stopListeningForGameUpdates() {
        gameListener?.remove()
        gameListener = null
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
                    resetGame()
                } catch (e: Exception) {
                    _message.value = "Error al eliminar la partida: ${e.localizedMessage}"
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
    }
}