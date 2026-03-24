package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ReadingProgressBar(
    currentChapter: Int,
    totalChapters: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Slider(
            value = currentChapter.toFloat(),
            onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..(totalChapters - 1).coerceAtLeast(1).toFloat(),
            steps = (totalChapters - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${currentChapter + 1}/$totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
            Text(
                "${((currentChapter + 1f) / totalChapters.coerceAtLeast(1) * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
