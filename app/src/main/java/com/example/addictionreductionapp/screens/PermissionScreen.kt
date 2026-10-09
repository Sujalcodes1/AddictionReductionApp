package com.example.addictionreductionapp.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.components.ACCESSIBILITY_DISCLOSURE
import com.example.addictionreductionapp.components.ComplianceDialog
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.utils.PermissionUtils
import kotlinx.coroutines.launch

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit,
    val isCritical: Boolean,
    val settingsAction: (android.content.Context) -> Unit
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = PermissionUtils.hasUsageAccess(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                hasAccessibility = PermissionUtils.hasAccessibilityPermission(context)
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // LAG FIX: removed while(true) 2s poll — it forced a full recompose every 2s
    // (Binder IPC x4 per tick). ON_RESUME observer above is sufficient; user
    // returns from Settings -> ON_RESUME refreshes. No background polling.

    val permissions = remember {
        listOf(
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Detects which app you're using in real-time so we can track usage and block distracting apps when limits are reached.",
                icon = { Icon(Icons.Default.Accessibility, null, tint = RegainTeal, modifier = Modifier.size(30.dp)) },
                isCritical = true,
                settingsAction = { ctx -> ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            ),
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps",
                description = "Shows a blocking overlay when you've reached your limit, preventing access to distracting apps.",
                icon = { Icon(Icons.Default.Block, null, tint = RegainTeal, modifier = Modifier.size(30.dp)) },
                isCritical = true,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                    })
                }
            ),
            PermissionItem(
                id = "usage",
                title = "Usage Access",
                description = "Analyzes your app usage history to create a personalized reduction plan. Shows how much time you spend on Social Media, Entertainment, and Games.",
                icon = { Icon(Icons.Default.Timer, null, tint = RegainTeal, modifier = Modifier.size(30.dp)) },
                isCritical = false,
                settingsAction = { ctx -> ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            ),
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                description = "Sends gentle reminders when you're nearing your daily limit and daily/weekly usage reports.",
                icon = { Icon(Icons.Default.Notifications, null, tint = RegainTeal, modifier = Modifier.size(30.dp)) },
                isCritical = false,
                settingsAction = { ctx ->
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    })
                }
            )
        )
    }

    fun isGranted(id: String): Boolean = when (id) {
        "usage" -> hasUsageAccess
        "overlay" -> hasOverlay
        "accessibility" -> hasAccessibility
        "notifications" -> hasNotificationPermission
        else -> true
    }

    fun grant(permission: PermissionItem) {
        when (permission.id) {
            "accessibility" -> showAccessibilityDialog = true
            "notifications" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else permission.settingsAction(context)
            else -> permission.settingsAction(context)
        }
    }

    val allCriticalGranted = hasAccessibility && hasOverlay

    // 2 + 2 flow: Page1 = accessibility + overlay, Page2 = usage + notifications, Page3 = Summary
    val page1 = remember(permissions) { listOf(permissions[0], permissions[1]) }
    val page2 = remember(permissions) { listOf(permissions[2], permissions[3]) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val permRadial = remember {
        Brush.radialGradient(
            colors = listOf(DarkBackground, Color.Transparent),
            center = Offset(0.5f * 1000f, 0.45f * 1000f),
            radius = 900f
        )
    }
    val permCtaBrush = remember { Brush.horizontalGradient(listOf(RegainTeal, RegainTeal)) }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .background(permRadial)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header — left-aligned, fixed
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 12.dp)
            ) {
                Text(
                    "Permissions Required",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "SmartFocus needs these permissions to track usage and help you reduce screen time. Critical permissions are required.",
                    color = TextGray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Pager area — weight 1f, never overlaps bottom bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 0.dp
                ) { page ->
                    when (page) {
                        0 -> PagerPageTwoCards(
                            permissions = page1,
                            isGranted = { isGranted(it.id) },
                            onGrant = { grant(it) }
                        )
                        1 -> PagerPageTwoCards(
                            permissions = page2,
                            isGranted = { isGranted(it.id) },
                            onGrant = { grant(it) }
                        )
                        2 -> SummaryPage(
                            permissions = permissions,
                            isGranted = { isGranted(it.id) },
                            onGrant = { grant(it) },
                            allCriticalGranted = allCriticalGranted
                        )
                    }
                }
            }

            // Dots — 3 dots for Page1 / Page2 / Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) RegainTeal else TextGray.copy(alpha = 0.35f))
                    )
                    // Arrow between dots hint (teal)
                    if (index < 2) {
                        // tiny gap is enough; arrows are visual in mock but we keep dots minimal
                    }
                }
            }

            HorizontalDivider(color = DarkCardLight, thickness = 1.dp)

            // Bottom bar — fixed, navigationBarsPadding ensures never cut off
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextGray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Skip for Now", color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }

                val isSummaryPage = pagerState.currentPage == 2
                Button(
                    onClick = {
                        if (isSummaryPage) {
                            onContinue()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(
                            elevation = if (isSummaryPage && !allCriticalGranted) 0.dp else 4.dp,
                            shape = RoundedCornerShape(28.dp),
                            clip = false,
                            ambientColor = RegainTeal.copy(alpha = 0.25f),
                            spotColor = RegainTeal.copy(alpha = 0.25f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = if (isSummaryPage) allCriticalGranted else true
                ) {
                    val isDisabled = isSummaryPage && !allCriticalGranted
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (isDisabled) DarkCardLight else Color.Transparent)
                            .then(if (!isDisabled) Modifier.background(permCtaBrush, RoundedCornerShape(28.dp)) else Modifier)
                            .border(
                                1.dp,
                                if (isDisabled) DarkCardLight else Color.Transparent,
                                RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Continue",
                            color = if (isDisabled) TextGray.copy(alpha = 0.7f) else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }

    if (showAccessibilityDialog) {
        ComplianceDialog(
            title = "Accessibility Service",
            message = ACCESSIBILITY_DISCLOSURE,
            onConfirm = {
                showAccessibilityDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
}

@Composable
private fun PagerPageTwoCards(
    permissions: List<PermissionItem>,
    isGranted: (PermissionItem) -> Boolean,
    onGrant: (PermissionItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        permissions.forEach { perm ->
            PagerPermissionCard(
                permission = perm,
                isGranted = isGranted(perm),
                onGrant = { onGrant(perm) }
            )
        }
        // Extra bottom spacing so cards don't sit under dots
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PagerPermissionCard(
    permission: PermissionItem,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val cardCtaBrush = remember { Brush.horizontalGradient(listOf(RegainTeal, RegainTeal)) }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardLight),
        modifier = Modifier
            .fillMaxWidth()
            .glassGlow(
                color = RegainTeal,
                borderRadius = 20.dp,
                glowRadius = 10.dp,
                glowAlpha = 0.12f
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkCardLight)
                    .border(1.dp, DarkCardLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                permission.icon()
            }
            Spacer(Modifier.height(14.dp))
            Text(
                permission.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (permission.isCritical) {
                Spacer(Modifier.height(6.dp))
                RequiredBadge()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                permission.description,
                color = TextGray,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Permission granted", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = RegainTeal.copy(alpha = 0.22f),
                            spotColor = RegainTeal.copy(alpha = 0.22f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardCtaBrush, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Grant", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPage(
    permissions: List<PermissionItem>,
    isGranted: (PermissionItem) -> Boolean,
    onGrant: (PermissionItem) -> Unit,
    allCriticalGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        permissions.forEach { perm ->
            SummaryPermissionCard(
                permission = perm,
                isGranted = isGranted(perm),
                onGrant = { onGrant(perm) }
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (allCriticalGranted) "All critical permissions granted!" else "Accessibility + Overlay are required to block apps",
                    color = TextGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryPermissionCard(
    permission: PermissionItem,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val summaryCtaBrush = remember { Brush.horizontalGradient(listOf(RegainTeal, RegainTeal)) }
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCardLight)
                        .border(1.dp, DarkCardLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Scale icon down for summary
                    Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        permission.icon()
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            permission.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (permission.isCritical) RequiredBadgeCompact()
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isGranted) "Permission granted" else "Not yet granted",
                        color = if (isGranted) SuccessGreen else TextGray,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (isGranted) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                } else {
                    // Small Grant in summary not needed — Pager already has Grant, but keep for completeness
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(summaryCtaBrush)
                            .clickable { onGrant() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Grant", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(permission.description, color = TextGray, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun RequiredBadge() {
    Surface(
        color = ErrorRed.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f))
    ) {
        Text(
            "Required",
            color = ErrorRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RequiredBadgeCompact() {
    Surface(
        color = ErrorRed.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.22f))
    ) {
        Text(
            "Required",
            color = ErrorRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}
