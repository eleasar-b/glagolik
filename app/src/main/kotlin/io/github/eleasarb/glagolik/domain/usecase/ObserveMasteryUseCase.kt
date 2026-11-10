package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.model.averageProgress
import io.github.eleasarb.glagolik.domain.model.masteryLevel
import io.github.eleasarb.glagolik.domain.model.verbLevel
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest

/**
 * Mastery of every trainable card of the core verbs, updated after each answer: forms (in
 * total and per verb) and meaning (both directions, in total).
 *
 * All trainable forms count, independent of the tenses selected for training; meaning cards
 * count independent of the meaning setting. Forms marked NO_TRAINING and cards that are no
 * longer trainable are ignored.
 *
 * [MasteryOverview.dueNow] also changes without an answer: the flow emits again whenever the
 * next card becomes due, so a waiting screen stays current.
 */
class ObserveMasteryUseCase @Inject constructor(
    private val verbs: VerbRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<MasteryOverview> {
        val trainable = flow {
            val meaning = MeaningCatalog(verbs.getCoreVerbs()).verbs.flatMap { verb ->
                Exercise.MEANINGS.map { CardKey(verb.id, it) }
            }
            emit(TrainableCards(forms = verbs.getTrainableForms().map { it.key }, meaning))
        }
        return combine(trainable, progress.observeCards(), settings.settings, ::Snapshot)
            .transformLatest { (refs, cards, config) ->
                val dues = (refs.forms + refs.meaning)
                    .filter { config.trains(it.exercise) }
                    .mapNotNull { cards[it]?.due }
                while (true) {
                    val now = clock.instant()
                    val introduced = progress.countIntroducedSince(startOfDay(now, clock.zone))
                    emit(overview(refs, cards, now, config, introduced))
                    // At midnight the daily limit for new cards starts again.
                    val next = (dues + nextMidnight(now, clock.zone))
                        .filter { it.isAfter(now) }
                        .minOrNull() ?: break
                    // +1 ms: toMillis() truncates, the wait must not end just before `next`.
                    delay(Duration.between(now, next).toMillis() + 1)
                }
            }
            .distinctUntilChanged()
    }

    private data class Snapshot(
        val refs: TrainableCards,
        val cards: Map<CardKey, SrsCard>,
        val config: TrainingSettings,
    )
}

/** Keys of all cards that count for the overview. */
internal data class TrainableCards(val forms: List<CardKey>, val meaning: List<CardKey>)

internal fun overview(
    refs: TrainableCards,
    cards: Map<CardKey, SrsCard>,
    now: Instant,
    config: TrainingSettings = TrainingSettings(),
    introducedToday: Int = 0,
): MasteryOverview {
    fun counts(keys: List<CardKey>) = MasteryCounts.of(keys.map { cards[it].masteryLevel() })
    val all = refs.forms + refs.meaning
    val trained = all.filter { config.trains(it.exercise) }
    val due = trained.count { cards[it]?.due?.isAfter(now) == false }
    return MasteryOverview(
        forms = counts(refs.forms),
        byVerb = refs.forms.groupBy { it.verbId }.mapValues { (_, keys) -> counts(keys) },
        // Same filter and limits as the session: only cards it would ask.
        dueNow = due,
        newNow = minOf(config.newSlots(due, introducedToday), trained.count { it !in cards }),
        meaning = counts(refs.meaning),
        verbs = MasteryCounts.of(
            all.groupBy { it.verbId }.values.map { keys ->
                verbLevel(keys.map { cards[it].masteryLevel() })
            },
        ),
        progress = averageProgress(all.map { cards[it] }),
    )
}
