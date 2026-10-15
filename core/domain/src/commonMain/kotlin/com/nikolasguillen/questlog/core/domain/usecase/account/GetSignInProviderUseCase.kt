package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.AuthProvider

/** Which sign-in method this platform offers, so the screen can name it. */
class GetSignInProviderUseCase(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): AuthProvider = accountRepository.signInProvider
}
