package com.linghualive.flamekit.feature.sync.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class WebDavClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    suspend fun upload(
        url: String,
        username: String,
        password: String,
        data: ByteArray,
        remotePath: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fullUrl = buildUrl(url, remotePath)
            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", Credentials.basic(username, password))
                .put(data.toRequestBody("application/octet-stream".toMediaType()))
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code != 201 && response.code != 204) {
                throw Exception("Upload failed: ${response.code} ${response.message}")
            }
        }
    }

    suspend fun download(
        url: String,
        username: String,
        password: String,
        remotePath: String,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val fullUrl = buildUrl(url, remotePath)
            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", Credentials.basic(username, password))
                .get()
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code} ${response.message}")
            }
            response.body?.bytes() ?: throw Exception("Empty response body")
        }
    }

    suspend fun mkdir(
        url: String,
        username: String,
        password: String,
        remotePath: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fullUrl = buildUrl(url, remotePath)
            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", Credentials.basic(username, password))
                .method("MKCOL", null)
                .build()
            val response = okHttpClient.newCall(request).execute()
            // 405 = already exists, which is fine
            if (!response.isSuccessful && response.code != 405) {
                throw Exception("Mkdir failed: ${response.code} ${response.message}")
            }
        }
    }

    suspend fun exists(
        url: String,
        username: String,
        password: String,
        remotePath: String,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val fullUrl = buildUrl(url, remotePath)
            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", Credentials.basic(username, password))
                .method("PROPFIND", null)
                .header("Depth", "0")
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful || response.code == 207
        }
    }

    private fun buildUrl(baseUrl: String, path: String): String {
        val base = baseUrl.trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$base/$cleanPath"
    }
}
