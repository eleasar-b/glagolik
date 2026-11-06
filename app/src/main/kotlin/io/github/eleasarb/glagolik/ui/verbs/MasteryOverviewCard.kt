package io.github.eleasarb.glagolik.ui.verbs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/** Mastery of all trainable forms, shown above the verb list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasteryOverviewCard(overview: MasteryOverview, modifier: Modifier = Modifier) {
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
                    text = stringResource(R.string.mastery_due, overview.dueNow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            MasteryBar(
                counts = overview.total,
                modifier = Modifier.fillMaxWidth(),
                height = 10.dp,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MASTERY_DISPLAY_ORDER.forEach { level ->
                    LegendItem(level = level, count = overview.total[level])
                }
            }
            Text(
                text = stringResource(R.string.mastery_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendItem(level: MasteryLevel, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MasteryDot(level = level)
        Text(
            text = stringResource(R.string.mastery_legend, stringResource(level.labelRes()), count),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MasteryOverviewCardPreview() {
    GlagolikTheme(dynamicColor = false) {
        MasteryOverviewCard(
            overview = MasteryOverview(
                total = MasteryCounts(new = 9_412, learning = 23, young = 310, mature = 128),
                byVerb = emptyMap(),
                dueNow = 17,
            ),
        )
    }
}
