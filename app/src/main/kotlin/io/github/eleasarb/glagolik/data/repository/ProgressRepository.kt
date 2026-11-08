package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.ScheduledCard
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    /** Current state, or `null` if the card was never answered. */
    suspend fun getCard(key: CardKey): SrsCard?

    /**
     * Applies [rating] with the FSRS scheduler and stores the new state together with a
     * review log entry. Unknown cards start as new cards.
     */
    suspend fun recordReview(
        key: CardKey,
        rating: Rating,
        at: Instant,
        duration: Duration? = null,
    ): SrsCard

    /** Cards due at or before [now], most overdue first. */
    suspend fun getDueCards(now: Instant, limit: Int): List<ScheduledCard>

    /** All cards answered at least once (to tell new cards from known ones). */
    suspend fun getKnownCards(): Set<CardKey>

    /** Number of cards answered for the first time at or after [since]. */
    suspend fun countIntroducedSince(since: Instant): Int

    /** State of every answered card; emits again after each change. */
    fun observeCards(): Flow<Map<CardKey, SrsCard>>

    /** State of the answered cards of one verb; emits again after each change. */
    fun observeCards(verbId: Int): Flow<Map<Exercise, SrsCard>>
}
