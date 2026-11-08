package io.github.eleasarb.glagolik.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val RUSSIAN = "ru"
private const val RUSSIA = "RU"
private val RUSSIAN_RUSSIA: Locale = Locale.forLanguageTag("ru-RU")
private const val UTTERANCE_ID = "glagolik"

/** [Speaker] backed by [TextToSpeech]. Create one per screen and close it when done. */
class AndroidSpeaker(context: Context) : Speaker {
    private val _availability = MutableStateFlow(SpeechAvailability.INITIALIZING)
    override val availability: StateFlow<SpeechAvailability> = _availability.asStateFlow()

    private var engine: TextToSpeech? = null

    /** Set if the engine reports its status before the constructor has returned. */
    private var earlyStatus: Int? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status -> onInit(status) }
        earlyStatus?.let { onInit(it) }
    }

    private fun onInit(status: Int) {
        val tts = engine
        if (tts == null) {
            earlyStatus = status
            return
        }
        if (_availability.value != SpeechAvailability.INITIALIZING) return
        val ready = status == TextToSpeech.SUCCESS && useRussianVoice(tts)
        _availability.value =
            if (ready) SpeechAvailability.READY else SpeechAvailability.UNAVAILABLE
    }

    private fun useRussianVoice(tts: TextToSpeech): Boolean {
        val voices = tts.voices.orEmpty()
        return if (voices.isEmpty()) {
            // Engines without the voice API: fall back to the language.
            tts.setLanguage(RUSSIAN_RUSSIA) >= TextToSpeech.LANG_AVAILABLE
        } else {
            val voice = selectRussianVoice(voices)
            voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS
        }
    }

    override fun speak(text: String) {
        if (_availability.value != SpeechAvailability.READY) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        _availability.value = SpeechAvailability.UNAVAILABLE
    }
}

/**
 * Best installed Russian voice that works offline, or `null`. Prefers ru-RU over other
 * regions, then higher quality, then lower latency.
 */
internal fun selectRussianVoice(voices: Collection<Voice>): Voice? = voices
    .filter { it.locale.language == RUSSIAN }
    .filterNot { it.isNetworkConnectionRequired }
    .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
    .maxWithOrNull(
        compareBy<Voice> { it.locale.country == RUSSIA }
            .thenBy { it.quality }
            .thenByDescending { it.latency },
    )
