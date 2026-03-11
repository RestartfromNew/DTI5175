package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.DarkText
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.data.ChatHistoryManager
import com.example.chatpart.data.ChatSession
import com.example.chatpart.domain.Profile
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    isDarkMode: Boolean = false,
    onChatClick: (String) -> Unit = {},
    characters: List<Profile> = emptyList()
) {
    val context = LocalContext.current
    val chatHistoryManager = remember { ChatHistoryManager(context) }

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

    // Load chat sessions
    val chatSessions = remember { chatHistoryManager.getAllChatSessions() }

    // Map character IDs to names
    val characterMap = remember(characters) {
        characters.associateBy { it.id }
    }

    // Get display name for a character
    fun getCharacterName(characterId: String): String {
        return when {
            characterId == "assistant" -> "AI Assistant"
            characterId == "teacher" -> "Teacher"
            characterId == "coding" -> "Coder"
            characterId.startsWith("custom_") -> {
                val profileId = characterId.removePrefix("custom_")
                characterMap[profileId]?.name ?: "Custom Character"
            }
            else -> "AI Chat"
        }
    }

    // Get emoji for a character
    fun getCharacterEmoji(characterId: String): String {
        return when {
            characterId == "assistant" -> "🤖"
            characterId == "teacher" -> "👩‍🏫"
            characterId == "coding" -> "💻"
            characterId.startsWith("custom_") -> {
                val profileId = characterId.removePrefix("custom_")
                characterMap[profileId]?.customAvatarPath?.takeIf { it.length <= 4 }
                    ?: characterMap[profileId]?.gender?.first()?.toString()
                    ?: "👤"
            }
            else -> "💬"
        }
    }

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

        if (chatSessions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No chat history yet",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Start a conversation to see it here",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(chatSessions) { index, session ->
                    HistoryCard(
                        session = session,
                        title = getCharacterName(session.characterId),
                        emoji = getCharacterEmoji(session.characterId),
                        index = index,
                        onClick = { onChatClick(session.characterId) },
                        onDelete = { chatHistoryManager.clearMessages(session.characterId) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    session: ChatSession,
    title: String,
    emoji: String,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val iconColors = listOf(Peach, Lavender, Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF5722))
    val iconColor = iconColors[index % iconColors.size]

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        emoji,
                        fontSize = 24.sp
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
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DarkText
                    )
                    Text(
                        session.getFormattedTime(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    session.lastMessage,
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
                            "${session.messageCount} messages",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = iconColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray
                )
            }
        }
    }
}
