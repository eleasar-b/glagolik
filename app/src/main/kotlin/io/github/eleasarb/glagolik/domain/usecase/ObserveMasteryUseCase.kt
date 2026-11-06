package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.FormRef
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.domain.model.masteryLevel
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Mastery of every trainable form of the core verbs, updated after each answer.
 *
 * All trainable forms count, independent of the tenses selected for training. Forms marked
 * NO_TRAINING and cards of forms that are no longer trainable are ignored.
 */
class ObserveMasteryUseCase @Inject constructor(
    private val verbs: VerbRepository,
    private val progress: ProgressRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<MasteryOverview> {
        val trainable = flow { emit(verbs.getTrainableForms().map { it.ref }) }
        return combine(trainable, progress.observeCards()) { refs, cards ->
            overview(refs, cards, clock.instant())
        }
    }
}

internal fun overview(
    refs: List<FormRef>,
    cards: Map<FormRef, SrsCard>,
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
