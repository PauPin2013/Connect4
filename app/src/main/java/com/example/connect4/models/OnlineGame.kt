package com.example.connect4.models

import com.google.firebase.firestore.DocumentId

data class OnlineGame(
    @DocumentId
    val gameId: String = "",
    val player1Id: String? = null,
    val player2Id: String? = null,
    val player1Name: String? = null,
    val player2Name: String? = null,
    val boardCells: List<List<Int>> = List(6) { List(7) { 0 } },
    val currentPlayerId: String? = null,
    val status: String = "waiting",
    val winnerId: String? = null,
    val lastMoveColumn: Int? = null,
    val lastQuestion: VocabularyWord? = null,
    val isQuestionCorrect: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(
        boardCells = List(6) { List(7) { 0 } }
    )
}