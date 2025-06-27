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

@Composable
fun PlayerInfo(playerName: String, isCurrentPlayer: Boolean, isMe: Boolean, playerColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = playerName + if (isMe) " (Tú)" else "",
            color = if (isCurrentPlayer) playerColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(playerColor, CircleShape)
                .border(2.dp, if (isCurrentPlayer) Color.White else Color.Transparent, CircleShape)
        )
    }
}