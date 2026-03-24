package com.linghualive.flamekit.feature.source.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linghualive.flamekit.feature.source.domain.model.BookSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourceEditorViewModel = hiltViewModel(),
) {
    val source by viewModel.source.collectAsState()
    val message by viewModel.message.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
            if (it == "保存成功") onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "编辑书源" else "新建书源") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionCard(title = "基本信息") {
                EditorField("书源名称", source.sourceName) {
                    viewModel.updateSource(source.copy(sourceName = it))
                }
                EditorField("书源 URL", source.sourceUrl, enabled = !viewModel.isEditing) {
                    viewModel.updateSource(source.copy(sourceUrl = it))
                }
                EditorField("分组", source.sourceGroup ?: "") {
                    viewModel.updateSource(source.copy(sourceGroup = it.ifBlank { null }))
                }
                EditorField("请求头 (JSON)", source.header ?: "") {
                    viewModel.updateSource(source.copy(header = it.ifBlank { null }))
                }
            }

            SectionCard(title = "搜索规则") {
                EditorField("搜索 URL ({{key}} 为关键词)", source.searchUrl ?: "") {
                    viewModel.updateSource(source.copy(searchUrl = it.ifBlank { null }))
                }
                EditorField("列表规则 (CSS)", source.searchList ?: "") {
                    viewModel.updateSource(source.copy(searchList = it.ifBlank { null }))
                }
                EditorField("书名规则", source.searchName ?: "") {
                    viewModel.updateSource(source.copy(searchName = it.ifBlank { null }))
                }
                EditorField("作者规则", source.searchAuthor ?: "") {
                    viewModel.updateSource(source.copy(searchAuthor = it.ifBlank { null }))
                }
                EditorField("封面规则", source.searchCover ?: "") {
                    viewModel.updateSource(source.copy(searchCover = it.ifBlank { null }))
                }
                EditorField("书籍链接规则", source.searchBookUrl ?: "") {
                    viewModel.updateSource(source.copy(searchBookUrl = it.ifBlank { null }))
                }
            }

            SectionCard(title = "详情规则") {
                EditorField("书名规则", source.detailName ?: "") {
                    viewModel.updateSource(source.copy(detailName = it.ifBlank { null }))
                }
                EditorField("作者规则", source.detailAuthor ?: "") {
                    viewModel.updateSource(source.copy(detailAuthor = it.ifBlank { null }))
                }
                EditorField("封面规则", source.detailCover ?: "") {
                    viewModel.updateSource(source.copy(detailCover = it.ifBlank { null }))
                }
                EditorField("简介规则", source.detailIntro ?: "") {
                    viewModel.updateSource(source.copy(detailIntro = it.ifBlank { null }))
                }
                EditorField("目录 URL 规则", source.detailTocUrl ?: "") {
                    viewModel.updateSource(source.copy(detailTocUrl = it.ifBlank { null }))
                }
            }

            SectionCard(title = "目录规则") {
                EditorField("章节列表规则", source.tocList ?: "") {
                    viewModel.updateSource(source.copy(tocList = it.ifBlank { null }))
                }
                EditorField("章节名规则", source.tocName ?: "") {
                    viewModel.updateSource(source.copy(tocName = it.ifBlank { null }))
                }
                EditorField("章节链接规则", source.tocUrl ?: "") {
                    viewModel.updateSource(source.copy(tocUrl = it.ifBlank { null }))
                }
            }

            SectionCard(title = "正文规则") {
                EditorField("正文规则", source.contentRule ?: "") {
                    viewModel.updateSource(source.copy(contentRule = it.ifBlank { null }))
                }
                EditorField("下一页 URL 规则", source.contentNextUrl ?: "") {
                    viewModel.updateSource(source.copy(contentNextUrl = it.ifBlank { null }))
                }
            }

            // Test section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("测试", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        if (isTesting) {
                            CircularProgressIndicator()
                        } else {
                            OutlinedButton(onClick = { viewModel.testSearch() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text("测试搜索")
                            }
                        }
                    }
                    testResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "- $title" else "+ $title",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
    )
}
