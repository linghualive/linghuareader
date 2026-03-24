package com.linghualive.flamekit.feature.reader.engine

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class FontInfo(
    val id: String,
    val displayName: String,
    val typeface: Typeface?,
)

@Singleton
class FontManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val builtInFonts = listOf(
        FontInfo("default", "系统默认", null),
        FontInfo("serif", "衬线体", Typeface.SERIF),
        FontInfo("sans_serif", "无衬线体", Typeface.SANS_SERIF),
        FontInfo("monospace", "等宽体", Typeface.MONOSPACE),
    )

    private val customFontDir: File
        get() = File(context.filesDir, "fonts").also { it.mkdirs() }

    fun loadFont(fontFamily: String): Typeface {
        // Check built-in fonts first
        builtInFonts.find { it.id == fontFamily }?.let { info ->
            return info.typeface ?: Typeface.DEFAULT
        }

        // Check custom fonts
        val fontFile = File(customFontDir, fontFamily)
        if (fontFile.exists()) {
            return Typeface.createFromFile(fontFile)
        }

        return Typeface.DEFAULT
    }

    fun getCustomFonts(): List<FontInfo> {
        val dir = customFontDir
        if (!dir.exists()) return emptyList()

        return dir.listFiles()
            ?.filter { it.extension.lowercase() in listOf("ttf", "otf", "ttc") }
            ?.map { file ->
                FontInfo(
                    id = file.name,
                    displayName = file.nameWithoutExtension,
                    typeface = try {
                        Typeface.createFromFile(file)
                    } catch (_: Exception) {
                        null
                    },
                )
            }
            ?: emptyList()
    }

    fun getAllFonts(): List<FontInfo> = builtInFonts + getCustomFonts()

    suspend fun importFont(uri: Uri): FontInfo = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open font URI")

        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_font.ttf"
        val targetFile = File(customFontDir, fileName)

        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val typeface = try {
            Typeface.createFromFile(targetFile)
        } catch (e: Exception) {
            targetFile.delete()
            throw IllegalArgumentException("Invalid font file", e)
        }

        FontInfo(
            id = fileName,
            displayName = targetFile.nameWithoutExtension,
            typeface = typeface,
        )
    }
}
