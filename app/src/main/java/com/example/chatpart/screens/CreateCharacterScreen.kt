package com.example.chatpart.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.LocalizedString
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun CreateCharacterScreen(
    isDarkMode: Boolean = false,
    existingProfile: Profile? = null,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2F)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val peachColor = Peach

    // Wizard state
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 3

    // Profile data state
    var gender by remember { mutableStateOf(existingProfile?.gender ?: "女") }
    var customAvatarPath by remember { mutableStateOf(existingProfile?.customAvatarPath ?: "") }
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var relationship by remember { mutableStateOf(existingProfile?.relationship ?: "朋友") }
    var background by remember { mutableStateOf(existingProfile?.background ?: "") }
    var personality by remember { mutableStateOf(existingProfile?.personality ?: "") }
    var speakStyle by remember { mutableStateOf(existingProfile?.speakStyle?.joinToString(", ") ?: "") }
    var doRules by remember { mutableStateOf(existingProfile?.doRules?.joinToString("\n") ?: "") }
    var dontRules by remember { mutableStateOf(existingProfile?.dontRules?.joinToString("\n") ?: "") }

    val isEditing = existingProfile != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor, gradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = LocalizedString("BACK"),
                            tint = textColor
                        )
                    }
                } else {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = LocalizedString("BACK"),
                            tint = textColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) LocalizedString("EDIT_CHARACTER") else LocalizedString("CREATE_CHARACTER"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step indicator
            StepIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                stepLabels = listOf(
                    LocalizedString("GENDER_AVATAR"),
                    LocalizedString("BASIC_INFO"),
                    LocalizedString("MORE_DETAILS")
                ),
                textColor = textColor,
                peachColor = peachColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step content with animation
            AnimatedContent(
                targetState = currentStep,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> Step1GenderAvatar(
                        gender = gender,
                        customAvatarPath = customAvatarPath,
                        onGenderChange = { gender = it },
                        onAvatarChange = { customAvatarPath = it },
                        textColor = textColor,
                        hintColor = hintColor,
                        surfaceColor = surfaceColor,
                        peachColor = peachColor,
                        isDarkMode = isDarkMode
                    )
                    2 -> Step2BasicInfo(
                        name = name,
                        relationship = relationship,
                        onNameChange = { name = it },
                        onRelationshipChange = { relationship = it },
                        textColor = textColor,
                        hintColor = hintColor,
                        surfaceColor = surfaceColor,
                        peachColor = peachColor,
                        isDarkMode = isDarkMode
                    )
                    3 -> Step3MoreDetails(
                        background = background,
                        personality = personality,
                        speakStyle = speakStyle,
                        doRules = doRules,
                        dontRules = dontRules,
                        onBackgroundChange = { background = it },
                        onPersonalityChange = { personality = it },
                        onSpeakStyleChange = { speakStyle = it },
                        onDoRulesChange = { doRules = it },
                        onDontRulesChange = { dontRules = it },
                        textColor = textColor,
                        hintColor = hintColor,
                        surfaceColor = surfaceColor,
                        peachColor = peachColor,
                        isDarkMode = isDarkMode
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = hintColor
                        )
                    ) {
                        Text(LocalizedString("BACK"), fontSize = 16.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            // Save profile
                            val profile = Profile(
                                id = existingProfile?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                gender = gender,
                                relationship = relationship,
                                background = background,
                                personality = personality,
                                speakStyle = speakStyle.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                doRules = doRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                                dontRules = dontRules.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                                voiceId = existingProfile?.voiceId ?: "",
                                facetimeUrl = existingProfile?.facetimeUrl ?: "",
                                customAvatarPath = customAvatarPath
                            )
                            onSave(profile)
                        }
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
                    Text(
                        if (currentStep < totalSteps) LocalizedString("NEXT") else LocalizedString("SAVE"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String>,
    textColor: Color,
    peachColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { index, label ->
                val stepNumber = index + 1
                val isActive = stepNumber == currentStep
                val isCompleted = stepNumber < currentStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when {
                                    isCompleted -> peachColor
                                    isActive -> peachColor
                                    else -> textColor.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                color = if (isActive) Color.White else textColor.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (isActive) peachColor else textColor.copy(alpha = 0.5f),
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }

                if (index < totalSteps - 1) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(2.dp)
                            .background(
                                if (isCompleted) peachColor else textColor.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1GenderAvatar(
    gender: String,
    customAvatarPath: String,
    onGenderChange: (String) -> Unit,
    onAvatarChange: (String) -> Unit,
    textColor: Color,
    hintColor: Color,
    surfaceColor: Color,
    peachColor: Color,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val fileName = "avatar_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                customAvatarUri = Uri.fromFile(file)
                selectedEmoji = null
                onAvatarChange(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Preset avatars based on gender
    val presetAvatars = when (gender) {
        "男" -> listOf(
            "👨", "👱", "🧔", "👨‍🦰", "👨‍🦱", "👨‍⚕️", "👨‍🎓", "👨‍💻",
            "🎅", "🧙", "🦸", "🧑‍🚀", "🧑‍🔬", "👲", "👳", "🧑‍🎤"
        )
        "女" -> listOf(
            "👩", "👱‍♀️", "👩‍🦰", "👩‍🦱", "👩‍⚕️", "👩‍🎓", "👩‍💻",
            "🧙‍♀️", "🦸‍♀️", "🧑‍🚀", "🧑‍🔬", "👸", "👛", "👗", "💄"
        )
        else -> listOf(
            "🧑", "👤", "👱", "🧔", "👩‍🎤", "👨‍🎤", "🧑‍💻", "👩‍💻",
            "🧑‍🔬", "👨‍🔬", "🧑‍🎓", "👨‍🎓", "🦸", "🦹", "🧙", "🧚"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Gender Selection
        Text(
            text = LocalizedString("CHOOSE_GENDER"),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        val genders = listOf(
            Triple("男", "👦", "男"),
            Triple("女", "👧", "女"),
            Triple("其他", "👤", "其他")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genders.forEach { (label, emoji, value) ->
                val isSelected = gender == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) peachColor.copy(alpha = 0.2f) else surfaceColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) peachColor else if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onGenderChange(value) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) peachColor else textColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar Selection
        Text(
            text = LocalizedString("CHOOSE_AVATAR"),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Upload photo box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(surfaceColor, RoundedCornerShape(16.dp))
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (customAvatarUri != null || (customAvatarPath.isNotEmpty() && customAvatarPath.startsWith("/"))) {
                val uri = customAvatarUri ?: Uri.fromFile(File(customAvatarPath))
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
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
                        modifier = Modifier.size(32.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = LocalizedString("OR_CHOOSE_PRESET"),
            fontSize = 14.sp,
            color = hintColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset avatars grid — plain rows to avoid nested scroll conflict
        val chunked = presetAvatars.chunked(4)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji || (customAvatarPath == emoji)
                    Box(
                        modifier = Modifier
                            .weight(1f)
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
                            .clickable {
                                selectedEmoji = emoji
                                customAvatarUri = null
                                onAvatarChange(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                    }
                }
                // Fill remaining slots in last row if needed
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Step2BasicInfo(
    name: String,
    relationship: String,
    onNameChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
    textColor: Color,
    hintColor: Color,
    surfaceColor: Color,
    peachColor: Color,
    isDarkMode: Boolean = false
) {
    val relationshipOptions = listOf("朋友", "恋人", "家人", "老师", "同事", "偶像")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Name field
        Text(
            text = LocalizedString("CHARACTER_NAME"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(LocalizedString("ENTER_NAME"), color = hintColor)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = peachColor,
                unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Relationship field
        Text(
            text = LocalizedString("RELATIONSHIP"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = relationship,
            onValueChange = onRelationshipChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(LocalizedString("ENTER_RELATIONSHIP"), color = hintColor)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = peachColor,
                unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick select chips
        Text(
            text = LocalizedString("OR_SELECT"),
            fontSize = 14.sp,
            color = hintColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            relationshipOptions.take(3).forEach { option ->
                FilterChip(
                    selected = relationship == option,
                    onClick = { onRelationshipChange(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = peachColor.copy(alpha = 0.2f),
                        selectedLabelColor = peachColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = relationship == option,
                        borderColor = if (relationship == option) peachColor else hintColor,
                        selectedBorderColor = peachColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            relationshipOptions.drop(3).forEach { option ->
                FilterChip(
                    selected = relationship == option,
                    onClick = { onRelationshipChange(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = peachColor.copy(alpha = 0.2f),
                        selectedLabelColor = peachColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = relationship == option,
                        borderColor = if (relationship == option) peachColor else hintColor,
                        selectedBorderColor = peachColor
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun Step3MoreDetails(
    background: String,
    personality: String,
    speakStyle: String,
    doRules: String,
    dontRules: String,
    onBackgroundChange: (String) -> Unit,
    onPersonalityChange: (String) -> Unit,
    onSpeakStyleChange: (String) -> Unit,
    onDoRulesChange: (String) -> Unit,
    onDontRulesChange: (String) -> Unit,
    textColor: Color,
    hintColor: Color,
    surfaceColor: Color,
    peachColor: Color,
    isDarkMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Background field
        FormTextField(
            label = LocalizedString("BACKGROUND"),
            value = background,
            onValueChange = onBackgroundChange,
            placeholder = LocalizedString("BACKGROUND_PLACEHOLDER"),
            textColor = textColor,
            hintColor = hintColor,
            surfaceColor = surfaceColor,
            peachColor = peachColor,
            minLines = 3,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Personality field
        FormTextField(
            label = LocalizedString("PERSONALITY"),
            value = personality,
            onValueChange = onPersonalityChange,
            placeholder = LocalizedString("PERSONALITY_PLACEHOLDER"),
            textColor = textColor,
            hintColor = hintColor,
            surfaceColor = surfaceColor,
            peachColor = peachColor,
            minLines = 2,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Speak Style field
        FormTextField(
            label = LocalizedString("SPEAK_STYLE"),
            value = speakStyle,
            onValueChange = onSpeakStyleChange,
            placeholder = LocalizedString("SPEAK_STYLE_PLACEHOLDER"),
            textColor = textColor,
            hintColor = hintColor,
            surfaceColor = surfaceColor,
            peachColor = peachColor,
            minLines = 2,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Do Rules field
        FormTextField(
            label = LocalizedString("DO_RULES"),
            value = doRules,
            onValueChange = onDoRulesChange,
            placeholder = LocalizedString("DO_RULES_PLACEHOLDER"),
            textColor = textColor,
            hintColor = hintColor,
            surfaceColor = surfaceColor,
            peachColor = peachColor,
            minLines = 2,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Don't Rules field
        FormTextField(
            label = LocalizedString("DONT_RULES"),
            value = dontRules,
            onValueChange = onDontRulesChange,
            placeholder = LocalizedString("DONT_RULES_PLACEHOLDER"),
            textColor = textColor,
            hintColor = hintColor,
            surfaceColor = surfaceColor,
            peachColor = peachColor,
            minLines = 2,
            isDarkMode = isDarkMode
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    hintColor: Color,
    surfaceColor: Color,
    peachColor: Color,
    minLines: Int = 1,
    isDarkMode: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 16.sp,
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = peachColor,
                unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = minLines
        )
    }
}
