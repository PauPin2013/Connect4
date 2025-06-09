package com.example.connect4.models

/**
 * Sealed class representing the different states of the game.
 */
sealed class GameState {
    object Playing : GameState() // Game is ongoing
    data class Winner(val player: Int) : GameState() // A player has won
    object Draw : GameState() // The game is a draw
}
