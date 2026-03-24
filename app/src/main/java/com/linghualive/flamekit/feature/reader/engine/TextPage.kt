package com.linghualive.flamekit.feature.reader.engine

data class TextPage(
    val index: Int,
    val lines: List<TextLine>,
    val chapterIndex: Int,
    val chapterTitle: String,
)

data class TextLine(
    val text: String,
    val y: Float,
    val isTitle: Boolean = false,
    val indent: Float = 0f,
)
