package io.github.eleasarb.glagolik.data.backup

import io.github.eleasarb.glagolik.data.db.CardEntity
import io.github.eleasarb.glagolik.data.db.ReviewLogEntity
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric provides the real org.json implementation of the Android framework. */
@RunWith(RobolectricTestRunner::class)
class ProgressBackupCodecTest {
    private val backup = ProgressBackup(
        exportedAt = Instant.parse("2026-01-10T07:00:00Z"),
        settings = TrainingSettings(
            newFormsPerDay = 15,
            sessionSize = 25,
            strictYo = true,
            tenses = setOf(Tense.FUTURE, Tense.PAST),
            autoPlayAudio = true,
        ),
        cards = listOf(
            CardEntity(
                verbId = 2,
                form = "FUTURE_SG1",
                state = "REVIEW",
                step = null,
                stability = 2.3065,
                difficulty = 2.118103970459016,
                dueAt = 1_767_427_260_000,
                lastReviewAt = 1_767_254_460_000,
                introducedAt = 1_767_254_400_000,
            ),
            CardEntity(
                verbId = 1,
                form = "PAST_M",
                state = "LEARNING",
                step = 0,
                stability = null,
                difficulty = null,
                dueAt = 1_767_254_400_000,
                lastReviewAt = null,
                introducedAt = 1_767_254_400_000,
            ),
        ),
        logs = listOf(
            ReviewLogEntity(0, 2, "FUTURE_SG1", "AGAIN", 1_767_254_400_000, 4_000),
            ReviewLogEntity(0, 2, "FUTURE_SG1", "GOOD", 1_767_254_460_000, null),
        ),
    )

    @Test
    fun `encode and decode round-trip all fields`() {
        assertEquals(backup, ProgressBackupCodec.decode(ProgressBackupCodec.encode(backup)))
    }

    @Test
    fun `files without the audio setting from 0_1 are still accepted`() {
        val json = JSONObject(ProgressBackupCodec.encode(backup))
        json.getJSONObject("settings").remove("autoPlayAudio")
        val old = json.toString()
        assertTrue(!old.contains("autoPlayAudio"))

        assertEquals(false, ProgressBackupCodec.decode(old).settings.autoPlayAudio)
    }

    @Test
    fun `other json is rejected`() {
        assertInvalid("""{"hello": "world"}""")
    }

    @Test
    fun `newer file version is rejected`() {
        val newer = ProgressBackupCodec.encode(backup).replace("\"version\": 1", "\"version\": 2")
        val error = assertThrows(BackupFormatException::class.java) {
            ProgressBackupCodec.decode(newer)
        }
        assertTrue(error.message.orEmpty().contains("newer"))
    }

    @Test
    fun `unknown form, state or rating is rejected`() {
        val encoded = ProgressBackupCodec.encode(backup)
        assertInvalid(encoded.replace("\"PAST_M\"", "\"PAST_X\""))
        assertInvalid(encoded.replace("\"LEARNING\"", "\"SLEEPING\""))
        assertInvalid(encoded.replace("\"AGAIN\"", "\"MAYBE\""))
    }

    @Test
    fun `missing field and broken json are rejected`() {
        assertInvalid(ProgressBackupCodec.encode(backup).replace("\"dueAt\"", "\"due\""))
        assertInvalid("{ not json")
        assertInvalid("")
    }

    private fun assertInvalid(text: String) {
        assertThrows(BackupFormatException::class.java) { ProgressBackupCodec.decode(text) }
    }
}
