package com.croverlay

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var hudView: HudView
    private lateinit var captureEngine: ScreenCaptureEngine
    private val strategyEngine = StrategyEngine()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        hudView = HudView(this)
        windowManager.addView(hudView, params)

        val mpCode = intent?.getIntExtra("mp_code", -1) ?: -1
        val mpIntent = intent?.getParcelableExtra<Intent>("mp_intent")

        if (mpIntent != null && mpCode != -1) {
            captureEngine = ScreenCaptureEngine(this, mpCode, mpIntent) { bitmap ->
                val state = VisionAnalyzer.analyze(bitmap)
                val recs = strategyEngine.recommend(state)
                hudView.post { hudView.update(recs) }
            }
            captureEngine.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        if (::captureEngine.isInitialized) captureEngine.stop()
        if (::hudView.isInitialized) windowManager.removeView(hudView)
    }

    private fun buildNotification(): Notification {
        val channelId = "cr_overlay"
        val channel = NotificationChannel(channelId, "CR Analyzer", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("CR Analyzer активен")
            .setContentText("Анализирую арену...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
