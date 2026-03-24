package com.linghualive.flamekit.feature.reader.format

import javax.inject.Inject

class BookParserFactory @Inject constructor(
    private val txtParser: TxtParser,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
) {

    fun getParser(fileName: String): BookParser {
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "epub" -> epubParser
            "pdf" -> pdfParser
            else -> txtParser
        }
    }
}
