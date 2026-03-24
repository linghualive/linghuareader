package com.linghualive.flamekit.feature.reader.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val HIGHLIGHT_YELLOW = 0xFFFFEB3BL
val HIGHLIGHT_GREEN = 0xFF4CAF50L
val HIGHLIGHT_BLUE = 0xFF2196F3L
val HIGHLIGHT_PINK = 0xFFE91E63L

private val highlightColors = listOf(
    HIGHLIGHT_YELLOW,
    HIGHLIGHT_GREEN,
    HIGHLIGHT_BLUE,
    HIGHLIGHT_PINK,
)

@Composable
fun AddNoteDialog(
    selectedText: String,
    onSave: (noteContent: String?, highlightColor: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var noteContent by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(HIGHLIGHT_YELLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加笔记") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(selectedColor).copy(alpha = 0.3f))
                        .padding(12.dp),
                    maxLines = 4,
                )

                Text(
                    text = "高亮颜色",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    highlightColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = color },
                        )
                    }
                }

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("笔记 (可选)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        noteContent.ifBlank { null },
                        selectedColor,
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
