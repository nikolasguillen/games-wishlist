package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// An ID token with less than this left is replaced before it is used, so a request never carries one that expires
// on the way.
private val REFRESH_MARGIN = 5.minutes

private const val GOOGLE_PROVIDER_ID = "google.com"
private const val APPLE_PROVIDER_ID = "apple.com"

/**
 * [CloudAuth] for desktop over Firebase's REST API: Google's installed-app sign-in gives an ID token, Identity Toolkit
 * turns it into a Firebase session, and the session survives restarts in a [DesktopCloudSessionStore]. Desktop offers
 * Google only (R4).
 *
 * The Firebase ID token is kept in memory and renewed from the stored refresh token when it has under five minutes
 * left. A refresh Firebase refuses ends the session: the user is signed out and the file is removed.
 */
internal class RestCloudAuth(
    private val identity: IdentityToolkitClient,
    private val google: GoogleIdTokenSource,
    private val sessions: DesktopCloudSessionStore,
    private val clock: Clock = Clock.System
) : CloudAuth, IdTokenSupplier {

    private val lock = Mutex()
    private val user = MutableStateFlow(sessions.load()?.toCloudUser())

    private var idToken: String? = null
    private var idTokenExpiresAt: Instant = Instant.DISTANT_PAST

    // Whether the user signed in again since this run began: deleting an account needs it.
    private var reauthenticated = false

    override val provider: CloudProvider = CloudProvider.GOOGLE

    override val currentUser: Flow<CloudUser?> = user.asStateFlow()

    override suspend fun signIn(onAccountPicked: () -> Unit): CloudUser = guard {
        val googleToken = google.obtainIdToken(onAccountPicked)
        lock.withLock {
            val result = identity.signInWithIdp(googleToken)
            val account = lookupOrFallback(result.idToken, result.email)
            val session = DesktopCloudSession(
                uid = result.uid,
                email = account.email ?: result.email,
                providers = account.providerIds,
                refreshToken = result.refreshToken
            )
            sessions.save(session)
            remember(result.idToken, result.expiresInSeconds)
            reauthenticated = false
            session.toCloudUser().also { user.value = it }
        }
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit) = guard {
        val current = user.value ?: throw CloudException.Unauthenticated("Nobody is signed in")
        val googleToken = google.obtainIdToken(onAccountPicked)
        lock.withLock {
            val result = identity.signInWithIdp(googleToken)
            // The account the user just chose is not the one that is signed in: that is not a re-authentication.
            if (result.uid != current.uid) throw CloudException.CredentialCollision("A different account was chosen")
            sessions.load()?.let { sessions.save(it.copy(refreshToken = result.refreshToken)) }
            remember(result.idToken, result.expiresInSeconds)
            reauthenticated = true
        }
    }

    override suspend fun signOut() = guard {
        lock.withLock { forgetSession() }
    }

    override suspend fun deleteUser() = guard {
        if (user.value == null) throw CloudException.Unauthenticated("Nobody is signed in")
        if (!reauthenticated) throw CloudException.RecentLoginRequired("Re-authenticate before deleting the user")
        val token = idToken()
        identity.delete(token)
        lock.withLock { forgetSession() }
    }

    override suspend fun idToken(): String = guard {
        lock.withLock {
            val cached = idToken
            if (cached != null && clock.now() + REFRESH_MARGIN < idTokenExpiresAt) return@withLock cached
            val session = sessions.load() ?: throw CloudException.Unauthenticated("Nobody is signed in")
            try {
                val refreshed = identity.refresh(session.refreshToken)
                sessions.save(session.copy(refreshToken = refreshed.refreshToken))
                remember(refreshed.idToken, refreshed.expiresInSeconds)
                refreshed.idToken
            } catch (e: CloudException.Unauthenticated) {
                // Firebase no longer accepts this session (revoked, disabled or the account is gone).
                forgetSession()
                throw e
            }
        }
    }

    private suspend fun lookupOrFallback(idToken: String, email: String?): AccountLookupResult = try {
        identity.lookup(idToken)
    } catch (e: CancellationException) {
        throw e
    } catch (_: CloudException) {
        // The sign-in itself worked; what the lookup adds is only the list of linked methods.
        AccountLookupResult(email, listOf(GOOGLE_PROVIDER_ID))
    }

    private fun remember(token: String, expiresInSeconds: Long) {
        idToken = token
        idTokenExpiresAt = clock.now() + expiresInSeconds.seconds
    }

    private fun forgetSession() {
        sessions.clear()
        idToken = null
        idTokenExpiresAt = Instant.DISTANT_PAST
        reauthenticated = false
        user.value = null
    }

    // Every member leaves only as a CloudException (or a cancellation), whatever went wrong inside.
    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CloudException) {
        throw e
    } catch (e: Exception) {
        throw CloudException.Unknown(cause = e)
    }

    private fun DesktopCloudSession.toCloudUser() = CloudUser(
        uid = uid,
        email = email,
        providers = providers.mapNotNull {
            when (it) {
                GOOGLE_PROVIDER_ID -> CloudProvider.GOOGLE
                APPLE_PROVIDER_ID -> CloudProvider.APPLE
                else -> null
            }
        }.toSet()
    )
}
