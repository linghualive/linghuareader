package com.linghualive.flamekit.feature.update.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.linghualive.flamekit.feature.update.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun UpdateDialog(
    release: AppRelease,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("发现新版本 v${release.version}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = release.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (release.description.isNotBlank()) {
                    Text(
                        text = release.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "下载中 ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (downloadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = downloadError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!release.hasDirectDownload) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                        context.startActivity(intent)
                        onDismiss()
                    } else {
                        isDownloading = true
                        downloadError = null
                        scope.launch {
                            try {
                                val file = downloadApk(context, release) { p ->
                                    progress = p
                                }
                                installApk(context, file)
                            } catch (e: Exception) {
                                downloadError = "下载失败：${e.message}"
                            } finally {
                                isDownloading = false
                            }
                        }
                    }
                },
                enabled = !isDownloading,
            ) {
                Text(
                    if (isDownloading) "下载中..."
                    else if (release.hasDirectDownload) "下载更新"
                    else "前往下载"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading,
            ) {
                Text("稍后再说")
            }
        },
    )
}

private suspend fun downloadApk(
    context: Context,
    release: AppRelease,
    onProgress: (Float) -> Unit,
): File = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val request = Request.Builder()
        .url(release.downloadUrl)
        .addHeader(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
        )
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

    val body = response.body ?: throw Exception("空响应")
    val contentLength = body.contentLength()

    val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val apkFile = File(updateDir, "linghua-reader-${release.version}.apk")

    body.byteStream().use { input ->
        apkFile.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var bytesRead: Long = 0
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                bytesRead += read
                if (contentLength > 0) {
                    withContext(Dispatchers.Main) {
                        onProgress(bytesRead.toFloat() / contentLength)
                    }
                }
            }
        }
    }

    apkFile
}

private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
