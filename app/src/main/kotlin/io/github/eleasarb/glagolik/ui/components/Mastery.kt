package io.github.eleasarb.glagolik.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

private const val YOUNG_ALPHA = 0.45f

/** Levels from strongest to weakest, the order of the bar and the legend. */
val MASTERY_DISPLAY_ORDER: List<MasteryLevel> = MasteryLevel.entries.reversed()

@StringRes
fun MasteryLevel.labelRes(): Int = when (this) {
    MasteryLevel.NEW -> R.string.mastery_new
    MasteryLevel.LEARNING -> R.string.mastery_learning
    MasteryLevel.YOUNG -> R.string.mastery_young
    MasteryLevel.MATURE -> R.string.mastery_mature
}

/** Theme roles, so the colours follow dynamic colour and dark mode. */
@Composable
@ReadOnlyComposable
fun MasteryLevel.color(): Color = when (this) {
    MasteryLevel.NEW -> MaterialTheme.colorScheme.outlineVariant
    MasteryLevel.LEARNING -> MaterialTheme.colorScheme.tertiary
    MasteryLevel.YOUNG -> MaterialTheme.colorScheme.primary.copy(alpha = YOUNG_ALPHA)
    MasteryLevel.MATURE -> MaterialTheme.colorScheme.primary
}

/** Stacked bar of [counts], strongest level first. Announced as "x of y forms mature". */
@Composable
fun MasteryBar(counts: MasteryCounts, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val description = pluralStringResource(
        R.plurals.mastery_summary,
        counts.total,
        counts.mature,
        counts.total,
    )
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .semantics { contentDescription = description },
    ) {
        MASTERY_DISPLAY_ORDER.forEach { level ->
            val count = counts[level]
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(level.color()),
                )
            }
        }
    }
}

/** Coloured dot for one level. Has no semantics; the caller describes it. */
@Composable
fun MasteryDot(level: MasteryLevel, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(level.color()),
    )
}

@Preview(showBackground = true)
@Composable
private fun MasteryBarPreview() {
    GlagolikTheme(dynamicColor = false) {
        MasteryBar(counts = MasteryCounts(new = 5, learning = 1, young = 3, mature = 8))
    }
}
