package com.linghualive.flamekit.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linghualive.flamekit.feature.stats.ui.components.ReadingCalendar
import com.linghualive.flamekit.feature.stats.ui.components.StatsSummaryCard
import com.linghualive.flamekit.feature.stats.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("阅读统计") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Summary card
                StatsSummaryCard(
                    totalDurationSeconds = state.totalDurationSeconds,
                    totalReadingDays = state.totalReadingDays,
                    consecutiveDays = state.consecutiveDays,
                    weekDurationSeconds = state.weekDurationSeconds,
                )

                // Calendar heatmap
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "阅读日历",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ReadingCalendar(readingDays = state.calendarData)
                    }
                }

                // Weekly bar chart
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "最近 7 天",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WeeklyBarChart(
                            data = state.weeklyData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxSeconds = data.maxOf { it.second }.coerceAtLeast(60)
        val barWidth = size.width / data.size * 0.6f
        val barSpacing = size.width / data.size
        val chartHeight = size.height - 40f // leave room for labels

        data.forEachIndexed { index, (label, seconds) ->
            val barHeight = if (maxSeconds > 0) {
                (seconds.toFloat() / maxSeconds) * chartHeight
            } else {
                0f
            }

            val x = index * barSpacing + (barSpacing - barWidth) / 2

            // Bar
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }

            // Label
            val textLayoutResult = textMeasurer.measure(label, textStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                color = textColor,
                topLeft = Offset(
                    x + (barWidth - textLayoutResult.size.width) / 2,
                    chartHeight + 8f,
                ),
            )

            // Value on top of bar
            if (seconds > 0) {
                val valueText = if (seconds >= 3600) {
                    "${seconds / 3600}h"
                } else {
                    "${seconds / 60}m"
                }
                val valueLayout = textMeasurer.measure(valueText, textStyle)
                drawText(
                    textLayoutResult = valueLayout,
                    color = textColor,
                    topLeft = Offset(
                        x + (barWidth - valueLayout.size.width) / 2,
                        (chartHeight - barHeight - valueLayout.size.height - 2f).coerceAtLeast(0f),
                    ),
                )
            }
        }
    }
}
