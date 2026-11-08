package io.github.eleasarb.glagolik.ui.verbs

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.MasteryCounts
import io.github.eleasarb.glagolik.domain.model.MasteryLevel
import io.github.eleasarb.glagolik.domain.model.MasteryOverview
import io.github.eleasarb.glagolik.ui.components.MASTERY_DISPLAY_ORDER
import io.github.eleasarb.glagolik.ui.components.MasteryBar
import io.github.eleasarb.glagolik.ui.components.MasteryDot
import io.github.eleasarb.glagolik.ui.components.labelRes
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

/**
 * Mastery of all trainable cards, shown above the verb list: the [ProgressCard] plus the number
 * of started verbs, the legend and what the levels mean.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasteryOverviewCard(overview: MasteryOverview, modifier: Modifier = Modifier) {
    ProgressCard(
        forms = overview.forms,
        meaning = overview.meaning,
        dueNow = overview.dueNow,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(
                R.string.mastery_verbs_started,
                overview.startedVerbs,
                overview.totalVerbs,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MASTERY_DISPLAY_ORDER.forEach { level -> LegendItem(level = level) }
        }
        Text(
            text = stringResource(R.string.mastery_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Progress" with the due cards and one bar each for the forms and the meaning ("x of y
 * learned · z %"), for all verbs or a single one. Without [meaning] (verbs without meaning
 * cards) only the forms are shown. [footer] follows below the bars.
 */
@Composable
fun ProgressCard(
    forms: MasteryCounts,
    meaning: MasteryCounts?,
    dueNow: Int,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.mastery_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.mastery_due, dueNow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            KindMastery(
                label = stringResource(R.string.mastery_forms),
                counts = forms,
                summary = R.plurals.mastery_summary,
            )
            if (meaning != null) {
                KindMastery(
                    label = stringResource(R.string.mastery_meaning),
                    counts = meaning,
                    summary = R.plurals.mastery_summary_cards,
                )
            }
            footer()
        }
    }
}

/** Label with "x of y learned · z %" above the bar of one exercise kind. */
@Composable
private fun KindMastery(
    label: String,
    counts: MasteryCounts,
    @PluralsRes summary: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(
                    R.plurals.mastery_learned,
                    counts.total,
                    counts.learned,
                    counts.total,
                    counts.learnedPercentText(),
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        MasteryBar(
            counts = counts,
            modifier = Modifier.fillMaxWidth(),
            height = 10.dp,
            summary = summary,
        )
    }
}

@Composable
private fun LegendItem(level: MasteryLevel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MasteryDot(level = level)
        Text(text = stringResource(level.labelRes()), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun MasteryOverviewCardPreview() {
    GlagolikTheme(dynamicColor = false) {
        MasteryOverviewCard(
            overview = MasteryOverview(
                forms = MasteryCounts(new = 9_412, learning = 23, young = 310, mature = 128),
                byVerb = emptyMap(),
                dueNow = 17,
                meaning = MasteryCounts(new = 1_080, learning = 12, young = 60, mature = 48),
                startedVerbs = 41,
                totalVerbs = 600,
            ),
        )
    }
}
