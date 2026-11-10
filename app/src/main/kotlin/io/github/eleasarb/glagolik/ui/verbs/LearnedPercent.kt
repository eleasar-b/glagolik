package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.floor

private const val PERCENT = 100.0
private const val ONE_DECIMAL_BELOW = 10.0
private const val DECIMAL_STEPS = 10.0
private const val SMALLEST_SHOWN = 0.1

/** A percentage ready to show; [isBelowSmallest] means "less than [text]". */
internal data class LearnedPercent(val text: String, val isBelowSmallest: Boolean = false)

/** Share of learned cards in the device language, see [sharePercent]. */
@Composable
internal fun MasteryCounts.learnedPercentText(): String =
    shareText(if (total > 0) learned.toDouble() / total else 0.0)

/** [share] (0 to 1) as a percentage in the device language, see [sharePercent]. */
@Composable
internal fun shareText(share: Double): String {
    val percent = sharePercent(share, LocalConfiguration.current.locales[0])
    return if (percent.isBelowSmallest) {
        stringResource(R.string.mastery_percent_below, percent.text)
    } else {
        percent.text
    }
}

/**
 * [share] (0 to 1) as a percentage in [locale] ("0,4 %", "0.4%"). One decimal below 10 %,
 * whole numbers above. Rounded down, so 100 % means everything; anything above 0 is never
 * shown as 0 but as "less than 0.1 %".
 */
internal fun sharePercent(share: Double, locale: Locale): LearnedPercent {
    val percent = share * PERCENT
    val decimals = if (percent < ONE_DECIMAL_BELOW) 1 else 0
    val format = NumberFormat.getPercentInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
    }
    if (share > 0 && percent < SMALLEST_SHOWN) {
        return LearnedPercent(format.format(SMALLEST_SHOWN / PERCENT), isBelowSmallest = true)
    }
    val step = if (decimals == 1) DECIMAL_STEPS else 1.0
    return LearnedPercent(format.format(floor(percent * step + ROUNDING_SLACK) / step / PERCENT))
}

/** [learned] of [total] as a percentage, see [sharePercent]. */
internal fun learnedPercent(learned: Int, total: Int, locale: Locale): LearnedPercent =
    sharePercent(if (total > 0) learned.toDouble() / total else 0.0, locale)

/** Keeps 0.3 * 100 * 10 = 29.999… from being rounded down to 2.9. */
private const val ROUNDING_SLACK = 1e-9
