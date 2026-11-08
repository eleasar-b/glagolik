package io.github.eleasarb.glagolik.data.tts

import kotlinx.coroutines.flow.StateFlow

enum class SpeechAvailability {
    /** The system TTS engine is still starting. */
    INITIALIZING,

    /** A Russian offline voice is ready. */
    READY,

    /** No TTS engine installed (or it failed to start). */
    NO_ENGINE,

    /** The engine knows Russian, but the voice is only online or not downloaded yet. */
    VOICE_NOT_INSTALLED,

    /** The engine has no Russian voice at all. */
    NO_RUSSIAN_VOICE,
}

/**
 * Russian pronunciation via the system text-to-speech engine (CLAUDE.md §6 Audio).
 * Only offline voices are used. Closing releases the engine.
 */
interface Speaker : AutoCloseable {
    val availability: StateFlow<SpeechAvailability>

    /**
     * Speaks [text] as written, including stress marks (U+0301), and interrupts anything
     * still playing. Does nothing unless [availability] is [SpeechAvailability.READY].
     */
    fun speak(text: String)

    /**
     * Checks the engine again if no voice was ready, e.g. after the learner installed one
     * in the system settings.
     */
    fun refresh()
}
