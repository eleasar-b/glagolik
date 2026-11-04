package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.domain.model.VerbDetail
import io.github.eleasarb.glagolik.domain.model.VerbSummary
import kotlinx.coroutines.flow.Flow

interface VerbRepository {
    /** Core verbs in frequency order, filtered by [query] (infinitive or translation). */
    fun observeVerbs(query: String): Flow<List<VerbSummary>>

    /** Full conjugation and aspect partners, or `null` if the id is unknown. */
    suspend fun getVerbDetail(id: Int): VerbDetail?
}
