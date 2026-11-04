package io.github.eleasarb.glagolik.domain.checking

import java.text.Normalizer
import java.util.Locale

/** U+0301 COMBINING ACUTE ACCENT, the stress mark used in the data. */
private const val COMBINING_ACUTE = '\u0301'

/** U+0300 COMBINING GRAVE ACCENT, sometimes typed as stress mark. */
private const val COMBINING_GRAVE = '\u0300'

/** Apostrophe, the stress notation of the source data; learners may type it too. */
private const val APOSTROPHE = '\''

private val WHITESPACE = Regex("\\s+")
private val LATIN = Regex("[A-Za-z]")

sealed interface AnswerResult {
    /** Correct answer. [matched] is the expected form (with stress) the input matched. */
    data class Correct(val matched: String, val yoTypedAsYe: Boolean = false) : AnswerResult

    /**
     * Wrong answer. [yoTypedAsYe] is set when the input only differs by е instead of ё and
     * strict ё checking is on, so the UI can explain why.
     */
    data class Incorrect(val expected: List<String>, val yoTypedAsYe: Boolean = false) :
        AnswerResult

    /** Input contains Latin letters (e.g. a Latin "a" for Cyrillic "а"). Not graded. */
    data class LatinLetters(val letters: List<Char>) : AnswerResult

    /** Nothing entered. Not graded. */
    data object Empty : AnswerResult
}

/**
 * Compares typed input with the expected variants of a form (CLAUDE.md §6).
 *
 * - Unicode NFC, case-insensitive, surrounding and repeated whitespace ignored
 * - stress marks are ignored on both sides (U+0301, U+0300, apostrophe)
 * - е typed for ё counts as correct with a hint, or as wrong if [strictYo] is set
 * - Latin letters are reported instead of being graded as wrong
 */
class AnswerChecker(private val strictYo: Boolean = false) {
    fun check(input: String, expected: List<String>): AnswerResult {
        require(expected.isNotEmpty()) { "expected must not be empty" }
        val typed = normalize(input)
        val latin = LATIN.findAll(typed).map { it.value.single() }.distinct().toList()
        return when {
            typed.isEmpty() -> AnswerResult.Empty
            latin.isNotEmpty() -> AnswerResult.LatinLetters(latin)
            else -> grade(typed, expected)
        }
    }

    private fun grade(typed: String, expected: List<String>): AnswerResult {
        expected.firstOrNull { normalize(it) == typed }?.let { return AnswerResult.Correct(it) }

        val yoMatch = expected.firstOrNull { variant ->
            val normalized = normalize(variant)
            'ё' in normalized && 'ё' !in typed && withoutYo(normalized) == withoutYo(typed)
        }
        return when {
            yoMatch == null -> AnswerResult.Incorrect(expected)
            strictYo -> AnswerResult.Incorrect(expected, yoTypedAsYe = true)
            else -> AnswerResult.Correct(yoMatch, yoTypedAsYe = true)
        }
    }

    private fun withoutYo(text: String): String = text.replace('ё', 'е')

    companion object {
        /** Lower case, NFC, no stress marks, single spaces; keeps ё and й intact. */
        fun normalize(text: String): String {
            val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
                .filterNot { it == COMBINING_ACUTE || it == COMBINING_GRAVE || it == APOSTROPHE }
            return Normalizer.normalize(decomposed, Normalizer.Form.NFC)
                .lowercase(Locale.ROOT)
                .trim()
                .replace(WHITESPACE, " ")
        }
    }
}
