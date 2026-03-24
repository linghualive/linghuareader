package com.linghualive.flamekit.feature.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val presetColors = listOf(
    Color(0xFFFFFFFF), // White
    Color(0xFF1C1B1F), // Dark
    Color(0xFFF5F5DC), // Beige
    Color(0xFFCEEBCE), // Green
    Color(0xFFF5E6C8), // Parchment
    Color(0xFFE8D5B7), // Warm
    Color(0xFFD4E6F1), // Light blue
    Color(0xFFF8E8EE), // Pink
    Color(0xFF333333), // Charcoal
    Color(0xFFE6E1E5), // Light gray
    Color(0xFF1A2E1A), // Dark green
    Color(0xFF3E2723), // Brown
)

@Composable
fun ColorPickerRow(
    label: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustom by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)

        // Preset color palette
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presetColors.take(6).forEach { color ->
                val isSelected = colorMatches(color, selectedColor)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, Color.Gray, CircleShape)
                            }
                        )
                        .clickable { onColorSelected(color) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presetColors.drop(6).forEach { color ->
                val isSelected = colorMatches(color, selectedColor)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, Color.Gray, CircleShape)
                            }
                        )
                        .clickable { onColorSelected(color) },
                )
            }
        }

        TextButton(onClick = { showCustom = !showCustom }) {
            Text(if (showCustom) "收起自定义" else "自定义颜色")
        }

        if (showCustom) {
            CustomColorSliders(
                currentColor = selectedColor,
                onColorChanged = onColorSelected,
            )
        }
    }
}

@Composable
private fun CustomColorSliders(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Preview
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, Color.Gray, CircleShape),
            )
            Text(
                text = "#%06X".format(0xFFFFFF and colorToArgbInt(currentColor)),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // R Slider
        SliderRow("R", currentColor.red) { value ->
            onColorChanged(currentColor.copy(red = value))
        }

        // G Slider
        SliderRow("G", currentColor.green) { value ->
            onColorChanged(currentColor.copy(green = value))
        }

        // B Slider
        SliderRow("B", currentColor.blue) { value ->
            onColorChanged(currentColor.copy(blue = value))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.size(16.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(value * 255).toInt()}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun colorMatches(a: Color, b: Color): Boolean {
    return (a.red - b.red).let { it * it } +
            (a.green - b.green).let { it * it } +
            (a.blue - b.blue).let { it * it } < 0.001f
}

private fun colorToArgbInt(color: Color): Int {
    return (
        ((color.alpha * 255).toInt() shl 24) or
        ((color.red * 255).toInt() shl 16) or
        ((color.green * 255).toInt() shl 8) or
        (color.blue * 255).toInt()
    )
}
