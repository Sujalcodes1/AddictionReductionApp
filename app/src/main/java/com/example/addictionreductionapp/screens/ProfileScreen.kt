package com.example.addictionreductionapp.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.AchievementBadge
import com.example.addictionreductionapp.data.AppDataStore
import com.example.addictionreductionapp.ui.theme.*

@Composable
fun ProfileScreen(onNavigateToApps: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showNameDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "Profile",
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        // Profile Card
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = RegainTeal.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            AppDataStore.userName.value.firstOrNull()?.uppercase() ?: "U",
                            color = RegainTeal,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    AppDataStore.userName.value,
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { showNameDialog = true }) {
                    Text("Edit Name", color = RegainTeal, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Stats Row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(
                        value = "${AppDataStore.streakCount.intValue}",
                        label = "STREAK",
                        emoji = "🔥"
                    )
                    ProfileStat(
                        value = "${AppDataStore.totalFocusMinutes.intValue}m",
                        label = "FOCUS",
                        emoji = "⏱️"
                    )
                    ProfileStat(
                        value = "${AppDataStore.sessionsCompleted.intValue}",
                        label = "SESSIONS",
                        emoji = "✅"
                    )
                    ProfileStat(
                        value = "${AppDataStore.longestStreak.intValue}",
                        label = "BEST",
                        emoji = "🏆"
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Achievements
        Text(
            "Achievements",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${AppDataStore.achievements.count { it.isUnlocked }} / ${AppDataStore.achievements.size} unlocked",
            color = TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))

        // Achievements Grid (non-scrollable, fixed height)
        val achievements = AppDataStore.achievements
        val rows = (achievements.size + 2) / 3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until rows) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        if (index < achievements.size) {
                            val achievement = achievements[index]
                            AchievementBadge(
                                emoji = achievement.icon,
                                title = achievement.title,
                                isUnlocked = achievement.isUnlocked,
                                progress = achievement.progress,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Settings Section
        Text(
            "Settings",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Default.Shield,
            title = "Manage App Limits",
            subtitle = "${AppDataStore.apps.count { it.isSelected }} apps tracked",
            onClick = onNavigateToApps
        )

        SettingsItem(
            icon = Icons.Default.Accessibility,
            title = "Accessibility Service",
            subtitle = "Required for app blocking",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        SettingsItem(
            icon = Icons.Default.DataUsage,
            title = "Usage Access",
            subtitle = "Required for screen time tracking",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Manage notification preferences",
            onClick = { }
        )

        SettingsItem(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "Version 1.0.0",
            onClick = { }
        )

        Spacer(Modifier.height(24.dp))
    }

    // Name Edit Dialog
    if (showNameDialog) {
        var newName by remember { mutableStateOf(AppDataStore.userName.value) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RegainTeal,
                        focusedLabelColor = RegainTeal,
                        cursorColor = RegainTeal,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppDataStore.userName.value = newName.ifBlank { "User" }
                        AppDataStore.saveToPrefs(context)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RegainTeal)
                ) {
                    Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = TextGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = DarkCardLight,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextGray, fontSize = 12.sp)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
