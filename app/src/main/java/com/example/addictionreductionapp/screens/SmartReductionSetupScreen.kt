package com.example.addictionreductionapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.addictionreductionapp.viewmodel.CategoryOption
import com.example.addictionreductionapp.viewmodel.SmartReductionSetupViewModel
import com.example.addictionreductionapp.ui.theme.*

@Composable
fun SmartReductionSetupScreen(
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    viewModel: SmartReductionSetupViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val hasRealData by viewModel.hasRealData.collectAsState()

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
                    "Smart Reduction",
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                if (hasRealData) {
                    Text(
                        "We analyzed your past week of app usage. Choose which categories to gradually reduce.",
                        color = TextGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                } else {
                    Text(
                        "No historical data found. Default baselines are shown — grant Usage Access in Settings for personalized targets.",
                        color = RegainAmber,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RegainTeal)
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                categories.forEach { option ->
                    CategoryCard(
                        option = option,
                        onToggle = { viewModel.toggleCategory(option.category) },
                        onStepDownChange = { viewModel.setStepDown(option.category, it) }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = RegainTeal.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Reduction Plan",
                            color = RegainTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            summary,
                            color = TextGray,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
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
                Text("Skip", color = TextGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = { viewModel.confirmPlans(onComplete) },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading
            ) {
                Text(
                    "Start Plan",
                    color = DarkBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    option: CategoryOption,
    onToggle: () -> Unit,
    onStepDownChange: (Int) -> Unit
) {
    val stepDownOptions = listOf(5, 10, 15, 20)
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (option.isSelected) DarkCard else DarkCard.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = RegainTeal,
                        uncheckedColor = TextGray
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        option.category,
                        color = if (option.isSelected) TextWhite else TextGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val h = option.averageMinutes / 60
                    val m = option.averageMinutes % 60
                    Text(
                        if (h > 0) "${h}h ${m}m daily average" else "${m}m daily average",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                if (option.isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RegainTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (option.isSelected) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardLight, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reduce by:", color = TextGray, fontSize = 12.sp)

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(TextGray.copy(alpha = 0.3f))
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${option.stepDown} min/day",
                                color = RegainTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = RegainTeal,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            stepDownOptions.forEach { step ->
                                DropdownMenuItem(
                                    text = { Text("${step} min/day") },
                                    onClick = {
                                        onStepDownChange(step)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
