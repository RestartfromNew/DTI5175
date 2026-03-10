package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.DarkText
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import androidx.compose.material3.MaterialTheme

data class ChatHistory(
    val title: String,
    val lastMessage: String,
    val time: String,
    val messageCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(isDarkMode: Boolean = false) {
    val backgroundColor = if (isDarkMode) {
        MaterialTheme.colorScheme.background
    } else {
        SoftWhite
    }
    val gradientEnd = if (isDarkMode) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF3F0FF)
    }

    val historyItems = listOf(
        ChatHistory("Travel Planning", "Can you help me plan a trip to Tokyo?", "Today", 12),
        ChatHistory("Code Review", "Here's the function I need help with...", "Yesterday", 8),
        ChatHistory("Recipe Ideas", "I'd like some healthy dinner suggestions", "Feb 9", 15),
        ChatHistory("Study Notes", "Explain quantum computing in simple terms", "Feb 8", 6),
        ChatHistory("Creative Writing", "Help me write a short story about...", "Feb 7", 20),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor, gradientEnd)))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = Lavender,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Chat History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = DarkText
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(historyItems) { index, item ->
                HistoryCard(item, index)
            }
        }
    }
}

@Composable
fun HistoryCard(item: ChatHistory, index: Int) {
    val iconColors = listOf(Peach, Lavender, Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF5722))
    val iconColor = iconColors[index % iconColors.size]

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DarkText
                    )
                    Text(
                        item.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.lastMessage,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = iconColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${item.messageCount} messages",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = iconColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
