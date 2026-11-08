package io.github.eleasarb.glagolik.data.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceSelectionTest {
    private fun voice(
        name: String,
        tag: String,
        quality: Int = Voice.QUALITY_NORMAL,
        latency: Int = Voice.LATENCY_NORMAL,
        network: Boolean = false,
        features: Set<String> = emptySet(),
    ) = Voice(name, Locale.forLanguageTag(tag), quality, latency, network, features)

    @Test
    fun `network voices and voices that are not installed are never chosen`() {
        val voices = listOf(
            voice("net", "ru-RU", quality = Voice.QUALITY_VERY_HIGH, network = true),
            voice(
                "missing",
                "ru-RU",
                quality = Voice.QUALITY_VERY_HIGH,
                features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
            ),
            voice("local", "ru-RU"),
        )

        assertEquals("local", selectRussianVoice(voices)?.name)
    }

    @Test
    fun `ru-RU is preferred, then quality, then latency`() {
        val voices = listOf(
            voice("ua", "ru-UA", quality = Voice.QUALITY_VERY_HIGH),
            voice("slow", "ru-RU", quality = Voice.QUALITY_HIGH, latency = Voice.LATENCY_HIGH),
            voice("fast", "ru-RU", quality = Voice.QUALITY_HIGH, latency = Voice.LATENCY_LOW),
            voice("low", "ru-RU", quality = Voice.QUALITY_LOW, latency = Voice.LATENCY_VERY_LOW),
        )

        assertEquals("fast", selectRussianVoice(voices)?.name)
    }

    @Test
    fun `no russian voice gives null`() {
        assertEquals(null, selectRussianVoice(listOf(voice("de", "de-CH"), voice("en", "en-US"))))
    }
}
