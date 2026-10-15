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
    /** The signed-in account, or `null` when nobody is. Emits again when a sign-in method is linked. */
    fun observeAccount(): Flow<Account?>

    /**
     * Opens [provider]'s sign-in and signs in with it. A signed-out caller is the normal case here.
     *
     * Fails with `SignInCancelled` when the user closes the sign-in, which is not an error to show.
     */
    suspend fun signIn(provider: AuthProvider): AppResult<Account>
}
