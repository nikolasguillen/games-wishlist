package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult

/** Signs in with the one method this platform offers. */
class SignInUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): AppResult<Account> = accountRepository.signIn()
}
