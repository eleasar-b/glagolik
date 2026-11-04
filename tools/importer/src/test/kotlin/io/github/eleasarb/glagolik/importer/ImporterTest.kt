package io.github.eleasarb.glagolik.importer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImporterTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val input = fixture("fixture-verbs.csv")
    private val overrides = fixture("fixture-overrides.csv")

    /**
     * Snapshot of the full output for the fixture. To update after an intended change,
     * run `./gradlew :tools:importer:test -PupdateSnapshots=true` and review the diff.
     */
    @Test
    fun `output matches snapshot`() {
        val result = import()
        val database = temp.newFile("verbs.db")
        writeDatabase(database, result.verbs, result.links)
        val actual = buildString {
            appendLine("## verb-ids.csv")
            append(result.idsCsv)
            appendLine("## selection.csv")
            append(result.selectionCsv)
            appendLine("## report")
            append(result.report)
            appendLine("## database")
            append(dumpDatabase(database))
        }
        val snapshot = File("src/test/resources/snapshot.txt")
        if (System.getProperty("updateSnapshots") == "true") snapshot.writeText(actual)
        assertEquals(snapshot.readText(), actual)
    }

    @Test
    fun `invalid core verb fails the import`() {
        val noOverrides = temp.newFile("overrides.csv").apply { writeText(OVERRIDES_HEADER) }
        val error =
            assertThrows(ImportException::class.java) { import(overridesFile = noOverrides) }
        assertTrue(error.message.orEmpty().contains("хоте'ться (imperfective): past_m: missing"))
    }

    @Test
    fun `override for unknown verb fails the import`() {
        val typo = temp.newFile("overrides.csv").apply {
            writeText(OVERRIDES_HEADER + "несуществующий,perfective,EXCLUDE,,,typo\n")
        }
        val error = assertThrows(ImportException::class.java) { import(overridesFile = typo) }
        assertTrue(error.message.orEmpty().contains("without matching source row"))
    }

    @Test
    fun `excluded verb is replaced by the next one`() {
        val result = import(coreSize = SIZE_WITH_EXCLUDED)
        val core = result.verbs.filter { it.isCore }.map { toBare(it.infinitive) }
        assertTrue("установить" !in core)
        assertTrue("поднимать" in core)
    }

    @Test
    fun `partner outside the core set is added and linked both ways`() {
        val result = import()
        val podnimat = result.verbs.single { toBare(it.infinitive) == "поднимать" }
        val podnyat = result.verbs.single { toBare(it.infinitive) == "поднять" }
        assertEquals(false, podnimat.isCore)
        assertTrue(podnyat.id to podnimat.id in result.links)
        assertTrue(podnimat.id to podnyat.id in result.links)
    }

    @Test
    fun `NO_TRAINING keeps the forms but marks them as not trainable`() {
        val byt = import().verbs.single { it.infinitive == "быть" }
        val present = byt.forms.filter { it.form.name.startsWith("PRESENT") }
        val future = byt.forms.filter { it.form.name.startsWith("FUTURE") }
        assertEquals(6, present.size)
        assertTrue(present.none { it.isTrainable })
        assertTrue(future.all { it.isTrainable })
    }

    @Test
    fun `first import numbers verbs densely in frequency order`() {
        val result = import()
        assertEquals((1..result.verbs.size).toList(), result.verbs.map { it.id })
        assertEquals(result.verbs.sortedBy { it.frequencyRank }, result.verbs)
    }

    @Test
    fun `existing ids are kept and retired ids are never reused`() {
        val ids = temp.newFile("verb-ids.csv").apply {
            writeText(
                "id,accented,aspect\n" +
                    "10,сказа'ть,perfective\n" +
                    "20,стари'нный,perfective\n",
            )
        }
        val result = import(idsFile = ids)
        val byInfinitive = result.verbs.associateBy { toBare(it.infinitive) }

        assertEquals(10, byInfinitive.getValue("сказать").id)
        assertEquals(21, byInfinitive.getValue("быть").id)
        assertTrue(result.verbs.none { it.id == 20 })
        assertTrue(result.idsCsv.contains("20,стари'нный,perfective"))
    }

    @Test
    fun `duplicate id in verb-ids csv fails the import`() {
        val ids = temp.newFile("verb-ids.csv").apply {
            writeText("id,accented,aspect\n1,быть,imperfective\n1,сказа'ть,perfective\n")
        }
        val error = assertThrows(ImportException::class.java) { import(idsFile = ids) }
        assertTrue(error.message.orEmpty().contains("duplicate id"))
    }

    @Test
    fun `verify detects stale output`() {
        val paths = ImportPaths(
            input = input,
            overrides = overrides,
            ids = File(temp.root, "verb-ids.csv"),
            selection = temp.newFile("selection.csv"),
            report = temp.newFile("report.md"),
            database = File(temp.root, "verbs.db"),
        )
        execute(paths, verify = false, coreSize = CORE_SIZE)
        execute(paths, verify = true, coreSize = CORE_SIZE)
        assertFalse(paths.ids.readText().isBlank())

        paths.selection.appendText("tampered\n")
        assertThrows(ImportException::class.java) {
            execute(paths, verify = true, coreSize = CORE_SIZE)
        }
    }

    private fun import(
        overridesFile: File = overrides,
        idsFile: File = File(temp.root, "no-ids.csv"),
        coreSize: Int = CORE_SIZE,
    ): ImportResult = runImport(input, overridesFile, idsFile, coreSize)

    private companion object {
        const val CORE_SIZE = 5
        const val SIZE_WITH_EXCLUDED = 7
        const val OVERRIDES_HEADER = "accented,aspect,action,fields,value,reason\n"
    }
}
