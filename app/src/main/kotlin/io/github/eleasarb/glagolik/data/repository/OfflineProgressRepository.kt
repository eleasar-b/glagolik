package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.data.db.CardEntity
import io.github.eleasarb.glagolik.data.db.ProgressDao
import io.github.eleasarb.glagolik.data.db.ReviewLogEntity
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
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

class OfflineProgressRepository @Inject constructor(
    private val dao: ProgressDao,
    private val scheduler: FsrsScheduler,
) : ProgressRepository {
    override suspend fun getCard(ref: FormRef): SrsCard? =
        dao.getCard(ref.verbId, ref.form.name)?.toSrsCard()

    override suspend fun recordReview(
        ref: FormRef,
        rating: Rating,
        at: Instant,
        duration: Duration?,
    ): SrsCard {
        val stored = dao.getCard(ref.verbId, ref.form.name)
        val current = stored?.toSrsCard() ?: SrsCard(due = at)
        val reviewed = scheduler.review(current, rating, at)
        dao.saveReview(
            card = reviewed.toEntity(ref, introducedAt = stored?.introducedAt ?: at.toEpochMilli()),
            log = ReviewLogEntity(
                verbId = ref.verbId,
                form = ref.form.name,
                rating = rating.name,
                reviewedAt = at.toEpochMilli(),
                durationMs = duration?.toMillis(),
            ),
        )
        return reviewed
    }

    override suspend fun getDueCards(now: Instant, limit: Int): List<ScheduledCard> =
        dao.getDueCards(now.toEpochMilli(), limit).map { ScheduledCard(it.ref(), it.toSrsCard()) }

    override suspend fun getKnownForms(): Set<FormRef> =
        dao.getAllCards().mapTo(mutableSetOf()) { it.ref() }

    override suspend fun countIntroducedSince(since: Instant): Int =
        dao.countIntroducedSince(since.toEpochMilli())

    override fun observeCards(): Flow<Map<FormRef, SrsCard>> =
        dao.observeAllCards().map { rows -> rows.associate { it.ref() to it.toSrsCard() } }

    override fun observeCards(verbId: Int): Flow<Map<FormKey, SrsCard>> =
        dao.observeCards(verbId).map { rows ->
            rows.associate { FormKey.valueOf(it.form) to it.toSrsCard() }
        }
}

private fun CardEntity.ref(): FormRef = FormRef(verbId, FormKey.valueOf(form))

internal fun CardEntity.toSrsCard(): SrsCard = SrsCard(
    due = Instant.ofEpochMilli(dueAt),
    state = CardState.valueOf(state),
    step = step,
    stability = stability,
    difficulty = difficulty,
    lastReview = lastReviewAt?.let(Instant::ofEpochMilli),
)

internal fun SrsCard.toEntity(ref: FormRef, introducedAt: Long): CardEntity = CardEntity(
    verbId = ref.verbId,
    form = ref.form.name,
    state = state.name,
    step = step,
    stability = stability,
    difficulty = difficulty,
    dueAt = due.toEpochMilli(),
    lastReviewAt = lastReview?.toEpochMilli(),
    introducedAt = introducedAt,
)
