package io.github.eleasarb.glagolik.importer

import java.io.File
import org.junit.Assert.assertEquals
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
     * run the test once with `-DupdateSnapshots=true` and review the diff.
     */
    @Test
    fun `output matches snapshot`() {
        val result = runImport(input, overrides, coreSize = CORE_SIZE)
        val database = temp.newFile("verbs.db")
        writeDatabase(database, result.verbs, result.links)
        val actual = buildString {
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
        val noOverrides = temp.newFile("overrides.csv").apply { writeText(HEADER) }
        val error = assertThrows(ImportException::class.java) {
            runImport(input, noOverrides, coreSize = CORE_SIZE)
        }
        assertTrue(error.message.orEmpty().contains("хоте'ться (imperfective): past_m: missing"))
    }

    @Test
    fun `override for unknown verb fails the import`() {
        val typo = temp.newFile("overrides.csv").apply {
            writeText(HEADER + "несуществующий,perfective,EXCLUDE,,,typo\n")
        }
        val error = assertThrows(ImportException::class.java) {
            runImport(input, typo, coreSize = CORE_SIZE)
        }
        assertTrue(error.message.orEmpty().contains("without matching source row"))
    }

    @Test
    fun `excluded verb is replaced by the next one`() {
        val result = runImport(input, overrides, coreSize = SIZE_WITH_EXCLUDED)
        val core = result.verbs.filter { it.isCore }.map { toBare(it.infinitive) }
        assertTrue("установить" !in core)
        assertTrue("поднимать" in core)
    }

    @Test
    fun `partner outside the core set is added and linked both ways`() {
        val result = runImport(input, overrides, coreSize = CORE_SIZE)
        val podnimat = result.verbs.single { toBare(it.infinitive) == "поднимать" }
        val podnyat = result.verbs.single { toBare(it.infinitive) == "поднять" }
        assertEquals(false, podnimat.isCore)
        assertTrue(podnyat.id to podnimat.id in result.links)
        assertTrue(podnimat.id to podnyat.id in result.links)
    }

    @Test
    fun `verify detects stale output`() {
        val paths = ImportPaths(
            input = input,
            overrides = overrides,
            selection = temp.newFile("selection.csv"),
            report = temp.newFile("report.md"),
            database = File(temp.root, "verbs.db"),
        )
        execute(paths, verify = false, coreSize = CORE_SIZE)
        execute(paths, verify = true, coreSize = CORE_SIZE)

        paths.selection.appendText("tampered\n")
        assertThrows(ImportException::class.java) {
            execute(paths, verify = true, coreSize = CORE_SIZE)
        }
    }

    private companion object {
        const val CORE_SIZE = 5
        const val SIZE_WITH_EXCLUDED = 7
        const val HEADER = "accented,aspect,action,fields,value,reason\n"
    }
}
