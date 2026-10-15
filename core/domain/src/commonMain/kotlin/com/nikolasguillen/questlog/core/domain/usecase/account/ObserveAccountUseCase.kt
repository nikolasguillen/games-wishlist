package com.nikolasguillen.questlog.core.domain.usecase.account

import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import kotlinx.coroutines.flow.Flow

/** The signed-in account, or `null`. */
class ObserveAccountUseCase(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<Account?> = accountRepository.observeAccount()
}
