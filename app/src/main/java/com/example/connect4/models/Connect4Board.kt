package com.example.connect4.models

data class Connect4Board(
    val cells: List<List<Int>>
) {
    val rows: Int = cells.size
    val columns: Int = cells.firstOrNull()?.size ?: 0

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

    fun getCell(row: Int, col: Int): Int {
        if (row < 0 || row >= rows || col < 0 || col >= columns) {
            throw IndexOutOfBoundsException("Invalid cell coordinates: ($row, $col)")
        }
        return cells[row][col]
    }
}