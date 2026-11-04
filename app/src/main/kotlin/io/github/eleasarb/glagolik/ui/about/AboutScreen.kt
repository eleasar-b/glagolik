package io.github.eleasarb.glagolik.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.BuildConfig
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

private const val OPENRUSSIAN_URL = "https://en.openrussian.org"
private const val CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
private const val SOURCE_URL = "https://github.com/eleasar-b/glagolik"

/** App info and data attribution (required by CC BY-SA, see ATTRIBUTION.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.titleMedium,
            )
            AboutSection(
                title = stringResource(R.string.about_data_title),
                text = stringResource(R.string.about_data_text),
                links = listOf(OPENRUSSIAN_URL, CC_BY_SA_URL),
            )
            AboutSection(
                title = stringResource(R.string.about_code_title),
                text = stringResource(R.string.about_code_text),
                links = listOf(SOURCE_URL),
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    text: String,
    links: List<String>,
    modifier: Modifier = Modifier,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
        links.forEach { url ->
            Text(
                text = buildAnnotatedString {
                    withLink(LinkAnnotation.Url(url, linkStyles)) { append(url) }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    GlagolikTheme(dynamicColor = false) {
        AboutScreen(onBack = {})
    }
}
