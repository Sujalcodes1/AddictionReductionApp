package com.example.addictionreductionapp.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.AchievementBadge
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.viewmodel.AppBlockerViewModel
import com.example.addictionreductionapp.viewmodel.HomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onLogout: () -> Unit = {},
    blockerViewModel: AppBlockerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val startCompose = android.os.SystemClock.elapsedRealtime()
    val context = LocalContext.current
    val selectedAppCount by blockerViewModel.selectedAppCount.collectAsState()
    val profile by homeViewModel.profile.collectAsState()
    val achievements by homeViewModel.achievements.collectAsState()
    val recentInterventions by homeViewModel.recentInterventions.collectAsState()
    val scrollState = rememberScrollState()
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val authViewModel: com.example.addictionreductionapp.viewmodel.AuthViewModel = hiltViewModel()

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
                            profile?.userName?.firstOrNull()?.uppercase() ?: "U",
                            color = RegainTeal,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    profile?.userName ?: "User",
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
                        icon = Icons.Default.LocalFireDepartment,
                        value = "${profile?.streakCount ?: 0}",
                        label = "STREAK",
                        tint = RegainOrange
                    )
                    ProfileStat(
                        icon = Icons.Default.Timer,
                        value = "${profile?.totalFocusMinutes ?: 0}m",
                        label = "FOCUS",
                        tint = RegainTeal
                    )
                    ProfileStat(
                        icon = Icons.Default.CheckCircle,
                        value = "${profile?.sessionsCompleted ?: 0}",
                        label = "SESSIONS",
                        tint = SuccessGreen
                    )
                    ProfileStat(
                        icon = Icons.Default.EmojiEvents,
                        value = "${profile?.longestStreak ?: 0}",
                        label = "BEST",
                        tint = RegainAmber
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
            "${achievements.count { it.isUnlocked }} / ${achievements.size} unlocked",
            color = TextGray,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))

        // Achievements Grid (non-scrollable, fixed height)
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

        if (recentInterventions.isNotEmpty()) {
            Text(
                "Recent Interventions",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            recentInterventions.forEach { intervention ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (intervention.type) {
                                "breathing" -> Icons.Default.Air
                                "journal" -> Icons.Default.Edit
                                "affirmation" -> Icons.Default.Favorite
                                else -> Icons.Default.SelfImprovement
                            },
                            contentDescription = null,
                            tint = RegainPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                intervention.type.replaceFirstChar { it.uppercase() },
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (!intervention.journalText.isNullOrBlank()) {
                                Text(
                                    intervention.journalText.take(50),
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

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
            subtitle = "$selectedAppCount apps tracked",
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
            onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            }
        )

        SettingsItem(
            icon = Icons.Default.Security,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = onNavigateToPrivacy
        )

        SettingsItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Logout",
            subtitle = "Sign out of your account",
            onClick = onLogout
        )

        Spacer(Modifier.height(16.dp))

        // Danger Zone
        Card(
            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Danger Zone",
                    color = ErrorRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Permanently delete your account and all associated data. This action cannot be undone.",
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account", color = ErrorRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ── Delete Account Confirmation Dialog ─────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Delete Account", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to permanently delete your account?",
                        color = TextGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This will:\n• Sign you out on all devices\n• Clear all local data (usage, goals, messages)\n• Remove your account from our servers",
                        color = TextGray.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This action cannot be undone.",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            authViewModel.deleteAccount { success ->
                                isDeleting = false
                                showDeleteDialog = false
                                onLogout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Name Edit Dialog
    if (showNameDialog) {
        var newName by remember { mutableStateOf(profile?.userName ?: "User") }
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
                        newName = newName.ifBlank { "User" }
                        scope.launch {
                            val current = homeViewModel.profile.value ?: com.example.addictionreductionapp.data.local.entities.UserProfileEntity()
                            homeViewModel.updateUserName(newName)
                        }
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
    androidx.compose.runtime.SideEffect {
        val duration = android.os.SystemClock.elapsedRealtime() - startCompose
        android.util.Log.d("PerfDebug", "SettingsScreen composed in $duration ms")
    }
}

@Composable
private fun ProfileStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
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
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = RegainTeal
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
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
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
