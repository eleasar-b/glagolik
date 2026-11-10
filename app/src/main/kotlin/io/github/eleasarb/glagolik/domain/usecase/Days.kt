package io.github.eleasarb.glagolik.domain.usecase

import io.github.eleasarb.glagolik.domain.model.TrainingSettings
import java.time.Instant
import java.time.ZoneId

/* The learning day starts at midnight local time (CLAUDE.md §6). */

internal fun startOfDay(now: Instant, zone: ZoneId): Instant =
    now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

internal fun nextMidnight(now: Instant, zone: ZoneId): Instant =
    now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

/** New cards still allowed today: the daily limit minus the cards introduced today. */
internal fun TrainingSettings.newAllowedToday(introducedToday: Int): Int =
    (newCardsPerDay - introducedToday).coerceAtLeast(0)

/**
 * New cards the next session takes: what is left of the session after [due] due cards, at
 * most [newAllowedToday]. The session may find fewer new cards than that.
 */
internal fun TrainingSettings.newSlots(due: Int, introducedToday: Int): Int =
    minOf(sessionSize - minOf(due, sessionSize), newAllowedToday(introducedToday))
