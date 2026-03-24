package com.linghualive.flamekit.feature.bookshelf.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import com.linghualive.flamekit.feature.reader.format.BookParserFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ImportBookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BookRepository,
    private val parserFactory: BookParserFactory,
) {

    suspend operator fun invoke(uri: Uri): Long = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "unknown.txt"
        val fileTitle = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "txt").lowercase()

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val destFile = File(booksDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val parser = parserFactory.getParser(fileName)
        val result = parser.parse(context, Uri.fromFile(destFile))

        val book = Book(
            title = result.title.ifEmpty { fileTitle },
            author = result.author ?: "",
            filePath = Uri.fromFile(destFile).toString(),
            format = extension,
            coverPath = result.coverPath,
            totalChapters = result.chapters.size,
        )

        repository.addBook(book)
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}
