package com.example.connect4.models

sealed class GameState {
    object Playing : GameState()
    data class Winner(val player: Int) : GameState()
    object Draw : GameState()
    object WaitingToStart : GameState()
    object Paused : GameState()
    data class AskingQuestion(val word: VocabularyWord) : GameState()
}