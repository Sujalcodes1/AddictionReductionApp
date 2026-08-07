package com.example.addictionreductionapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = remember {
        listOf(
            Triple("home", "Home", Icons.Default.Home),
            Triple("timer", "Timer", Icons.Default.Timer),
            Triple("analytics", "Stats", Icons.Default.BarChart),
            Triple("coach", "Coach", Icons.Default.Psychology),
            Triple("settings", "Settings", Icons.Default.Settings)
        )
    }
    Box(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0F171E)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (route, label, icon) ->
                BottomNavItem(route = route, label = label, icon = icon, isSelected = currentRoute == route,
                    onNavigate = {
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })
            }
        }
    }
}

@Composable
fun BottomNavItem(
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onNavigate: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgAlpha = if (isSelected) 0.15f else 0f
    val iconColor = if (isSelected) Color(0xFF00BFA5) else Color.Gray
    val selectedColor = Color(0xFF00BFA5)

    Box(
        modifier = Modifier.clickable(indication = null, interactionSource = interactionSource, onClick = onNavigate),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
                .background(selectedColor.copy(alpha = bgAlpha)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
            if (isSelected) {
                Spacer(Modifier.width(5.dp))
                Text(text = label, color = selectedColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, softWrap = false)
            }
        }
    }
}
