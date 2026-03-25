package com.linghualive.flamekit.core.datastore

import kotlinx.serialization.Serializable

@Serializable
data class ReadingPreferences(
    val fontSize: Int = 24,
    val lineSpacingMultiplier: Float = 1.5f,
    val pageMode: PageMode = PageMode.SCROLL,
    val readerTheme: ReaderThemeType = ReaderThemeType.LIGHT,
    val fontFamily: String = "default",
    val paragraphIndent: Int = 2,
    val marginHorizontal: Int = 28,
    val marginVertical: Int = 24,
    val customTheme: CustomReaderTheme = CustomReaderTheme(),
    val keepScreenOn: Boolean = true,
    val volumeKeyPageTurn: Boolean = false,
    val brightness: Float = -1f, // -1 = follow system, 0-1 = custom
    val autoPageTurn: Boolean = false,
    val autoPageTurnInterval: Long = 5000L, // milliseconds
    val screenOrientation: ScreenOrientation = ScreenOrientation.AUTO,
    val contentCleanEnabled: Boolean = true,
    val customCleanRules: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val dynamicColor: Boolean = true,
)

@Serializable
enum class PageMode {
    SCROLL,
    HORIZONTAL_FLIP,
    SIMULATION_FLIP,
    COVER_FLIP,
}

@Serializable
enum class ReaderThemeType {
    LIGHT,
    DARK,
    EYE_CARE,
    PARCHMENT,
    CUSTOM,
}

@Serializable
enum class ScreenOrientation {
    AUTO,
    PORTRAIT,
    LANDSCAPE,
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class CustomReaderTheme(
    val name: String = "自定义",
    val backgroundColor: Long = 0xFFF5F5DC,
    val textColor: Long = 0xFF333333,
    val secondaryTextColor: Long = 0xFF666666,
)
