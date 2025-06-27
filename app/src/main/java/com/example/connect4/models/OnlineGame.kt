package com.example.connect4.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Represents an online Connect4 game.
data class OnlineGame(
    // Unique identifier for the game.
    val gameId: String = "",
    // ID of player 1.
    val player1Id: String = "",
    // ID of player 2 (nullable if not joined yet).
    val player2Id: String? = null,
    // Name of player 1.
    val player1Name: String? = null,
    // Name of player 2.
    val player2Name: String? = null,
    // JSON string representation of the board cells.
    val boardCellsJson: String = Gson().toJson(List(6) { List(7) { 0 } }),
    // ID of the player whose turn it is.
    val currentPlayerId: String? = null,
    // Current status of the game (e.g., "waiting", "playing", "finished").
    val status: String = "waiting",
    // ID of the winning player.
    val winnerId: String? = null,
    // Column of the last move made.
    val lastMoveColumn: Int? = null,
    // Current vocabulary question word.
    val currentQuestionWord: String? = null,
    // Correct translation for the current question word.
    val currentQuestionTranslation: String? = null,
    // Indicates if the last question was answered correctly.
    val questionAnsweredCorrectly: Boolean? = null,
    // ID of the player to whom the question was asked.
    val questionAskedToPlayerId: String? = null,
    // Timestamp of when the game was created.
    val createdAt: Long = System.currentTimeMillis()
) {
    // Exclude from Firestore to avoid redundant storage, convert JSON to List.
    @get:Exclude
    val boardCells: List<List<Int>>
        get() {
            val type = object : TypeToken<List<List<Int>>>() {}.type
            return Gson().fromJson(boardCellsJson, type) ?: List(6) { List(7) { 0 } }
        }

    // Secondary constructor for creating an OnlineGame with default board.
    constructor() : this(
        boardCellsJson = Gson().toJson(List(6) { List(7) { 0 } })
    )
}