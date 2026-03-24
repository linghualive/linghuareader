package com.linghualive.flamekit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.linghualive.flamekit.core.database.dao.BookDao
import com.linghualive.flamekit.core.database.dao.BookSourceDao
import com.linghualive.flamekit.core.database.dao.BookmarkDao
import com.linghualive.flamekit.core.database.dao.NoteDao
import com.linghualive.flamekit.core.database.dao.ReadingProgressDao
import com.linghualive.flamekit.core.database.dao.ReadingStatDao
import com.linghualive.flamekit.core.database.dao.SourceSubscriptionDao
import com.linghualive.flamekit.core.database.entity.BookEntity
import com.linghualive.flamekit.core.database.entity.BookSourceEntity
import com.linghualive.flamekit.core.database.entity.BookmarkEntity
import com.linghualive.flamekit.core.database.entity.NoteEntity
import com.linghualive.flamekit.core.database.entity.ReadingProgressEntity
import com.linghualive.flamekit.core.database.entity.ReadingStatEntity
import com.linghualive.flamekit.core.database.entity.SourceSubscriptionEntity

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        ReadingProgressEntity::class,
        BookSourceEntity::class,
        ReadingStatEntity::class,
        SourceSubscriptionEntity::class,
        NoteEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookSourceDao(): BookSourceDao
    abstract fun readingStatDao(): ReadingStatDao
    abstract fun sourceSubscriptionDao(): SourceSubscriptionDao
    abstract fun noteDao(): NoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `book_sources` (
                        `sourceUrl` TEXT NOT NULL,
                        `sourceName` TEXT NOT NULL,
                        `sourceGroup` TEXT,
                        `sourceType` INTEGER NOT NULL DEFAULT 0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `header` TEXT,
                        `loginUrl` TEXT,
                        `lastUpdateTime` INTEGER NOT NULL DEFAULT 0,
                        `searchUrl` TEXT,
                        `searchList` TEXT,
                        `searchName` TEXT,
                        `searchAuthor` TEXT,
                        `searchCover` TEXT,
                        `searchBookUrl` TEXT,
                        `detailName` TEXT,
                        `detailAuthor` TEXT,
                        `detailCover` TEXT,
                        `detailIntro` TEXT,
                        `detailTocUrl` TEXT,
                        `tocList` TEXT,
                        `tocName` TEXT,
                        `tocUrl` TEXT,
                        `contentRule` TEXT,
                        `contentNextUrl` TEXT,
                        PRIMARY KEY(`sourceUrl`)
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reading_stats` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `pagesRead` INTEGER NOT NULL,
                        `chaptersRead` INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE book_sources ADD COLUMN lastCheckTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE book_sources ADD COLUMN lastCheckSuccess INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `source_subscriptions` (
                        `url` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `lastUpdate` INTEGER NOT NULL DEFAULT 0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `sourceCount` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`url`)
                    )"""
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `chapterIndex` INTEGER NOT NULL,
                        `startPosition` INTEGER NOT NULL,
                        `endPosition` INTEGER NOT NULL,
                        `selectedText` TEXT NOT NULL,
                        `noteContent` TEXT,
                        `highlightColor` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId` ON `notes` (`bookId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN sourceUrl TEXT DEFAULT NULL")
            }
        }
    }
}
