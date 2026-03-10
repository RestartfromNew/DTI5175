package com.example.chatpart.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chatpart.DarkText
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: FirebaseUser? = null,
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    onNavigateToCharacters: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Theme-aware colors
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

    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White

    // Theme-aware text colors
    val textColor = if (isDarkMode) Color(0xFFE0E0E0) else DarkText
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val dividerColor = if (isDarkMode) Color(0xFF444444) else Color.LightGray.copy(alpha = 0.3f)
    val iconTint = if (isDarkMode) Color(0xFFE0E0E0) else textColor

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
                        Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = Lavender,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = textColor
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
            // Profile Card with Google Avatar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar - use Google profile photo if available
                    if (currentUser?.photoUrl != null) {
                        AsyncImage(
                            model = currentUser.photoUrl.toString(),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Peach.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = Peach,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            currentUser?.displayName ?: "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                        Text(
                            currentUser?.email ?: "Not signed in",
                            fontSize = 14.sp,
                            color = subtitleColor
                        )
                    }
                }
            }

            // General Section
            Text(
                "General",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = subtitleColor,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsToggleItem(
                        icon = Icons.Rounded.DarkMode,
                        iconColor = Lavender,
                        title = "Dark Mode",
                        subtitle = "Switch to dark theme",
                        checked = isDarkMode,
                        onCheckedChange = { onDarkModeChange(it) },
                        isDarkMode = isDarkMode
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = dividerColor
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.Notifications,
                        iconColor = Peach,
                        title = "Notifications",
                        subtitle = "Enable push notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        isDarkMode = isDarkMode
                    )
                }
            }

            // AI Settings Section
            Text(
                "AI Settings",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = subtitleColor,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsNavItem(
                        icon = Icons.Rounded.Psychology,
                        iconColor = Color(0xFF4CAF50),
                        title = "AI Model",
                        subtitle = "GPT-4",
                        isDarkMode = isDarkMode
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = dividerColor
                    )
                    SettingsNavItem(
                        icon = Icons.Rounded.Tune,
                        iconColor = Color(0xFF2196F3),
                        title = "Response Style",
                        subtitle = "Balanced",
                        isDarkMode = isDarkMode
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = dividerColor
                    )
                    SettingsNavItem(
                        icon = Icons.Rounded.Person,
                        iconColor = Peach,
                        title = "My Characters",
                        subtitle = "Manage AI characters",
                        onClick = onNavigateToCharacters,
                        isDarkMode = isDarkMode
                    )
                    SettingsNavItem(
                        icon = Icons.Rounded.Translate,
                        iconColor = Color(0xFFFF9800),
                        title = "Language",
                        subtitle = "English",
                        isDarkMode = isDarkMode
                    )
                }
            }

            // About Section
            Text(
                "About",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = subtitleColor,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surfaceColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsNavItem(
                        icon = Icons.Rounded.Info,
                        iconColor = Lavender,
                        title = "Version",
                        subtitle = "1.0.0",
                        isDarkMode = isDarkMode
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = dividerColor
                    )
                    SettingsNavItem(
                        icon = Icons.Rounded.PrivacyTip,
                        iconColor = Color(0xFFE91E63),
                        title = "Privacy Policy",
                        subtitle = "",
                        isDarkMode = isDarkMode
                    )
                }
            }

            // Sign Out Button
            if (currentUser != null) {
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444).copy(alpha = 0.1f),
                        contentColor = Color(0xFFFF4444)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        Icons.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sign Out",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean = false
) {
    // Theme-aware colors for this composable
    val textColor = if (isDarkMode) Color(0xFFE0E0E0) else DarkText
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = textColor)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 13.sp, color = subtitleColor)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Peach,
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    isDarkMode: Boolean = false
) {
    // Theme-aware colors for this composable
    val textColor = if (isDarkMode) Color(0xFFE0E0E0) else DarkText
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = textColor)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 13.sp, color = subtitleColor)
            }
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = subtitleColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
