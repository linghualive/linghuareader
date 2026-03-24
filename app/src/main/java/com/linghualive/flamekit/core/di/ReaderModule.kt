package com.linghualive.flamekit.core.di

import com.linghualive.flamekit.feature.reader.data.repository.BookmarkRepositoryImpl
import com.linghualive.flamekit.feature.reader.data.repository.NoteRepositoryImpl
import com.linghualive.flamekit.feature.reader.data.repository.ReaderRepositoryImpl
import com.linghualive.flamekit.feature.reader.domain.repository.BookmarkRepository
import com.linghualive.flamekit.feature.reader.domain.repository.NoteRepository
import com.linghualive.flamekit.feature.reader.domain.repository.ReaderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReaderModule {

    @Binds
    abstract fun bindReaderRepository(impl: ReaderRepositoryImpl): ReaderRepository

    @Binds
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}
