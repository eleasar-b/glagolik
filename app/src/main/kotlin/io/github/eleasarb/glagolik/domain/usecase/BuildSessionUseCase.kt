package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/** What the learner is asked on one card. */
sealed interface TrainingTask {
    val key: CardKey
    val verb: VerbSummary
}

/** Type a conjugated form. */
data class ConjugationTask(val form: TrainableForm) : TrainingTask {
    override val key: CardKey get() = form.key
    override val verb: VerbSummary get() = form.verb
}

/** Russian infinitive shown, pick the translation among [choice] (RU→DE). */
data class MeaningChoiceTask(override val verb: VerbSummary, val choice: MeaningChoice) :
    TrainingTask {
    override val key: CardKey get() = CardKey(verb.id, Exercise.MeaningRuDe)
}

/**
 * Translation and aspect shown, type the infinitive (DE→RU). Typing one of [alsoFitting] is not
 * graded, the learner is asked for another verb.
 */
data class MeaningRecallTask(
    override val verb: VerbSummary,
    val alsoFitting: List<VerbSummary> = emptyList(),
) : TrainingTask {
    override val key: CardKey get() = CardKey(verb.id, Exercise.MeaningDeRu)
    val expected: List<String> get() = listOf(verb.infinitive)
}

/** One card of a session. */
data class SessionCard(val task: TrainingTask, val isNew: Boolean)

data class TrainingSession(
    val cards: List<SessionCard>,
    /** Due cards that did not fit into this session. */
    val remainingDue: Int,
    /** New cards still allowed today after this session. */
    val remainingNewToday: Int,
)

/**
 * Builds a session (CLAUDE.md §6):
 * 1. due cards first, most overdue first
 * 2. then new cards, limited by one daily allowance for all exercises: per verb in frequency
 *    order first RU→DE, then DE→RU, then the forms
 *
 * Only forms of the selected tenses are used; meaning cards only if meaning practice is on.
 * New cards are shuffled within the session so the cards of one verb do not come in table
 * order. RU→DE of a verb always stays before its DE→RU: recognising comes before recalling.
 */
class BuildSessionUseCase @Inject constructor(
    private val verbs: VerbRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val random: Random,
) {
    suspend operator fun invoke(): TrainingSession {
        val config = settings.current()
        val now = clock.instant()
        val forms = verbs.getTrainableForms().filter { it.form.tense in config.tenses }
        val coreVerbs = verbs.getCoreVerbs()
        val catalog = MeaningCatalog(if (config.practiceMeaning) coreVerbs else emptyList())
        val tasks = TaskFactory(forms, catalog, random)

        val allDue = progress.getDueCards(now, DUE_SCAN_LIMIT)
            .map { it.key }
            .filter(tasks::isActive)
        val dueKeys = allDue.take(config.sessionSize)
        val due = dueKeys.mapNotNull(tasks::of)

        val newAllowed = (config.newCardsPerDay - progress.countIntroducedSince(startOfDay(now)))
            .coerceAtLeast(0)
        val newSlots = minOf(config.sessionSize - due.size, newAllowed)
        val known = progress.getKnownCards()
        val formsByVerb = forms.groupBy { it.verb.id }
        val verbOrder = (coreVerbs.map { it.summary.id } + formsByVerb.keys).distinct()
        val new = verbOrder.asSequence()
            .flatMap { verbId ->
                val meanings = if (catalog.contains(verbId)) Exercise.MEANINGS else emptyList()
                meanings.map { CardKey(verbId, it) } +
                    formsByVerb[verbId].orEmpty().map { it.key }
            }
            .filter { it !in known }
            .mapNotNull(tasks::of)
            .take(newSlots)
            .toList()

        return TrainingSession(
            cards = due.map { SessionCard(it, isNew = false) } +
                new.shuffledKeepingMeaningOrder().map { SessionCard(it, isNew = true) },
            remainingDue = allDue.size - dueKeys.size,
            remainingNewToday = newAllowed - new.size,
        )
    }

    private fun List<TrainingTask>.shuffledKeepingMeaningOrder(): List<TrainingTask> {
        val result = shuffled(random).toMutableList()
        result.indices.forEach { index ->
            val task = result[index]
            if (task is MeaningRecallTask) {
                val choice = result.indexOfFirst {
                    it is MeaningChoiceTask && it.verb.id == task.verb.id
                }
                if (choice > index) {
                    result[index] = result[choice]
                    result[choice] = task
                }
            }
        }
        return result
    }

    private fun startOfDay(now: Instant): Instant =
        now.atZone(clock.zone).toLocalDate().atStartOfDay(clock.zone).toInstant()

    private companion object {
        /** Upper bound of due cards read before filtering by exercise. */
        const val DUE_SCAN_LIMIT = 10_000
    }
}

/** Turns a card key into a task, or `null` if the card is not trained right now. */
private class TaskFactory(
    forms: List<TrainableForm>,
    private val catalog: MeaningCatalog,
    private val random: Random,
) {
    private val formsByKey = forms.associateBy { it.key }

    /** Cheap check without building the task (no distractors drawn). */
    fun isActive(key: CardKey): Boolean = when (key.exercise) {
        is Exercise.Form -> key in formsByKey
        Exercise.MeaningRuDe, Exercise.MeaningDeRu -> catalog.contains(key.verbId)
    }

    fun of(key: CardKey): TrainingTask? = when (key.exercise) {
        is Exercise.Form -> formsByKey[key]?.let(::ConjugationTask)

        Exercise.MeaningRuDe -> catalog.verb(key.verbId)?.let { verb ->
            catalog.choice(key.verbId, random)?.let { MeaningChoiceTask(verb, it) }
        }

        Exercise.MeaningDeRu -> catalog.verb(key.verbId)?.let { verb ->
            MeaningRecallTask(verb, catalog.alsoFitting(key.verbId))
        }
    }
}
