package com.linghualive.flamekit.feature.reader.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.min

data class FlipState(
    val pathA: Path,
    val pathB: Path,
    val pathC: Path,
    val shadowPath: Path,
    val cornerX: Float,
    val cornerY: Float,
    val progress: Float,
)

class SimulationPageFlip {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()

    fun calculateFlipState(
        touchX: Float,
        touchY: Float,
        pageWidth: Float,
        pageHeight: Float,
    ): FlipState {
        // Clamp touch point
        val tx = touchX.coerceIn(0f, pageWidth)
        val ty = touchY.coerceIn(0f, pageHeight)

        // Corner origin (always right edge)
        val cornerX = pageWidth
        val cornerY = if (ty < pageHeight / 2) 0f else pageHeight

        val progress = 1f - (tx / pageWidth).coerceIn(0f, 1f)

        // Build simplified paths for the three regions
        val pathA = Path() // Current page visible area
        val pathB = Path() // Folded page (back side)
        val pathC = Path() // Next page revealed area

        // Simplified fold simulation using a diagonal line
        val foldLeft = tx.coerceIn(0f, pageWidth)
        val foldTop = 0f
        val foldBottom = pageHeight

        // Path C: next page area (right of fold line, behind current page)
        pathC.apply {
            moveTo(foldLeft, foldTop)
            lineTo(pageWidth, foldTop)
            lineTo(pageWidth, foldBottom)
            lineTo(foldLeft, foldBottom)
            close()
        }

        // Path A: current page remaining area (left of fold line)
        pathA.apply {
            moveTo(0f, foldTop)
            lineTo(foldLeft, foldTop)
            lineTo(foldLeft, foldBottom)
            lineTo(0f, foldBottom)
            close()
        }

        // Path B: folded part (mirrored)
        val foldWidth = min(pageWidth - foldLeft, foldLeft)
        pathB.apply {
            moveTo(foldLeft, foldTop)
            lineTo(foldLeft + foldWidth * 0.6f, foldTop)
            lineTo(foldLeft + foldWidth * 0.6f, foldBottom)
            lineTo(foldLeft, foldBottom)
            close()
        }

        // Shadow along the fold line
        val shadowPath = Path().apply {
            moveTo(foldLeft - 15f, foldTop)
            lineTo(foldLeft + 15f, foldTop)
            lineTo(foldLeft + 15f, foldBottom)
            lineTo(foldLeft - 15f, foldBottom)
            close()
        }

        return FlipState(
            pathA = pathA,
            pathB = pathB,
            pathC = pathC,
            shadowPath = shadowPath,
            cornerX = cornerX,
            cornerY = cornerY,
            progress = progress,
        )
    }

    fun draw(
        canvas: Canvas,
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        flipState: FlipState,
        pageWidth: Float,
    ) {
        val foldX = pageWidth * (1f - flipState.progress)

        // 1. Draw next page (revealed area)
        canvas.save()
        canvas.clipPath(flipState.pathC)
        canvas.drawBitmap(backBitmap, 0f, 0f, paint)
        canvas.restore()

        // 2. Draw current page (remaining area)
        canvas.save()
        canvas.clipPath(flipState.pathA)
        canvas.drawBitmap(frontBitmap, 0f, 0f, paint)
        canvas.restore()

        // 3. Draw folded back with darkened effect
        canvas.save()
        canvas.clipPath(flipState.pathB)
        // Mirror the front bitmap for the fold back
        matrix.reset()
        matrix.preScale(-1f, 1f, foldX, 0f)
        canvas.drawBitmap(frontBitmap, matrix, paint)
        // Darken the fold
        paint.color = Color.argb(40, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawPath(flipState.pathB, paint)
        paint.color = Color.TRANSPARENT
        canvas.restore()

        // 4. Draw shadow along fold
        canvas.save()
        canvas.clipPath(flipState.shadowPath)
        shadowPaint.shader = LinearGradient(
            foldX - 15f, 0f, foldX + 15f, 0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(60, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(flipState.shadowPath, shadowPaint)
        shadowPaint.shader = null
        canvas.restore()
    }
}
