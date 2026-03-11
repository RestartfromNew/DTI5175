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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.i18n.LocalizedString

@Composable
fun CharacterBasicScreen(
    isDarkMode: Boolean = false,
    selectedGender: String,
    selectedAvatar: String,
    onNext: (name: String, relationship: String) -> Unit,
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

    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    val isFormValid = name.isNotBlank() && relationship.isNotBlank()

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
                    text = LocalizedString("BASIC_INFO"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedString("BASIC_INFO"),
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
                // Avatar preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    // Check if it's a file path (custom image) or emoji
                    if (selectedAvatar.startsWith("/") || selectedAvatar.contains("avatar_")) {
                        // It's a custom image
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // It's an emoji
                        Text(
                            text = selectedAvatar,
                            fontSize = 64.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Name input
                Text(
                    text = LocalizedString("NAME_REQUIRED"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(LocalizedString("NAME_PLACEHOLDER"), color = hintColor)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = peachColor,
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = peachColor
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Relationship input
                Text(
                    text = LocalizedString("RELATIONSHIP_REQUIRED"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(LocalizedString("RELATIONSHIP_PLACEHOLDER"), color = hintColor)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = peachColor,
                        unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = peachColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick suggestions
                Text(
                    text = LocalizedString("QUICK_SELECT"),
                    fontSize = 12.sp,
                    color = hintColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                val quickRelationships = listOf(
                    LocalizedString("REL_FRIEND"),
                    LocalizedString("REL_LOVER"),
                    LocalizedString("REL_TEACHER"),
                    LocalizedString("REL_ASSISTANT"),
                    LocalizedString("REL_FAMILY"),
                    LocalizedString("REL_CLASSMATE")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickRelationships.take(3).forEach { rel ->
                        SuggestionChip(
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = surfaceColor,
                                labelColor = hintColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickRelationships.drop(3).forEach { rel ->
                        SuggestionChip(
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 12.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = surfaceColor,
                                labelColor = hintColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        if (isFormValid) {
                            onNext(name, relationship)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = peachColor,
                        contentColor = Color.White
                    ),
                    enabled = isFormValid
                ) {
                    Text(LocalizedString("NEXT"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
