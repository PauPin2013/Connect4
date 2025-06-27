package com.example.connect4.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Composable function to display player information.
@Composable
fun PlayerInfo(playerName: String, isCurrentPlayer: Boolean, isMe: Boolean, playerColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Player's name, with "(You)" if it's the current user.
        Text(
            text = playerName + if (isMe) " (You)" else "",
            // Text color changes if it's the current player's turn.
            color = if (isCurrentPlayer) playerColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            // Font weight changes if it's the current player.
            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal
        )
        // Spacer for vertical separation.
        Spacer(modifier = Modifier.height(4.dp))
        // Box to display the player's color.
        Box(
            modifier = Modifier
                .size(24.dp) // Size of the color indicator.
                .background(playerColor, CircleShape) // Player's color with circular shape.
                // Border highlights the current player.
                .border(2.dp, if (isCurrentPlayer) Color.White else Color.Transparent, CircleShape)
        )
    }
}