package io.github.eleasarb.glagolik.importer

private val PERSONS = listOf(
    SourceField.PRESFUT_SG1,
    SourceField.PRESFUT_SG2,
    SourceField.PRESFUT_SG3,
    SourceField.PRESFUT_PL1,
    SourceField.PRESFUT_PL2,
    SourceField.PRESFUT_PL3,
)
private val PRESENT = listOf(
    FormKey.PRESENT_SG1,
    FormKey.PRESENT_SG2,
    FormKey.PRESENT_SG3,
    FormKey.PRESENT_PL1,
    FormKey.PRESENT_PL2,
    FormKey.PRESENT_PL3,
)
private val FUTURE = listOf(
    FormKey.FUTURE_SG1,
    FormKey.FUTURE_SG2,
    FormKey.FUTURE_SG3,
    FormKey.FUTURE_PL1,
    FormKey.FUTURE_PL2,
    FormKey.FUTURE_PL3,
)

/**
 * Auxiliary for the compound future of imperfective verbs (буду + infinitive).
 * This is the only rule-based generation allowed by CLAUDE.md §2.
 */
private val FUTURE_AUXILIARY = listOf("бу'ду", "бу'дешь", "бу'дет", "бу'дем", "бу'дете", "бу'дут")

/** быть forms its future from the auxiliary alone (буду, not *буду быть). */
private const val BYT = "быть"

private val DIRECT = mapOf(
    SourceField.PAST_M to FormKey.PAST_M,
    SourceField.PAST_F to FormKey.PAST_F,
    SourceField.PAST_N to FormKey.PAST_N,
    SourceField.PAST_PL to FormKey.PAST_PL,
    SourceField.IMPERATIVE_SG to FormKey.IMPERATIVE_SG,
    SourceField.IMPERATIVE_PL to FormKey.IMPERATIVE_PL,
)

/**
 * Maps a validated source row to database forms (values in app notation, with U+0301).
 *
 * - imperfective: `presfut` → present, future = auxiliary + infinitive
 * - perfective: `presfut` → simple future, no present
 * - biaspectual: `presfut` → present and simple future
 *
 * A future form is only generated for persons that have a present form, so impersonal
 * verbs (хочется → будет хотеться) stay consistent.
 *
 * Forms built from a field in [notTrainable] are marked as not trainable. The generated
 * compound future does not come from a source cell and is always trainable.
 */
fun buildForms(
    verb: SourceVerb,
    aspect: Aspect,
    notTrainable: Set<SourceField> = emptySet(),
): List<VerbForm> {
    val forms = mutableListOf<VerbForm>()
    fun fromCell(key: FormKey, field: SourceField) {
        forms += variantsOf(key, verb.fields.getValue(field), field !in notTrainable)
    }
    DIRECT.forEach { (field, key) -> fromCell(key, field) }
    PERSONS.forEachIndexed { person, field ->
        when (aspect) {
            Aspect.IMPERFECTIVE -> {
                fromCell(PRESENT[person], field)
                if (verb.fields.getValue(field).isNotEmpty()) forms += compoundFuture(verb, person)
            }

            Aspect.PERFECTIVE -> fromCell(FUTURE[person], field)

            Aspect.BIASPECTUAL -> {
                fromCell(PRESENT[person], field)
                fromCell(FUTURE[person], field)
            }
        }
    }
    return forms.sortedWith(compareBy({ it.form.ordinal }, { it.variant }))
}

private fun variantsOf(key: FormKey, cell: String, isTrainable: Boolean): List<VerbForm> =
    splitVariants(cell).mapIndexed { index, word ->
        VerbForm(key, index, toStressed(word), isTrainable)
    }

private fun compoundFuture(verb: SourceVerb, person: Int): VerbForm {
    val auxiliary = FUTURE_AUXILIARY[person]
    val value = if (verb.bare == BYT) {
        auxiliary
    } else {
        "$auxiliary ${splitVariants(verb.accented).first()}"
    }
    return VerbForm(FUTURE[person], 0, toStressed(value))
}
