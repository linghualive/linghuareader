package com.linghualive.flamekit.feature.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linghualive.flamekit.core.datastore.CustomReaderTheme

@Composable
fun ThemeEditorDialog(
    currentTheme: CustomReaderTheme,
    onSave: (CustomReaderTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentTheme.name) }
    var bgColor by remember { mutableStateOf(Color(currentTheme.backgroundColor.toULong())) }
    var textColor by remember { mutableStateOf(Color(currentTheme.textColor.toULong())) }
    var secondaryColor by remember { mutableStateOf(Color(currentTheme.secondaryTextColor.toULong())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑自定义主题") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("主题名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ColorPickerRow(
                    label = "背景色",
                    selectedColor = bgColor,
                    onColorSelected = { bgColor = it },
                )

                ColorPickerRow(
                    label = "文字颜色",
                    selectedColor = textColor,
                    onColorSelected = { textColor = it },
                )

                ColorPickerRow(
                    label = "次要文字颜色",
                    selectedColor = secondaryColor,
                    onColorSelected = { secondaryColor = it },
                )

                // Preview
                Text("预览效果", style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "正文文字效果预览",
                            color = textColor,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = "次要文字效果预览",
                            color = secondaryColor,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CustomReaderTheme(
                            name = name,
                            backgroundColor = bgColor.value.toLong(),
                            textColor = textColor.value.toLong(),
                            secondaryTextColor = secondaryColor.value.toLong(),
                        )
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
