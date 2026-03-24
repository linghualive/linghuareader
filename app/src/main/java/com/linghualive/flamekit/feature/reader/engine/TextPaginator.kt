package com.linghualive.flamekit.feature.reader.engine

import android.text.TextPaint

data class PaginateConfig(
    val pageWidth: Int,
    val pageHeight: Int,
    val fontSize: Float,
    val lineSpacingMultiplier: Float,
    val paragraphIndent: Int,
    val marginHorizontal: Int,
    val marginVertical: Int,
    val titleFontSize: Float,
)

class TextPaginator {

    fun paginate(
        text: String,
        chapterIndex: Int,
        chapterTitle: String,
        config: PaginateConfig,
    ): List<TextPage> {
        val paint = TextPaint().apply {
            textSize = config.fontSize
            isAntiAlias = true
        }

        val contentWidth = config.pageWidth - config.marginHorizontal * 2
        val contentHeight = config.pageHeight - config.marginVertical * 2
        val lineHeight = paint.fontMetrics.let { it.descent - it.ascent } * config.lineSpacingMultiplier

        val paragraphs = text.split("\n").filter { it.isNotBlank() }
        val allLines = mutableListOf<TextLine>()

        // Chapter title as the first line
        val titlePaint = TextPaint().apply {
            textSize = config.titleFontSize
            isAntiAlias = true
            isFakeBoldText = true
        }
        val titleLineHeight = titlePaint.fontMetrics.let { it.descent - it.ascent } * config.lineSpacingMultiplier

        // Break title into lines if needed
        var remainingTitle = chapterTitle
        while (remainingTitle.isNotEmpty()) {
            val count = titlePaint.breakText(remainingTitle, true, contentWidth.toFloat(), null)
            if (count == 0) break
            allLines.add(TextLine(text = remainingTitle.substring(0, count), y = 0f, isTitle = true))
            remainingTitle = remainingTitle.substring(count)
        }
        // Add an empty line after title
        allLines.add(TextLine(text = "", y = 0f))

        val indentStr = "\u3000".repeat(config.paragraphIndent)
        val indentWidth = paint.measureText(indentStr)

        for (paragraph in paragraphs) {
            val trimmed = paragraph.trim()
            var remaining = trimmed
            var isFirstLine = true

            while (remaining.isNotEmpty()) {
                val availableWidth = if (isFirstLine) contentWidth - indentWidth else contentWidth.toFloat()
                val count = paint.breakText(remaining, true, availableWidth, null)
                if (count == 0) break
                val lineText = remaining.substring(0, count)
                allLines.add(
                    TextLine(
                        text = lineText,
                        y = 0f,
                        indent = if (isFirstLine) indentWidth else 0f,
                    )
                )
                remaining = remaining.substring(count)
                isFirstLine = false
            }
        }

        // Paginate by page height
        val pages = mutableListOf<TextPage>()
        var currentLines = mutableListOf<TextLine>()
        var currentY = config.marginVertical.toFloat()

        for (line in allLines) {
            val lh = if (line.isTitle) titleLineHeight else lineHeight
            if (currentY + lh > config.pageHeight - config.marginVertical && currentLines.isNotEmpty()) {
                // Finalize current page
                pages.add(
                    TextPage(
                        index = pages.size,
                        lines = currentLines.toList(),
                        chapterIndex = chapterIndex,
                        chapterTitle = chapterTitle,
                    )
                )
                currentLines = mutableListOf()
                currentY = config.marginVertical.toFloat()
            }
            currentLines.add(line.copy(y = currentY - paint.fontMetrics.ascent))
            currentY += lh
        }

        // Last page
        if (currentLines.isNotEmpty()) {
            pages.add(
                TextPage(
                    index = pages.size,
                    lines = currentLines.toList(),
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                )
            )
        }

        if (pages.isEmpty()) {
            pages.add(
                TextPage(
                    index = 0,
                    lines = emptyList(),
                    chapterIndex = chapterIndex,
                    chapterTitle = chapterTitle,
                )
            )
        }

        return pages
    }
}
