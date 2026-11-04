package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.random.Random

/** One card of a session. */
data class SessionCard(val form: TrainableForm, val isNew: Boolean)

data class TrainingSession(
    val cards: List<SessionCard>,
    /** Due forms that did not fit into this session. */
    val remainingDue: Int,
    /** New forms still allowed today after this session. */
    val remainingNewToday: Int,
)

/**
 * Builds a session (CLAUDE.md §6):
 * 1. due forms first, most overdue first
 * 2. then new forms, limited by the daily allowance, in frequency order of the verbs
 * Only forms of the selected tenses are used. New forms are shuffled within the session
 * so the forms of one verb do not come in table order.
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
        val forms = verbs.getTrainableForms().filter { it.ref.form.tense in config.tenses }
        val byRef = forms.associateBy { it.ref }

        val allDue = progress.getDueCards(now, DUE_SCAN_LIMIT).mapNotNull { byRef[it.ref] }
        val due = allDue.take(config.sessionSize)

        val newAllowed = (config.newFormsPerDay - progress.countIntroducedSince(startOfDay(now)))
            .coerceAtLeast(0)
        val newSlots = minOf(config.sessionSize - due.size, newAllowed)
        val known = progress.getKnownForms()
        val new = forms.asSequence().filter { it.ref !in known }.take(newSlots).toList()

        return TrainingSession(
            cards = due.map { SessionCard(it, isNew = false) } +
                new.shuffled(random).map { SessionCard(it, isNew = true) },
            remainingDue = allDue.size - due.size,
            remainingNewToday = newAllowed - new.size,
        )
    }

    private fun startOfDay(now: Instant): Instant =
        now.atZone(clock.zone).toLocalDate().atStartOfDay(clock.zone).toInstant()

    private companion object {
        /** Upper bound of due cards read before filtering by tense. */
        const val DUE_SCAN_LIMIT = 10_000
    }
}
