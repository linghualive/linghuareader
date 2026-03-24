package com.linghualive.flamekit.feature.reader.format

data class Chapter(
    val index: Int,
    val title: String,
    val content: String,
    val startOffset: Long,
)
