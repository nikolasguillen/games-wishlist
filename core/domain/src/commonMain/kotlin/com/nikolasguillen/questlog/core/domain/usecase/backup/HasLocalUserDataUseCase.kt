package com.nikolasguillen.questlog.core.domain.usecase.backup

import com.nikolasguillen.questlog.core.domain.repository.BackupRepository

/** Whether the device holds user data worth protecting, so an empty device is not backed up by accident. */
class HasLocalUserDataUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Boolean = backupRepository.hasLocalUserData()
}
