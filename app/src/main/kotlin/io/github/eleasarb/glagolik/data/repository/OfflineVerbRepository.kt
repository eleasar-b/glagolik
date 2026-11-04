package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.data.db.VerbDao
import io.github.eleasarb.glagolik.data.db.VerbEntity
import io.github.eleasarb.glagolik.data.db.VerbFormEntity
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Translation
import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** U+0301 COMBINING ACUTE ACCENT (non-printable on its own, hence the escape). */
private const val STRESS_MARK = '\u0301'

class OfflineVerbRepository @Inject constructor(private val dao: VerbDao) : VerbRepository {
    override fun observeVerbs(query: String): Flow<List<VerbSummary>> =
        dao.observeCoreVerbs(normalizeQuery(query)).map { verbs -> verbs.map { it.toSummary() } }

    override suspend fun getVerbDetail(id: Int): VerbDetail? {
        val verb = dao.getVerb(id) ?: return null
        return VerbDetail(
            summary = verb.toSummary(),
            forms = dao.getForms(id).toConjugatedForms(),
            partners = dao.getPartners(id).map { it.toSummary() },
        )
    }
}

/** Strips stress marks and surrounding whitespace; the bare columns carry no marks. */
internal fun normalizeQuery(query: String): String =
    query.trim().replace(STRESS_MARK.toString(), "")

internal fun VerbEntity.toSummary(): VerbSummary = VerbSummary(
    id = id,
    infinitive = infinitive,
    aspect = Aspect.valueOf(aspect),
    translation = Translation(german = translationDe, english = translationEn),
)

/** Groups variants per slot and orders slots by grammar (FormKey), not alphabetically. */
internal fun List<VerbFormEntity>.toConjugatedForms(): List<ConjugatedForm> =
    groupBy { FormKey.valueOf(it.form) }
        .toSortedMap()
        .map { (key, rows) ->
            val sorted = rows.sortedBy { it.variant }
            ConjugatedForm(
                key = key,
                variants = sorted.map { it.value },
                isTrainable = sorted.all { it.isTrainable },
            )
        }
