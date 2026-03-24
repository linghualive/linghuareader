package com.linghualive.flamekit.feature.reader.format

import android.content.Context
import android.net.Uri

interface BookParser {
    suspend fun parse(context: Context, uri: Uri): ParseResult
}

data class ParseResult(
    val title: String,
    val author: String?,
    val chapters: List<Chapter>,
    val coverPath: String? = null,
)
