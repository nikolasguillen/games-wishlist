package com.nikolasguillen.questlog.core.domain.repository

import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.AuthProvider
import kotlinx.coroutines.flow.Flow

/**
 * The signed-in account the cloud backup belongs to. Separate from [GameRepository] because it shares no data with
 * it: it is the owner-approved second repository, and has its own implementation in `:core:data`.
 */
interface AccountRepository {
    /**
     * The only sign-in method this platform offers: Google on Android, Apple on iOS. Both reach the same account when
     * they share an email address.
     */
    val signInProvider: AuthProvider

    /** The signed-in account, or `null` when nobody is. Emits again when a sign-in method is linked. */
    fun observeAccount(): Flow<Account?>

    /**
     * Opens [signInProvider]'s sign-in and signs in with it. A signed-out caller is the normal case here.
     *
     * Fails with `SignInCancelled` when the user closes the sign-in, which is not an error to show.
     */
    suspend fun signIn(): AppResult<Account>

    /** Signs out. The device's data and the cloud backup are untouched. */
    suspend fun signOut(): AppResult<Unit>

    /**
     * A fresh sign-in with [signInProvider], which the provider requires before it deletes an account.
     *
     * Fails with `SignInCancelled` when the user closes the sign-in.
     */
    suspend fun reauthenticate(): AppResult<Unit>

    /**
     * Deletes the account, revoking Apple's access first where the platform signs in with Apple. Call [reauthenticate]
     * first; if the provider still asks for a recent sign-in, this asks once more and retries once.
     */
    suspend fun deleteAccount(): AppResult<Unit>
}
