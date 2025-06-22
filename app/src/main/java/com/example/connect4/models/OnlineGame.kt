package com.example.connect4.models

import com.google.firebase.firestore.DocumentId
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class OnlineGame(
    @DocumentId
    val gameId: String = "",
    val player1Id: String? = null,
    val player2Id: String? = null,
    val player1Name: String? = null,
    val player2Name: String? = null,
    val boardCellsJson: String = Gson().toJson(List(6) { List(7) { 0 } }),
    val currentPlayerId: String? = null,
    val status: String = "waiting",
    val winnerId: String? = null,
    val lastMoveColumn: Int? = null,
    val lastQuestion: VocabularyWord? = null,
    val isQuestionCorrect: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val boardCells: List<List<Int>>
        get() {
            val type = object : TypeToken<List<List<Int>>>() {}.type
            return Gson().fromJson(boardCellsJson, type) ?: List(6) { List(7) { 0 } }
        }

    constructor() : this(
        boardCellsJson = Gson().toJson(List(6) { List(7) { 0 } })
    )
}