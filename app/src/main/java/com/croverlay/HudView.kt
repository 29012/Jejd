package com.croverlay

import android.content.Context
import android.graphics.*
import android.view.View

class HudView(context: Context) : View(context) {

    private var recommendations: List<Recommendation> = emptyList()

    private val urgentPaint = Paint().apply {
        color = Color.argb(220, 255, 50, 50)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val highPaint = Paint().apply {
        color = Color.argb(200, 255, 165, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val medPaint = Paint().apply {
        color = Color.argb(180, 50, 220, 50)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val lowPaint = Paint().apply {
        color = Color.argb(150, 100, 180, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }
    private val smallTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val arrowPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val bubbleBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val statusBgPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
    }
    private val statusTextPaint = Paint().apply {
        color = Color.argb(220, 100, 255, 100)
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }

    fun update(recs: List<Recommendation>) {
        recommendations = recs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (recommendations.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // Arena occupies roughly middle 70% vertically (15% to 85%)
        val arenaTop = h * 0.15f
        val arenaBottom = h * 0.85f
        val arenaH = arenaBottom - arenaTop

        recommendations.take(2).forEach { rec ->
            val screenX = rec.placementX * w
            val screenY = arenaTop + rec.placementY * arenaH

            val paint = when (rec.priority) {
                Priority.URGENT -> urgentPaint
                Priority.HIGH   -> highPaint
                Priority.MEDIUM -> medPaint
                Priority.LOW    -> lowPaint
            }

            val radius = if (rec.priority == Priority.URGENT) 32f else 24f
            canvas.drawCircle(screenX, screenY, radius, paint)
            canvas.drawCircle(screenX, screenY, radius, borderPaint)

            // Arrow from card slot to placement
            val slotX = w * 0.1f + rec.card.slot * w * 0.2f
            val slotY = h * 0.93f
            canvas.drawLine(slotX, slotY, screenX, screenY, arrowPaint)

            // Reason bubble
            val bubbleX = screenX - 10f
            val bubbleY = screenY - radius - 10f
            val textWidth = smallTextPaint.measureText(rec.reason)
            val bubbleRect = RectF(bubbleX - 5f, bubbleY - 24f,
                bubbleX + textWidth + 5f, bubbleY + 4f)
            canvas.drawRoundRect(bubbleRect, 8f, 8f, bubbleBgPaint)
            canvas.drawText(rec.reason, bubbleX, bubbleY, smallTextPaint)

            val priorityLabel = when (rec.priority) {
                Priority.URGENT -> "⚠ СРОЧНО"
                Priority.HIGH   -> "▲ ВЫСОКИЙ"
                Priority.MEDIUM -> "● СРЕДНИЙ"
                Priority.LOW    -> "▽ ЦИКЛ"
            }
            canvas.drawText(priorityLabel, screenX - radius, screenY + radius + 20f, textPaint)
        }

        drawStatusBar(canvas, w, h)
    }

    private fun drawStatusBar(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRoundRect(RectF(w - 160f, 20f, w - 10f, 80f), 12f, 12f, statusBgPaint)
        canvas.drawText("CR Analyzer", w - 155f, 55f, statusTextPaint)
    }
}
