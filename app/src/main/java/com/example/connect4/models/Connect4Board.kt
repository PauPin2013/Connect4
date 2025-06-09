package com.example.connect4.models

/**
 * Represents the Connect 4 game board.
 * Each inner list is a row, and each element in the inner list represents a cell:
 * 0 = empty, 1 = Player 1's piece, 2 = Player 2's piece.
 */
data class Connect4Board(
    val cells: List<List<Int>>
) {
    val rows: Int = cells.size
    val columns: Int = cells.firstOrNull()?.size ?: 0

    // Convenience function to create a new board with a piece dropped
    fun dropPiece(column: Int, player: Int): Connect4Board {
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        for (r in rows - 1 downTo 0) {
            if (newCells[r][column] == 0) {
                newCells[r][column] = player
                break
            }
        }
        return Connect4Board(newCells)
    }

    // Helper to get the value of a cell
    fun getCell(row: Int, col: Int): Int {
        return cells[row][col]
    }
}