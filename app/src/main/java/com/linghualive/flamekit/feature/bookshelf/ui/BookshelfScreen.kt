package com.linghualive.flamekit.feature.bookshelf.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linghualive.flamekit.feature.bookshelf.ui.components.BookCard
import com.linghualive.flamekit.feature.bookshelf.ui.components.EmptyBookshelf
import com.linghualive.flamekit.feature.update.ui.UpdateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    availableUpdate?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = viewModel::dismissUpdate,
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importBook(it) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("搜索书架...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* already filtering live */ }),
                            trailingIcon = {
                                IconButton(onClick = { onSearchClick() }) {
                                    Icon(Icons.Default.TravelExplore, contentDescription = "在线搜索")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    }
                },
                actions = {
                    if (!showSearchBar) {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    } else {
                        IconButton(onClick = {
                            showSearchBar = false
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                        }
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "排序")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "最近阅读",
                                        color = if (sortOrder == SortOrder.RECENT) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.RECENT)
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "书名",
                                        color = if (sortOrder == SortOrder.TITLE) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.TITLE)
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "导入时间",
                                        color = if (sortOrder == SortOrder.ADDED) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    viewModel.updateSortOrder(SortOrder.ADDED)
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("text/plain", "application/epub+zip", "application/pdf")) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "导入书籍")
            }
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
                books.isEmpty() -> {
                    EmptyBookshelf()
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(books, key = { it.id }) { book ->
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book.id) },
                                onDelete = { viewModel.deleteBook(book) },
                                downloadProgress = activeDownloads[book.id],
                            )
                        }
                    }
                }
            }
        }
    }
}
