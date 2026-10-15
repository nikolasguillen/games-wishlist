package com.nikolasguillen.questlog.core.data.backup

/** The snapshot format this build writes, and the newest it can read. A backup above it is refused, never guessed at. */
internal const val BACKUP_FORMAT_VERSION = 1

/**
 * The most base64 characters one Firestore part may hold. A document is capped at 1 MiB, and this leaves about 33%
 * for the other fields and the encoding overhead (research R6).
 */
internal const val PART_MAX_CHARS = 700_000

/** About 28 MB of base64: beyond any plausible collection, so a larger one is refused with `BackupTooLarge`. */
internal const val MAX_BACKUP_PARTS = 40
