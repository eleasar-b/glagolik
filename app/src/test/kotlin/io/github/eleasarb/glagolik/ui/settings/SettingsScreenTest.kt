package io.github.eleasarb.glagolik.ui.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.data.backup.BackupSummary
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

    @Test
    fun `export and import buttons report clicks`() {
        var exported = false
        var imported = false
        show(TrainingSettings(), onExportClick = { exported = true }, onImportClick = {
            imported =
                true
        })

        compose.onNodeWithText(
            context.getString(R.string.backup_export),
        ).performScrollTo().performClick()
        compose.onNodeWithText(
            context.getString(R.string.backup_import),
        ).performScrollTo().performClick()

        assertEquals(true, exported)
        assertEquals(true, imported)
    }

    @Test
    fun `backup results are shown`() {
        show(TrainingSettings(), backupStatus = BackupStatus.Exported(BackupSummary(3, 10)))
        val exported = context.resources.getQuantityString(R.plurals.backup_exported, 3, 3)
        compose.onNodeWithText(exported).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `invalid import file is explained`() {
        show(TrainingSettings(), backupStatus = BackupStatus.InvalidFile)
        compose.onNodeWithText(context.getString(R.string.backup_invalid_file))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `buttons are disabled while working`() {
        show(TrainingSettings(), backupStatus = BackupStatus.Working)
        compose.onNodeWithText(context.getString(R.string.backup_export))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    private fun label(action: Int, setting: Int): String =
        "${context.getString(action)} ${context.getString(setting)}"

    private fun show(
        settings: TrainingSettings,
        onNewFormsPerDayChange: (Int) -> Unit = {},
        onSessionSizeChange: (Int) -> Unit = {},
        onStrictYoChange: (Boolean) -> Unit = {},
        onTenseToggle: (Tense) -> Unit = {},
        backupStatus: BackupStatus? = null,
        onExportClick: () -> Unit = {},
        onImportClick: () -> Unit = {},
    ) {
        compose.setContent {
            GlagolikTheme(dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState(settings, backupStatus),
                    onNewFormsPerDayChange = onNewFormsPerDayChange,
                    onSessionSizeChange = onSessionSizeChange,
                    onStrictYoChange = onStrictYoChange,
                    onTenseToggle = onTenseToggle,
                    onExportClick = onExportClick,
                    onImportClick = onImportClick,
                    onBack = {},
                )
            }
        }
    }
}
