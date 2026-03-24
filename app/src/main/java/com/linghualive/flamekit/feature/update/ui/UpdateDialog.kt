package com.linghualive.flamekit.feature.update.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.linghualive.flamekit.feature.update.domain.model.AppRelease

@Composable
fun UpdateDialog(
    release: AppRelease,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    downloadAndInstall(context, release)
                    onDismiss()
                },
            ) {
                Text(if (release.hasDirectDownload) "下载更新" else "前往下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        },
    )
}

private fun downloadAndInstall(context: Context, release: AppRelease) {
    if (!release.hasDirectDownload) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
        context.startActivity(intent)
        return
    }

    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "linghua-reader-${release.version}.apk"

        val request = DownloadManager.Request(Uri.parse(release.downloadUrl))
            .setTitle("灵华阅读 v${release.version}")
            .setDescription("正在下载更新...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        downloadManager.enqueue(request)
        Toast.makeText(context, "开始下载，完成后点击通知安装", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
        context.startActivity(intent)
    }
}
