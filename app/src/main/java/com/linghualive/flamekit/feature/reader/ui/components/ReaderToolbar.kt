package com.linghualive.flamekit.feature.reader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderToolbar(
    visible: Boolean,
    title: String,
    currentChapter: Int,
    totalChapters: Int,
    isAutoPageTurning: Boolean,
    onBack: () -> Unit,
    onChapterList: () -> Unit,
    onBookmarkList: () -> Unit,
    onAddBookmark: () -> Unit,
    onSettings: () -> Unit,
    onTts: () -> Unit,
    onAutoPageTurn: () -> Unit,
    onSeekChapter: (Int) -> Unit,
    onSearch: () -> Unit,
    onNoteList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top bar
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )
        }

        // Bottom bar
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 4.dp),
            ) {
                // Progress bar
                if (totalChapters > 0) {
                    ReadingProgressBar(
                        currentChapter = currentChapter,
                        totalChapters = totalChapters,
                        onSeek = onSeekChapter,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onChapterList) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "目录",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onBookmarkList) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = "书签列表",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onAddBookmark) {
                        Icon(
                            Icons.Filled.BookmarkAdd,
                            contentDescription = "添加书签",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onNoteList) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = "笔记",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onAutoPageTurn) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "自动翻页",
                            tint = if (isAutoPageTurning) Color.Yellow else Color.White,
                        )
                    }
                    IconButton(onClick = onTts) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "朗读",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
