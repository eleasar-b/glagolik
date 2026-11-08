package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.data.db.CardEntity
import io.github.eleasarb.glagolik.data.db.ProgressDao
import io.github.eleasarb.glagolik.data.db.ReviewLogEntity
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.ScheduledCard
import io.github.eleasarb.glagolik.domain.srs.CardState
import io.github.eleasarb.glagolik.domain.srs.FsrsScheduler
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Rows with an unknown `item` (none are written, see Exercise.fromId) are skipped. */
class OfflineProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val scheduler: FsrsScheduler,
) : ProgressRepository {
    override suspend fun getCard(key: CardKey): SrsCard? =
        dao.getCard(key.verbId, key.exercise.id)?.toSrsCard()

    override suspend fun recordReview(
        key: CardKey,
        rating: Rating,
        at: Instant,
        duration: Duration?,
    ): SrsCard {
        val stored = dao.getCard(key.verbId, key.exercise.id)
        val current = stored?.toSrsCard() ?: SrsCard(due = at)
        val reviewed = scheduler.review(current, rating, at)
        dao.saveReview(
            card = reviewed.toEntity(key, introducedAt = stored?.introducedAt ?: at.toEpochMilli()),
            log = ReviewLogEntity(
                verbId = key.verbId,
                item = key.exercise.id,
                rating = rating.name,
                reviewedAt = at.toEpochMilli(),
                durationMs = duration?.toMillis(),
            ),
        )
        return reviewed
    }

    override suspend fun getDueCards(now: Instant, limit: Int): List<ScheduledCard> =
        dao.getDueCards(now.toEpochMilli(), limit).mapNotNull { row ->
            row.key()?.let { ScheduledCard(it, row.toSrsCard()) }
        }

    override suspend fun getKnownCards(): Set<CardKey> =
        dao.getAllCards().mapNotNullTo(mutableSetOf()) { it.key() }

    override suspend fun countIntroducedSince(since: Instant): Int =
        dao.countIntroducedSince(since.toEpochMilli())

    override fun observeCards(): Flow<Map<CardKey, SrsCard>> = dao.observeAllCards().map { rows ->
        rows.mapNotNull { row -> row.key()?.let { it to row.toSrsCard() } }.toMap()
    }

    override fun observeCards(verbId: Int): Flow<Map<Exercise, SrsCard>> =
        dao.observeCards(verbId).map { rows ->
            rows.mapNotNull { row -> Exercise.fromId(row.item)?.let { it to row.toSrsCard() } }
                .toMap()
        }
}

private fun CardEntity.key(): CardKey? = Exercise.fromId(item)?.let { CardKey(verbId, it) }

internal fun CardEntity.toSrsCard(): SrsCard = SrsCard(
    due = Instant.ofEpochMilli(dueAt),
    state = CardState.valueOf(state),
    step = step,
    stability = stability,
    difficulty = difficulty,
    lastReview = lastReviewAt?.let(Instant::ofEpochMilli),
)

internal fun SrsCard.toEntity(key: CardKey, introducedAt: Long): CardEntity = CardEntity(
    verbId = key.verbId,
    item = key.exercise.id,
    state = state.name,
    step = step,
    stability = stability,
    difficulty = difficulty,
    dueAt = due.toEpochMilli(),
    lastReviewAt = lastReview?.toEpochMilli(),
    introducedAt = introducedAt,
)
