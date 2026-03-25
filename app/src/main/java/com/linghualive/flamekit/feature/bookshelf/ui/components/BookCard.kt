package com.linghualive.flamekit.feature.bookshelf.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.linghualive.flamekit.feature.bookshelf.domain.model.Book
import kotlin.math.absoluteValue

private val coverGradients = listOf(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), // indigo → violet
    listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)), // blue → cyan
    listOf(Color(0xFF10B981), Color(0xFF34D399)), // emerald
    listOf(Color(0xFFF59E0B), Color(0xFFF97316)), // amber → orange
    listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // pink → rose
    listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)), // violet → fuchsia
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)), // sky → indigo
    listOf(Color(0xFF14B8A6), Color(0xFF3B82F6)), // teal → blue
)

private fun formatRelativeTime(timestamp: Long?): String? {
    if (timestamp == null) return null
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000}小时前"
        diff < 2_592_000_000L -> "${diff / 86_400_000}天前"
        else -> "${diff / 2_592_000_000}月前"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val gradientIndex = remember(book.id) { book.id.hashCode().absoluteValue % coverGradients.size }
    val gradient = coverGradients[gradientIndex]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
    ) {
        // Cover
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            tonalElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (book.coverPath != null) {
                    AsyncImage(
                        model = book.coverPath,
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = gradient,
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = book.title.take(6),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 22.sp,
                            )
                            if (book.author.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = book.author,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // Format badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            Color.Black.copy(alpha = 0.35f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = book.format.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        // Title & meta
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val relativeTime = formatRelativeTime(book.lastReadAt)
        val subtitle = relativeTime ?: "${book.totalChapters} 章"
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                },
            )
        }
    }
}
