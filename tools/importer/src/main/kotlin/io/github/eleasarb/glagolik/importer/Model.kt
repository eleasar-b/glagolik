package io.github.eleasarb.glagolik.importer

enum class Aspect {
    IMPERFECTIVE,
    PERFECTIVE,
    BIASPECTUAL,
    ;

    companion object {
        fun fromSource(value: String): Aspect? = when (value) {
            "imperfective" -> IMPERFECTIVE
            "perfective" -> PERFECTIVE
            "both" -> BIASPECTUAL
            else -> null
        }
    }
}

/** Columns of `data/raw/verbs.csv` that hold verb forms. */
enum class SourceField(val column: String) {
    IMPERATIVE_SG("imperative_sg"),
    IMPERATIVE_PL("imperative_pl"),
    PAST_M("past_m"),
    PAST_F("past_f"),
    PAST_N("past_n"),
    PAST_PL("past_pl"),
    PRESFUT_SG1("presfut_sg1"),
    PRESFUT_SG2("presfut_sg2"),
    PRESFUT_SG3("presfut_sg3"),
    PRESFUT_PL1("presfut_pl1"),
    PRESFUT_PL2("presfut_pl2"),
    PRESFUT_PL3("presfut_pl3"),
    ;

    companion object {
        fun fromColumn(column: String): SourceField? = entries.firstOrNull { it.column == column }
    }
}

/** Grammatical form as stored in the database. */
enum class FormKey {
    PRESENT_SG1,
    PRESENT_SG2,
    PRESENT_SG3,
    PRESENT_PL1,
    PRESENT_PL2,
    PRESENT_PL3,
    FUTURE_SG1,
    FUTURE_SG2,
    FUTURE_SG3,
    FUTURE_PL1,
    FUTURE_PL2,
    FUTURE_PL3,
    PAST_M,
    PAST_F,
    PAST_N,
    PAST_PL,
    IMPERATIVE_SG,
    IMPERATIVE_PL,
}

/** One row of the source file, untouched except for the applied overrides. */
data class SourceVerb(
    val rank: Int,
    val bare: String,
    val accented: String,
    val translationEn: String,
    val translationDe: String,
    val aspect: String,
    val partners: List<String>,
    val fields: Map<SourceField, String>,
) {
    val key: VerbKey get() = VerbKey(accented, aspect)
}

/** Identifies a source row in overrides: accented infinitive (source notation) and aspect. */
data class VerbKey(val accented: String, val aspect: String) {
    override fun toString(): String = "$accented ($aspect)"
}

data class VerbForm(val form: FormKey, val variant: Int, val value: String)

data class Verb(
    val id: Int,
    val infinitive: String,
    val aspect: Aspect,
    val isCore: Boolean,
    val translationEn: String,
    val translationDe: String,
    val forms: List<VerbForm>,
)
