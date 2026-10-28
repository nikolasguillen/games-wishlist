package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider

/**
 * Placeholder that lets the desktop Koin graph resolve while the cloud account is built (specs/020-desktop-app, T021).
 * Signing in always fails; everything else does nothing. It is deleted when the REST client is bound in its place
 * (T096).
 */
internal class UnavailablePlatformProviderAuth : PlatformProviderAuth {

    override val provider: CloudProvider = CloudProvider.GOOGLE

    override suspend fun signIn(onAccountPicked: () -> Unit) {
        throw CloudException.Unknown("Cloud is not available yet on desktop")
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit): AppleGrant? {
        throw CloudException.Unknown("Cloud is not available yet on desktop")
    }

    override suspend fun revokeApple(grant: AppleGrant) = Unit

    override suspend fun clearProviderState() = Unit
}
