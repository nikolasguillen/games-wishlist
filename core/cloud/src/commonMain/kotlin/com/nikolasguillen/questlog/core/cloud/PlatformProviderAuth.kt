package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider

/** The provider-specific half of sign-in, the only thing that differs per platform. */
internal interface PlatformProviderAuth {
    /** Opens [provider]'s sign-in; ends with Firebase signed in. */
    suspend fun signIn(provider: CloudProvider)

    /** Opens [provider]'s sign-in and links the result to Firebase's current user. */
    suspend fun link(provider: CloudProvider)

    /** Signs in again with [provider]. Returns a grant only for [CloudProvider.APPLE]. */
    suspend fun reauthenticate(provider: CloudProvider): AppleGrant?

    /** Tells Apple to revoke the app's access, as the App Store requires on account deletion. */
    suspend fun revokeApple(grant: AppleGrant)

    /** Forgets the provider's cached credential. Called on sign-out. */
    suspend fun clearProviderState()
}
