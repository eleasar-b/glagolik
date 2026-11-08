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

    @Query("SELECT * FROM verb WHERE is_core = 1 ORDER BY frequency_rank")
    suspend fun getCoreVerbs(): List<VerbEntity>

    /** All aspect partner links; each pair is stored in both directions. */
    @Query("SELECT * FROM aspect_partner")
    suspend fun getAllPartners(): List<AspectPartnerEntity>

    @Query("SELECT * FROM verb WHERE id = :id")
    suspend fun getVerb(id: Int): VerbEntity?

    @Query("SELECT * FROM verb_form WHERE verb_id = :verbId ORDER BY form, variant")
    suspend fun getForms(verbId: Int): List<VerbFormEntity>

    /** Trainable forms of core verbs, in frequency order of the verbs. */
    @Query(
        """
        SELECT verb.id AS verb_id, verb.infinitive, verb.aspect, verb.translation_en,
            verb.translation_de, verb_form.form, verb_form.variant, verb_form.value
        FROM verb_form
        JOIN verb ON verb.id = verb_form.verb_id
        WHERE verb.is_core = 1 AND verb_form.is_trainable = 1
        ORDER BY verb.frequency_rank, verb_form.form, verb_form.variant
        """,
    )
    suspend fun getTrainableForms(): List<TrainableFormRow>

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
