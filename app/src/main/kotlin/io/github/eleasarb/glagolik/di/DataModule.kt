package io.github.eleasarb.glagolik.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.eleasarb.glagolik.data.db.VerbDao
import io.github.eleasarb.glagolik.data.db.VerbsDatabase
import io.github.eleasarb.glagolik.data.repository.OfflineVerbRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVerbsDatabase(@ApplicationContext context: Context): VerbsDatabase =
        VerbsDatabase.create(context)

    @Provides
    fun provideVerbDao(database: VerbsDatabase): VerbDao = database.verbDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindVerbRepository(repository: OfflineVerbRepository): VerbRepository
}
