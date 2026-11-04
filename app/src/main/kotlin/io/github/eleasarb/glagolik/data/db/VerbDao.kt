package io.github.eleasarb.glagolik.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerbDao {
    /**
     * Core verbs in frequency order. An empty [query] matches everything; otherwise the
     * infinitive (without stress marks) or a translation must contain it.
     */
    @Query(
        """
        SELECT * FROM verb
        WHERE is_core = 1
          AND (:query = ''
            OR infinitive_bare LIKE '%' || :query || '%'
            OR translation_de LIKE '%' || :query || '%'
            OR translation_en LIKE '%' || :query || '%')
        ORDER BY frequency_rank
        """,
    )
    fun observeCoreVerbs(query: String): Flow<List<VerbEntity>>

    @Query("SELECT * FROM verb WHERE id = :id")
    suspend fun getVerb(id: Int): VerbEntity?

    @Query("SELECT * FROM verb_form WHERE verb_id = :verbId ORDER BY form, variant")
    suspend fun getForms(verbId: Int): List<VerbFormEntity>

    @Query(
        """
        SELECT verb.* FROM verb
        JOIN aspect_partner ON aspect_partner.partner_id = verb.id
        WHERE aspect_partner.verb_id = :verbId
        ORDER BY verb.frequency_rank
        """,
    )
    suspend fun getPartners(verbId: Int): List<VerbEntity>
}
