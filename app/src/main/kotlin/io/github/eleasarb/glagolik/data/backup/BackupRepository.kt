package io.github.eleasarb.glagolik.data.backup

import android.content.ContentResolver
import android.net.Uri
import io.github.eleasarb.glagolik.data.db.ProgressBackupDao
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Number of forms and answers in an exported or imported file. */
data class BackupSummary(val cards: Int, val reviews: Int)

interface BackupRepository {
    /** Writes progress and settings to [uri] (a document chosen by the learner). */
    suspend fun export(uri: Uri): BackupSummary

    /**
     * Replaces progress and settings with the content of [uri]. The file is validated
     * completely first; on any error nothing is changed and [BackupFormatException]
     * (or [IOException]) is thrown.
     */
    suspend fun import(uri: Uri): BackupSummary
}

class DocumentBackupRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val dao: ProgressBackupDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : BackupRepository {
    private val io: CoroutineDispatcher = Dispatchers.IO

    override suspend fun export(uri: Uri): BackupSummary = withContext(io) {
        val stream = contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("cannot open $uri for writing")
        stream.use { write(it) }
    }

    override suspend fun import(uri: Uri): BackupSummary = withContext(io) {
        val stream = contentResolver.openInputStream(uri)
            ?: throw IOException("cannot open $uri for reading")
        stream.use { read(it) }
    }

    internal suspend fun write(output: OutputStream): BackupSummary {
        val backup = ProgressBackup(
            exportedAt = clock.instant(),
            settings = settings.current(),
            cards = dao.getAllCards(),
            logs = dao.getAllLogs(),
        )
        output.write(ProgressBackupCodec.encode(backup).toByteArray(Charsets.UTF_8))
        return BackupSummary(cards = backup.cards.size, reviews = backup.logs.size)
    }

    internal suspend fun read(input: InputStream): BackupSummary {
        val backup = ProgressBackupCodec.decode(input.readBytes().toString(Charsets.UTF_8))
        // Log ids are local; let Room assign new ones.
        dao.replaceAll(backup.cards, backup.logs.map { it.copy(id = 0) })
        settings.replaceAll(backup.settings)
        return BackupSummary(cards = backup.cards.size, reviews = backup.logs.size)
    }
}
