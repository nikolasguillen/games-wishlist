package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.Flow

/**
 * The cloud account. Every member throws only `Cloud*Exception` types and `CancellationException`, which is always
 * rethrown.
 */
interface CloudAuth {
    /** The signed-in user, or `null`. It also emits again after [link], because that changes the linked providers. */
    val currentUser: Flow<CloudUser?>

    /**
     * Opens [provider]'s sign-in and signs in with the result.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException the user closed the sign-in.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException the email belongs to an
     * account that uses another method.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudConnectivityException there is no connection.
     */
    suspend fun signIn(provider: CloudProvider): CloudUser

    /**
     * Adds [provider] to the signed-in user. Linking a provider that is already linked returns the current user
     * unchanged.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException nobody is signed in.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudCredentialAlreadyLinkedException the method belongs to
     * another account.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException the user closed the sign-in.
     */
    suspend fun link(provider: CloudProvider): CloudUser

    /**
     * A fresh sign-in with Apple when it is linked, otherwise with Google. Required before [deleteUser]. It also keeps
     * the grant Apple needs to revoke the token on deletion.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException the user closed the sign-in.
     */
    suspend fun reauthenticate()

    /** Signs out and clears the provider's cached credential and any held grant. */
    suspend fun signOut()

    /**
     * Revokes the held Apple grant (best effort, a failure is not thrown) and deletes the user.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudRecentLoginRequiredException [reauthenticate] was not
     * called first.
     */
    suspend fun deleteUser()
}
