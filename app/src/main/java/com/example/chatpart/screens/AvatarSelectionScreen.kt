package com.example.chatpart.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun AvatarSelectionScreen(
    isDarkMode: Boolean = false,
    selectedGender: String,
    onAvatarSelected: (String) -> Unit, // 可以是本地图片路径或emoji
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

    val context = LocalContext.current
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAvatar by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to app internal storage
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val fileName = "avatar_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                customAvatarUri = Uri.fromFile(file)
                selectedAvatar = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Preset avatars based on gender
    val presetAvatars = when (selectedGender) {
        LocalizedString("MALE") -> listOf(
            "👨", "👱", "🧔", "👨‍🦰", "👨‍🦱", "👨‍⚕️", "👨‍🎓", "👨‍💻",
            "🎅", "🧙", "🦸", "🧑‍🚀", "🧑‍🔬", "👲", "👳", "🧑‍🎤"
        )
        LocalizedString("FEMALE") -> listOf(
            "👩", "👱‍♀️", "👩‍🦰", "👩‍🦱", "👩‍⚕️", "👩‍🎓", "👩‍💻",
            "🧙‍♀️", "🦸‍♀️", "🧑‍🚀", "🧑‍🔬", "👸", "👛", "👗", "💄"
        )
        else -> listOf(
            "🧑", "👤", "👱", "🧔", "👩‍🎤", "👨‍🎤", "🧑‍💻", "👩‍💻",
            "🧑‍🔬", "👨‍🔬", "🧑‍🎓", "👨‍🎓", "🦸", "🦹", "🧙", "🧚"
        )
    }

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
                    text = LocalizedString("CHOOSE_AVATAR"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedString("UPLOAD_PHOTO"),
                fontSize = 14.sp,
                color = hintColor,
                modifier = Modifier.padding(start = 48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Custom avatar preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(surfaceColor, RoundedCornerShape(16.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (customAvatarUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customAvatarUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Custom avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Upload",
                            modifier = Modifier.size(40.dp),
                            tint = hintColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LocalizedString("UPLOAD_PHOTO"),
                            fontSize = 14.sp,
                            color = hintColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = LocalizedString("OR_CHOOSE_PRESET"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Preset avatars grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(presetAvatars) { emoji ->
                    val isSelected = selectedAvatar == emoji
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (isSelected) peachColor.copy(alpha = 0.3f) else surfaceColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = peachColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedAvatar = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 32.sp
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
                        if (selectedAvatar != null) {
                            onAvatarSelected(selectedAvatar!!)
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
                    enabled = selectedAvatar != null
                ) {
                    Text(LocalizedString("NEXT"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
