package com.linghualive.flamekit.feature.source.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linghualive.flamekit.feature.source.domain.model.BookSource
import com.linghualive.flamekit.feature.source.domain.model.SourceSubscription
import com.linghualive.flamekit.feature.source.engine.SourceHealthChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagerScreen(
    onBack: () -> Unit,
    onEditSource: (String) -> Unit = {},
    onNewSource: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SourceManagerViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val selectedUrls by viewModel.selectedUrls.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val healthResults by viewModel.healthResults.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()
    val checkProgress by viewModel.checkProgress.collectAsState()
    val isRefreshingSub by viewModel.isRefreshingSubscription.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("书源列表", "订阅管理", "健康检测")

    LaunchedEffect(importMessage) {
        importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (selectedUrls.isNotEmpty()) {
                        Text("已选 ${selectedUrls.size} 项")
                    } else {
                        Text("书源管理")
                    }
                },
                navigationIcon = {
                    if (selectedUrls.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消选择")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (selectedUrls.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "全选")
                        }
                        IconButton(onClick = { viewModel.enableSelected() }) {
                            Text("启", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { viewModel.disableSelected() }) {
                            Text("禁", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = {
                            val jsonExport = viewModel.exportSelectedAsJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("book_sources", jsonExport))
                        }) {
                            Text("导", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(
                            onClick = onNewSource,
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "新建书源")
                        }
                        Spacer(Modifier.height(12.dp))
                        FloatingActionButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                val text = clip?.getItemAt(0)?.text?.toString()
                                if (!text.isNullOrBlank()) {
                                    viewModel.importFromJson(text)
                                }
                            },
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "从剪贴板导入")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> SourceListTab(
                    sources = sources,
                    selectedUrls = selectedUrls,
                    healthResults = healthResults,
                    onToggleEnabled = { source, enabled -> viewModel.toggleSourceEnabled(source, enabled) },
                    onDelete = { viewModel.deleteSource(it) },
                    onEdit = { onEditSource(it.sourceUrl) },
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onLongPress = { viewModel.toggleSelection(it) },
                )
                1 -> SubscriptionTab(
                    subscriptions = subscriptions,
                    isRefreshing = isRefreshingSub,
                    onAdd = { name, url -> viewModel.addSubscription(name, url) },
                    onRefresh = { viewModel.refreshSubscription(it) },
                    onRefreshAll = { viewModel.refreshAllSubscriptions() },
                    onRemove = { viewModel.removeSubscription(it.url) },
                )
                2 -> HealthCheckTab(
                    sources = sources.filter { it.enabled },
                    healthResults = healthResults,
                    isChecking = isChecking,
                    checkProgress = checkProgress,
                    onStartCheck = { viewModel.startHealthCheck() },
                    onAutoDisable = { viewModel.autoDisableUnhealthy() },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceListTab(
    sources: List<BookSource>,
    selectedUrls: Set<String>,
    healthResults: Map<String, SourceHealthChecker.HealthResult>,
    onToggleEnabled: (BookSource, Boolean) -> Unit,
    onDelete: (BookSource) -> Unit,
    onEdit: (BookSource) -> Unit,
    onToggleSelection: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    val isSelecting = selectedUrls.isNotEmpty()

    if (sources.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无书源\n点击右下角按钮导入或新建",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sources, key = { it.sourceUrl }) { source ->
                val isSelected = source.sourceUrl in selectedUrls
                val healthResult = healthResults[source.sourceUrl]
                val cardColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    label = "cardColor",
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isSelecting) {
                                    onToggleSelection(source.sourceUrl)
                                } else {
                                    onEdit(source)
                                }
                            },
                            onLongClick = { onLongPress(source.sourceUrl) },
                        ),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelecting) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelection(source.sourceUrl) },
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.sourceName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (healthResult != null) {
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (healthResult.isHealthy) "OK" else "FAIL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (healthResult.isHealthy)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                    )
                                    if (healthResult.isHealthy && healthResult.responseTime > 0) {
                                        Text(
                                            text = " ${healthResult.responseTime}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = source.sourceUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            source.sourceGroup?.let { group ->
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (!isSelecting) {
                            Switch(
                                checked = source.enabled,
                                onCheckedChange = { onToggleEnabled(source, it) },
                            )
                            IconButton(onClick = { onDelete(source) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionTab(
    subscriptions: List<SourceSubscription>,
    isRefreshing: Boolean,
    onAdd: (String, String) -> Unit,
    onRefresh: (SourceSubscription) -> Unit,
    onRefreshAll: () -> Unit,
    onRemove: (SourceSubscription) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("添加订阅")
            }
            OutlinedButton(
                onClick = onRefreshAll,
                enabled = !isRefreshing && subscriptions.isNotEmpty(),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Text("全部刷新")
            }
        }

        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无订阅\n添加远程书源订阅 URL",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(subscriptions, key = { it.url }) { sub ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = sub.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${sub.sourceCount} 个书源",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                if (sub.lastUpdate > 0) {
                                    Text(
                                        text = "最后更新: ${formatTime(sub.lastUpdate)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { onRefresh(sub) }, enabled = !isRefreshing) {
                                    Text("刷新")
                                }
                                TextButton(onClick = { onRemove(sub) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                onAdd(name, url)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("订阅名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("订阅 URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, url) },
                enabled = name.isNotBlank() && url.isNotBlank(),
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun HealthCheckTab(
    sources: List<BookSource>,
    healthResults: Map<String, SourceHealthChecker.HealthResult>,
    isChecking: Boolean,
    checkProgress: Float,
    onStartCheck: () -> Unit,
    onAutoDisable: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onStartCheck,
                enabled = !isChecking,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isChecking) "检测中..." else "一键检测")
            }
            OutlinedButton(
                onClick = onAutoDisable,
                enabled = healthResults.isNotEmpty(),
            ) {
                Text("自动禁用失效")
            }
        }

        if (isChecking) {
            LinearProgressIndicator(
                progress = { checkProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${healthResults.size} / ${sources.size}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (healthResults.isEmpty() && !isChecking) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "点击\"一键检测\"开始检测所有书源",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val sortedResults = healthResults.values.sortedWith(
                compareBy<SourceHealthChecker.HealthResult> { it.isHealthy }
                    .thenBy { it.responseTime }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(sortedResults, key = { it.sourceUrl }) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isHealthy)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.sourceName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                result.error?.let { error ->
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (result.isHealthy) {
                                Text(
                                    text = "${result.responseTime}ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Text(
                                    text = "FAIL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
