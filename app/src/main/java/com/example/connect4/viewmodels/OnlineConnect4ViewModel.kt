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

// ViewModel for managing online Connect4 game logic and interactions with Firebase.
class OnlineConnect4ViewModel : ViewModel() {
    // Board dimensions.
    private val boardRows = 6
    private val boardColumns = 7
    // Initial state of the board cells.
    private val initialBoardCells = List(boardRows) { List(boardColumns) { 0 } }

    // Firebase Authentication instance.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Firebase Firestore instance.
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    // Listener registration for real-time game updates.
    private var gameListener: ListenerRegistration? = null

    // MutableStateFlow for the game board.
    private val _board = MutableStateFlow(Connect4Board(initialBoardCells))
    // Public StateFlow for observing the game board.
    val board: StateFlow<Connect4Board> = _board.asStateFlow()

    // MutableStateFlow for the current player (1 or 2).
    private val _currentPlayer = MutableStateFlow(1)
    // Public StateFlow for observing the current player.
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    // MutableStateFlow for the current game state.
    private val _gameState = MutableStateFlow<GameState>(GameState.WaitingToStart)
    // Public StateFlow for observing the game state.
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // MutableStateFlow for the current user's ID.
    private val _currentUserId = MutableStateFlow<String?>(null)
    // Public StateFlow for observing the current user's ID.
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // MutableStateFlow for the online game object.
    private val _onlineGame = MutableStateFlow<OnlineGame?>(null)
    // Public StateFlow for observing the online game.
    val onlineGame: StateFlow<OnlineGame?> = _onlineGame.asStateFlow()

    // MutableStateFlow to indicate if it's the current user's turn.
    private val _isMyTurn = MutableStateFlow(false)
    // Public StateFlow for observing if it's the current user's turn.
    val isMyTurn: StateFlow<Boolean> = _isMyTurn.asStateFlow()

    // MutableStateFlow for displaying messages to the user.
    private val _message = MutableStateFlow<String?>(null)
    // Public StateFlow for observing messages.
    val message: StateFlow<String?> = _message.asStateFlow()

    // MutableStateFlow to indicate loading state.
    private val _isLoading = MutableStateFlow(false)
    // Public StateFlow for observing loading state.
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // MutableStateFlow for the user's answer attempt to a vocabulary question.
    private val _answerAttempt = MutableStateFlow("")
    // Public StateFlow for observing the answer attempt.
    val answerAttempt: StateFlow<String> = _answerAttempt.asStateFlow()

    // Gson instance for JSON serialization/deserialization.
    private val gson = Gson()

    // Initializes the ViewModel, setting up an authentication state listener.
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

    // Resets the online game state. Only the game creator can reset.
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
                    _message.value = "Game reset."
                    Log.d("OnlineConnect4ViewModel", "Online game ${game.gameId} reset by ${currentUid}")
                } catch (e: Exception) {
                    _message.value = "Error resetting game: ${e.localizedMessage}"
                    Log.e("OnlineConnect4ViewModel", "Error resetting online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Only the game creator can reset the game."
        }
    }

    /**
     * Attempts to drop a piece into the specified column.
     * Checks for game state, current player, and vocabulary question status.
     * @param column The column where the piece will be dropped.
     */
    fun dropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value

            if (game == null || currentUid == null) {
                _message.value = "You are not in an active online game."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): No active online game.")
                return@launch
            }

            // Check if it's the current player's turn and game is playing.
            if (game.currentPlayerId != currentUid || game.status != "playing") {
                _message.value = "It's not your turn or the game is not active."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): Not turn or not active. GameState: ${_gameState.value}")
                return@launch
            }

            // Check if a vocabulary question is active and not correctly answered.
            if (game.currentQuestionWord != null && game.questionAnsweredCorrectly != true) {
                _message.value = "You must answer the vocabulary question to make a move."
                Log.d("OnlineConnect4ViewModel", "DropPiece (Online): Question not answered correctly. questionAnsweredCorrectly=${game.questionAnsweredCorrectly}")
                return@launch
            }

            // If the question has been answered correctly or no question, proceed with the move.
            proceedWithDropPiece(column)
        }
    }

    /**
     * Proceeds with dropping a piece after all checks (turn, question) have passed.
     * Updates the board, checks for winner/draw, and updates game state in Firestore.
     * @param column The column where the piece will be dropped.
     */
    fun proceedWithDropPiece(column: Int) {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value
            if (game == null || currentUid == null) {
                Log.e("OnlineConnect4ViewModel", "proceedWithDropPiece called with onlineGame or currentUid null. This is a logic error.")
                return@launch
            }
            if (game.currentPlayerId != currentUid) {
                _message.value = "It's not your turn."
                Log.d("OnlineConnect4ViewModel", "ProceedDropPiece: Not your turn. CurrentPlayerId: ${game.currentPlayerId}, CurrentUid: $currentUid")
                return@launch
            }
            val currentBoard = Connect4Board(game.boardCells)
            if (currentBoard.getCell(0, column) != 0) {
                _message.value = "Column is full. Choose another one."
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

            // Update the game state in Firestore after the move.
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
            Log.d("OnlineConnect4ViewModel", "Piece dropped in online game ${game.gameId} by $currentUid in column $column. New status: $newStatus")

            // If the game is still playing, ask a new question to the next player.
            if (newStatus == "playing") {
                Log.d("OnlineConnect4ViewModel", "Game still playing. Asking new word to next player: ${nextPlayerId}")
                nextPlayerId?.let { fetchRandomVocabularyWord(game.gameId, it) }
            } else {
                Log.d("OnlineConnect4ViewModel", "Game has ended or is a draw. No new word will be asked.")
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

    // Creates a new online game in Firestore.
    fun createOnlineGame() {
        val currentUid = _currentUserId.value
        if (currentUid == null) {
            _message.value = "You must be logged in to create a game."
            return
        }
        _isLoading.value = true
        _message.value = "Creating online game..."
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
                _message.value = "Game created with ID: $gameId. Waiting for opponent..."
                _gameState.value = GameState.WaitingToStart
                Log.d("OnlineConnect4ViewModel", "Online game ${gameId} created by ${currentUid}")
            } catch (e: Exception) {
                _message.value = "Error creating game: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error creating online game: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Joins an existing online game.
     * @param gameId The ID of the game to join.
     */
    fun joinOnlineGame(gameId: String) {
        val currentUid = _currentUserId.value
        if (currentUid == null) {
            _message.value = "You must be logged in to join a game."
            return
        }
        _isLoading.value = true
        _message.value = "Joining game $gameId..."
        viewModelScope.launch {
            try {
                val gameDoc = db.collection("onlineGames").document(gameId).get().await()
                val game = gameDoc.toObject(OnlineGame::class.java)
                if (game == null) {
                    _message.value = "Game not found."
                    return@launch
                }
                if (game.player1Id == currentUid) {
                    _message.value = "You are already Player 1 in this game."
                    _onlineGame.value = game
                    startListeningForGameUpdates(gameId)
                    _gameState.value = GameState.WaitingToStart
                    return@launch
                }
                if (game.player2Id != null) {
                    _message.value = "The game is already full."
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
                _message.value = "You have joined game $gameId. Let the game begin!"
                _gameState.value = GameState.Playing // Game starts now

                // Ask the first question to Player 1 when the second player joins and game starts.
                Log.d("OnlineConnect4ViewModel", "Player 2 joined. Asking first question to Player 1: ${game.player1Id}")
                fetchRandomVocabularyWord(gameId, game.player1Id)

            } catch (e: Exception) {
                _message.value = "Error joining game: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error joining online game: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Starts listening for real-time updates to an online game document in Firestore.
     * @param gameId The ID of the game to listen to.
     */
    fun startListeningForGameUpdates(gameId: String) {
        stopListeningForGameUpdates()
        gameListener = db.collection("onlineGames").document(gameId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _message.value = "Error listening to game: ${e.localizedMessage}"
                    Log.e("OnlineConnect4ViewModel", "Error in game listener: ${e.localizedMessage}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val game = snapshot.toObject(OnlineGame::class.java)
                    game?.let {
                        Log.d("OnlineConnect4ViewModel", "Game update received: GameId=${it.gameId}, CurrentPlayerId=${it.currentPlayerId}, QuestionWord=${it.currentQuestionWord}, QuestionTranslation=${it.currentQuestionTranslation}, QuestionAnsweredCorrectly=${it.questionAnsweredCorrectly}, QuestionAskedToPlayerId=${it.questionAskedToPlayerId}, Status=${it.status}")

                        _onlineGame.value = it
                        _board.value = Connect4Board(it.boardCells)
                        _currentPlayer.value = if (it.currentPlayerId == it.player1Id) 1 else 2
                        val currentUid = _currentUserId.value

                        // Determine if it's YOUR turn and if you need to answer a question.
                        val isCurrentPlayerMyTurn = it.currentPlayerId == currentUid && it.status == "playing"
                        _isMyTurn.value = isCurrentPlayerMyTurn

                        // Logic for game state with questions.
                        _gameState.value = when (it.status) {
                            "waiting" -> GameState.WaitingToStart
                            "playing" -> {
                                // If there's a word, it's your turn, and it hasn't been answered, ask.
                                // Or if it's the opponent's turn and a question was just asked to them.
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

                        // UI Messages based on current state.
                        when (it.status) {
                            "waiting" -> {
                                if (it.player1Id == currentUid) _message.value = "Waiting for opponent for game ${it.gameId}..."
                                else _message.value = "Waiting for Player 1 to start the game..."
                            }
                            "playing" -> {
                                if (it.currentPlayerId == currentUid) {
                                    if (it.currentQuestionWord != null && (it.questionAnsweredCorrectly == null || it.questionAnsweredCorrectly == false)) {
                                        _message.value = "It's your turn! Answer the question to move."
                                    } else if (it.currentQuestionWord != null && it.questionAnsweredCorrectly == true) {
                                        _message.value = "Correct answer. Now make your move!"
                                    }
                                    else {
                                        _message.value = "It's your turn! Make your move."
                                    }
                                } else {
                                    val opponentName = if (currentUid == it.player1Id) it.player2Name else it.player1Name
                                    if (it.currentQuestionWord != null && (it.questionAnsweredCorrectly == null || it.questionAnsweredCorrectly == false) && it.questionAskedToPlayerId == it.currentPlayerId) {
                                        _message.value = "Turn of ${opponentName ?: "opponent"}. Waiting for question answer..."
                                    } else {
                                        _message.value = "Turn of ${opponentName ?: "opponent"}..."
                                    }
                                }
                            }
                            "finished" -> {
                                if (it.winnerId == currentUid) _message.value = "You have won the game!"
                                else _message.value = "You lost. Winner: ${it.winnerId}"
                            }
                            "draw" -> _message.value = "The game ended in a draw."
                        }
                    }
                } else {
                    _message.value = "The game no longer exists."
                    _onlineGame.value = null
                    _gameState.value = GameState.WaitingToStart // Back to a state where a new game can be started or joined.
                    Log.d("OnlineConnect4ViewModel", "Online game does not exist or was deleted. Returning to waiting state.")
                }
            }
    }

    // Stops listening for real-time game updates.
    fun stopListeningForGameUpdates() {
        gameListener?.remove()
        gameListener = null
        Log.d("OnlineConnect4ViewModel", "Stopped listening for game updates.")
    }

    // Deletes the current online game. Only the game creator can delete it.
    fun deleteOnlineGame() {
        val game = _onlineGame.value ?: return
        val currentUid = _currentUserId.value ?: return
        if (game.player1Id == currentUid) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    db.collection("onlineGames").document(game.gameId).delete().await()
                    _message.value = "Game deleted."
                    // Explicitly reset online-specific state.
                    _onlineGame.value = null
                    _board.value = Connect4Board(initialBoardCells)
                    _currentPlayer.value = 1
                    _gameState.value = GameState.WaitingToStart // Return to initial online state.
                    Log.d("OnlineConnect4ViewModel", "Game ${game.gameId} deleted by ${currentUid}.")
                } catch (e: Exception) {
                    _message.value = "Error deleting game: ${e.localizedMessage}"
                    Log.e("OnlineConnect4ViewModel", "Error deleting online game: ${e.localizedMessage}")
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _message.value = "Only the game creator can delete the game."
        }
    }

    // VOCABULARY SYSTEM FUNCTIONS

    /**
     * Sets the user's answer attempt for the vocabulary question.
     * @param answer The user's typed answer.
     */
    fun setAnswerAttempt(answer: String) {
        _answerAttempt.value = answer
    }

    /**
     * Fetches a random vocabulary word from Firestore and updates the online game document.
     * This word will be presented as a question to the specified player.
     * @param gameId The ID of the online game.
     * @param playerIdAsking The ID of the player who will be asked the question.
     */
    private suspend fun fetchRandomVocabularyWord(gameId: String, playerIdAsking: String) {
        _isLoading.value = true
        Log.d("OnlineConnect4ViewModel", "Attempting to get vocabulary word for playerId: $playerIdAsking")
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
                    Log.d("OnlineConnect4ViewModel", "Word obtained and updated in Firestore for $playerIdAsking: ${randomWord.word} -> ${randomWord.translation}")
                    // Message will be set by the listener.
                } else {
                    _message.value = "No vocabulary words found. Game will continue without questions."
                    Log.w("OnlineConnect4ViewModel", "No vocabulary words found in Firestore. Proceeding without question for $playerIdAsking.")
                    // If no words, allow the game to proceed without a question.
                    val gameRef = db.collection("onlineGames").document(gameId)
                    gameRef.update(
                        "questionAnsweredCorrectly", true, // Assume correct if no question
                        "questionAskedToPlayerId", playerIdAsking
                    ).await()
                }
            } catch (e: Exception) {
                _message.value = "Error fetching vocabulary word: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error fetching vocabulary word for $playerIdAsking: ${e.localizedMessage}")
                // In case of error, assume correct to avoid blocking the game.
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

    // Submits the user's answer to the vocabulary question.
    fun submitAnswer() {
        viewModelScope.launch {
            val game = _onlineGame.value
            val currentUid = _currentUserId.value
            if (game == null || currentUid == null || game.currentQuestionWord == null || game.currentQuestionTranslation == null) {
                _message.value = "No active question to answer."
                Log.d("OnlineConnect4ViewModel", "SubmitAnswer: No active question or game state invalid.")
                return@launch
            }
            if (game.currentPlayerId != currentUid) {
                _message.value = "It's not your turn to answer."
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
                        // Keep currentQuestionWord and Translation until move is made for display.
                        // Don't change currentPlayerId yet.
                    ).await()
                    Log.d("OnlineConnect4ViewModel", "CORRECT answer by $currentUid. Now can move.")
                    _message.value = "Correct answer! Now you can make your move."
                    _answerAttempt.value = "" // Clear the input field.
                } else {
                    val nextPlayerId = if (game.player1Id == currentUid) game.player2Id else game.player1Id
                    gameRef.update(
                        "questionAnsweredCorrectly", false,
                        "currentPlayerId", nextPlayerId, // Pass turn.
                        "currentQuestionWord", null, // Clear question.
                        "currentQuestionTranslation", null, // Clear translation.
                        "questionAskedToPlayerId", null // Clear who was asked.
                    ).await()
                    Log.d("OnlineConnect4ViewModel", "INCORRECT answer by $currentUid. Turn passed to $nextPlayerId.")
                    _message.value = "Incorrect answer. Turn passed to opponent."
                    _answerAttempt.value = "" // Clear the input field.
                    // Automatically fetch new question for the next player.
                    nextPlayerId?.let { fetchRandomVocabularyWord(game.gameId, it) }
                }
            } catch (e: Exception) {
                _message.value = "Error submitting answer: ${e.localizedMessage}"
                Log.e("OnlineConnect4ViewModel", "Error submitting answer by $currentUid: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Called when the ViewModel is no longer used, stops the game listener.
    override fun onCleared() {
        super.onCleared()
        stopListeningForGameUpdates()
        Log.d("OnlineConnect4ViewModel", "ViewModel onCleared. Listener stopped.")
    }
}