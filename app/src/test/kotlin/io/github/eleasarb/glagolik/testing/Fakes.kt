package io.github.eleasarb.glagolik.testing

import io.github.eleasarb.glagolik.data.db.VerbDao
import io.github.eleasarb.glagolik.data.db.VerbEntity
import io.github.eleasarb.glagolik.data.db.VerbFormEntity
import io.github.eleasarb.glagolik.data.repository.VerbRepository
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
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

    private fun VerbEntity.matches(query: String): Boolean =
        listOf(infinitiveBare, translationDe, translationEn).any { it.contains(query) }
}

/** Repository backed by a fixed list; the query filters by infinitive prefix. */
class FakeVerbRepository(
    private val verbs: List<VerbSummary>,
    private val details: Map<Int, VerbDetail> = emptyMap(),
) : VerbRepository {
    private val source = MutableStateFlow(verbs)

    override fun observeVerbs(query: String): Flow<List<VerbSummary>> =
        source.map { all -> all.filter { it.infinitive.startsWith(query) } }

    override suspend fun getVerbDetail(id: Int): VerbDetail? = details[id]
}

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

fun verbSummary(id: Int, infinitive: String): VerbSummary = VerbSummary(
    id = id,
    infinitive = infinitive,
    aspect = Aspect.IMPERFECTIVE,
    translation = Translation(german = "", english = ""),
)
