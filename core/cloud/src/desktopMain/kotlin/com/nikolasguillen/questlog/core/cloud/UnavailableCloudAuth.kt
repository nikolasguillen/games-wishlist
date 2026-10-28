package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Stand-in for the Firebase account on desktop until the REST client exists (specs/020-desktop-app, T083 and T096):
 * nobody is signed in and signing in fails. Without it the Firebase implementation would be created, and GitLive
 * throws when no Firebase app was initialised, which Settings hits as soon as it opens.
 */
internal class UnavailableCloudAuth : CloudAuth {

    override val provider: CloudProvider = CloudProvider.GOOGLE

    override val currentUser: Flow<CloudUser?> = flowOf(null)

    override suspend fun signIn(onAccountPicked: () -> Unit): CloudUser {
        throw CloudException.Unknown("Cloud is not available yet on desktop")
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit) {
        throw CloudException.Unknown("Cloud is not available yet on desktop")
    }

    override suspend fun signOut() = Unit

    override suspend fun deleteUser() = Unit
}
