package io.github.eleasarb.glagolik.testing

import io.github.eleasarb.glagolik.data.db.AspectPartnerEntity
import io.github.eleasarb.glagolik.data.db.TrainableFormRow
import io.github.eleasarb.glagolik.data.db.VerbDao
import io.github.eleasarb.glagolik.data.db.VerbEntity
import io.github.eleasarb.glagolik.data.db.VerbFormEntity
import io.github.eleasarb.glagolik.data.repository.ProgressRepository
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.data.settings.SettingsRepository
import io.github.eleasarb.glagolik.data.tts.Speaker
import io.github.eleasarb.glagolik.data.tts.SpeechAvailability
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.CardKey
import io.github.eleasarb.glagolik.domain.model.CoreVerb
import io.github.eleasarb.glagolik.domain.model.Exercise
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.ScheduledCard
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainableForm
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import io.github.eleasarb.glagolik.domain.srs.FsrsScheduler
import io.github.eleasarb.glagolik.domain.srs.Rating
import io.github.eleasarb.glagolik.domain.srs.SrsCard
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** U+0301 COMBINING ACUTE ACCENT. */
private const val STRESS_MARK = "\u0301"

/** In-memory DAO with the same filter semantics as the SQL query. */
class FakeVerbDao(
    private val verbs: List<VerbEntity>,
    private val forms: List<VerbFormEntity> = emptyList(),
    private val partners: Map<Int, List<Int>> = emptyMap(),
) : VerbDao {
    var lastQuery: String? = null
        private set

    override fun observeCoreVerbs(query: String): Flow<List<VerbEntity>> {
        lastQuery = query
        return MutableStateFlow(
            verbs.filter { it.isCore }
                .filter { query.isEmpty() || it.matches(query) }
                .sortedBy { it.frequencyRank },
        )
    }

    override suspend fun getVerb(id: Int): VerbEntity? = verbs.firstOrNull { it.id == id }

    override suspend fun getForms(verbId: Int): List<VerbFormEntity> =
        forms.filter { it.verbId == verbId }.sortedWith(compareBy({ it.form }, { it.variant }))

    override suspend fun getPartners(verbId: Int): List<VerbEntity> =
        partners[verbId].orEmpty().mapNotNull { id -> verbs.firstOrNull { it.id == id } }

    override suspend fun getCoreVerbs(): List<VerbEntity> =
        verbs.filter { it.isCore }.sortedBy { it.frequencyRank }

    override suspend fun getAllPartners(): List<AspectPartnerEntity> =
        partners.flatMap { (verbId, ids) -> ids.map { AspectPartnerEntity(verbId, it) } }

    override suspend fun getTrainableForms(): List<TrainableFormRow> =
        verbs.filter { it.isCore }.sortedBy { it.frequencyRank }.flatMap { verb ->
            forms.filter { it.verbId == verb.id && it.isTrainable }
                .sortedWith(compareBy({ it.form }, { it.variant }))
                .map { form ->
                    TrainableFormRow(
                        verbId = verb.id,
                        infinitive = verb.infinitive,
                        aspect = verb.aspect,
                        translationEn = verb.translationEn,
                        translationDe = verb.translationDe,
                        form = form.form,
                        variant = form.variant,
                        value = form.value,
                    )
                }
        }

    private fun VerbEntity.matches(query: String): Boolean =
        listOf(infinitiveBare, translationDe, translationEn).any { it.contains(query) }
}

/**
 * Repository backed by a fixed list; the query filters by infinitive prefix. Core verbs are
 * [verbs] followed by the verbs of [trainable], in that order.
 */
class FakeVerbRepository(
    private val verbs: List<VerbSummary>,
    private val details: Map<Int, VerbDetail> = emptyMap(),
    private val trainable: List<TrainableForm> = emptyList(),
    private val partners: Map<Int, Set<Int>> = emptyMap(),
) : VerbRepository {
    private val source = MutableStateFlow(verbs)

    override fun observeVerbs(query: String): Flow<List<VerbSummary>> =
        source.map { all -> all.filter { it.infinitive.startsWith(query) } }

    override suspend fun getVerbDetail(id: Int): VerbDetail? = details[id]

    override suspend fun getTrainableForms(): List<TrainableForm> = trainable

    override suspend fun getCoreVerbs(): List<CoreVerb> =
        (verbs + trainable.map { it.verb }).distinctBy { it.id }.map { verb ->
            val linked = partners[verb.id].orEmpty() +
                partners.filterValues { verb.id in it }.keys
            CoreVerb(verb, linked)
        }
}

/** Progress backed by a map, using the real FSRS scheduler without fuzz. */
class FakeProgressRepository(private val scheduler: FsrsScheduler = FsrsScheduler(fuzz = null)) :
    ProgressRepository {
    private val state = MutableStateFlow<Map<CardKey, SrsCard>>(emptyMap())
    val cards: Map<CardKey, SrsCard> get() = state.value
    val introducedAt = mutableMapOf<CardKey, Instant>()
    val reviews = mutableListOf<Pair<CardKey, Rating>>()

    override suspend fun getCard(key: CardKey): SrsCard? = cards[key]

    override suspend fun recordReview(
        key: CardKey,
        rating: Rating,
        at: Instant,
        duration: Duration?,
    ): SrsCard {
        val reviewed = scheduler.review(cards[key] ?: SrsCard(due = at), rating, at)
        state.value += key to reviewed
        introducedAt.putIfAbsent(key, at)
        reviews += key to rating
        return reviewed
    }

    override suspend fun getDueCards(now: Instant, limit: Int): List<ScheduledCard> =
        cards.filterValues { !it.due.isAfter(now) }
            .entries.sortedBy { it.value.due }
            .take(limit)
            .map { ScheduledCard(it.key, it.value) }

    override suspend fun getKnownCards(): Set<CardKey> = cards.keys.toSet()

    override suspend fun countIntroducedSince(since: Instant): Int =
        introducedAt.values.count { !it.isBefore(since) }

    override fun observeCards(): Flow<Map<CardKey, SrsCard>> = state

    override fun observeCards(verbId: Int): Flow<Map<Exercise, SrsCard>> = state.map { all ->
        all.filterKeys { it.verbId == verbId }.mapKeys { it.key.exercise }
    }
}

class FakeSettingsRepository(initial: TrainingSettings = TrainingSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<TrainingSettings> = state

    override suspend fun setNewCardsPerDay(value: Int) {
        state.value = state.value.copy(newCardsPerDay = value)
    }

    override suspend fun setSessionSize(value: Int) {
        state.value = state.value.copy(sessionSize = value)
    }

    override suspend fun setStrictYo(value: Boolean) {
        state.value = state.value.copy(strictYo = value)
    }

    override suspend fun setAutoPlayAudio(value: Boolean) {
        state.value = state.value.copy(autoPlayAudio = value)
    }

    override suspend fun setPracticeMeaning(value: Boolean) {
        state.value = state.value.copy(practiceMeaning = value)
    }

    override suspend fun setTenses(tenses: Set<Tense>) {
        if (tenses.isNotEmpty()) state.value = state.value.copy(tenses = tenses)
    }

    override suspend fun replaceAll(settings: TrainingSettings) {
        state.value = settings
    }
}

fun trainableForm(
    verbId: Int,
    form: FormKey,
    vararg expected: String,
    infinitive: String = "verb$verbId",
): TrainableForm = TrainableForm(
    verb = verbSummary(verbId, infinitive),
    form = form,
    expected = expected.toList(),
)

fun formKey(verbId: Int, form: FormKey): CardKey = CardKey(verbId, Exercise.Form(form))

fun verbEntity(
    id: Int,
    infinitive: String,
    aspect: Aspect = Aspect.IMPERFECTIVE,
    isCore: Boolean = true,
    translationDe: String = "",
    translationEn: String = "",
): VerbEntity = VerbEntity(
    id = id,
    infinitive = infinitive,
    infinitiveBare = infinitive.replace(STRESS_MARK, ""),
    aspect = aspect.name,
    frequencyRank = id,
    isCore = isCore,
    translationEn = translationEn,
    translationDe = translationDe,
)

fun formEntity(
    verbId: Int,
    form: String,
    value: String,
    variant: Int = 0,
    isTrainable: Boolean = true,
): VerbFormEntity = VerbFormEntity(
    verbId = verbId,
    form = form,
    variant = variant,
    value = value,
    valueBare = value.replace(STRESS_MARK, ""),
    isTrainable = isTrainable,
)

fun verbSummary(
    id: Int,
    infinitive: String,
    aspect: Aspect = Aspect.IMPERFECTIVE,
    german: String = "",
    english: String = "",
): VerbSummary = VerbSummary(
    id = id,
    infinitive = infinitive,
    aspect = aspect,
    translation = Translation(german = german, english = english),
)

/** Records spoken text instead of playing it; speaks only when READY, like the real one. */
class FakeSpeaker(initial: SpeechAvailability = SpeechAvailability.READY) : Speaker {
    override val availability = MutableStateFlow(initial)
    val spoken = mutableListOf<String>()
    var isClosed = false
        private set

    override fun speak(text: String) {
        if (availability.value == SpeechAvailability.READY) spoken += text
    }

    var refreshes = 0
        private set

    override fun refresh() {
        refreshes++
    }

    override fun close() {
        isClosed = true
    }
}
