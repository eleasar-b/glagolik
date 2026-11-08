package io.github.eleasarb.glagolik.ui.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.ConjugatedForm
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.ui.verbs.labelRes
import io.github.eleasarb.glagolik.ui.verbs.pronounRes

private const val VARIANT_SEPARATOR = " · "

/** Same width as in the verb table, so the forms line up the same way. */
private val PRONOUN_WIDTH = 96.dp

/** All forms of [tense], with the [asked] one highlighted. */
@Composable
internal fun TenseFormsCard(
    tense: Tense,
    forms: List<ConjugatedForm>,
    asked: FormKey,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.training_tense_forms,
                    stringResource(tense.labelRes()),
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            forms.forEach { form -> TenseFormRow(form = form, isAsked = form.key == asked) }
        }
    }
}

@Composable
private fun TenseFormRow(form: ConjugatedForm, isAsked: Boolean, modifier: Modifier = Modifier) {
    val weight = if (isAsked) FontWeight.Bold else FontWeight.Normal
    val color = if (isAsked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(modifier = modifier.padding(vertical = 2.dp)) {
        Text(
            text = stringResource(form.key.pronounRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = weight,
            modifier = Modifier
                .width(PRONOUN_WIDTH)
                .alignByBaseline(),
        )
        Text(
            text = form.variants.joinToString(VARIANT_SEPARATOR),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = weight,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
    }
}
