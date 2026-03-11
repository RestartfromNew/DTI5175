package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.LocalizedString

@Composable
fun CharacterDetailScreen(
    isDarkMode: Boolean = false,
    basicProfile: Profile,
    existingProfile: Profile? = null,
    onSave: (Profile) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val peachColor = Peach

    // Pre-fill with existing profile data if editing
    var personality by remember { mutableStateOf(existingProfile?.personality ?: "") }
    var background by remember { mutableStateOf(existingProfile?.background ?: "") }
    var speakStyle by remember { mutableStateOf(existingProfile?.speakStyle?.joinToString(", ") ?: "") }
    var doRules by remember { mutableStateOf(existingProfile?.doRules?.joinToString("\n") ?: "") }
    var dontRules by remember { mutableStateOf(existingProfile?.dontRules?.joinToString("\n") ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, gradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = LocalizedString("BACK"),
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LocalizedString("MORE_DETAILS"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedString("OPTIONAL_SKIP"),
                fontSize = 14.sp,
                color = hintColor,
                modifier = Modifier.padding(start = 48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Personality
                DetailInputField(
                    label = LocalizedString("PERSONALITY"),
                    placeholder = LocalizedString("PERSONALITY_PLACEHOLDER"),
                    value = personality,
                    onValueChange = { personality = it },
                    textColor = textColor,
                    hintColor = hintColor,
                    surfaceColor = surfaceColor,
                    peachColor = peachColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Background
                DetailInputField(
                    label = LocalizedString("BACKGROUND_STORY"),
                    placeholder = LocalizedString("BACKGROUND_PLACEHOLDER"),
                    value = background,
                    onValueChange = { background = it },
                    textColor = textColor,
                    hintColor = hintColor,
                    surfaceColor = surfaceColor,
                    peachColor = peachColor,
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Speak Style
                DetailInputField(
                    label = LocalizedString("SPEAK_STYLE"),
                    placeholder = LocalizedString("SPEAK_STYLE_PLACEHOLDER"),
                    value = speakStyle,
                    onValueChange = { speakStyle = it },
                    textColor = textColor,
                    hintColor = hintColor,
                    surfaceColor = surfaceColor,
                    peachColor = peachColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Do Rules
                DetailInputField(
                    label = LocalizedString("DO_RULES"),
                    placeholder = LocalizedString("DO_RULES_PLACEHOLDER"),
                    value = doRules,
                    onValueChange = { doRules = it },
                    textColor = textColor,
                    hintColor = hintColor,
                    surfaceColor = surfaceColor,
                    peachColor = peachColor,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Don't Rules
                DetailInputField(
                    label = LocalizedString("DONT_RULES"),
                    placeholder = LocalizedString("DONT_RULES_PLACEHOLDER"),
                    value = dontRules,
                    onValueChange = { dontRules = it },
                    textColor = textColor,
                    hintColor = hintColor,
                    surfaceColor = surfaceColor,
                    peachColor = peachColor,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = hintColor
                    )
                ) {
                    Text(LocalizedString("SKIP"), fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        val profile = basicProfile.copy(
                            personality = personality.trim(),
                            background = background.trim(),
                            speakStyle = speakStyle.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            doRules = doRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                            dontRules = dontRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        onSave(profile)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = peachColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(LocalizedString("FINISH"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color,
    hintColor: Color,
    surfaceColor: Color,
    peachColor: Color,
    minLines: Int = 1
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, color = hintColor)
            },
            minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = peachColor,
                unfocusedBorderColor = if (textColor == Color.White) Color(0xFF444444) else Color(0xFFE0E0E0),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = peachColor
            )
        )
    }
}
