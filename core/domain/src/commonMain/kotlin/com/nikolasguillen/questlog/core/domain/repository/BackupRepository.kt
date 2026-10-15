package com.nikolasguillen.questlog.core.domain.repository

import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo

/**
 * The user's data in the cloud: lists, saved games and owned platforms. The owner-approved third repository, kept apart
 * from [GameRepository] because it moves whole snapshots rather than individual games. Every member that touches the
 * network returns `NotSignedIn` for a signed-out caller.
 */
interface BackupRepository {
    /** `Success(null)` means signed in with no backup yet. */
    suspend fun getBackupInfo(): AppResult<BackupInfo?>

    /** Replaces the backup with the device's current data and returns what it now holds. */
    suspend fun backUp(): AppResult<BackupInfo>

    /**
     * Whether the device holds anything beyond a fresh install's seeded default wishlist. DB-only, hence no
     * [AppResult].
     */
    suspend fun hasLocalUserData(): Boolean
}
