package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.domain.Profile
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    isDarkMode: Boolean = false,
    characters: List<Profile>,
    selectedCharacterId: String?,
    onSelectCharacter: (Profile) -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onBack: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }

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
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Lavender,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "My Characters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = textColor
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (characters.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = hintColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No characters yet",
                        fontSize = 18.sp,
                        color = hintColor
                    )
                    Text(
                        "Create your first AI character!",
                        fontSize = 14.sp,
                        color = hintColor
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
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        isSelected = character.id == selectedCharacterId,
                        isDarkMode = isDarkMode,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        hintColor = hintColor,
                        onClick = { onSelectCharacter(character) },
                        onEdit = { onEdit(character) },
                        onDelete = { showDeleteDialog = character }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Floating Action Button
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onCreateNew,
            containerColor = Peach,
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Create new character",
                tint = Color.White
            )
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { character ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Character") },
            text = { Text("Are you sure you want to delete \"${character.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(character)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CharacterCard(
    character: Profile,
    isSelected: Boolean,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textColor: Color,
    hintColor: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Peach)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = if (isSelected) Peach.copy(alpha = 0.2f) else Lavender.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        character.name.firstOrNull()?.uppercase() ?: "A",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Peach else Lavender
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Character info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        character.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Peach.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Active",
                                fontSize = 10.sp,
                                color = Peach,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "${character.gender} · ${character.relationship}",
                    fontSize = 12.sp,
                    color = hintColor
                )
                if (character.personality.isNotEmpty()) {
                    Text(
                        character.personality,
                        fontSize = 13.sp,
                        color = hintColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = hintColor
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
