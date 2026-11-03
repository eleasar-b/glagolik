package io.github.eleasarb.glagolik.importer

/** U+0301 COMBINING ACUTE ACCENT, the stress mark used throughout the app. */
const val STRESS_MARK = '\u0301'

private const val SOURCE_STRESS_MARK = '\''
private const val VOWELS = "аеёиоуыэюя"
private val CYRILLIC_WORD = Regex("^[а-яё']+$")
private val VARIANT_SEPARATOR = Regex("\\s*(?:,|//)\\s*")

/** Splits a source cell into its variants (`по'днял,подня'л` → two variants). */
fun splitVariants(cell: String): List<String> = cell.trim()
    .split(VARIANT_SEPARATOR)
    .filter { it.isNotEmpty() }

/** Converts source notation (`говори'ть`) to the app notation (`говори́ть`). */
fun toStressed(word: String): String = word.replace(SOURCE_STRESS_MARK, STRESS_MARK)

/** Removes all stress marks, keeps `ё`. */
fun toBare(word: String): String = word.replace(STRESS_MARK.toString(), "")
    .replace(SOURCE_STRESS_MARK.toString(), "")

/**
 * Checks a single word in source notation. Returns a problem description or `null`.
 *
 * Rules (CLAUDE.md §5): only Cyrillic letters, exactly one stress mark directly after a
 * vowel. A missing mark is fine for monosyllables and for words with `ё`.
 */
fun checkWord(word: String): String? {
    val marks = word.count { it == SOURCE_STRESS_MARK }
    val syllables = word.count { it in VOWELS }
    val problem = when {
        !CYRILLIC_WORD.matches(word) -> "contains non-Cyrillic characters"
        marks > 1 -> "more than one stress mark"
        marks == 1 && !isMarkAfterVowel(word) -> "stress mark not after a vowel"
        marks == 0 && syllables > 1 && 'ё' !in word -> "missing stress mark"
        else -> null
    }
    return problem?.let { "'$word': $it" }
}

private fun isMarkAfterVowel(word: String): Boolean {
    val index = word.indexOf(SOURCE_STRESS_MARK)
    return index > 0 && word[index - 1] in VOWELS
}
