package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.AuthProvider

/** Signs in with Google or Apple. */
class SignInUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(provider: AuthProvider): AppResult<Account> = accountRepository.signIn(provider)
}
