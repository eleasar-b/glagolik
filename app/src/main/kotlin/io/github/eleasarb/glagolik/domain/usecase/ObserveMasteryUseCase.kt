package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.domain.model.masteryLevel
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Mastery of every trainable form of the core verbs, updated after each answer.
 *
 * All trainable forms count, independent of the tenses selected for training. Forms marked
 * NO_TRAINING and cards of forms that are no longer trainable are ignored.
 *
 * [MasteryOverview.dueNow] also changes without an answer: the flow emits again whenever the
 * next card becomes due, so a waiting screen stays current.
 */
class ObserveMasteryUseCase @Inject constructor(
    private val verbs: VerbRepository,
    private val progress: ProgressRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<MasteryOverview> {
        val trainable = flow { emit(verbs.getTrainableForms().map { it.key }) }
        return combine(trainable, progress.observeCards(), ::Snapshot)
            .transformLatest { (refs, cards) ->
                val dues = refs.mapNotNull { cards[it]?.due }
                while (true) {
                    val now = clock.instant()
                    emit(overview(refs, cards, now))
                    val next = dues.filter { it.isAfter(now) }.minOrNull() ?: break
                    // +1 ms: toMillis() truncates, the wait must not end just before `next`.
                    delay(Duration.between(now, next).toMillis() + 1)
                }
            }
    }

    private data class Snapshot(val refs: List<CardKey>, val cards: Map<CardKey, SrsCard>)
}

internal fun overview(
    refs: List<CardKey>,
    cards: Map<CardKey, SrsCard>,
    now: Instant,
): MasteryOverview {
    val byVerb = refs.groupBy { it.verbId }.mapValues { (_, verbRefs) ->
        MasteryCounts.of(verbRefs.map { cards[it].masteryLevel() })
    }
    return MasteryOverview(
        total = MasteryCounts.of(refs.map { cards[it].masteryLevel() }),
        byVerb = byVerb,
        dueNow = refs.count { ref -> cards[ref]?.let { !it.due.isAfter(now) } ?: false },
    )
}
