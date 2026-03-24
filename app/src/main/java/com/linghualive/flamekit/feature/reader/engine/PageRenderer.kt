package com.linghualive.flamekit.feature.reader.engine

import android.graphics.Canvas
import android.text.TextPaint

class PageRenderer {

    private val paint = TextPaint().apply {
        isAntiAlias = true
    }

    fun render(
        canvas: Canvas,
        page: TextPage,
        config: PaginateConfig,
        textColor: Int,
        backgroundColor: Int,
        titleColor: Int,
    ) {
        canvas.drawColor(backgroundColor)

        paint.textSize = config.fontSize
        paint.color = textColor

        for (line in page.lines) {
            if (line.isTitle) {
                paint.textSize = config.titleFontSize
                paint.isFakeBoldText = true
                paint.color = titleColor
                canvas.drawText(line.text, config.marginHorizontal.toFloat(), line.y, paint)
                paint.textSize = config.fontSize
                paint.isFakeBoldText = false
                paint.color = textColor
            } else if (line.text.isNotEmpty()) {
                val x = config.marginHorizontal + line.indent
                canvas.drawText(line.text, x, line.y, paint)
            }
        }

        // Page number at bottom
        paint.textSize = config.fontSize * 0.7f
        paint.color = (textColor and 0x00FFFFFF) or 0x80000000.toInt()
        val pageInfo = "${page.chapterTitle}  ${page.index + 1}"
        val infoWidth = paint.measureText(pageInfo)
        canvas.drawText(
            pageInfo,
            (config.pageWidth - infoWidth) / 2,
            config.pageHeight - config.marginVertical / 2f,
            paint,
        )

        // Restore paint
        paint.textSize = config.fontSize
        paint.color = textColor
    }
}
