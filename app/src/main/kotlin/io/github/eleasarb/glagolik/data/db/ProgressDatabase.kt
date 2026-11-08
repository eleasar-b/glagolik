package io.github.eleasarb.glagolik.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.JournalMode
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * User learning progress. Never destructive: a schema change needs a version bump and a
 * Migration (or AutoMigration), checked against the exported schema in app/schemas.
 *
 * Journal mode TRUNCATE instead of WAL keeps everything in the single file progress.db,
 * which is what the backup rules (res/xml) include.
 */
@Database(
    entities = [CardEntity::class, ReviewLogEntity::class],
    version = ProgressDatabase.VERSION,
    exportSchema = true,
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    abstract fun progressBackupDao(): ProgressBackupDao

    companion object {
        const val VERSION = 2
        const val NAME = "progress.db"

        /** All migrations, oldest first. Every released version must be reachable. */
        val MIGRATIONS: List<Migration> = listOf(Migration1To2)

        fun create(context: Context): ProgressDatabase = builder(context, NAME).build()

        /** The configuration of [create]; tests use it with their own file name. */
        internal fun builder(context: Context, name: String): Builder<ProgressDatabase> =
            Room.databaseBuilder(context, ProgressDatabase::class.java, name)
                .setJournalMode(JournalMode.TRUNCATE)
                .apply { MIGRATIONS.forEach { addMigrations(it) } }
    }
}

/**
 * Version 2: a card is "verb + exercise" instead of "verb + form", so the column `form` of
 * `card` and `review_log` becomes `item` (primary key and index included). Existing values are
 * FormKey names, which stay valid exercise ids.
 *
 * `ALTER TABLE … RENAME COLUMN` needs SQLite 3.25 (API 30), minSdk is 26: the tables are
 * recreated and copied instead. Review log ids are kept, so AUTOINCREMENT continues after the
 * highest one.
 */
internal object Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `card_new` (`verb_id` INTEGER NOT NULL, `item` TEXT NOT NULL, " +
                "`state` TEXT NOT NULL, `step` INTEGER, `stability` REAL, `difficulty` REAL, " +
                "`due_at` INTEGER NOT NULL, `last_review_at` INTEGER, " +
                "`introduced_at` INTEGER NOT NULL, PRIMARY KEY(`verb_id`, `item`))",
        )
        db.execSQL(
            "INSERT INTO `card_new` (`verb_id`, `item`, `state`, `step`, `stability`, " +
                "`difficulty`, `due_at`, `last_review_at`, `introduced_at`) " +
                "SELECT `verb_id`, `form`, `state`, `step`, `stability`, `difficulty`, " +
                "`due_at`, `last_review_at`, `introduced_at` FROM `card`",
        )
        db.execSQL("DROP TABLE `card`")
        db.execSQL("ALTER TABLE `card_new` RENAME TO `card`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_card_due_at` ON `card` (`due_at`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_card_introduced_at` ON `card` (`introduced_at`)",
        )

        db.execSQL(
            "CREATE TABLE `review_log_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`verb_id` INTEGER NOT NULL, `item` TEXT NOT NULL, `rating` TEXT NOT NULL, " +
                "`reviewed_at` INTEGER NOT NULL, `duration_ms` INTEGER)",
        )
        db.execSQL(
            "INSERT INTO `review_log_new` (`id`, `verb_id`, `item`, `rating`, `reviewed_at`, " +
                "`duration_ms`) SELECT `id`, `verb_id`, `form`, `rating`, `reviewed_at`, " +
                "`duration_ms` FROM `review_log`",
        )
        db.execSQL("DROP TABLE `review_log`")
        db.execSQL("ALTER TABLE `review_log_new` RENAME TO `review_log`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_review_log_verb_id_item` " +
                "ON `review_log` (`verb_id`, `item`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_review_log_reviewed_at` " +
                "ON `review_log` (`reviewed_at`)",
        )
    }
}
