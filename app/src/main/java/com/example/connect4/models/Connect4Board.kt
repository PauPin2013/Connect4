package com.example.connect4.models

// Represents the Connect4 game board
data class Connect4Board(
    val cells: List<List<Int>>
) {
    // Number of rows on the board
    val rows: Int = cells.size
    // Number of columns on the board
    val columns: Int = cells.firstOrNull()?.size ?: 0

    /**
     * Drops a player's piece into the specified column.
     * @param column The column where the piece will be dropped.
     * @param player The player (1 or 2) dropping the piece.
     * @return A new Connect4Board with the piece dropped.
     */
    fun dropPiece(column: Int, player: Int): Connect4Board {
        // Create a mutable copy of the board cells
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        // Iterate from bottom to top
        for (r in rows - 1 downTo 0) {
            // Find the first empty cell
            if (newCells[r][column] == 0) {
                // Place the player's piece
                newCells[r][column] = player
                // Print debug message (optional)
                println("Player $player piece placed at ($r, $column)")
                break // Exit loop once piece is placed
            }
        }
        return Connect4Board(newCells)
    }

    /**
     * Gets the value of the cell at the specified coordinates.
     * @param row The row of the cell.
     * @param col The column of the cell.
     * @return The value of the cell (0 for empty, 1 for player 1, 2 for player 2).
     * @throws IndexOutOfBoundsException if the coordinates are invalid.
     */
    fun getCell(row: Int, col: Int): Int {
        // Validate cell coordinates
        if (row < 0 || row >= rows || col < 0 || col >= columns) {
            throw IndexOutOfBoundsException("Invalid cell coordinates: ($row, $col)")
        }
        return cells[row][col]
    }
}