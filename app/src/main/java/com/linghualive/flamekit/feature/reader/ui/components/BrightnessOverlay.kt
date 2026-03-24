package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BrightnessOverlay(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFollowingSystem = brightness < 0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("亮度", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { onBrightnessChange(-1f) }) {
                Text(if (isFollowingSystem) "跟随系统" else "自定义")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.BrightnessLow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Slider(
                value = if (!isFollowingSystem) brightness else 0.5f,
                onValueChange = { onBrightnessChange(it) },
                valueRange = 0.01f..1f,
                enabled = !isFollowingSystem,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.BrightnessHigh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
