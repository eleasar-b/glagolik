package io.github.eleasarb.glagolik.importer

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** Bump when the schema changes; the app's Room database version must match. */
const val SCHEMA_VERSION = 1

/**
 * Schema of `verbs.db`, written in Room's style (backticks, index names) so that the Room
 * entities can mirror it 1:1 and `createFromAsset` validates.
 */
private val SCHEMA = listOf(
    """
    CREATE TABLE `verb` (
        `id` INTEGER NOT NULL,
        `infinitive` TEXT NOT NULL,
        `infinitive_bare` TEXT NOT NULL,
        `aspect` TEXT NOT NULL,
        `frequency_rank` INTEGER NOT NULL,
        `is_core` INTEGER NOT NULL,
        `translation_en` TEXT NOT NULL,
        `translation_de` TEXT NOT NULL,
        PRIMARY KEY(`id`)
    )
    """,
    "CREATE INDEX `index_verb_infinitive_bare` ON `verb` (`infinitive_bare`)",
    """
    CREATE TABLE `verb_form` (
        `verb_id` INTEGER NOT NULL,
        `form` TEXT NOT NULL,
        `variant` INTEGER NOT NULL,
        `value` TEXT NOT NULL,
        `value_bare` TEXT NOT NULL,
        `is_trainable` INTEGER NOT NULL,
        PRIMARY KEY(`verb_id`, `form`, `variant`),
        FOREIGN KEY(`verb_id`) REFERENCES `verb`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )
    """,
    "CREATE INDEX `index_verb_form_verb_id` ON `verb_form` (`verb_id`)",
    """
    CREATE TABLE `aspect_partner` (
        `verb_id` INTEGER NOT NULL,
        `partner_id` INTEGER NOT NULL,
        PRIMARY KEY(`verb_id`, `partner_id`),
        FOREIGN KEY(`verb_id`) REFERENCES `verb`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
        FOREIGN KEY(`partner_id`) REFERENCES `verb`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    )
    """,
    "CREATE INDEX `index_aspect_partner_verb_id` ON `aspect_partner` (`verb_id`)",
    "CREATE INDEX `index_aspect_partner_partner_id` ON `aspect_partner` (`partner_id`)",
)

private val DUMP_QUERIES = listOf(
    "SELECT * FROM verb ORDER BY id",
    "SELECT * FROM verb_form ORDER BY verb_id, form, variant",
    "SELECT * FROM aspect_partner ORDER BY verb_id, partner_id",
)

fun writeDatabase(file: File, verbs: List<Verb>, links: List<Pair<Int, Int>>) {
    file.parentFile?.mkdirs()
    file.delete()
    connect(file).use { connection ->
        connection.autoCommit = false
        connection.createStatement().use { statement ->
            SCHEMA.forEach { statement.execute(it.trimIndent()) }
            statement.execute("PRAGMA user_version = $SCHEMA_VERSION")
        }
        insertVerbs(connection, verbs)
        insertLinks(connection, links)
        connection.commit()
        connection.autoCommit = true
        connection.createStatement().use { it.execute("VACUUM") }
    }
}

/** Canonical text form of the database content, used to compare two databases. */
fun dumpDatabase(file: File): String = connect(file).use { connection ->
    buildString {
        connection.createStatement().use { statement ->
            appendLine("user_version=" + statement.executeQuery("PRAGMA user_version").getInt(1))
            DUMP_QUERIES.forEach { query ->
                appendLine("# $query")
                statement.executeQuery(query).use { rows ->
                    val columns = rows.metaData.columnCount
                    while (rows.next()) {
                        appendLine((1..columns).joinToString("\t") { rows.getString(it) })
                    }
                }
            }
        }
    }
}

private fun connect(file: File): Connection =
    DriverManager.getConnection("jdbc:sqlite:${file.path}")

private fun insertVerbs(connection: Connection, verbs: List<Verb>) {
    insertRows(
        connection,
        table = "verb",
        rows = verbs.map { verb ->
            listOf(
                verb.id,
                verb.infinitive,
                toBare(verb.infinitive),
                verb.aspect.name,
                verb.frequencyRank,
                if (verb.isCore) 1 else 0,
                verb.translationEn,
                verb.translationDe,
            )
        },
    )
    insertRows(
        connection,
        table = "verb_form",
        rows = verbs.flatMap { verb ->
            verb.forms.map { form ->
                listOf(
                    verb.id,
                    form.form.name,
                    form.variant,
                    form.value,
                    toBare(form.value),
                    if (form.isTrainable) 1 else 0,
                )
            }
        },
    )
}

private fun insertLinks(connection: Connection, links: List<Pair<Int, Int>>) {
    insertRows(
        connection,
        table = "aspect_partner",
        rows = links.sortedWith(compareBy({ it.first }, { it.second }))
            .map { (verb, partner) -> listOf(verb, partner) },
    )
}

/** Inserts rows positionally; every row must have one value per table column. */
private fun insertRows(connection: Connection, table: String, rows: List<List<Any>>) {
    if (rows.isEmpty()) return
    val placeholders = rows.first().joinToString(", ") { "?" }
    connection.prepareStatement("INSERT INTO `$table` VALUES ($placeholders)").use { insert ->
        rows.forEach { row ->
            row.forEachIndexed { index, value -> insert.setObject(index + 1, value) }
            insert.executeUpdate()
        }
    }
}
