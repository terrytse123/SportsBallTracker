package com.example.sportsballtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject

class TrackingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 40f
        isAntiAlias = true
    }

    private var detectedObjects: List<DetectedObject> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1

    fun updateDetections(objects: List<DetectedObject>, imgWidth: Int, imgHeight: Int) {
        detectedObjects = objects
        imageWidth = imgWidth
        imageHeight = imgHeight
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detectedObjects.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (obj in detectedObjects) {
            val box = obj.boundingBox
            val rect = RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            )
            canvas.drawRect(rect, paint)

            val label = "ID:${obj.trackingId ?: "-"}"
            canvas.drawText(label, rect.left, rect.top - 10, textPaint)
        }
    }
}
