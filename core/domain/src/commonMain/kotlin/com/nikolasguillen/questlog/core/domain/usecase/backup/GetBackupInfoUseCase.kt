package com.nikolasguillen.questlog.core.domain.usecase.backup

import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo

/** What the account's backup holds, or `null` when there is none. */
class GetBackupInfoUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): AppResult<BackupInfo?> = backupRepository.getBackupInfo()
}
