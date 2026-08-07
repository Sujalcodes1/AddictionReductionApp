package com.example.addictionreductionapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.ui.theme.*

@Composable
fun ComplianceDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, color = TextGray, fontSize = 14.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RegainTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continue", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp)
    )
}

val ACCESSIBILITY_DISCLOSURE = """SmartFocus uses the Accessibility Service to detect when social or distracting apps are opened, so it can show you a focus reminder or block the app based on your limits.

No personal data is collected — we only check the package name of the currently open app. No keystrokes, screen content, or personal information is accessed."""

val NOTIFICATION_DISCLOSURE = """SmartFocus can optionally monitor notification frequency to help you understand which apps trigger the most distractions.

No notification content is read — only the count per app is recorded. This feature is optional and can be disabled at any time."""
