package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.AppResult

/** Signs out, leaving the device's data and the cloud backup as they are. */
class SignOutUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = accountRepository.signOut()
}
