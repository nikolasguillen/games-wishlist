package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [DeleteAccountUseCase]: a fresh sign-in, then the backup, then the account, stopping at the first failure so
 * the user is never left with data in the cloud that no account can reach.
 */
class DeleteAccountUseCaseTest {

    private val accountRepository = mockk<AccountRepository>()
    private val backupRepository = mockk<BackupRepository>()

    private val useCase = DeleteAccountUseCase(accountRepository, backupRepository)

    @Test
    fun `the calls happen in order reauthenticate then backup then account`() = runTest {
        coEvery { accountRepository.reauthenticate(any()) } returns AppResult.success(Unit)
        coEvery { backupRepository.deleteBackup() } returns AppResult.success(Unit)
        coEvery { accountRepository.deleteAccount() } returns AppResult.success(Unit)

        val result = useCase()

        assertEquals(AppResult.success(Unit), result)
        coVerifyOrder {
            accountRepository.reauthenticate(any())
            backupRepository.deleteBackup()
            accountRepository.deleteAccount()
        }
    }

    @Test
    fun `a failed reauthentication stops everything after it`() = runTest {
        coEvery { accountRepository.reauthenticate(any()) } returns AppResult.failure(RepositoryError.SignInCancelled)

        val result = useCase()

        assertEquals(AppResult.failure(RepositoryError.SignInCancelled), result)
        coVerify(exactly = 0) { backupRepository.deleteBackup() }
        coVerify(exactly = 0) { accountRepository.deleteAccount() }
    }

    @Test
    fun `a failed backup delete leaves the account alone`() = runTest {
        coEvery { accountRepository.reauthenticate(any()) } returns AppResult.success(Unit)
        coEvery { backupRepository.deleteBackup() } returns AppResult.failure(RepositoryError.NoNetwork)

        val result = useCase()

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), result)
        coVerify(exactly = 0) { accountRepository.deleteAccount() }
    }

    @Test
    fun `a failed account delete returns that failure`() = runTest {
        coEvery { accountRepository.reauthenticate(any()) } returns AppResult.success(Unit)
        coEvery { backupRepository.deleteBackup() } returns AppResult.success(Unit)
        coEvery { accountRepository.deleteAccount() } returns AppResult.failure(RepositoryError.RequestTimeout)

        val result = useCase()

        assertEquals(AppResult.failure(RepositoryError.RequestTimeout), result)
    }
}
