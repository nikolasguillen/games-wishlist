package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider

/** The provider-specific half of sign-in, the only thing that differs per platform. */
internal interface PlatformProviderAuth {
    /** The only sign-in method this platform offers: Google on Android, Apple on iOS. */
    val provider: CloudProvider

    /** Opens [provider]'s sign-in; ends with Firebase signed in. */
    suspend fun signIn()

    /** Signs in again with [provider]. Returns a grant only where one has to be revoked later (Apple). */
    suspend fun reauthenticate(): AppleGrant?

    /** Tells Apple to revoke the app's access, as the App Store requires on account deletion. */
    suspend fun revokeApple(grant: AppleGrant)

    /** Forgets the provider's cached credential. Called on sign-out. */
    suspend fun clearProviderState()
}
