package io.github.eleasarb.glagolik.domain.model

import java.util.Locale

private const val MEANING_SEPARATORS = ",;"
private const val PREVIEW_JOINER = ", "
private val WHITESPACE = Regex("\\s+")

/**
 * Splits a translation into its meanings: at commas and semicolons outside of parentheses,
 * trimmed, empty parts dropped. "scheinen (den Anschein haben, anmuten), erscheinen" gives
 * two meanings.
 */
fun meaningsOf(translation: String): List<String> {
    val meanings = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    translation.forEach { char ->
        when {
            char == '(' -> depth++
            char == ')' -> depth = (depth - 1).coerceAtLeast(0)
        }
        if (depth == 0 && char in MEANING_SEPARATORS) {
            meanings += current.toString()
            current.clear()
        } else {
            current.append(char)
        }
    }
    meanings += current.toString()
    return meanings.map { it.replace(WHITESPACE, " ").trim() }.filter { it.isNotEmpty() }
}

/**
 * The first meanings of [translation] for a multiple-choice option: three if they fit into
 * [maxLength] characters, otherwise two (or fewer if there are not more).
 */
fun previewMeanings(translation: String, maxLength: Int = PREVIEW_MAX_LENGTH): String {
    val meanings = meaningsOf(translation)
    val three = meanings.take(PREVIEW_MAX_MEANINGS).joinToString(PREVIEW_JOINER)
    return if (three.length <= maxLength) {
        three
    } else {
        meanings.take(PREVIEW_MIN_MEANINGS).joinToString(PREVIEW_JOINER)
    }
}

/**
 * Meanings of a translation, normalised for comparison (lower case, single spaces). German and
 * English are kept apart: a German word never matches an English one.
 */
data class ComparableMeanings(val german: Set<String>, val english: Set<String>) {
    /** At least one meaning in common, in German or in English. */
    fun overlaps(other: ComparableMeanings): Boolean =
        german.any { it in other.german } || english.any { it in other.english }
}

fun Translation.comparableMeanings(): ComparableMeanings = ComparableMeanings(
    german = meaningsOf(german).mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) },
    english = meaningsOf(english).mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) },
)

const val PREVIEW_MAX_LENGTH = 40
private const val PREVIEW_MAX_MEANINGS = 3
private const val PREVIEW_MIN_MEANINGS = 2
