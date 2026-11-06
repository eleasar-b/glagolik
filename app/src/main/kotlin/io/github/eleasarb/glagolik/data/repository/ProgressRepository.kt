package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.ScheduledCard
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    /** Current state, or `null` if the form was never answered. */
    suspend fun getCard(ref: FormRef): SrsCard?

    /**
     * Applies [rating] with the FSRS scheduler and stores the new state together with a
     * review log entry. Unknown forms start as new cards.
     */
    suspend fun recordReview(
        ref: FormRef,
        rating: Rating,
        at: Instant,
        duration: Duration? = null,
    ): SrsCard

    /** Forms due at or before [now], most overdue first. */
    suspend fun getDueCards(now: Instant, limit: Int): List<ScheduledCard>

    /** All forms answered at least once (to tell new forms from known ones). */
    suspend fun getKnownForms(): Set<FormRef>

    /** Number of forms answered for the first time at or after [since]. */
    suspend fun countIntroducedSince(since: Instant): Int

    /** State of every answered form; emits again after each change. */
    fun observeCards(): Flow<Map<FormRef, SrsCard>>

    /** State of the answered forms of one verb; emits again after each change. */
    fun observeCards(verbId: Int): Flow<Map<FormKey, SrsCard>>
}
