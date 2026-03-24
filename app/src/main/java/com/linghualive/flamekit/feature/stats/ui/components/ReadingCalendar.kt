package com.linghualive.flamekit.feature.stats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun ReadingCalendar(
    readingDays: Map<String, Long>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val startDate = today.minusMonths(3).with(DayOfWeek.MONDAY)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.primary,
    )

    // Pre-compute days data
    data class DayData(val weekIndex: Int, val dayOfWeek: Int, val color: Color)

    val days = mutableListOf<DayData>()
    var currentDate = startDate
    var weekIndex = 0
    var prevWeekYear = startDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())

    while (!currentDate.isAfter(today)) {
        val currentWeek = currentDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
        val currentYear = currentDate.year
        if (currentDate != startDate && currentDate.dayOfWeek == DayOfWeek.MONDAY) {
            weekIndex++
        }
        val dayOfWeek = currentDate.dayOfWeek.value - 1 // 0=Monday

        val dateStr = currentDate.toString()
        val duration = readingDays[dateStr] ?: 0
        val color = when {
            duration <= 0 -> emptyColor
            duration < 30 * 60 -> colors[0]   // < 30min
            duration < 60 * 60 -> colors[1]   // < 1h
            duration < 120 * 60 -> colors[2]  // < 2h
            else -> colors[3]                  // >= 2h
        }
        days.add(DayData(weekIndex, dayOfWeek, color))
        currentDate = currentDate.plusDays(1)
    }

    val totalWeeks = weekIndex + 1
    val cellSize = 14f
    val gap = 3f
    val canvasWidth = totalWeeks * (cellSize + gap)
    val canvasHeight = 7 * (cellSize + gap)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(((canvasHeight + 8) / 2.5f).dp),
    ) {
        val scaleX = size.width / canvasWidth
        val scaleY = size.height / canvasHeight.coerceAtLeast(1f)
        val scale = minOf(scaleX, scaleY)
        val scaledCellSize = cellSize * scale
        val scaledGap = gap * scale
        val offsetX = (size.width - totalWeeks * (scaledCellSize + scaledGap)) / 2f

        for (day in days) {
            val x = offsetX + day.weekIndex * (scaledCellSize + scaledGap)
            val y = day.dayOfWeek * (scaledCellSize + scaledGap)
            drawRoundRect(
                color = day.color,
                topLeft = Offset(x, y),
                size = Size(scaledCellSize, scaledCellSize),
                cornerRadius = CornerRadius(2f * scale, 2f * scale),
            )
        }
    }
}
