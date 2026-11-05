package io.github.eleasarb.glagolik.data.backup

import io.github.eleasarb.glagolik.data.db.CardEntity
import io.github.eleasarb.glagolik.data.db.ReviewLogEntity
import io.github.eleasarb.glagolik.domain.model.FormKey
import io.github.eleasarb.glagolik.domain.model.Tense
import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import io.github.eleasarb.glagolik.domain.srs.CardState
import io.github.eleasarb.glagolik.domain.srs.Rating
import java.time.Instant
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Everything that belongs to the learner: progress and settings. */
data class ProgressBackup(
    val exportedAt: Instant,
    val settings: TrainingSettings,
    val cards: List<CardEntity>,
    val logs: List<ReviewLogEntity>,
)

class BackupFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * JSON format of the progress export (org.json from the Android framework, no extra
 * dependency). Readable on purpose, so a learner can inspect their own data.
 *
 * ```
 * { "format": "glagolik-progress", "version": 1, "exportedAt": "...Z",
 *   "settings": { "newFormsPerDay": 10, "sessionSize": 20, "strictYo": false,
 *                 "tenses": ["PRESENT", ...] },
 *   "cards": [ { "verbId": 2, "form": "FUTURE_SG1", "state": "REVIEW", "step": null,
 *                "stability": 2.3, "difficulty": 2.1, "dueAt": 1767427260000,
 *                "lastReviewAt": 1767254460000, "introducedAt": 1767254400000 } ],
 *   "reviewLog": [ { "verbId": 2, "form": "FUTURE_SG1", "rating": "GOOD",
 *                    "reviewedAt": 1767254400000, "durationMs": 4000 } ] }
 * ```
 *
 * Times are epoch milliseconds (UTC). Verbs are referenced by their stable id.
 */
object ProgressBackupCodec {
    const val FORMAT = "glagolik-progress"
    const val VERSION = 1

    fun encode(backup: ProgressBackup): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("exportedAt", backup.exportedAt.toString())
        .put("settings", encodeSettings(backup.settings))
        .put("cards", JSONArray(backup.cards.map(::encodeCard)))
        .put("reviewLog", JSONArray(backup.logs.map(::encodeLog)))
        .toString(2)

    /** Parses and validates the whole file; throws [BackupFormatException] on any problem. */
    fun decode(text: String): ProgressBackup = try {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("not a Glagolik progress file")
        }
        val version = root.getInt("version")
        if (version > VERSION) {
            throw BackupFormatException("file version $version is newer than supported $VERSION")
        }
        ProgressBackup(
            exportedAt = Instant.parse(root.getString("exportedAt")),
            settings = decodeSettings(root.getJSONObject("settings")),
            cards = root.getJSONArray("cards").objects().map(::decodeCard),
            logs = root.getJSONArray("reviewLog").objects().map(::decodeLog),
        )
    } catch (e: JSONException) {
        throw BackupFormatException("invalid file: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        // enum valueOf, Instant.parse
        throw BackupFormatException("invalid value: ${e.message}", e)
    } catch (e: java.time.format.DateTimeParseException) {
        throw BackupFormatException("invalid date: ${e.message}", e)
    }

    private fun encodeSettings(settings: TrainingSettings) = JSONObject()
        .put("newFormsPerDay", settings.newFormsPerDay)
        .put("sessionSize", settings.sessionSize)
        .put("strictYo", settings.strictYo)
        .put("tenses", JSONArray(settings.tenses.map { it.name }.sorted()))

    private fun decodeSettings(json: JSONObject) = TrainingSettings(
        newFormsPerDay = json.getInt("newFormsPerDay"),
        sessionSize = json.getInt("sessionSize"),
        strictYo = json.getBoolean("strictYo"),
        tenses = json.getJSONArray("tenses").strings().map(Tense::valueOf).toSet(),
    )

    private fun encodeCard(card: CardEntity) = JSONObject()
        .put("verbId", card.verbId)
        .put("form", card.form)
        .put("state", card.state)
        .put("step", card.step ?: JSONObject.NULL)
        .put("stability", card.stability ?: JSONObject.NULL)
        .put("difficulty", card.difficulty ?: JSONObject.NULL)
        .put("dueAt", card.dueAt)
        .put("lastReviewAt", card.lastReviewAt ?: JSONObject.NULL)
        .put("introducedAt", card.introducedAt)

    private fun decodeCard(json: JSONObject) = CardEntity(
        verbId = json.getInt("verbId"),
        form = FormKey.valueOf(json.getString("form")).name,
        state = CardState.valueOf(json.getString("state")).name,
        step = json.optIntOrNull("step"),
        stability = json.optDoubleOrNull("stability"),
        difficulty = json.optDoubleOrNull("difficulty"),
        dueAt = json.getLong("dueAt"),
        lastReviewAt = json.optLongOrNull("lastReviewAt"),
        introducedAt = json.getLong("introducedAt"),
    )

    private fun encodeLog(log: ReviewLogEntity) = JSONObject()
        .put("verbId", log.verbId)
        .put("form", log.form)
        .put("rating", log.rating)
        .put("reviewedAt", log.reviewedAt)
        .put("durationMs", log.durationMs ?: JSONObject.NULL)

    private fun decodeLog(json: JSONObject) = ReviewLogEntity(
        verbId = json.getInt("verbId"),
        form = FormKey.valueOf(json.getString("form")).name,
        rating = Rating.valueOf(json.getString("rating")).name,
        reviewedAt = json.getLong("reviewedAt"),
        durationMs = json.optLongOrNull("durationMs"),
    )
}
