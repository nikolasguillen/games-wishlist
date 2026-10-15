package com.nikolasguillen.questlog.core.model

import kotlin.time.Instant

/**
 * What the cloud backup holds, as shown on the account screen.
 *
 * @property createdAt when the backup was made, by the clock of the device that made it.
 * @property listCount how many lists the backup holds.
 * @property gameCount how many saved games the backup holds.
 */
data class BackupInfo(
    val createdAt: Instant,
    val listCount: Int,
    val gameCount: Int
)
