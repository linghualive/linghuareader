package com.linghualive.flamekit.core.di

import android.content.Context
import androidx.room.Room
import com.linghualive.flamekit.core.database.AppDatabase
import com.linghualive.flamekit.core.database.dao.BookDao
import com.linghualive.flamekit.core.database.dao.BookSourceDao
import com.linghualive.flamekit.core.database.dao.BookmarkDao
import com.linghualive.flamekit.core.database.dao.NoteDao
import com.linghualive.flamekit.core.database.dao.ReadingProgressDao
import com.linghualive.flamekit.core.database.dao.ReadingStatDao
import com.linghualive.flamekit.core.database.dao.SourceSubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "flamekit.db",
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
            )
            .build()
    }

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideReadingProgressDao(database: AppDatabase): ReadingProgressDao = database.readingProgressDao()

    @Provides
    fun provideBookSourceDao(database: AppDatabase): BookSourceDao = database.bookSourceDao()

    @Provides
    fun provideReadingStatDao(database: AppDatabase): ReadingStatDao = database.readingStatDao()

    @Provides
    fun provideSourceSubscriptionDao(database: AppDatabase): SourceSubscriptionDao = database.sourceSubscriptionDao()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()
}
