package com.croverlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureEngine(
    private val context: Context,
    private val resultCode: Int,
    private val resultData: Intent,
    private val onFrame: (Bitmap) -> Unit
) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handlerThread = HandlerThread("ScreenCapture")
    private lateinit var handler: Handler
    private var lastCaptureTime = 0L
    private val captureIntervalMs = 600L

    fun start() {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        handler.post {
            try {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.getRealMetrics(metrics)
                val w = metrics.widthPixels
                val h = metrics.heightPixels
                val dpi = metrics.densityDpi
                val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                projection = manager.getMediaProjection(resultCode, resultData)
                val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
                reader.setOnImageAvailableListener({ r ->
                    val now = System.currentTimeMillis()
                    if (now - lastCaptureTime < captureIntervalMs) { r.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
                    lastCaptureTime = now
                    r.acquireLatestImage()?.use { img ->
                        try {
                            val plane = img.planes[0]
                            val bmpW = plane.rowStride / plane.pixelStride
                            val bmp = Bitmap.createBitmap(bmpW, h, Bitmap.Config.ARGB_8888)
                            bmp.copyPixelsFromBuffer(plane.buffer)
                            onFrame(if (bmpW != w) Bitmap.createBitmap(bmp, 0, 0, w, h) else bmp)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }, handler)
                imageReader = reader
                virtualDisplay = projection?.createVirtualDisplay(
                    "CRCapture", w, h, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, handler)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun stop() {
        try { virtualDisplay?.release(); imageReader?.close(); projection?.stop(); handlerThread.quitSafely() } catch (e: Exception) {}
    }
}
