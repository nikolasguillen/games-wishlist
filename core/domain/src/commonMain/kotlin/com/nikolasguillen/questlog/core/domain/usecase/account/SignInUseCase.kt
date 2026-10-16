package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult

/** Signs in with the one method this platform offers. `onAccountPicked` is called once the user has chosen in its sheet. */
class SignInUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(onAccountPicked: () -> Unit = {}): AppResult<Account> =
        accountRepository.signIn(onAccountPicked)
}
