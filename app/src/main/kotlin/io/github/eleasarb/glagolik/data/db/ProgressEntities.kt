package io.github.eleasarb.glagolik.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Learning progress, stored in progress.db. Unlike verbs.db this is user data: it is
 * never replaced and every schema change needs a migration.
 *
 * Verbs are referenced by their stable id (data/verb-ids.csv), forms by FormKey name.
 * There is no foreign key into verbs.db because it is a separate database.
 * Times are epoch milliseconds (UTC).
 */

/** FSRS state of one verb form. A row exists once the form was answered at least once. */
@Entity(
    tableName = "card",
    primaryKeys = ["verb_id", "form"],
    indices = [Index("due_at"), Index("introduced_at")],
)
data class CardEntity(
    @ColumnInfo(name = "verb_id") val verbId: Int,
    val form: String,
    val state: String,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    @ColumnInfo(name = "due_at") val dueAt: Long,
    @ColumnInfo(name = "last_review_at") val lastReviewAt: Long?,
    /** First answer; drives the daily limit for new forms. */
    @ColumnInfo(name = "introduced_at") val introducedAt: Long,
)

/** Every answer, kept for statistics and a later FSRS parameter optimisation. */
@Entity(
    tableName = "review_log",
    indices = [Index("verb_id", "form"), Index("reviewed_at")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "verb_id") val verbId: Int,
    val form: String,
    val rating: String,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?,
)
