package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudRecentLoginRequiredException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.cloud.error.toCloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

private const val GOOGLE_PROVIDER_ID = "google.com"
private const val APPLE_PROVIDER_ID = "apple.com"

internal class FirebaseCloudAuth(
    private val platform: PlatformProviderAuth,
    private val auth: FirebaseAuth = Firebase.auth
) : CloudAuth {

    // Linking a provider changes the user's providers without changing who is signed in, so the auth-state listener
    // stays silent; this nudges the flow to read the user again.
    private val userChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var appleGrant: AppleGrant? = null
    private var reauthenticated = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<CloudUser?> = auth.authStateChanged.flatMapLatest { user ->
        flow {
            emit(user?.toCloudUser())
            userChanged.collect { emit(auth.currentUser?.toCloudUser()) }
        }
    }

    override suspend fun signIn(provider: CloudProvider): CloudUser = guard {
        platform.signIn(provider)
        requireUser().toCloudUser()
    }

    override suspend fun link(provider: CloudProvider): CloudUser = guard {
        val user = requireUser().toCloudUser()
        if (provider in user.providers) return@guard user
        platform.link(provider)
        userChanged.tryEmit(Unit)
        requireUser().toCloudUser()
    }

    override suspend fun reauthenticate() = guard {
        val user = requireUser().toCloudUser()
        val provider = if (CloudProvider.APPLE in user.providers) CloudProvider.APPLE else CloudProvider.GOOGLE
        platform.reauthenticate(provider)?.let { appleGrant = it }
        reauthenticated = true
    }

    override suspend fun signOut() = guard {
        auth.signOut()
        platform.clearProviderState()
        forgetSession()
    }

    override suspend fun deleteUser() = guard {
        val user = requireUser()
        if (!reauthenticated) throw CloudRecentLoginRequiredException("Re-authenticate before deleting the user")
        appleGrant?.let { grant ->
            // Best effort: a failed revocation must not keep the user from deleting their account.
            try {
                platform.revokeApple(grant)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
        user.delete()
        platform.clearProviderState()
        forgetSession()
    }

    private fun forgetSession() {
        appleGrant = null
        reauthenticated = false
    }

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw e.toCloudException()
    }
}

private fun FirebaseUser.toCloudUser() = CloudUser(
    uid = uid,
    email = email ?: providerData.firstNotNullOfOrNull { it.email },
    providers = providerData.mapNotNull { it.providerId.toCloudProvider() }.toSet()
)

private fun String.toCloudProvider(): CloudProvider? = when (this) {
    GOOGLE_PROVIDER_ID -> CloudProvider.GOOGLE
    APPLE_PROVIDER_ID -> CloudProvider.APPLE
    else -> null
}
