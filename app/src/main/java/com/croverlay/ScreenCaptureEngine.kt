package com.croverlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics

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

    private val metrics = DisplayMetrics().also {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
            .defaultDisplay.getRealMetrics(it)
    }

    private val width = metrics.widthPixels
    private val height = metrics.heightPixels
    private val dpi = metrics.densityDpi

    // Capture every 500ms — enough for real-time analysis without draining battery
    private val captureIntervalMs = 500L
    private var lastCaptureTime = 0L

    fun start() {
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, resultData)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastCaptureTime < captureIntervalMs) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastCaptureTime = now

            val image: Image? = reader.acquireLatestImage()
            image?.use { img ->
                val plane = img.planes[0]
                val bitmap = Bitmap.createBitmap(
                    plane.rowStride / plane.pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(plane.buffer)
                // Crop to actual width if row stride differs
                val cropped = if (bitmap.width != width)
                    Bitmap.createBitmap(bitmap, 0, 0, width, height)
                else bitmap
                onFrame(cropped)
            }
        }, handler)

        virtualDisplay = projection?.createVirtualDisplay(
            "CRCapture", width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )
    }

    fun stop() {
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        handlerThread.quitSafely()
    }
}
