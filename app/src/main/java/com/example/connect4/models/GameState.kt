package com.example.connect4.models

// Represents the different states of the Connect4 game.
sealed class GameState {
    // Game is currently in progress.
    object Playing : GameState()
    // A player has won the game.
    data class Winner(val player: Int) : GameState()
    // The game has ended in a draw.
    object Draw : GameState()
    // The game is waiting to begin.
    object WaitingToStart : GameState()
    // The game is currently paused.
    object Paused : GameState()
    // Game is asking a vocabulary question.
    data class AskingQuestion(val word: String) : GameState()
}