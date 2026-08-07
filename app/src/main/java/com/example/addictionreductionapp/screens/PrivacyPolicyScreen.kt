package com.example.addictionreductionapp.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.addictionreductionapp.ui.theme.*
import com.example.addictionreductionapp.R

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val privacyUrl = context.getString(R.string.privacy_policy_url)

    Column(
        Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("Privacy Policy", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Last Updated: August 2026", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))

                    SectionTitle("1. Information We Collect")
                    SectionBody(
                        "We collect the following types of information to provide and improve our service:\n\n" +
                        "• Account Information: Email address and display name provided during registration.\n" +
                        "• Usage Data: App usage statistics including which apps you use, duration, open counts, and usage patterns.\n" +
                        "• Focus Session Data: Timestamps and durations of focus sessions you complete.\n" +
                        "• Goals & Preferences: Personal goals, app block settings, and screen-time targets.\n" +
                        "• AI Coach Conversations: Messages you send to and receive from the AI coach feature.\n" +
                        "• Device Information: Device model, OS version, and app version for crash reporting."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("2. How We Use Your Information")
                    SectionBody(
                        "We use the collected data for the following purposes:\n\n" +
                        "• To provide personalized screen-time insights and usage analytics.\n" +
                        "• To power the Smart Reduction engine that gradually reduces your screen time.\n" +
                        "• To enable AI-powered coaching that adapts to your behavior patterns.\n" +
                        "• To maintain your account and preferences across devices.\n" +
                        "• To improve our service through anonymous usage analytics.\n" +
                        "• To comply with legal obligations."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("3. Data Storage & Security")
                    SectionBody(
                        "Your data is protected by industry-standard security measures:\n\n" +
                        "• Authentication tokens are encrypted using Android Keystore (AES-256 GCM).\n" +
                        "• Local data on your device is encrypted using SQLCipher (AES-256).\n" +
                        "• Data in transit is encrypted using HTTPS with certificate pinning.\n" +
                        "• Cloud data is stored on Supabase, a SOC 2 compliant infrastructure provider.\n" +
                        "• AI requests are proxied through our secure backend — no API keys are ever stored on your device."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("4. Data Sharing")
                    SectionBody(
                        "We do NOT sell your personal data. Your app usage data, focus sessions, and AI conversations are private to your account. We share data only in the following limited cases:\n\n" +
                        "• With Supabase (our cloud database provider) solely for data storage.\n" +
                        "• With Google Gemini API (our AI provider) solely to generate AI coach responses — and only through our secure backend proxy.\n" +
                        "• When required by law or to protect our rights."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("5. Your Rights")
                    SectionBody(
                        "You have the following rights regarding your data:\n\n" +
                        "• Access: You can view your usage data, goals, and chat history within the app at any time.\n" +
                        "• Deletion: You can delete your account and all associated data from the Profile screen. This permanently removes your account from Supabase and clears all local data.\n" +
                        "• Export: You may request a copy of your data by contacting us.\n" +
                        "• Correction: You can update your display name and preferences in the app settings.\n" +
                        "• Withdraw Consent: You can revoke accessibility and usage access permissions at any time through Android Settings."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("6. Accessibility Service")
                    SectionBody(
                        "FocusShield uses the Android Accessibility Service for two purposes:\n\n" +
                        "1. Usage Tracking: Detecting which app is currently in the foreground to record accurate screen time.\n" +
                        "2. App Blocking: Detecting when a restricted app is opened to show a focus reminder or blocking overlay.\n\n" +
                        "We access ONLY the package name of the foreground app. We do NOT read screen content, keystrokes, passwords, messages, or any personal information displayed on your screen. Both services are configured with canRetrieveWindowContent=\"false\"."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("7. Children's Privacy")
                    SectionBody(
                        "Our service is not directed to children under the age of 13. We do not knowingly collect personal information from children under 13. If you believe a child has provided us with personal information, please contact us so we can delete it."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("8. Changes to This Policy")
                    SectionBody(
                        "We may update this privacy policy from time to time. We will notify you of any changes by posting the new policy within the app. Your continued use of FocusShield after changes are posted constitutes acceptance of the updated policy."
                    )

                    Spacer(Modifier.height(16.dp))
                    SectionTitle("9. Contact Us")
                    SectionBody(
                        "If you have questions about this privacy policy or wish to exercise your data rights, please contact us at:\n\n" +
                        "Email: privacy@smartfocus.app\n" +
                        "Website: https://smartfocus.app/privacy"
                    )
                }
            }

            // Open in browser button
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 24.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(RegainTeal.copy(alpha = 0.4f))
                )
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = RegainTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Full Policy in Browser", color = RegainTeal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionBody(text: String) {
    Text(text, color = TextGray, fontSize = 13.sp, lineHeight = 20.sp)
}
