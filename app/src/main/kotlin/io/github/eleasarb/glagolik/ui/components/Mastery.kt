package io.github.eleasarb.glagolik.ui.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.luminance
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

/** Light and dark step of one level. */
private class LevelColor(val light: Color, val dark: Color)

/**
 * Fixed colours, independent of dynamic colour: with a wallpaper-based scheme the theme roles
 * can end up so close that the levels are hard to tell apart. Checked for colour vision
 * deficiencies (adjacent pairs), on the light and on the dark surface.
 */
private val NEW_COLOR = LevelColor(light = Color(0xFFCFCEC9), dark = Color(0xFF4A4A46))
private val LEARNING_COLOR = LevelColor(light = Color(0xFFEB6834), dark = Color(0xFFD95926))
private val YOUNG_COLOR = LevelColor(light = Color(0xFF1BAF7A), dark = Color(0xFF199E70))
private val MATURE_COLOR = LevelColor(light = Color(0xFF2A78D6), dark = Color(0xFF3987E5))

private const val DARK_SURFACE_LUMINANCE = 0.5f
private val SEGMENT_GAP = 2.dp

/** Levels from strongest to weakest, the order of the bar and the legend. */
val MASTERY_DISPLAY_ORDER: List<MasteryLevel> = MasteryLevel.entries.reversed()

@StringRes
fun MasteryLevel.labelRes(): Int = when (this) {
    MasteryLevel.NEW -> R.string.mastery_new
    MasteryLevel.LEARNING -> R.string.mastery_learning
    MasteryLevel.YOUNG -> R.string.mastery_young
    MasteryLevel.MATURE -> R.string.mastery_mature
}

/** Fixed colour of the level; the dark step is used when the theme surface is dark. */
@Composable
@ReadOnlyComposable
fun MasteryLevel.color(): Color {
    val color = when (this) {
        MasteryLevel.NEW -> NEW_COLOR
        MasteryLevel.LEARNING -> LEARNING_COLOR
        MasteryLevel.YOUNG -> YOUNG_COLOR
        MasteryLevel.MATURE -> MATURE_COLOR
    }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    return if (isDark) color.dark else color.light
}

/**
 * Stacked bar of [counts], strongest level first, with a small gap between the levels.
 * Announced with [summary], "x of y forms mature" by default.
 */
@Composable
fun MasteryBar(
    counts: MasteryCounts,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    @PluralsRes summary: Int = R.plurals.mastery_summary,
) {
    val description = pluralStringResource(
        summary,
        counts.total,
        counts.mature,
        counts.total,
    )
    Row(
        modifier = modifier
            .height(height)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
    ) {
        MASTERY_DISPLAY_ORDER.forEach { level ->
            val count = counts[level]
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(height / 2))
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
