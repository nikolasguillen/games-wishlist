package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider

/** The provider-specific half of sign-in, the only thing that differs per platform. */
internal interface PlatformProviderAuth {
    /** The only sign-in method this platform offers: Google on Android, Apple on iOS. */
    val provider: CloudProvider

    /**
     * Opens [provider]'s sign-in; ends with Firebase signed in. [onAccountPicked] runs once the user has chosen in the
     * provider's own UI, before the credential goes to Firebase.
     */
    suspend fun signIn(onAccountPicked: () -> Unit)

    /**
     * Signs in again with [provider]. [onAccountPicked] runs once the user has chosen in the provider's own UI. Returns
     * a grant only where one has to be revoked later (Apple).
     */
    suspend fun reauthenticate(onAccountPicked: () -> Unit): AppleGrant?

    /** Tells Apple to revoke the app's access, as the App Store requires on account deletion. */
    suspend fun revokeApple(grant: AppleGrant)

    /** Forgets the provider's cached credential. Called on sign-out. */
    suspend fun clearProviderState()
}
