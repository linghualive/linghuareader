package com.linghualive.flamekit.core.di

import com.linghualive.flamekit.feature.bookshelf.data.repository.BookRepositoryImpl
import com.linghualive.flamekit.feature.bookshelf.domain.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository
}
