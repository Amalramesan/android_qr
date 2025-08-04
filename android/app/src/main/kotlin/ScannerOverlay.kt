package com.example.android_qr


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val transparentPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw semi-transparent background
        val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bgCanvas = Canvas(background)
        bgCanvas.drawColor(Color.parseColor("#A6000000"))

        // Define the rectangle scanning area
        val frameWidth = width * 0.8f
        val frameHeight = height * 0.35f
        val left = (width - frameWidth) / 2
        val top = (height - frameHeight) / 2
        val right = left + frameWidth
        val bottom = top + frameHeight
        val rect = RectF(left, top, right, bottom)

        // Cut out the center
        bgCanvas.drawRoundRect(rect, 30f, 30f, transparentPaint)

        // Draw the white border
        bgCanvas.drawRoundRect(rect, 30f, 30f, paint)

        canvas.drawBitmap(background, 0f, 0f, null)
    }
}
