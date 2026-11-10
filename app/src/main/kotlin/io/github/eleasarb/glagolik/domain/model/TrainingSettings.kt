package io.github.eleasarb.glagolik.domain.model

/** User preferences for training sessions (CLAUDE.md §6). */
data class TrainingSettings(
    /** One daily allowance for new cards of all exercises (forms and meaning). */
    val newCardsPerDay: Int = DEFAULT_NEW_CARDS_PER_DAY,
    val sessionSize: Int = DEFAULT_SESSION_SIZE,
    /** When true, е typed for ё counts as wrong instead of correct with a hint. */
    val strictYo: Boolean = false,
    /** Tenses that are quizzed; never empty. */
    val tenses: Set<Tense> = Tense.entries.toSet(),
    /** Speak the correct form automatically once an answer is checked. */
    val autoPlayAudio: Boolean = false,
    /** Meaning cards (RU→DE and DE→RU) are part of the sessions. */
    val practiceMeaning: Boolean = true,
) {
    /**
     * Whether sessions ask cards of [exercise]: forms of the selected tenses, meaning only with
     * meaning practice on. "Due now" counts with the same rule.
     */
    fun trains(exercise: Exercise): Boolean = when (exercise) {
        is Exercise.Form -> exercise.form.tense in tenses
        Exercise.MeaningRuDe, Exercise.MeaningDeRu -> practiceMeaning
    }

    companion object {
        const val DEFAULT_NEW_CARDS_PER_DAY = 10
        const val DEFAULT_SESSION_SIZE = 20
        val NEW_CARDS_RANGE = 0..100
        val SESSION_SIZE_RANGE = 5..100
    }
}
