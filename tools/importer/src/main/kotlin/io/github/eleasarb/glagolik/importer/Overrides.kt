package io.github.eleasarb.glagolik.importer

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

enum class OverrideAction {
    /** Drop the verb from the data set. */
    EXCLUDE,

    /** The listed fields are legitimately empty (e.g. impersonal verbs). */
    ALLOW_MISSING,

    /**
     * Replace one form field (source notation) or one translation with a corrected value.
     * Human-reviewed only.
     */
    SET,

    /**
     * Keep the forms built from the listed fields in the data, but never quiz them
     * (e.g. быть: the present tense is not used in modern Russian).
     */
    NO_TRAINING,

    /**
     * Replace the frequency rank (the source row number) with the value. For rows where the
     * source attaches the frequency of a homograph to the wrong entry; move the rank of both
     * entries so every rank stays unique.
     */
    RANK,
}

data class Override(
    val key: VerbKey,
    val action: OverrideAction,
    val fields: List<SourceField>,
    val value: String,
    val reason: String,
    /** Translation replaced by a SET; then [fields] is empty. */
    val translation: TranslationField? = null,
) {
    /** All targeted columns as written in `overrides.csv`. */
    val columns: List<String>
        get() = fields.map { it.column } + listOfNotNull(translation?.column)

    /** What the override changes, for the import report. */
    val target: String
        get() = if (action == OverrideAction.RANK) "rank $value" else columns.joinToString(";")
}

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
        val rank = byKey[verb.key].orEmpty()
            .lastOrNull { it.action == OverrideAction.RANK }
            ?.value
            ?.toInt()
            ?: verb.rank
        val sets = byKey[verb.key].orEmpty().filter { it.action == OverrideAction.SET }
        if (sets.isEmpty()) return verb.copy(rank = rank)
        val fields = verb.fields.toMutableMap()
        var translationEn = verb.translationEn
        var translationDe = verb.translationDe
        sets.forEach { override ->
            override.fields.forEach { fields[it] = override.value }
            when (override.translation) {
                TranslationField.EN -> translationEn = override.value
                TranslationField.DE -> translationDe = override.value
                null -> Unit
            }
        }
        return verb.copy(
            rank = rank,
            translationEn = translationEn,
            translationDe = translationDe,
            fields = fields,
        )
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
            val columns = row["fields"].orEmpty()
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val translations = columns.mapNotNull { TranslationField.fromColumn(it) }
            val fields = columns
                .filter { TranslationField.fromColumn(it) == null }
                .map { column ->
                    SourceField.fromColumn(column)
                        ?: error("overrides.csv:$line: unknown field '$column'")
                }
            val value = row["value"].orEmpty().trim()
            val reason = row["reason"].orEmpty().trim()
            require(reason.isNotEmpty()) { "overrides.csv:$line: reason is mandatory" }
            require(action != OverrideAction.SET || columns.size == 1) {
                "overrides.csv:$line: SET needs exactly one field"
            }
            require(translations.isEmpty() || action == OverrideAction.SET) {
                "overrides.csv:$line: translations can only be changed with SET"
            }
            require(translations.isEmpty() || value.isNotEmpty()) {
                "overrides.csv:$line: SET of a translation needs a value"
            }
            require(action != OverrideAction.RANK || columns.isEmpty()) {
                "overrides.csv:$line: RANK takes no fields"
            }
            require(action != OverrideAction.RANK || (value.toIntOrNull() ?: 0) > 0) {
                "overrides.csv:$line: RANK needs a positive number as value"
            }
            require(action != OverrideAction.NO_TRAINING || fields.isNotEmpty()) {
                "overrides.csv:$line: NO_TRAINING needs at least one field"
            }
            return Override(
                key = VerbKey(row.getValue("accented").trim(), row.getValue("aspect").trim()),
                action = action,
                fields = fields,
                value = value,
                reason = reason,
                translation = translations.singleOrNull(),
            )
        }
    }
}
