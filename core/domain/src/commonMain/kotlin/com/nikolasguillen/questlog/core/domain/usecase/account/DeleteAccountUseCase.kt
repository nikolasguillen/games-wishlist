package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult

/**
 * Deletes the cloud backup and then the account, in that order, stopping at the first failure. The device's own data is
 * not touched.
 *
 * The order is what keeps the user from being stranded. The fresh sign-in comes first because the provider requires it
 * before deleting an account. The backup goes before the account because a failure after it leaves a signed-in account
 * with no backup, which Settings shows and lets the user delete again. The other way round, a failed backup delete would
 * leave data in the cloud that no signed-in account can reach.
 *
 * `onAccountPicked` is called once the user has chosen in the provider's sheet, so the caller can show progress for the
 * part that follows.
 */
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(onAccountPicked: () -> Unit = {}): AppResult<Unit> {
        val reauthenticated = accountRepository.reauthenticate(onAccountPicked)
        if (reauthenticated is AppResult.Failure) return reauthenticated

        val backupDeleted = backupRepository.deleteBackup()
        if (backupDeleted is AppResult.Failure) return backupDeleted

        return accountRepository.deleteAccount()
    }
}
