package io.github.eleasarb.glagolik.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM card WHERE verb_id = :verbId AND item = :item")
    suspend fun getCard(verbId: Int, item: String): CardEntity?

    /** Cards due at or before [now], most overdue first. */
    @Query("SELECT * FROM card WHERE due_at <= :now ORDER BY due_at LIMIT :limit")
    suspend fun getDueCards(now: Long, limit: Int): List<CardEntity>

    @Query("SELECT * FROM card")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM card")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM card WHERE verb_id = :verbId")
    fun observeCards(verbId: Int): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM card WHERE introduced_at >= :since")
    suspend fun countIntroducedSince(since: Long): Int

    @Upsert
    suspend fun upsertCard(card: CardEntity)

    @Insert
    suspend fun insertLog(log: ReviewLogEntity)

    @Query(
        "SELECT * FROM review_log WHERE verb_id = :verbId AND item = :item ORDER BY reviewed_at",
    )
    suspend fun getLogs(verbId: Int, item: String): List<ReviewLogEntity>

    /** Card and log are written together or not at all. */
    @Transaction
    suspend fun saveReview(card: CardEntity, log: ReviewLogEntity) {
        upsertCard(card)
        insertLog(log)
    }
}
