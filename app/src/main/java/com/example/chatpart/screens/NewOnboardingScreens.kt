package com.example.chatpart.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatpart.domain.Profile

@Composable
fun LanguageSelectionScreen(isDarkMode: Boolean, onLanguageSelected: (String) -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Language Selection (WIP)")
        Button(onClick = { onLanguageSelected("en") }, modifier = Modifier.padding(8.dp)) { Text("Next") }
        TextButton(onClick = onSkip) { Text("Skip") }
    }
}

@Composable
fun GenderSelectionScreen(isDarkMode: Boolean, onGenderSelected: (String) -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Gender Selection (WIP)")
        Button(onClick = { onGenderSelected("其他") }, modifier = Modifier.padding(8.dp)) { Text("Next") }
        TextButton(onClick = onSkip) { Text("Skip") }
    }
}

@Composable
fun AvatarSelectionScreen(isDarkMode: Boolean, selectedGender: String, onAvatarSelected: (String) -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Avatar Selection (WIP)")
        Button(onClick = { onAvatarSelected("👤") }, modifier = Modifier.padding(8.dp)) { Text("Next") }
        Row {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

@Composable
fun CharacterBasicScreen(isDarkMode: Boolean, selectedGender: String, selectedAvatar: String, onNext: (String, String) -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Character Basic Info (WIP)")
        Button(onClick = { onNext("Name", "Friend") }, modifier = Modifier.padding(8.dp)) { Text("Next") }
        Row {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

@Composable
fun CharacterDetailScreen(isDarkMode: Boolean, basicProfile: Profile, onSave: (Profile) -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Character Detailed Info (WIP)")
        Button(onClick = { onSave(basicProfile) }, modifier = Modifier.padding(8.dp)) { Text("Next") }
        Row {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}

@Composable
fun VoiceCloneScreen(isDarkMode: Boolean, characterName: String, onVoiceCloned: (String) -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Voice Clone (WIP)")
        Button(onClick = { onVoiceCloned("voice_123") }, modifier = Modifier.padding(8.dp)) { Text("Done") }
        Row {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    }
}
