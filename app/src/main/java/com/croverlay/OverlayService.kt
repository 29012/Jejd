package com.croverlay

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var hudView: HudView? = null
    private var captureEngine: ScreenCaptureEngine? = null
    private val strategyEngine = StrategyEngine()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        val mpCode = intent?.getIntExtra("mp_code", -1) ?: -1
        val mpIntent = intent?.getParcelableExtra<Intent>("mp_intent")
        Handler(Looper.getMainLooper()).postDelayed({
            try { setupOverlay(mpCode, mpIntent) } catch (e: Exception) { e.printStackTrace() }
        }, 500)
        return START_STICKY
    }

    private fun setupOverlay(mpCode: Int, mpIntent: Intent?) {
        if (!Settings.canDrawOverlays(this)) return
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
        try {
            val hud = HudView(this)
            windowManager.addView(hud, params)
            hudView = hud
            if (mpIntent != null && mpCode != -1) {
                val engine = ScreenCaptureEngine(this, mpCode, mpIntent) { bitmap ->
                    val state = VisionAnalyzer.analyze(bitmap)
                    val recs = strategyEngine.recommend(state)
                    Handler(Looper.getMainLooper()).post { hud.update(recs) }
                }
                engine.start()
                captureEngine = engine
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        captureEngine?.stop()
        try { hudView?.let { windowManager.removeView(it) } } catch (e: Exception) {}
    }

    private fun buildNotification(): Notification {
        val ch = "cr_overlay"
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(NotificationChannel(ch, "CR Analyzer", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, ch)
            .setContentTitle("CR Analyzer")
            .setContentText("Анализирую арену...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
