package com.linghualive.flamekit.core.di

import com.linghualive.flamekit.feature.source.data.repository.BookSourceRepositoryImpl
import com.linghualive.flamekit.feature.source.domain.repository.BookSourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SourceModule {

    @Binds
    abstract fun bindBookSourceRepository(impl: BookSourceRepositoryImpl): BookSourceRepository
}
