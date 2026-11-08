package io.github.eleasarb.glagolik.di

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import io.github.eleasarb.glagolik.data.backup.BackupRepository
import io.github.eleasarb.glagolik.data.backup.DocumentBackupRepository
import io.github.eleasarb.glagolik.data.db.ProgressBackupDao
import io.github.eleasarb.glagolik.data.db.ProgressDao
import io.github.eleasarb.glagolik.data.db.ProgressDatabase
import io.github.eleasarb.glagolik.data.db.VerbDao
import io.github.eleasarb.glagolik.data.db.VerbsDatabase
import io.github.eleasarb.glagolik.data.repository.OfflineProgressRepository
import io.github.eleasarb.glagolik.data.repository.OfflineVerbRepository
import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.DataStoreSettingsRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.data.tts.AndroidSpeaker
import io.github.eleasarb.glagolik.data.tts.Speaker
import io.github.eleasarb.glagolik.domain.srs.FsrsScheduler
import java.time.Clock
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVerbsDatabase(@ApplicationContext context: Context): VerbsDatabase =
        VerbsDatabase.create(context)

    @Provides
    fun provideVerbDao(database: VerbsDatabase): VerbDao = database.verbDao()

    @Provides
    @Singleton
    fun provideProgressDatabase(@ApplicationContext context: Context): ProgressDatabase =
        ProgressDatabase.create(context)

    @Provides
    fun provideProgressDao(database: ProgressDatabase): ProgressDao = database.progressDao()

    @Provides
    fun provideProgressBackupDao(database: ProgressDatabase): ProgressBackupDao =
        database.progressBackupDao()
}

@Module
@InstallIn(SingletonComponent::class)
object LearningModule {
    @Provides
    @Singleton
    fun provideScheduler(): FsrsScheduler = FsrsScheduler()

    /** Shuffles new forms within a session. */
    @Provides
    fun provideRandom(): Random = Random.Default

    /** The zone matters for "new forms per day": the day starts at local midnight. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(DataStoreSettingsRepository.FILE_NAME)
        }
}

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}

/** One engine per ViewModel; the ViewModel closes it in onCleared. */
@Module
@InstallIn(ViewModelComponent::class)
object SpeechModule {
    @Provides
    @ViewModelScoped
    fun provideSpeaker(@ApplicationContext context: Context): Speaker = AndroidSpeaker(context)
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindVerbRepository(repository: OfflineVerbRepository): VerbRepository

    @Binds
    fun bindProgressRepository(repository: OfflineProgressRepository): ProgressRepository

    @Binds
    fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun bindBackupRepository(repository: DocumentBackupRepository): BackupRepository
}
