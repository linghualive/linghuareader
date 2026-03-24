package com.linghualive.flamekit.feature.reader.format

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PdfParser @Inject constructor() : BookParser {

    override suspend fun parse(context: Context, uri: Uri): ParseResult =
        withContext(Dispatchers.IO) {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext ParseResult(
                    title = "PDF",
                    author = null,
                    chapters = emptyList(),
                )

            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount

            val chapters = (0 until pageCount).map { i ->
                Chapter(
                    index = i,
                    title = "\u7B2C ${i + 1} \u9875",
                    content = "",
                    startOffset = i.toLong(),
                )
            }

            renderer.close()
            fd.close()

            val fileName = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringBeforeLast(".")
                ?: "PDF\u6587\u6863"

            ParseResult(
                title = fileName,
                author = null,
                chapters = chapters,
                coverPath = null,
            )
        }
}
