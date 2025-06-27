package com.example.connect4.models

sealed class GameState {
    object Playing : GameState()
    data class Winner(val player: Int) : GameState()
    object Draw : GameState()
    object WaitingToStart : GameState()
    object Paused : GameState()
    // Nuevo estado para cuando se está haciendo una pregunta de vocabulario
    data class AskingQuestion(val word: String) : GameState()
}