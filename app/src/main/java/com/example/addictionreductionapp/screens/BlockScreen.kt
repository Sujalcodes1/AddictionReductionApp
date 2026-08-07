package com.example.addictionreductionapp.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.InterventionRepositoryEntryPoint
import kotlinx.coroutines.launch
import com.example.addictionreductionapp.ui.theme.DarkBackground
import com.example.addictionreductionapp.ui.theme.DarkCard
import com.example.addictionreductionapp.ui.theme.DarkCardLight
import com.example.addictionreductionapp.ui.theme.ErrorRed
import com.example.addictionreductionapp.ui.theme.RegainOrange
import com.example.addictionreductionapp.ui.theme.RegainTeal
import com.example.addictionreductionapp.ui.theme.SuccessGreen
import com.example.addictionreductionapp.ui.theme.TextGray
import com.example.addictionreductionapp.ui.theme.TextWhite

@Composable
fun BlockScreen(
    appName: String = "",
    reason: String = "",
    onExit: () -> Unit,
    onStartFocus: () -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            InterventionRepositoryEntryPoint::class.java
        ).interventionRepository()
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val displayName = appName.ifEmpty { "This app" }

    val (title, message, icon) = when (reason) {
        "focus" -> Triple("Focus Mode Active", "$displayName is blocked during Focus Mode", Icons.Default.Shield)
        "schedule" -> Triple("Scheduled Block", "$displayName is blocked during scheduled hours", Icons.Default.Schedule)
        else -> Triple("Limit Reached", "You've reached your $displayName limit", Icons.Default.Block)
    }

    val accentColor = when (reason) {
        "focus" -> RegainTeal
        "schedule" -> RegainOrange
        else -> ErrorRed
    }

    val tabs = listOf("Breathe", "Journal", "Affirm")
    val scope = rememberCoroutineScope()

    var breathePhase by remember { mutableStateOf(false) }
    val breatheScale by animateFloatAsState(
        targetValue = if (breathePhase) 1f else 0.6f,
        animationSpec = tween(4000, easing = androidx.compose.animation.core.LinearEasing),
        label = "breathe"
    )
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            while (selectedTab == 0) {
                breathePhase = true; kotlinx.coroutines.delay(4000)
                breathePhase = false; kotlinx.coroutines.delay(4000)
            }
        }
    }

    var journalText by remember { mutableStateOf("") }
    var journalSaved by remember { mutableStateOf(false) }

    val affirmations = remember {
        listOf(
            "I am in control of my choices.",
            "Every moment of restraint makes me stronger.",
            "I choose long-term growth over short-term pleasure.",
            "My focus is my superpower.",
            "I am building the person I want to become."
        )
    }
    var affirmationIndex by remember { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize().background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = accentColor.copy(alpha = 0.15f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, tab ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedTab = i; journalSaved = false },
                        color = if (selectedTab == i) accentColor.copy(alpha = 0.15f) else DarkCard,
                        shape = RoundedCornerShape(10.dp),
                        border = if (selectedTab == i) BorderStroke(1.dp, accentColor) else null
                    ) {
                        Text(tab, color = if (selectedTab == i) accentColor else TextGray, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                when (selectedTab) {
                    0 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(120.dp).scale(breatheScale.toFloat())
                                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                                .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(if (breathePhase) "Breathe in" else "Breathe out", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Follow the circle", color = TextGray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { scope.launch { repo.log("breathing", appName) } }) {
                                Text("Log this exercise", color = accentColor, fontSize = 12.sp)
                            }
                        }
                    }
                    1 -> {
                        Column(Modifier.fillMaxWidth()) {
                            if (journalSaved) {
                                Text("Entry saved!", color = SuccessGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Reflection helps build awareness.", color = TextGray, fontSize = 13.sp)
                            } else {
                                OutlinedTextField(value = journalText, onValueChange = { journalText = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    placeholder = { Text("What triggered this? How do you feel?", color = TextGray) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = DarkCardLight,
                                        focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = accentColor),
                                    shape = RoundedCornerShape(12.dp), maxLines = 4)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { scope.launch { repo.log("journal", appName, journalText) }; journalSaved = true; journalText = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(10.dp),
                                    enabled = journalText.isNotBlank()) {
                                    Text("Save Entry", color = DarkBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))) {
                                Text(affirmations[affirmationIndex], color = TextWhite, fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium, lineHeight = 24.sp, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { scope.launch { repo.log("affirmation", appName) }; affirmationIndex = (affirmationIndex + 1) % affirmations.size },
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), shape = RoundedCornerShape(20.dp)) {
                                    Text("Next", color = accentColor, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            OutlinedButton(onClick = { onStartFocus() }, modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, RegainTeal.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp)) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start a Focus Session", color = RegainTeal, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp).shadow(
                    elevation = 12.dp, shape = RoundedCornerShape(12.dp), clip = false,
                    ambientColor = accentColor.copy(alpha = 0.5f), spotColor = accentColor.copy(alpha = 0.5f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp)) {
                Text("Return Home", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
