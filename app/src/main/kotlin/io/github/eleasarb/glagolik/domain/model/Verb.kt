package io.github.eleasarb.glagolik.domain.model

enum class Aspect {
    IMPERFECTIVE,
    PERFECTIVE,
    BIASPECTUAL,
}

enum class Tense {
    PRESENT,
    FUTURE,
    PAST,
    IMPERATIVE,
}

/** Grammatical slot of a form. Names match the `form` column of verbs.db. */
enum class FormKey(val tense: Tense) {
    PRESENT_SG1(Tense.PRESENT),
    PRESENT_SG2(Tense.PRESENT),
    PRESENT_SG3(Tense.PRESENT),
    PRESENT_PL1(Tense.PRESENT),
    PRESENT_PL2(Tense.PRESENT),
    PRESENT_PL3(Tense.PRESENT),
    FUTURE_SG1(Tense.FUTURE),
    FUTURE_SG2(Tense.FUTURE),
    FUTURE_SG3(Tense.FUTURE),
    FUTURE_PL1(Tense.FUTURE),
    FUTURE_PL2(Tense.FUTURE),
    FUTURE_PL3(Tense.FUTURE),
    PAST_M(Tense.PAST),
    PAST_F(Tense.PAST),
    PAST_N(Tense.PAST),
    PAST_PL(Tense.PAST),
    IMPERATIVE_SG(Tense.IMPERATIVE),
    IMPERATIVE_PL(Tense.IMPERATIVE),
}

data class Translation(val german: String, val english: String)

/** Entry of the verb list. */
data class VerbSummary(
    val id: Int,
    val infinitive: String,
    val aspect: Aspect,
    val translation: Translation,
)

/** One form slot with all its variants (e.g. по́днял / подня́л). */
data class ConjugatedForm(val key: FormKey, val variants: List<String>, val isTrainable: Boolean)

data class VerbDetail(
    val summary: VerbSummary,
    val forms: List<ConjugatedForm>,
    val partners: List<VerbSummary>,
    /** One of the trained verbs; aspect partners outside of them are reference verbs. */
    val isCore: Boolean = true,
) {
    fun formsOf(tense: Tense): List<ConjugatedForm> = forms.filter { it.key.tense == tense }
}
