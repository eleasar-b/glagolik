package io.github.eleasarb.glagolik.domain.model

/** User preferences for training sessions (CLAUDE.md §6). */
data class TrainingSettings(
    val newFormsPerDay: Int = DEFAULT_NEW_FORMS_PER_DAY,
    val sessionSize: Int = DEFAULT_SESSION_SIZE,
    /** When true, е typed for ё counts as wrong instead of correct with a hint. */
    val strictYo: Boolean = false,
    /** Tenses that are quizzed; never empty. */
    val tenses: Set<Tense> = Tense.entries.toSet(),
) {
    companion object {
        const val DEFAULT_NEW_FORMS_PER_DAY = 10
        const val DEFAULT_SESSION_SIZE = 20
        val NEW_FORMS_RANGE = 0..100
        val SESSION_SIZE_RANGE = 5..100
    }
}
