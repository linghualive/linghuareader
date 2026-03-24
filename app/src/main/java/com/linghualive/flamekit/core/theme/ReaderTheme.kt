package com.linghualive.flamekit.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.linghualive.flamekit.core.datastore.CustomReaderTheme
import com.linghualive.flamekit.core.datastore.ReaderThemeType

@Immutable
data class ReaderColors(
    val background: Color,
    val textColor: Color,
    val secondaryText: Color,
)

val LocalReaderColors = staticCompositionLocalOf {
    ReaderColors(
        background = Color.White,
        textColor = Color.Black,
        secondaryText = Color.Gray,
    )
}

fun readerColorsFor(themeType: ReaderThemeType, customTheme: CustomReaderTheme? = null): ReaderColors = when (themeType) {
    ReaderThemeType.LIGHT -> ReaderColors(
        background = Color(0xFFFFFFFF),
        textColor = Color(0xFF1C1B1F),
        secondaryText = Color(0xFF49454F),
    )
    ReaderThemeType.DARK -> ReaderColors(
        background = Color(0xFF1C1B1F),
        textColor = Color(0xFFE6E1E5),
        secondaryText = Color(0xFFCAC4D0),
    )
    ReaderThemeType.EYE_CARE -> ReaderColors(
        background = Color(0xFFCEEBCE),
        textColor = Color(0xFF1A2E1A),
        secondaryText = Color(0xFF3D5C3D),
    )
    ReaderThemeType.PARCHMENT -> ReaderColors(
        background = Color(0xFFF5E6C8),
        textColor = Color(0xFF3E2723),
        secondaryText = Color(0xFF5D4037),
    )
    ReaderThemeType.CUSTOM -> {
        val theme = customTheme ?: CustomReaderTheme()
        ReaderColors(
            background = Color(theme.backgroundColor.toULong()),
            textColor = Color(theme.textColor.toULong()),
            secondaryText = Color(theme.secondaryTextColor.toULong()),
        )
    }
}
