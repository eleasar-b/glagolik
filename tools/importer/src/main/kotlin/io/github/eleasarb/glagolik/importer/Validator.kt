package io.github.eleasarb.glagolik.importer

/**
 * Validates one source row (CLAUDE.md §5). Returns all problems, empty if the row is fine.
 *
 * - aspect is one of imperfective / perfective / both
 * - infinitive and every form variant pass [checkWord]
 * - every form is present unless listed as ALLOW_MISSING in the overrides
 *
 * Present vs. simple future needs no extra check: the source has a single `presfut`
 * column whose meaning follows from the aspect (see [buildForms]).
 */
fun validate(verb: SourceVerb, allowedMissing: Set<SourceField>): List<String> {
    val problems = mutableListOf<String>()
    if (Aspect.fromSource(verb.aspect) == null) {
        problems += "unknown aspect '${verb.aspect}'"
    }
    problems += checkCell("infinitive", verb.accented)
    SourceField.entries.forEach { field ->
        val cell = verb.fields.getValue(field)
        if (cell.isEmpty()) {
            if (field !in allowedMissing) problems += "${field.column}: missing"
        } else {
            problems += checkCell(field.column, cell)
        }
    }
    return problems
}

private fun checkCell(name: String, cell: String): List<String> {
    val variants = splitVariants(cell)
    if (variants.isEmpty()) return listOf("$name: missing")
    return variants.mapNotNull { word -> checkWord(word)?.let { "$name: $it" } }
}
