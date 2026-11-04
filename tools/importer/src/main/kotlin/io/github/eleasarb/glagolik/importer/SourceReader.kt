package io.github.eleasarb.glagolik.importer

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

private const val NO_PARTNER = "-"

/** Reads `data/raw/verbs.csv`. The row number (1-based) is the frequency rank. */
fun readSource(file: File): List<SourceVerb> {
    val reader = csvReader {
        delimiter = '\t'
        // The file uses no quoting, but `"` appears inside translations.
        quoteChar = '\u0000'
        escapeChar = '\u0000'
    }
    return reader.readAllWithHeader(file).mapIndexed { index, row ->
        SourceVerb(
            rank = index + 1,
            bare = row.getValue("bare").trim(),
            accented = row.getValue("accented").trim(),
            translationEn = row.getValue("translations_en").trim(),
            translationDe = row.getValue("translations_de").trim(),
            aspect = row.getValue("aspect").trim(),
            partners = parsePartners(row.getValue("partner")),
            fields = SourceField.entries.associateWith { row.getValue(it.column).trim() },
        )
    }
}

/** Partners are separated by `;` (sometimes `,`) and occasionally carry stress marks. */
private fun parsePartners(cell: String): List<String> = cell.split(';', ',')
    .map { toBare(it.trim()) }
    .filter { it.isNotEmpty() && it != NO_PARTNER }
