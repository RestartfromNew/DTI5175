package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.i18n.Languages

@Composable
fun LanguageSelectionScreen(
    isDarkMode: Boolean = false,
    onLanguageSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val peachColor = Peach

    val context = androidx.compose.ui.platform.LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    var selectedLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }

    // Get localized strings based on current selection
    val titleText = Languages.getString(selectedLanguage, "CHOOSE_LANGUAGE")
    val skipText = Languages.getString(selectedLanguage, "SKIP")
    val nextText = Languages.getString(selectedLanguage, "NEXT")

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Title
            Text(
                text = titleText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Language options
            Languages.SUPPORTED_LANGUAGES.forEach { lang ->
                val isSelected = selectedLanguage == lang.code

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            color = if (isSelected) peachColor.copy(alpha = 0.2f) else surfaceColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) peachColor else if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            selectedLanguage = lang.code
                        }
                        .padding(vertical = 20.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Flag
                        Text(
                            text = lang.flag,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        // Display name in its own language
                        Text(
                            text = lang.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button
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
                    Text(skipText, fontSize = 16.sp)
                }

                // Next button
                Button(
                    onClick = {
                        languageManager.setLanguage(selectedLanguage)
                        onLanguageSelected(selectedLanguage)
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
                    Text(nextText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
