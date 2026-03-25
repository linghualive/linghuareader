package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linghualive.flamekit.core.datastore.CustomReaderTheme
import com.linghualive.flamekit.core.datastore.PageMode
import com.linghualive.flamekit.core.datastore.ReaderThemeType
import com.linghualive.flamekit.core.datastore.ReadingPreferences
import com.linghualive.flamekit.core.datastore.ScreenOrientation
import com.linghualive.flamekit.core.theme.readerColorsFor
import com.linghualive.flamekit.feature.reader.engine.FontInfo
import com.linghualive.flamekit.feature.settings.ui.components.ThemeEditorDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingSettingsPanel(
    prefs: ReadingPreferences,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onThemeChange: (ReaderThemeType) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    fonts: List<FontInfo> = emptyList(),
    onFontChange: (String) -> Unit = {},
    onCustomThemeChange: (CustomReaderTheme) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onVolumeKeyPageTurnChange: (Boolean) -> Unit = {},
    onAutoPageTurnChange: () -> Unit = {},
    onAutoPageTurnIntervalChange: (Long) -> Unit = {},
    onScreenOrientationChange: (ScreenOrientation) -> Unit = {},
    onContentCleanEnabledChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    var showThemeEditor by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Brightness
            BrightnessOverlay(
                brightness = prefs.brightness,
                onBrightnessChange = onBrightnessChange,
            )

            // Font size
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("字号", style = MaterialTheme.typography.titleSmall)
                    Text("${prefs.fontSize}", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = prefs.fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                    valueRange = 12f..36f,
                    steps = 23,
                )
            }

            // Line spacing
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("行距", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "%.1f".format(prefs.lineSpacingMultiplier),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Slider(
                    value = prefs.lineSpacingMultiplier,
                    onValueChange = {
                        onLineSpacingChange((it * 10).roundToInt() / 10f)
                    },
                    valueRange = 1.0f..3.0f,
                )
            }

            // Reader theme
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("阅读主题", style = MaterialTheme.typography.titleSmall)
                    if (prefs.readerTheme == ReaderThemeType.CUSTOM) {
                        TextButton(onClick = { showThemeEditor = true }) {
                            Text("编辑")
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ReaderThemeType.entries.forEach { themeType ->
                        val colors = readerColorsFor(themeType, prefs.customTheme)
                        val isSelected = prefs.readerTheme == themeType
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.background)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier.border(1.dp, Color.Gray, CircleShape)
                                    }
                                )
                                .clickable { onThemeChange(themeType) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (themeType == ReaderThemeType.CUSTOM) "C" else "A",
                                color = colors.textColor,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            // Page mode
            Column {
                Text("翻页模式", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val modes = listOf(
                        PageMode.SCROLL to "滚动",
                        PageMode.HORIZONTAL_FLIP to "左右",
                        PageMode.SIMULATION_FLIP to "仿真",
                        PageMode.COVER_FLIP to "覆盖",
                    )
                    modes.forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.pageMode == mode,
                            onClick = { onPageModeChange(mode) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // Screen orientation
            Column {
                Text("屏幕方向", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val orientations = listOf(
                        ScreenOrientation.AUTO to "自动",
                        ScreenOrientation.PORTRAIT to "竖屏",
                        ScreenOrientation.LANDSCAPE to "横屏",
                    )
                    orientations.forEach { (orientation, label) ->
                        FilterChip(
                            selected = prefs.screenOrientation == orientation,
                            onClick = { onScreenOrientationChange(orientation) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // Auto page turn
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("自动翻页", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = prefs.autoPageTurn,
                        onCheckedChange = { onAutoPageTurnChange() },
                    )
                }
                if (prefs.autoPageTurn) {
                    Text("翻页间隔", style = MaterialTheme.typography.bodySmall)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val intervals = listOf(
                            3000L to "3秒",
                            5000L to "5秒",
                            10000L to "10秒",
                            15000L to "15秒",
                            30000L to "30秒",
                        )
                        intervals.forEach { (interval, label) ->
                            FilterChip(
                                selected = prefs.autoPageTurnInterval == interval,
                                onClick = { onAutoPageTurnIntervalChange(interval) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            // Keep screen on
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("屏幕常亮", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = prefs.keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                )
            }

            // Volume key page turn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("音量键翻页", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = prefs.volumeKeyPageTurn,
                    onCheckedChange = onVolumeKeyPageTurnChange,
                )
            }

            // Content cleaning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("正文净化", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = prefs.contentCleanEnabled,
                    onCheckedChange = onContentCleanEnabledChange,
                )
            }
        }
    }

    if (showThemeEditor) {
        ThemeEditorDialog(
            currentTheme = prefs.customTheme,
            onSave = { theme ->
                onCustomThemeChange(theme)
                showThemeEditor = false
            },
            onDismiss = { showThemeEditor = false },
        )
    }
}
