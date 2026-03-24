package com.linghualive.flamekit.feature.update.data

import com.linghualive.flamekit.BuildConfig
import com.linghualive.flamekit.feature.update.domain.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val repo = BuildConfig.GITHUB_REPO

    suspend fun checkForUpdate(): AppRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<JsonObject>(body)

            val tagName = release["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
            val remoteVersion = tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            if (!isNewerVersion(remoteVersion, currentVersion)) return@withContext null

            val releaseName = release["name"]?.jsonPrimitive?.content ?: tagName
            val releaseBody = release["body"]?.jsonPrimitive?.content ?: ""
            val htmlUrl = release["html_url"]?.jsonPrimitive?.content ?: ""

            val apkUrl = release["assets"]?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { asset ->
                    val name = asset["name"]?.jsonPrimitive?.content ?: ""
                    name.endsWith(".apk")
                }
                ?.get("browser_download_url")?.jsonPrimitive?.content

            AppRelease(
                version = remoteVersion,
                name = releaseName,
                description = releaseBody,
                downloadUrl = apkUrl ?: htmlUrl,
                htmlUrl = htmlUrl,
                hasDirectDownload = apkUrl != null,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
