package com.linghualive.flamekit.feature.reader.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

class CoverPageFlip {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Draw cover flip effect.
     * @param offsetX 0 = fully showing current page, -pageWidth = fully showing next page
     */
    fun draw(
        canvas: Canvas,
        currentBitmap: Bitmap,
        nextBitmap: Bitmap,
        offsetX: Float,
        pageWidth: Float,
        pageHeight: Float,
    ) {
        // 1. Draw the next page (static, underneath)
        canvas.drawBitmap(nextBitmap, 0f, 0f, paint)

        // 2. Draw the current page sliding away
        canvas.save()
        canvas.translate(offsetX, 0f)
        canvas.drawBitmap(currentBitmap, 0f, 0f, paint)

        // 3. Draw shadow at the right edge of the sliding page
        val shadowWidth = 30f
        shadowPaint.shader = LinearGradient(
            pageWidth, 0f,
            pageWidth + shadowWidth, 0f,
            Color.argb(80, 0, 0, 0),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(pageWidth, 0f, pageWidth + shadowWidth, pageHeight, shadowPaint)
        shadowPaint.shader = null
        canvas.restore()
    }
}
