package com.example.addictionreductionapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class BlockOverlayService : Service() {

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_REASON = "reason"
        const val TAG = "BlockOverlay"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "block_overlay"
    }

    private var overlayView: FrameLayout? = null
    private lateinit var windowManager: WindowManager
    private var appName: String = ""
    private var reason: String = ""
    private var returnButton: Button? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        reason = intent?.getStringExtra(EXTRA_REASON) ?: "limit"
        showOverlay()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocking",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown when an app is blocked" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showOverlay() {
        try {
            removeOverlay()

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("$appName is blocked")
                .setContentText("Tap to return to SmartFocus")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)

            overlayView = FrameLayout(this).apply {
                setBackgroundColor(0xCC0D1521.toInt())
                isFocusableInTouchMode = true
                isFocusable = true
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        true
                    } else false
                }
            }

            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }

            val titleText = TextView(this).apply {
                text = when (reason) {
                    "focus" -> "Focus Mode Active"
                    "schedule" -> "Scheduled Block"
                    else -> "Limit Reached"
                }
                textSize = 24f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
            }
            contentLayout.addView(titleText)

            val messageText = TextView(this).apply {
                text = "$appName is restricted right now"
                textSize = 14f
                setTextColor(0xFF8899AA.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 32)
            }
            contentLayout.addView(messageText)

            val closeButton = Button(this).apply {
                text = "Please wait..."
                isEnabled = false
                setOnClickListener {
                    val homeIntent = Intent(this@BlockOverlayService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(homeIntent)
                    stopSelf()
                }
            }
            returnButton = closeButton
            contentLayout.addView(closeButton)

            overlayView?.addView(contentLayout)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(overlayView, params)

            handler.postDelayed({
                returnButton?.apply {
                    text = "Return to SmartFocus"
                    isEnabled = true
                }
            }, 5000)

            Log.i(TAG, "Overlay shown for $appName ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            stopSelf()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
        Log.i(TAG, "BlockOverlayService destroyed")
    }
}
