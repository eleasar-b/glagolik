package io.github.eleasarb.glagolik.importer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormBuilderTest {
    private val source = readSource(fixture("fixture-verbs.csv")).associateBy { it.bare }

    @Test
    fun `imperfective gets present and compound future`() {
        val forms = buildForms(source.getValue("есть"), Aspect.IMPERFECTIVE).byKey()
        assertEquals(listOf("е́м"), forms[FormKey.PRESENT_SG1])
        assertEquals(listOf("бу́ду есть"), forms[FormKey.FUTURE_SG1])
        assertEquals(listOf("бу́дут есть"), forms[FormKey.FUTURE_PL3])
    }

    @Test
    fun `perfective gets simple future and no present`() {
        val forms = buildForms(source.getValue("сказать"), Aspect.PERFECTIVE).byKey()
        assertEquals(listOf("скажу́"), forms[FormKey.FUTURE_SG1])
        assertTrue(PRESENT_KEYS.none { it in forms })
    }

    @Test
    fun `biaspectual gets the same forms as present and future`() {
        val forms = buildForms(source.getValue("использовать"), Aspect.BIASPECTUAL).byKey()
        assertEquals(forms[FormKey.PRESENT_SG1], forms[FormKey.FUTURE_SG1])
        assertEquals(listOf("испо́льзую"), forms[FormKey.PRESENT_SG1])
    }

    @Test
    fun `быть forms its future from the auxiliary alone`() {
        val forms = buildForms(source.getValue("быть"), Aspect.IMPERFECTIVE).byKey()
        assertEquals(listOf("бу́ду"), forms[FormKey.FUTURE_SG1])
    }

    @Test
    fun `impersonal verb gets future only where a present form exists`() {
        val forms = buildForms(source.getValue("хотеться"), Aspect.IMPERFECTIVE).byKey()
        assertEquals(listOf("бу́дет хоте́ться"), forms[FormKey.FUTURE_SG3])
        assertEquals(null, forms[FormKey.FUTURE_SG1])
    }

    @Test
    fun `variants keep their source order`() {
        val forms = buildForms(source.getValue("поднять"), Aspect.PERFECTIVE).byKey()
        assertEquals(listOf("по́днял", "подня́л"), forms[FormKey.PAST_M])
    }

    private fun List<VerbForm>.byKey(): Map<FormKey, List<String>> =
        groupBy({ it.form }, { it.value })

    private companion object {
        val PRESENT_KEYS = FormKey.entries.filter { it.name.startsWith("PRESENT") }
    }
}

internal fun fixture(name: String): File =
    File(requireNotNull(FormBuilderTest::class.java.getResource("/$name")).toURI())
