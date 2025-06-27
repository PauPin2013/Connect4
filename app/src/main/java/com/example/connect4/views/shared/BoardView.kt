package com.example.connect4.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoardView(board: List<List<Int>>, onColumnClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF3498DB)) // Color del tablero
            .padding(8.dp)
    ) {
        board.forEachIndexed { rowIdx, row ->
            Row {
                row.forEachIndexed { colIdx, cell ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(48.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape) // Color del agujero (fondo)
                            .clickable {
                                onColumnClick(colIdx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (cell) {
                                        1 -> Color.Red // Ficha Jugador 1
                                        2 -> Color.Yellow // Ficha Jugador 2
                                        else -> Color.White // Agujero vacío
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}