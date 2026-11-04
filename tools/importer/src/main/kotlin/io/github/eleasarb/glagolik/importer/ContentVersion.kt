package io.github.eleasarb.glagolik.importer

import java.io.File
import java.security.MessageDigest

/**
 * Version of verbs.db, stored in `data/content-version.txt`.
 *
 * The app only replaces an installed verbs.db with the bundled asset when the Room
 * version (VerbsDatabase.VERSION) changes. So every change of schema or content must bump
 * the version: the importer compares a SHA-256 of the database content with the stored
 * one and increments the version on any difference. The app's VerbsDatabaseSchemaTest
 * fails until VerbsDatabase.VERSION matches the new PRAGMA user_version.
 */
data class ContentVersion(val version: Int, val sha256: String) {
    fun toText(): String = "version=$version\nsha256=$sha256\n"

    /** The version to use for content with hash [sha256]: unchanged or incremented. */
    fun next(sha256: String): ContentVersion =
        if (sha256 == this.sha256) this else ContentVersion(version + 1, sha256)

    companion object {
        /** Version of the first committed verbs.db. */
        const val INITIAL_VERSION = 1

        fun read(file: File): ContentVersion? {
            if (!file.exists()) return null
            val values = file.readLines()
                .filter { '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
            val version = values["version"]?.toIntOrNull()
                ?: throw ImportException("${file.path}: missing or invalid 'version'")
            val sha256 = values["sha256"]
                ?: throw ImportException("${file.path}: missing 'sha256'")
            return ContentVersion(version, sha256)
        }

        fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
