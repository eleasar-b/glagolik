package io.github.eleasarb.glagolik.importer

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

/**
 * Stable verb ids, persisted in `data/verb-ids.csv`.
 *
 * Learning progress references these ids, so they must survive changes to the raw data:
 * - a verb keeps its id as long as its key (accented infinitive + aspect) is unchanged
 * - new verbs get the next free id, in frequency order
 * - ids of verbs that drop out of the selection stay reserved and are never reused
 */
class VerbIds private constructor(private val ids: Map<VerbKey, Int>) {
    fun idOf(key: VerbKey): Int = ids[key] ?: error("no id assigned for $key")

    /** Returns a copy in which every key in [keysInRankOrder] has an id. */
    fun assign(keysInRankOrder: List<VerbKey>): VerbIds {
        var next = (ids.values.maxOrNull() ?: 0) + 1
        val result = LinkedHashMap(ids)
        keysInRankOrder.forEach { key ->
            if (key !in result) result[key] = next++
        }
        return VerbIds(result)
    }

    fun toCsv(): String = buildString {
        appendLine(HEADER)
        ids.entries.sortedBy { it.value }.forEach { (key, id) ->
            appendLine("$id,${key.accented},${key.aspect}")
        }
    }

    companion object {
        private const val HEADER = "id,accented,aspect"

        val EMPTY = VerbIds(emptyMap())

        fun read(file: File): VerbIds {
            if (!file.exists()) return EMPTY
            val rows = csvReader().readAllWithHeader(file)
            val ids = linkedMapOf<VerbKey, Int>()
            rows.forEachIndexed { index, row ->
                val line = index + 2
                val id = row.getValue("id").trim().toIntOrNull()
                val key = VerbKey(row.getValue("accented").trim(), row.getValue("aspect").trim())
                val problem = when {
                    id == null -> "id is not a number"
                    key in ids -> "duplicate key $key"
                    id in ids.values -> "duplicate id $id"
                    else -> null
                }
                if (problem != null || id == null) {
                    throw ImportException("verb-ids.csv:$line: $problem")
                }
                ids[key] = id
            }
            return VerbIds(ids)
        }
    }
}
