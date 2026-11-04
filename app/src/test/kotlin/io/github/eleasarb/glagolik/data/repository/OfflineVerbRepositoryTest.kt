package io.github.eleasarb.glagolik.data.repository

import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.testing.FakeVerbDao
import io.github.eleasarb.glagolik.testing.formEntity
import io.github.eleasarb.glagolik.testing.verbEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineVerbRepositoryTest {
    private val dao = FakeVerbDao(
        verbs = listOf(
            verbEntity(1, "быть", translationDe = "sein", translationEn = "be"),
            verbEntity(2, "подня́ть", Aspect.PERFECTIVE, translationDe = "heben"),
            verbEntity(3, "поднима́ть", isCore = false),
        ),
        forms = listOf(
            formEntity(2, "PAST_M", "подня́л", variant = 1),
            formEntity(2, "PAST_M", "по́днял", variant = 0),
            formEntity(2, "FUTURE_SG1", "подниму́"),
            formEntity(2, "IMPERATIVE_SG", "подними́"),
            formEntity(1, "PRESENT_SG1", "есть", isTrainable = false),
        ),
        partners = mapOf(2 to listOf(3)),
    )
    private val repository = OfflineVerbRepository(dao)

    @Test
    fun `stress marks are removed from the search query`() = runTest {
        val result = repository.observeVerbs("  подня́ть ").first()
        assertEquals("поднять", dao.lastQuery)
        assertEquals(listOf(2), result.map { it.id })
    }

    @Test
    fun `entities are mapped to domain summaries`() = runTest {
        val byt = repository.observeVerbs("").first().first()
        assertEquals("быть", byt.infinitive)
        assertEquals(Aspect.IMPERFECTIVE, byt.aspect)
        assertEquals("sein", byt.translation.german)
        assertEquals("be", byt.translation.english)
    }

    @Test
    fun `forms are ordered by grammar and variants by source order`() = runTest {
        val detail = checkNotNull(repository.getVerbDetail(2))
        assertEquals(
            listOf(FormKey.FUTURE_SG1, FormKey.PAST_M, FormKey.IMPERATIVE_SG),
            detail.forms.map { it.key },
        )
        assertEquals(listOf("по́днял", "подня́л"), detail.formsOf(Tense.PAST).single().variants)
    }

    @Test
    fun `detail contains partners including non-core verbs`() = runTest {
        val detail = checkNotNull(repository.getVerbDetail(2))
        assertEquals(listOf(3), detail.partners.map { it.id })
    }

    @Test
    fun `trainable flag is carried over`() = runTest {
        val detail = checkNotNull(repository.getVerbDetail(1))
        assertEquals(false, detail.forms.single().isTrainable)
    }

    @Test
    fun `unknown id gives null`() = runTest {
        assertNull(repository.getVerbDetail(999))
    }
}
