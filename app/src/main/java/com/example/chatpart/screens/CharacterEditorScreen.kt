package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.domain.Profile
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditorScreen(
    isDarkMode: Boolean = false,
    existingProfile: Profile? = null,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    // Form fields
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var gender by remember { mutableStateOf(existingProfile?.gender ?: "女") }
    var relationship by remember { mutableStateOf(existingProfile?.relationship ?: "用户的朋友") }
    var background by remember { mutableStateOf(existingProfile?.background ?: "") }
    var personality by remember { mutableStateOf(existingProfile?.personality ?: "") }
    var speakStyle by remember { mutableStateOf(existingProfile?.speakStyle?.joinToString(", ") ?: "") }
    var doRules by remember { mutableStateOf(existingProfile?.doRules?.joinToString("\n") ?: "") }
    var dontRules by remember { mutableStateOf(existingProfile?.dontRules?.joinToString("\n") ?: "") }

    val isEditing = existingProfile != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor, gradientEnd)))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    if (isEditing) "Edit Character" else "Create Character",
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name field
            CharacterTextField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor
            )

            // Gender selector
            Text(
                "Gender",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = hintColor
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("男", "女", "其他").forEach { option ->
                    FilterChip(
                        selected = gender == option,
                        onClick = { gender = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lavender.copy(alpha = 0.3f),
                            selectedLabelColor = Lavender
                        )
                    )
                }
            }

            // Relationship field
            CharacterTextField(
                label = "Relationship (与用户的关系)",
                value = relationship,
                onValueChange = { relationship = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor
            )

            // Background field
            CharacterTextField(
                label = "Background (背景故事)",
                value = background,
                onValueChange = { background = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor,
                maxLines = 4
            )

            // Personality field
            CharacterTextField(
                label = "Personality (性格描述)",
                value = personality,
                onValueChange = { personality = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor,
                maxLines = 3
            )

            // Speak Style field
            CharacterTextField(
                label = "Speak Style (说话风格，用逗号分隔)",
                value = speakStyle,
                onValueChange = { speakStyle = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor,
                maxLines = 2
            )

            // Do Rules field
            CharacterTextField(
                label = "Do Rules (行为规则，每行一条)",
                value = doRules,
                onValueChange = { doRules = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor,
                maxLines = 4
            )

            // Don't Rules field
            CharacterTextField(
                label = "Don't Rules (禁止规则，每行一条)",
                value = dontRules,
                onValueChange = { dontRules = it },
                isDarkMode = isDarkMode,
                surfaceColor = surfaceColor,
                textColor = textColor,
                hintColor = hintColor,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val profile = Profile(
                            id = existingProfile?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            gender = gender,
                            background = background.trim(),
                            relationship = relationship.trim(),
                            personality = personality.trim(),
                            speakStyle = speakStyle.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            doRules = doRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                            dontRules = dontRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        onSave(profile)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Peach
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Icon(
                    Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save Character",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CharacterTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    textColor: Color,
    hintColor: Color,
    maxLines: Int = 1
) {
    Column {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = hintColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lavender,
                unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color.LightGray,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = Lavender
            ),
            placeholder = {
                Text("Enter $label", color = hintColor)
            },
            maxLines = maxLines
        )
    }
}
