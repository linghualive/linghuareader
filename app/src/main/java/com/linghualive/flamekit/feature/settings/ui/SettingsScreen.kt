package com.linghualive.flamekit.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import com.linghualive.flamekit.BuildConfig
import com.linghualive.flamekit.core.datastore.PageMode
import com.linghualive.flamekit.core.datastore.ReaderThemeType
import com.linghualive.flamekit.core.theme.readerColorsFor
import com.linghualive.flamekit.feature.update.ui.UpdateDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStatsClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.readingPrefs.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Show update dialog
    val availableRelease = (updateState as? SettingsViewModel.UpdateState.Available)?.release
    if (availableRelease != null) {
        UpdateDialog(
            release = availableRelease,
            onDismiss = viewModel::dismissUpdate,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Reading settings group
            SectionHeader("阅读设置")

            // Default font size
            ListItem(
                headlineContent = { Text("默认字号") },
                supportingContent = {
                    Column {
                        Text("${prefs.fontSize}")
                        Slider(
                            value = prefs.fontSize.toFloat(),
                            onValueChange = { viewModel.updateFontSize(it.roundToInt()) },
                            valueRange = 12f..36f,
                            steps = 23,
                        )
                    }
                },
            )

            // Default line spacing
            ListItem(
                headlineContent = { Text("默认行距") },
                supportingContent = {
                    Column {
                        Text("%.1f".format(prefs.lineSpacingMultiplier))
                        Slider(
                            value = prefs.lineSpacingMultiplier,
                            onValueChange = {
                                viewModel.updateLineSpacing((it * 10).roundToInt() / 10f)
                            },
                            valueRange = 1.0f..3.0f,
                        )
                    }
                },
            )

            // Default reader theme
            ListItem(
                headlineContent = { Text("默认阅读主题") },
                supportingContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ReaderThemeType.entries.forEach { themeType ->
                            val colors = readerColorsFor(themeType, prefs.customTheme)
                            val isSelected = prefs.readerTheme == themeType
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.background)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.Gray, CircleShape)
                                        }
                                    )
                                    .clickable { viewModel.updateReaderTheme(themeType) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (themeType == ReaderThemeType.CUSTOM) "C" else "A",
                                    color = colors.textColor,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                },
            )

            // Default page mode
            ListItem(
                headlineContent = { Text("默认翻页模式") },
                supportingContent = {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        SegmentedButton(
                            selected = prefs.pageMode == PageMode.SCROLL,
                            onClick = { viewModel.updatePageMode(PageMode.SCROLL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text("滚动")
                        }
                        SegmentedButton(
                            selected = prefs.pageMode == PageMode.HORIZONTAL_FLIP,
                            onClick = { viewModel.updatePageMode(PageMode.HORIZONTAL_FLIP) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text("翻页")
                        }
                    }
                },
            )

            // Stats
            SectionHeader("数据")

            ListItem(
                headlineContent = { Text("阅读统计") },
                supportingContent = { Text("查看阅读时长、日历热图") },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.clickable(onClick = onStatsClick),
            )

            ListItem(
                headlineContent = { Text("数据同步") },
                supportingContent = { Text("WebDAV 云端备份与恢复") },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.clickable(onClick = onSyncClick),
            )

            // About group
            SectionHeader("关于")

            ListItem(
                headlineContent = { Text("应用名称") },
                supportingContent = { Text("灵华阅读") },
            )

            ListItem(
                headlineContent = { Text("版本号") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )

            ListItem(
                headlineContent = { Text("检查更新") },
                supportingContent = {
                    when (updateState) {
                        is SettingsViewModel.UpdateState.Checking -> Text("检查中...")
                        is SettingsViewModel.UpdateState.UpToDate -> Text("已是最新版本")
                        else -> Text("点击检查新版本")
                    }
                },
                trailingContent = {
                    if (updateState is SettingsViewModel.UpdateState.Checking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                },
                modifier = Modifier.clickable(
                    enabled = updateState !is SettingsViewModel.UpdateState.Checking,
                    onClick = viewModel::checkForUpdate,
                ),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}
