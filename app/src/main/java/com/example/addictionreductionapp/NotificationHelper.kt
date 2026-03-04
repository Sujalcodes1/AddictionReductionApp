package com.example.addictionreductionapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_DAILY = "daily_report"
    const val CHANNEL_WEEKLY = "weekly_report"
    const val CHANNEL_MONTHLY = "monthly_report"
    const val CHANNEL_NUDGE = "live_nudge"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily screentime reports" }

            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY,
                "Weekly Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Weekly screentime reports" }

            val monthlyChannel = NotificationChannel(
                CHANNEL_MONTHLY,
                "Monthly Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Monthly screentime reports" }

            val nudgeChannel = NotificationChannel(
                CHANNEL_NUDGE,
                "Nudges",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Hourly motivational nudges" }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(dailyChannel)
            notificationManager.createNotificationChannel(weeklyChannel)
            notificationManager.createNotificationChannel(monthlyChannel)
            notificationManager.createNotificationChannel(nudgeChannel)
        }
    }

    fun sendNotification(context: Context, channelId: String, notifId: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Or any valid icon like R.drawable.ic_notification
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (channelId == CHANNEL_NUDGE) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notifId, builder.build())
    }
}
