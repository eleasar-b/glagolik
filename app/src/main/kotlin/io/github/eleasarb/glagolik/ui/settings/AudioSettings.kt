package io.github.eleasarb.glagolik.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.eleasarb.glagolik.R
import io.github.eleasarb.glagolik.data.tts.SpeechAvailability

/** Not in the public SDK constants, but supported by AOSP and GrapheneOS Settings. */
private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

/** Auto play, whether a Russian voice was found, and how to get one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AudioSettingsSection(
    autoPlay: Boolean,
    speech: SpeechAvailability,
    onAutoPlayChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onInstallVoice: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchSetting(
            label = stringResource(R.string.settings_auto_play),
            summary = stringResource(R.string.settings_auto_play_summary),
            checked = autoPlay,
            onCheckedChange = onAutoPlayChange,
        )
        val status = when (speech) {
            SpeechAvailability.INITIALIZING -> R.string.settings_speech_checking
            SpeechAvailability.READY -> R.string.settings_speech_ready
            SpeechAvailability.NO_ENGINE -> R.string.settings_speech_no_engine
            SpeechAvailability.VOICE_NOT_INSTALLED -> R.string.settings_speech_not_installed
            SpeechAvailability.NO_RUSSIAN_VOICE -> R.string.settings_speech_no_russian
        }
        val isProblem =
            speech != SpeechAvailability.READY && speech != SpeechAvailability.INITIALIZING
        Text(
            text = stringResource(status),
            style = MaterialTheme.typography.bodySmall,
            color = if (isProblem) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (speech == SpeechAvailability.READY) {
                OutlinedButton(onClick = onTest) {
                    Text(stringResource(R.string.settings_speech_test))
                }
            }
            if (speech == SpeechAvailability.VOICE_NOT_INSTALLED) {
                OutlinedButton(onClick = onInstallVoice) {
                    Text(stringResource(R.string.settings_speech_install))
                }
            }
            OutlinedButton(onClick = onOpenSystemSettings) {
                Text(stringResource(R.string.settings_speech_open))
            }
        }
    }
}

/** Opens the engine's voice download screen, or the text-to-speech settings. */
internal fun Context.openVoiceInstall() {
    try {
        startActivity(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        openSpeechSettings()
    }
}

/** Opens the system text-to-speech settings, or the main settings if they are missing. */
internal fun Context.openSpeechSettings() {
    try {
        startActivity(Intent(TTS_SETTINGS_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
