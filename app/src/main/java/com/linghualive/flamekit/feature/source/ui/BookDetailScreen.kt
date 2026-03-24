package com.linghualive.flamekit.feature.source.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addedToShelf by viewModel.addedToShelf.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: "书籍详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.addToBookshelf() },
                        enabled = detail != null && !addedToShelf,
                    ) {
                        Icon(
                            if (addedToShelf) Icons.Default.Check else Icons.Default.BookmarkAdd,
                            contentDescription = if (addedToShelf) "已加入书架" else "加入书架",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = error ?: "未知错误",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDetail() }) {
                            Text("重试")
                        }
                    }
                }
                detail != null -> {
                    val bookDetail = detail!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Book info header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                if (bookDetail.coverUrl != null) {
                                    AsyncImage(
                                        model = bookDetail.coverUrl,
                                        contentDescription = bookDetail.name,
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 140.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookDetail.name,
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    bookDetail.author?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.addToBookshelf() },
                                            enabled = !addedToShelf,
                                        ) {
                                            Text(if (addedToShelf) "已加入书架" else "加入书架")
                                        }
                                    }
                                }
                            }
                        }

                        // Intro
                        bookDetail.intro?.let { intro ->
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Text(
                                        text = "简介",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = intro,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Chapter list header
                        item {
                            HorizontalDivider()
                            Text(
                                text = "目录 (${bookDetail.chapters.size} 章)",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(16.dp),
                            )
                        }

                        // Chapters
                        itemsIndexed(bookDetail.chapters) { _, chapter ->
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
