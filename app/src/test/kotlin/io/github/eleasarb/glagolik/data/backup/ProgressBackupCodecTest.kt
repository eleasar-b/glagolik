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
            newCardsPerDay = 15,
            sessionSize = 25,
            strictYo = true,
            tenses = setOf(Tense.FUTURE, Tense.PAST),
            autoPlayAudio = true,
            practiceMeaning = false,
        ),
        cards = listOf(
            CardEntity(
                verbId = 2,
                item = "FUTURE_SG1",
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
                item = "PAST_M",
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
            ReviewLogEntity(0, 2, "MEANING_RU_DE", "GOOD", 1_767_254_470_000, 2_500),
            ReviewLogEntity(0, 2, "MEANING_DE_RU", "AGAIN", 1_767_254_480_000, 9_000),
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
    fun `version 2 names the exercise in item`() {
        val json = JSONObject(ProgressBackupCodec.encode(backup))

        assertEquals(2, json.getInt("version"))
        val card = json.getJSONArray("cards").getJSONObject(0)
        assertEquals("FUTURE_SG1", card.getString("item"))
        assertTrue(!card.has("form"))
        val log = json.getJSONArray("reviewLog").getJSONObject(2)
        assertEquals("MEANING_RU_DE", log.getString("item"))
    }

    @Test
    fun `version 1 files with form are still read`() {
        val decoded = ProgressBackupCodec.decode(VERSION_1_FILE)

        assertEquals(
            listOf(
                CardEntity(
                    verbId = 2,
                    item = "FUTURE_SG1",
                    state = "REVIEW",
                    step = null,
                    stability = 2.3,
                    difficulty = 2.1,
                    dueAt = 1_767_427_260_000,
                    lastReviewAt = 1_767_254_460_000,
                    introducedAt = 1_767_254_400_000,
                ),
            ),
            decoded.cards,
        )
        assertEquals(listOf("PAST_M"), decoded.logs.map { it.item })
        assertEquals(12, decoded.settings.newCardsPerDay)
        // Meaning cards did not exist in version 1; the default applies.
        assertEquals(true, decoded.settings.practiceMeaning)
    }

    @Test
    fun `version 1 files cannot contain meaning cards`() {
        assertInvalid(VERSION_1_FILE.replace("\"PAST_M\"", "\"MEANING_RU_DE\""))
    }

    @Test
    fun `unknown exercise in item is rejected`() {
        val encoded = ProgressBackupCodec.encode(backup)
        assertInvalid(encoded.replace("\"MEANING_DE_RU\"", "\"MEANING_XX\""))
        assertInvalid(encoded.replace("\"item\"", "\"form\""))
    }

    @Test
    fun `other json is rejected`() {
        assertInvalid("""{"hello": "world"}""")
    }

    @Test
    fun `newer file version is rejected`() {
        val newer = ProgressBackupCodec.encode(backup).replace("\"version\": 2", "\"version\": 3")
        assertTrue(newer.contains("\"version\": 3"))
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

    private companion object {
        /** Written by Glagolik 0.x before meaning cards (format version 1). */
        val VERSION_1_FILE = """
            { "format": "glagolik-progress", "version": 1, "exportedAt": "2026-01-10T07:00:00Z",
              "settings": { "newFormsPerDay": 12, "sessionSize": 20, "strictYo": false,
                            "tenses": ["PAST"], "autoPlayAudio": false },
              "cards": [ { "verbId": 2, "form": "FUTURE_SG1", "state": "REVIEW", "step": null,
                           "stability": 2.3, "difficulty": 2.1, "dueAt": 1767427260000,
                           "lastReviewAt": 1767254460000, "introducedAt": 1767254400000 } ],
              "reviewLog": [ { "verbId": 1, "form": "PAST_M", "rating": "GOOD",
                               "reviewedAt": 1767254400000, "durationMs": null } ] }
        """.trimIndent()
    }
}
