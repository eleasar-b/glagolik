package io.github.eleasarb.glagolik.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import java.util.MissingResourceException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val RUSSIAN = "ru"
private const val RUSSIAN_ISO3 = "rus"
private const val RUSSIA = "RU"
private const val RUSSIA_ISO3 = "RUS"
private val RUSSIAN_RUSSIA: Locale = Locale.forLanguageTag("ru-RU")
private const val UTTERANCE_ID = "glagolik"

/** [Speaker] backed by [TextToSpeech]. Create one per screen and close it when done. */
class AndroidSpeaker(context: Context) : Speaker {
    private val appContext = context.applicationContext
    private val _availability = MutableStateFlow(SpeechAvailability.INITIALIZING)
    override val availability: StateFlow<SpeechAvailability> = _availability.asStateFlow()

    private var engine: TextToSpeech? = null

    /** Set if the engine reports its status before the constructor has returned. */
    private var earlyStatus: Int? = null

    init {
        start()
    }

    private fun start() {
        _availability.value = SpeechAvailability.INITIALIZING
        earlyStatus = null
        engine = TextToSpeech(appContext) { status -> onInit(status) }
        earlyStatus?.let { onInit(it) }
    }

    private fun onInit(status: Int) {
        val tts = engine
        if (tts == null) {
            earlyStatus = status
            return
        }
        if (_availability.value != SpeechAvailability.INITIALIZING) return
        _availability.value = if (status == TextToSpeech.SUCCESS) {
            useRussianVoice(tts)
        } else {
            SpeechAvailability.NO_ENGINE
        }
    }

    private fun useRussianVoice(tts: TextToSpeech): SpeechAvailability {
        val voices = tts.voices.orEmpty()
        val voice = selectRussianVoice(voices)
        return when {
            voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS -> {
                SpeechAvailability.READY
            }

            // Russian voices exist, but only online or not downloaded: do not use them.
            voices.any { it.locale.isRussian() } -> SpeechAvailability.VOICE_NOT_INSTALLED

            // Engines that list no Russian voice (or none at all): try the language.
            else -> when (tts.setLanguage(RUSSIAN_RUSSIA)) {
                TextToSpeech.LANG_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
                -> SpeechAvailability.READY

                TextToSpeech.LANG_MISSING_DATA -> SpeechAvailability.VOICE_NOT_INSTALLED

                else -> SpeechAvailability.NO_RUSSIAN_VOICE
            }
        }
    }

    override fun speak(text: String) {
        if (_availability.value != SpeechAvailability.READY) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun refresh() {
        val current = _availability.value
        if (current == SpeechAvailability.INITIALIZING || current == SpeechAvailability.READY) {
            return
        }
        release()
        start()
    }

    override fun close() {
        release()
        _availability.value = SpeechAvailability.NO_ENGINE
    }

    private fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

/**
 * Best installed Russian voice that works offline, or `null`. Prefers ru-RU over other
 * regions, then higher quality, then lower latency.
 */
internal fun selectRussianVoice(voices: Collection<Voice>): Voice? = voices
    .filter { it.locale.isRussian() }
    .filterNot { it.isNetworkConnectionRequired }
    .filterNot { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in it.features.orEmpty() }
    .maxWithOrNull(
        compareBy<Voice> { it.locale.country == RUSSIA || it.locale.country == RUSSIA_ISO3 }
            .thenBy { it.quality }
            .thenByDescending { it.latency },
    )

/** Engines report either ISO 639-1 ("ru") or ISO 639-2 ("rus") language codes. */
private fun Locale.isRussian(): Boolean {
    val iso3 = try {
        isO3Language
    } catch (_: MissingResourceException) {
        ""
    }
    return language == RUSSIAN || language == RUSSIAN_ISO3 || iso3 == RUSSIAN_ISO3
}
