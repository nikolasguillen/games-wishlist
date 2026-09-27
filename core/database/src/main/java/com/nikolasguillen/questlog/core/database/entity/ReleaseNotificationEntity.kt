package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved game's release-day reminder opt-in. Presence of a row means the reminder is on for [gameId];
 * turning it off deletes the row rather than flipping a flag, so the table only ever holds live opt-ins.
 *
 * @property enabledAt Epoch seconds when the user turned the reminder on. Drives the management list's
 * order (most recently opted-in first) rather than being shown as text.
 * @property notifiedForDate The resolved release date a reminder was actually delivered for, `null` until
 * one is. Stored as a date rather than a boolean so a delayed game that already fired can be re-armed once
 * it resolves to a different future date.
 */
@Entity(tableName = "release_notifications")
data class ReleaseNotificationEntity(
    @PrimaryKey val gameId: Int,
    val enabledAt: Long,
    val notifiedForDate: Long? = null
)
