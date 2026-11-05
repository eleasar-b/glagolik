package io.github.eleasarb.glagolik.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Bulk access to all progress, used only by export and import. */
@Dao
interface ProgressBackupDao {
    @Query("SELECT * FROM card")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM review_log ORDER BY reviewed_at, id")
    suspend fun getAllLogs(): List<ReviewLogEntity>

    @Query("DELETE FROM card")
    suspend fun deleteAllCards()

    @Query("DELETE FROM review_log")
    suspend fun deleteAllLogs()

    @Insert
    suspend fun insertCards(cards: List<CardEntity>)

    @Insert
    suspend fun insertLogs(logs: List<ReviewLogEntity>)

    /** Replaces all progress (import). Runs in one transaction: all or nothing. */
    @Transaction
    suspend fun replaceAll(cards: List<CardEntity>, logs: List<ReviewLogEntity>) {
        deleteAllCards()
        deleteAllLogs()
        insertCards(cards)
        insertLogs(logs)
    }
}
