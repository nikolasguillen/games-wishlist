package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.Flow

/**
 * The cloud account. Every member throws only `Cloud*Exception` types and `CancellationException`, which is always
 * rethrown.
 *
 * Each platform offers exactly one sign-in method, [provider]: Google on Android and Apple on iOS. Both reach the same
 * account when they share an email address, which Firebase links by itself.
 */
interface CloudAuth {
    /** The only sign-in method this platform offers. */
    val provider: CloudProvider

    /** The signed-in user, or `null`. */
    val currentUser: Flow<CloudUser?>

    /**
     * Opens [provider]'s sign-in and signs in with the result. [onAccountPicked] is called once the user has chosen in
     * the provider's own sheet, so the caller can show progress for the part that follows; it is not called when the
     * user closes the sheet.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException the user closed the sign-in.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudNoAccountException the device has no account to sign
     * in with.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException the email belongs to an
     * account that uses a method Firebase did not link by itself.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudConnectivityException there is no connection.
     */
    suspend fun signIn(onAccountPicked: () -> Unit = {}): CloudUser

    /**
     * A fresh sign-in with [provider]. Required before [deleteUser]. On iOS it also keeps the grant Apple needs to
     * revoke the token on deletion. [onAccountPicked] is called once the user has chosen in the provider's own sheet; it
     * is not called when the user closes the sheet.
     *
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException nobody is signed in.
     * @throws com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException the user closed the sign-in.
     */
    suspend fun reauthenticate(onAccountPicked: () -> Unit = {})

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
