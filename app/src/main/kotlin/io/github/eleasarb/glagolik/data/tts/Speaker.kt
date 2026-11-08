package io.github.eleasarb.glagolik.data.tts

import kotlinx.coroutines.flow.StateFlow

enum class SpeechAvailability {
    /** The system TTS engine is still starting. */
    INITIALIZING,

    /** A Russian offline voice is ready. */
    READY,

    /** No TTS engine or no Russian offline voice installed. */
    UNAVAILABLE,
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
}
