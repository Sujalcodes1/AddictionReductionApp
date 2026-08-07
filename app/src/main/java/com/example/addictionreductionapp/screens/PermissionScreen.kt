package com.example.addictionreductionapp.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.utils.PermissionUtils
import kotlinx.coroutines.launch
import com.example.addictionreductionapp.components.ComplianceDialog
import com.example.addictionreductionapp.components.ACCESSIBILITY_DISCLOSURE

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit,
    val isCritical: Boolean,
    val settingsAction: (android.content.Context) -> Unit
)

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(PermissionUtils.hasUsageAccess(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionUtils.hasAccessibilityPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Lifecycle-aware permission polling (replaces infinite while(true) loop — M7.1)
    DisposableEffect(Unit) {
        var polling = true
        kotlinx.coroutines.MainScope().launch {
            while (polling) {
                kotlinx.coroutines.delay(1000)
                hasUsageAccess = PermissionUtils.hasUsageAccess(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                hasAccessibility = PermissionUtils.hasAccessibilityPermission(context)
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
            }
        }
        onDispose { polling = false }
    }

    val permissions = remember {
        listOf(
            PermissionItem(
                id = "usage",
                title = "Usage Access",
                description = "Analyzes your app usage history to create a personalized reduction plan. Shows how much time you spend on Social Media, Entertainment, and Games.",
                icon = { Icon(Icons.Default.Timer, null, tint = RegainTeal, modifier = Modifier.size(28.dp)) },
                isCritical = false,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            ),
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Detects which app you're using in real-time so we can track usage and block distracting apps when limits are reached.",
                icon = { Icon(Icons.Default.Accessibility, null, tint = RegainOrange, modifier = Modifier.size(28.dp)) },
                isCritical = true,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            ),
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps",
                description = "Shows a blocking overlay when you've reached your limit, preventing access to distracting apps.",
                icon = { Icon(Icons.Default.Block, null, tint = RegainPurple, modifier = Modifier.size(28.dp)) },
                isCritical = true,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                    })
                }
            ),
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                description = "Sends gentle reminders when you're nearing your daily limit and daily/weekly usage reports.",
                icon = { Icon(Icons.Default.Notifications, null, tint = RegainBlue, modifier = Modifier.size(28.dp)) },
                isCritical = false,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    })
                }
            )
        )
    }

    val allCriticalGranted = hasAccessibility && hasOverlay

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Permissions Required",
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "SmartFocus needs these permissions to track usage and help you reduce screen time. Critical permissions are required.",
                    color = TextGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            permissions.forEach { permission ->
                PermissionCard(
                    permission = permission,
                    isGranted = when (permission.id) {
                        "usage" -> hasUsageAccess
                        "overlay" -> hasOverlay
                        "accessibility" -> hasAccessibility
                        "notifications" -> hasNotificationPermission
                        else -> true
                    },
                    onGrant = {
                        if (permission.id == "accessibility") {
                            showAccessibilityDialog = true
                        } else if (permission.id == "notifications" && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permission.settingsAction(context)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (allCriticalGranted) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (allCriticalGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (allCriticalGranted) SuccessGreen else ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (allCriticalGranted) "All critical permissions granted!" else "Accessibility + Overlay are required to block apps",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        HorizontalDivider(color = DarkCardLight, thickness = 1.dp)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(TextGray.copy(alpha = 0.4f))
                )
            ) {
                Text("Skip for Now", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allCriticalGranted) RegainTeal else DarkCardLight
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = allCriticalGranted
            ) {
                Text(
                    "Continue",
                    color = if (allCriticalGranted) DarkBackground else TextGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // ── Accessibility Disclosure Dialog ──────────────────────────────────
    if (showAccessibilityDialog) {
        ComplianceDialog(
            title = "Accessibility Service",
            message = ACCESSIBILITY_DISCLOSURE,
            onConfirm = {
                showAccessibilityDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = {
                showAccessibilityDialog = false
            }
        )
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionItem,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) DarkCard.copy(alpha = 0.5f) else DarkCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DarkCardLight,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        permission.icon()
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            permission.title,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (permission.isCritical) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Required",
                                    color = ErrorRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isGranted) "Permission granted" else "Not yet granted",
                        color = if (isGranted) SuccessGreen else TextGray,
                        fontSize = 12.sp
                    )
                }

                if (isGranted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onGrant,
                        colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DarkCardLight, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Text(
                permission.description,
                color = TextGray,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
