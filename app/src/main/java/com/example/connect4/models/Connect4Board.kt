package com.example.connect4.models

data class Connect4Board(
    val cells: List<List<Int>>
) {
    val rows: Int = cells.size
    val columns: Int = cells.firstOrNull()?.size ?: 0

    fun dropPiece(column: Int, player: Int): Connect4Board {
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        for (r in rows - 1 downTo 0) { // Itera desde abajo hacia arriba
            if (newCells[r][column] == 0) { // Encuentra la primera celda vacía
                newCells[r][column] = player // Coloca la ficha del jugador
                println("Ficha del jugador $player colocada en ($r, $column)") // <-- Añadir para depuración
                break // Sale del bucle una vez que la ficha se coloca
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