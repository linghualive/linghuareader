package com.linghualive.flamekit.feature.sync.data

import android.content.Context
import android.net.Uri
import com.linghualive.flamekit.core.database.dao.BookDao
import com.linghualive.flamekit.core.database.dao.BookSourceDao
import com.linghualive.flamekit.core.database.dao.BookmarkDao
import com.linghualive.flamekit.core.database.dao.ReadingProgressDao
import com.linghualive.flamekit.core.datastore.ReadingPrefsDataStore
import com.linghualive.flamekit.feature.sync.domain.model.BackupData
import com.linghualive.flamekit.feature.sync.domain.model.BookDto
import com.linghualive.flamekit.feature.sync.domain.model.BookSourceDto
import com.linghualive.flamekit.feature.sync.domain.model.BookmarkDto
import com.linghualive.flamekit.feature.sync.domain.model.ReadingProgressDto
import com.linghualive.flamekit.feature.sync.domain.model.SyncConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDavClient: WebDavClient,
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val readingProgressDao: ReadingProgressDao,
    private val bookSourceDao: BookSourceDao,
    private val readingPrefsDataStore: ReadingPrefsDataStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun backup(config: SyncConfig): Result<Unit> = runCatching {
        val backupData = collectBackupData()
        val jsonStr = json.encodeToString(backupData)

        webDavClient.mkdir(
            config.serverUrl, config.username, config.password, config.remotePath,
        ).getOrThrow()

        webDavClient.upload(
            config.serverUrl,
            config.username,
            config.password,
            jsonStr.toByteArray(Charsets.UTF_8),
            "${config.remotePath.trimEnd('/')}/backup.json",
        ).getOrThrow()

        readingPrefsDataStore.updateSyncConfig { it.copy(lastSyncTime = System.currentTimeMillis()) }
    }

    suspend fun restore(config: SyncConfig): Result<Unit> = runCatching {
        val bytes = webDavClient.download(
            config.serverUrl,
            config.username,
            config.password,
            "${config.remotePath.trimEnd('/')}/backup.json",
        ).getOrThrow()

        val backupData = json.decodeFromString<BackupData>(bytes.toString(Charsets.UTF_8))
        importBackupData(backupData)
    }

    suspend fun exportToLocal(): Uri = withContext(Dispatchers.IO) {
        val backupData = collectBackupData()
        val jsonStr = json.encodeToString(backupData)
        val file = java.io.File(context.cacheDir, "flamekit_backup.json")
        file.writeText(jsonStr)
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    suspend fun importFromLocal(uri: Uri): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Cannot open file")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val backupData = json.decodeFromString<BackupData>(jsonStr)
            importBackupData(backupData)
        }
    }

    private suspend fun collectBackupData(): BackupData {
        val books = bookDao.getAllBooksOnce().map { book ->
            BookDto(
                id = book.id,
                title = book.title,
                author = book.author,
                filePath = book.filePath,
                format = book.format,
                coverPath = book.coverPath,
                addedAt = book.addedAt,
                lastReadAt = book.lastReadAt,
                totalChapters = book.totalChapters,
                sourceUrl = book.sourceUrl,
            )
        }
        val bookmarks = bookmarkDao.getAllBookmarks().map { bm ->
            BookmarkDto(
                id = bm.id,
                bookId = bm.bookId,
                chapterIndex = bm.chapterIndex,
                position = bm.position,
                title = bm.title,
                createdAt = bm.createdAt,
            )
        }
        val progress = readingProgressDao.getAllProgress().map { p ->
            ReadingProgressDto(
                bookId = p.bookId,
                chapterIndex = p.chapterIndex,
                scrollPosition = p.scrollPosition,
                lastReadAt = p.lastReadAt,
            )
        }
        val sources = bookSourceDao.getAllSourcesOnce().map { s ->
            BookSourceDto(
                sourceUrl = s.sourceUrl,
                sourceName = s.sourceName,
                sourceGroup = s.sourceGroup,
                sourceType = s.sourceType,
                enabled = s.enabled,
                header = s.header,
                loginUrl = s.loginUrl,
                lastUpdateTime = s.lastUpdateTime,
                searchUrl = s.searchUrl,
                searchList = s.searchList,
                searchName = s.searchName,
                searchAuthor = s.searchAuthor,
                searchCover = s.searchCover,
                searchBookUrl = s.searchBookUrl,
                detailName = s.detailName,
                detailAuthor = s.detailAuthor,
                detailCover = s.detailCover,
                detailIntro = s.detailIntro,
                detailTocUrl = s.detailTocUrl,
                tocList = s.tocList,
                tocName = s.tocName,
                tocUrl = s.tocUrl,
                contentRule = s.contentRule,
                contentNextUrl = s.contentNextUrl,
            )
        }
        val prefs = readingPrefsDataStore.getPreferences()

        return BackupData(
            books = books,
            bookmarks = bookmarks,
            readingProgress = progress,
            bookSources = sources,
            readingPrefs = prefs,
            backupTime = System.currentTimeMillis(),
            appVersion = "1.0.0",
        )
    }

    private suspend fun importBackupData(data: BackupData) {
        // Import books (upsert)
        data.books.forEach { dto ->
            val entity = com.linghualive.flamekit.core.database.entity.BookEntity(
                id = dto.id,
                title = dto.title,
                author = dto.author,
                filePath = dto.filePath,
                format = dto.format,
                coverPath = dto.coverPath,
                addedAt = dto.addedAt,
                lastReadAt = dto.lastReadAt,
                totalChapters = dto.totalChapters,
                sourceUrl = dto.sourceUrl,
            )
            bookDao.insertBook(entity)
        }

        // Import bookmarks
        data.bookmarks.forEach { dto ->
            val entity = com.linghualive.flamekit.core.database.entity.BookmarkEntity(
                id = dto.id,
                bookId = dto.bookId,
                chapterIndex = dto.chapterIndex,
                position = dto.position,
                title = dto.title,
                createdAt = dto.createdAt,
            )
            bookmarkDao.insertBookmark(entity)
        }

        // Import reading progress
        data.readingProgress.forEach { dto ->
            val entity = com.linghualive.flamekit.core.database.entity.ReadingProgressEntity(
                bookId = dto.bookId,
                chapterIndex = dto.chapterIndex,
                scrollPosition = dto.scrollPosition,
                lastReadAt = dto.lastReadAt,
            )
            readingProgressDao.upsertProgress(entity)
        }

        // Import book sources
        val sourceEntities = data.bookSources.map { dto ->
            com.linghualive.flamekit.core.database.entity.BookSourceEntity(
                sourceUrl = dto.sourceUrl,
                sourceName = dto.sourceName,
                sourceGroup = dto.sourceGroup,
                sourceType = dto.sourceType,
                enabled = dto.enabled,
                header = dto.header,
                loginUrl = dto.loginUrl,
                lastUpdateTime = dto.lastUpdateTime,
                searchUrl = dto.searchUrl,
                searchList = dto.searchList,
                searchName = dto.searchName,
                searchAuthor = dto.searchAuthor,
                searchCover = dto.searchCover,
                searchBookUrl = dto.searchBookUrl,
                detailName = dto.detailName,
                detailAuthor = dto.detailAuthor,
                detailCover = dto.detailCover,
                detailIntro = dto.detailIntro,
                detailTocUrl = dto.detailTocUrl,
                tocList = dto.tocList,
                tocName = dto.tocName,
                tocUrl = dto.tocUrl,
                contentRule = dto.contentRule,
                contentNextUrl = dto.contentNextUrl,
            )
        }
        bookSourceDao.insertSources(sourceEntities)

        // Import reading preferences
        readingPrefsDataStore.update { data.readingPrefs }
    }
}
