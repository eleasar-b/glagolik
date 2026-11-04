package io.github.eleasarb.glagolik.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Mirrors the schema written by tools/importer (DatabaseWriter.kt). Room validates the
 * bundled verbs.db against these entities on first open; VerbsDatabaseSchemaTest checks
 * the same on the JVM.
 */

@Entity(
    tableName = "verb",
    indices = [Index("infinitive_bare")],
)
data class VerbEntity(
    @PrimaryKey val id: Int,
    val infinitive: String,
    @ColumnInfo(name = "infinitive_bare") val infinitiveBare: String,
    val aspect: String,
    @ColumnInfo(name = "frequency_rank") val frequencyRank: Int,
    @ColumnInfo(name = "is_core") val isCore: Boolean,
    @ColumnInfo(name = "translation_en") val translationEn: String,
    @ColumnInfo(name = "translation_de") val translationDe: String,
)

@Entity(
    tableName = "verb_form",
    primaryKeys = ["verb_id", "form", "variant"],
    foreignKeys = [
        ForeignKey(
            entity = VerbEntity::class,
            parentColumns = ["id"],
            childColumns = ["verb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("verb_id")],
)
data class VerbFormEntity(
    @ColumnInfo(name = "verb_id") val verbId: Int,
    val form: String,
    val variant: Int,
    val value: String,
    @ColumnInfo(name = "value_bare") val valueBare: String,
    @ColumnInfo(name = "is_trainable") val isTrainable: Boolean,
)

@Entity(
    tableName = "aspect_partner",
    primaryKeys = ["verb_id", "partner_id"],
    foreignKeys = [
        ForeignKey(
            entity = VerbEntity::class,
            parentColumns = ["id"],
            childColumns = ["verb_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VerbEntity::class,
            parentColumns = ["id"],
            childColumns = ["partner_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("verb_id"), Index("partner_id")],
)
data class AspectPartnerEntity(
    @ColumnInfo(name = "verb_id") val verbId: Int,
    @ColumnInfo(name = "partner_id") val partnerId: Int,
)
