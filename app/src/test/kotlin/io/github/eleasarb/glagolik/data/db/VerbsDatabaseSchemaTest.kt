package io.github.eleasarb.glagolik.data.db

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards against a crash on first start: Room rejects a pre-packaged database whose
 * schema differs from the entities. This test builds a database from Room's exported
 * schema (app/schemas) and compares it, table by table, with the bundled asset.
 */
class VerbsDatabaseSchemaTest {
    private val asset = File("src/main/assets/verbs.db")
    private val roomSchema = File(
        "schemas/${VerbsDatabase::class.qualifiedName}/${VerbsDatabase.VERSION}.json",
    )

    @Test
    fun `asset version matches the Room database version`() {
        connect(asset).use { db ->
            assertEquals(VerbsDatabase.VERSION, db.query("PRAGMA user_version").single().single())
        }
    }

    @Test
    fun `asset schema matches the Room entities`() {
        val statements = createStatements(roomSchema.readText())
        DriverManager.getConnection("jdbc:sqlite::memory:").use { expected ->
            expected.createStatement().use { s -> statements.forEach(s::execute) }
            connect(asset).use { actual ->
                assertEquals(tables(expected), tables(actual))
                tables(expected).forEach { table ->
                    assertEquals(table, describe(expected, table), describe(actual, table))
                }
            }
        }
    }

    private fun connect(file: File): Connection =
        DriverManager.getConnection("jdbc:sqlite:${file.path}")

    private fun tables(db: Connection): List<String> = db.query(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
            "AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name",
    ).map { it.single().toString() }

    /** Columns, foreign keys and indices in an order-independent, comparable form. */
    private fun describe(db: Connection, table: String): Map<String, Any> = mapOf(
        "columns" to db.query("PRAGMA table_info(`$table`)"),
        "foreignKeys" to db.query("PRAGMA foreign_key_list(`$table`)")
            .map { it.drop(1) }
            .sortedBy { it.toString() },
        "indices" to db.query("PRAGMA index_list(`$table`)")
            .map { row ->
                val name = row[1].toString()
                listOf(name, row[2], row[3]) + db.query("PRAGMA index_info(`$name`)")
            }
            .sortedBy { it.toString() },
    )

    private fun Connection.query(sql: String): List<List<Any?>> = createStatement().use { s ->
        s.executeQuery(sql).use { rows ->
            val count = rows.metaData.columnCount
            buildList {
                while (rows.next()) add((1..count).map { rows.getObject(it) })
            }
        }
    }

    private companion object {
        private val ENTITY = Regex("\"tableName\"\\s*:\\s*\"(\\w+)\"")
        private val CREATE_SQL = Regex("\"createSql\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

        /** Extracts CREATE TABLE / CREATE INDEX statements per entity from Room's JSON. */
        fun createStatements(json: String): List<String> {
            val starts = ENTITY.findAll(json).toList()
            return starts.flatMapIndexed { index, match ->
                val end = starts.getOrNull(index + 1)?.range?.first ?: json.length
                val block = json.substring(match.range.first, end)
                val table = match.groupValues[1]
                CREATE_SQL.findAll(block).map { sql ->
                    sql.groupValues[1].replace("\\\"", "\"").replace("\${TABLE_NAME}", table)
                }.toList()
            }
        }
    }
}
