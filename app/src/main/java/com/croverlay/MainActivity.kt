package com.croverlay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_OVERLAY = 1001
        const val REQUEST_PROJECTION = 1002
    }

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        statusText = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnStart).setOnClickListener { checkAndStart() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopOverlay() }
    }

    private fun checkAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "Нужно разрешение на отображение поверх других окон"
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY)
        } else {
            requestProjection()
        }
    }

    private fun requestProjection() {
        statusText.text = "Запрос захвата экрана..."
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_PROJECTION)
    }

    private fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
        statusText.text = "Остановлено"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY -> {
                if (Settings.canDrawOverlays(this)) requestProjection()
                else statusText.text = "Разрешение отклонено"
            }
            REQUEST_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val intent = Intent(this, OverlayService::class.java).apply {
                        putExtra("mp_code", resultCode)
                        putExtra("mp_intent", data)
                    }
                    startForegroundService(intent)
                    statusText.text = "Анализатор запущен ✓"
                }
            }
        }
    }
}
