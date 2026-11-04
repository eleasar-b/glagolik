package io.github.eleasarb.glagolik.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `steppers change values in steps of five`() {
        var newForms: Int? = null
        var sessionSize: Int? = null
        show(
            TrainingSettings(newFormsPerDay = 10, sessionSize = 20),
            onNewFormsPerDayChange = { newForms = it },
            onSessionSizeChange = { sessionSize = it },
        )

        compose.onNodeWithContentDescription(
            label(R.string.settings_increase, R.string.settings_new_forms),
        )
            .performClick()
        compose.onNodeWithContentDescription(
            label(R.string.settings_decrease, R.string.settings_session_size),
        )
            .performClick()

        assertEquals(15, newForms)
        assertEquals(15, sessionSize)
    }

    @Test
    fun `decrease is disabled at the lower bound`() {
        show(TrainingSettings(newFormsPerDay = 0))

        compose.onNodeWithContentDescription(
            label(R.string.settings_decrease, R.string.settings_new_forms),
        )
            .assertIsNotEnabled()
    }

    @Test
    fun `strict yo switch and tense chips report changes`() {
        var strict: Boolean? = null
        var toggled: Tense? = null
        show(
            TrainingSettings(),
            onStrictYoChange = { strict = it },
            onTenseToggle = { toggled = it },
        )

        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText(context.getString(R.string.tense_past)).performClick()

        assertEquals(true, strict)
        assertEquals(Tense.PAST, toggled)
    }

    private fun label(action: Int, setting: Int): String =
        "${context.getString(action)} ${context.getString(setting)}"

    private fun show(
        settings: TrainingSettings,
        onNewFormsPerDayChange: (Int) -> Unit = {},
        onSessionSizeChange: (Int) -> Unit = {},
        onStrictYoChange: (Boolean) -> Unit = {},
        onTenseToggle: (Tense) -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                SettingsScreen(
                    settings = settings,
                    onNewFormsPerDayChange = onNewFormsPerDayChange,
                    onSessionSizeChange = onSessionSizeChange,
                    onStrictYoChange = onStrictYoChange,
                    onTenseToggle = onTenseToggle,
                    onBack = {},
                )
            }
        }
    }
}
