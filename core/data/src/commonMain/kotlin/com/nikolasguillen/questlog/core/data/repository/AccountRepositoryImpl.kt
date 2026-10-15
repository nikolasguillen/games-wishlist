package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.data.mapper.toAccount
import com.nikolasguillen.questlog.core.data.mapper.toCloudProvider
import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.AuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AccountRepositoryImpl(
    private val cloudAuth: CloudAuth
) : AccountRepository {

    override fun observeAccount(): Flow<Account?> = cloudAuth.currentUser.map { it?.toAccount() }

    override suspend fun signIn(provider: AuthProvider): AppResult<Account> = try {
        AppResult.success(cloudAuth.signIn(provider.toCloudProvider()).toAccount())
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }
}
