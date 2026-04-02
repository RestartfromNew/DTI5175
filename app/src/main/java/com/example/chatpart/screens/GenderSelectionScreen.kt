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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.i18n.LocalizedString

@Composable
fun GenderSelectionScreen(
    isDarkMode: Boolean = false,
    initialGender: String? = null,
    onGenderSelected: (String) -> Unit,
    onSkip: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val peachColor = Peach

    var selectedGender by remember { mutableStateOf<String?>(initialGender) }

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
                text = LocalizedString("CHOOSE_GENDER"),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedString("GENDER_DESC"),
                fontSize = 14.sp,
                color = hintColor
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Gender options
            val genders = listOf(
                Pair(LocalizedString("MALE"), "👦"),
                Pair(LocalizedString("FEMALE"), "👧"),
                Pair(LocalizedString("OTHER"), "👤")
            )

            genders.forEach { (label, emoji) ->
                val isSelected = selectedGender == label

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
                            selectedGender = label
                        }
                        .padding(vertical = 20.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Emoji
                        Text(
                            text = emoji,
                            fontSize = 36.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        // Only show label
                        Text(
                            text = label,
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
                    Text(LocalizedString("SKIP"), fontSize = 16.sp)
                }

                // Next button
                Button(
                    onClick = {
                        if (selectedGender != null) {
                            onGenderSelected(selectedGender!!)
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
                    enabled = selectedGender != null
                ) {
                    Text(LocalizedString("NEXT"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
