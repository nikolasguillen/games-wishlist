package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.data.mapper.toAccount
import com.nikolasguillen.questlog.core.data.mapper.toAuthProvider
import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.model.RepositoryError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AccountRepositoryImpl(
    private val cloudAuth: CloudAuth
) : AccountRepository {

    override val signInProvider: AuthProvider get() = cloudAuth.provider.toAuthProvider()

    override fun observeAccount(): Flow<Account?> = cloudAuth.currentUser.map { it?.toAccount() }

    override suspend fun signIn(onAccountPicked: () -> Unit): AppResult<Account> = try {
        AppResult.success(cloudAuth.signIn(onAccountPicked).toAccount())
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }

    override suspend fun signOut(): AppResult<Unit> = try {
        cloudAuth.signOut()
        AppResult.success(Unit)
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit): AppResult<Unit> = try {
        cloudAuth.reauthenticate(onAccountPicked)
        AppResult.success(Unit)
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }

    // The provider can still ask for a recent sign-in even right after reauthenticate(), when the user took long over
    // it. That is answered with one more sign-in and one more attempt, never a loop: a second refusal means the session
    // is not usable, which is what NotSignedIn tells the screen.
    override suspend fun deleteAccount(): AppResult<Unit> = try {
        try {
            cloudAuth.deleteUser()
        } catch (e: CloudException.RecentLoginRequired) {
            cloudAuth.reauthenticate()
            cloudAuth.deleteUser()
        }
        AppResult.success(Unit)
    } catch (e: CloudException.RecentLoginRequired) {
        AppResult.failure(RepositoryError.NotSignedIn)
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }
}
