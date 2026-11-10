package io.github.eleasarb.glagolik.ui.verbs

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Aspect
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.Translation

private const val GERMAN = "de"

/** German translation on German devices, English otherwise (and as fallback). */
@Composable
@ReadOnlyComposable
internal fun Translation.localized(): String {
    val language = LocalConfiguration.current.locales[0].language
    return if (language == GERMAN && german.isNotBlank()) german else english
}

@StringRes
internal fun Aspect.labelRes(): Int = when (this) {
    Aspect.IMPERFECTIVE -> R.string.aspect_imperfective
    Aspect.PERFECTIVE -> R.string.aspect_perfective
    Aspect.BIASPECTUAL -> R.string.aspect_biaspectual
}

/** Abbreviation shown after a translation, e.g. "pf.". */
@StringRes
internal fun Aspect.shortLabelRes(): Int = when (this) {
    Aspect.IMPERFECTIVE -> R.string.aspect_short_imperfective
    Aspect.PERFECTIVE -> R.string.aspect_short_perfective
    Aspect.BIASPECTUAL -> R.string.aspect_short_biaspectual
}

@StringRes
internal fun Tense.labelRes(): Int = when (this) {
    Tense.PRESENT -> R.string.tense_present
    Tense.FUTURE -> R.string.tense_future
    Tense.PAST -> R.string.tense_past
    Tense.IMPERATIVE -> R.string.tense_imperative
}

/** Russian pronoun / gender label shown in front of a form. */
@StringRes
internal fun FormKey.pronounRes(): Int = when (this) {
    FormKey.PRESENT_SG1, FormKey.FUTURE_SG1 -> R.string.pronoun_sg1
    FormKey.PRESENT_SG2, FormKey.FUTURE_SG2, FormKey.IMPERATIVE_SG -> R.string.pronoun_sg2
    FormKey.PRESENT_SG3, FormKey.FUTURE_SG3 -> R.string.pronoun_sg3
    FormKey.PRESENT_PL1, FormKey.FUTURE_PL1 -> R.string.pronoun_pl1
    FormKey.PRESENT_PL2, FormKey.FUTURE_PL2, FormKey.IMPERATIVE_PL -> R.string.pronoun_pl2
    FormKey.PRESENT_PL3, FormKey.FUTURE_PL3 -> R.string.pronoun_pl3
    FormKey.PAST_M -> R.string.pronoun_past_m
    FormKey.PAST_F -> R.string.pronoun_past_f
    FormKey.PAST_N -> R.string.pronoun_past_n
    FormKey.PAST_PL -> R.string.pronoun_past_pl
}
