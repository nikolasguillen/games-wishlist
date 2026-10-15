package com.nikolasguillen.questlog.core.domain.usecase.backup

import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo

/** Replaces the device's data with the cloud backup. */
class RestoreBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): AppResult<BackupInfo> = backupRepository.restore()
}
