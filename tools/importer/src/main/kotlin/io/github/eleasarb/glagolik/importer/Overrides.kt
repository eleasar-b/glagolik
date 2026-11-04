package io.github.eleasarb.glagolik.importer

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

enum class OverrideAction {
    /** Drop the verb from the data set. */
    EXCLUDE,

    /** The listed fields are legitimately empty (e.g. impersonal verbs). */
    ALLOW_MISSING,

    /** Replace one field with a corrected value (source notation). Human-reviewed only. */
    SET,

    /**
     * Keep the forms built from the listed fields in the data, but never quiz them
     * (e.g. быть: the present tense is not used in modern Russian).
     */
    NO_TRAINING,
}

data class Override(
    val key: VerbKey,
    val action: OverrideAction,
    val fields: List<SourceField>,
    val value: String,
    val reason: String,
)

class Overrides(private val entries: List<Override>) {
    private val byKey = entries.groupBy { it.key }

    fun isExcluded(key: VerbKey): Boolean =
        byKey[key].orEmpty().any { it.action == OverrideAction.EXCLUDE }

    fun allowedMissing(key: VerbKey): Set<SourceField> = byKey[key].orEmpty()
        .filter { it.action == OverrideAction.ALLOW_MISSING }
        .flatMap { it.fields }
        .toSet()

    fun notTrainable(key: VerbKey): Set<SourceField> = byKey[key].orEmpty()
        .filter { it.action == OverrideAction.NO_TRAINING }
        .flatMap { it.fields }
        .toSet()

    fun apply(verb: SourceVerb): SourceVerb {
        val sets = byKey[verb.key].orEmpty().filter { it.action == OverrideAction.SET }
        if (sets.isEmpty()) return verb
        val fields = verb.fields.toMutableMap()
        sets.forEach { override -> override.fields.forEach { fields[it] = override.value } }
        return verb.copy(fields = fields)
    }

    /** Overrides that point to no source row, usually a typo in `overrides.csv`. */
    fun unmatched(source: List<SourceVerb>): List<Override> {
        val keys = source.map { it.key }.toSet()
        return entries.filter { it.key !in keys }
    }

    val all: List<Override> get() = entries

    companion object {
        fun read(file: File): Overrides {
            if (!file.exists()) return Overrides(emptyList())
            val rows = csvReader().readAllWithHeader(file)
            return Overrides(rows.mapIndexed { index, row -> parse(row, line = index + 2) })
        }

        private fun parse(row: Map<String, String>, line: Int): Override {
            val action = OverrideAction.entries.firstOrNull { it.name == row["action"]?.trim() }
                ?: error("overrides.csv:$line: unknown action '${row["action"]}'")
            val fields = row["fields"].orEmpty()
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { column ->
                    SourceField.fromColumn(column)
                        ?: error("overrides.csv:$line: unknown field '$column'")
                }
            val reason = row["reason"].orEmpty().trim()
            require(reason.isNotEmpty()) { "overrides.csv:$line: reason is mandatory" }
            require(action != OverrideAction.SET || fields.size == 1) {
                "overrides.csv:$line: SET needs exactly one field"
            }
            require(action != OverrideAction.NO_TRAINING || fields.isNotEmpty()) {
                "overrides.csv:$line: NO_TRAINING needs at least one field"
            }
            return Override(
                key = VerbKey(row.getValue("accented").trim(), row.getValue("aspect").trim()),
                action = action,
                fields = fields,
                value = row["value"].orEmpty().trim(),
                reason = reason,
            )
        }
    }
}
