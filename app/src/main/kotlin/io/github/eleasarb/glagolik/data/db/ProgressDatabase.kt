package io.github.eleasarb.glagolik.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * User learning progress. Never destructive: a schema change needs a version bump and a
 * Migration (or AutoMigration), checked against the exported schema in app/schemas.
 */
@Database(
    entities = [CardEntity::class, ReviewLogEntity::class],
    version = ProgressDatabase.VERSION,
    exportSchema = true,
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        const val VERSION = 1
        const val NAME = "progress.db"

        fun create(context: Context): ProgressDatabase =
            Room.databaseBuilder(context, ProgressDatabase::class.java, NAME).build()
    }
}
