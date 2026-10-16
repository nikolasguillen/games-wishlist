package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val APPLE_PROVIDER_ID = "apple.com"

/** Sign in with Apple over the Swift [IdentityProviderBridge]; the tokens it returns go to GitLive. */
internal class IosProviderAuth(private val bridge: IdentityProviderBridge) : PlatformProviderAuth {

    override val provider = CloudProvider.APPLE

    private val auth get() = Firebase.auth

    override suspend fun signIn(onAccountPicked: () -> Unit) {
        val apple = appleSignIn()
        onAccountPicked()
        auth.signInWithCredential(apple.credential)
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit): AppleGrant? {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        val apple = appleSignIn()
        onAccountPicked()
        user.reauthenticate(apple.credential)
        return AppleGrant(apple.authorizationCode)
    }

    override suspend fun revokeApple(grant: AppleGrant) {
        val revoked = suspendCancellableCoroutine { continuation ->
            bridge.revokeAppleToken(grant.value) { if (continuation.isActive) continuation.resume(it) }
        }
        if (!revoked) throw CloudException("Apple did not revoke the token")
    }

    // Apple keeps no credential on the device for this app to forget.
    override suspend fun clearProviderState() = Unit

    private suspend fun appleSignIn(): AppleSignIn {
        val result = suspendCancellableCoroutine { continuation ->
            bridge.signInWithApple { if (continuation.isActive) continuation.resume(it) }
        }
        when (result.outcome) {
            ProviderOutcome.SUCCESS -> Unit
            ProviderOutcome.CANCELLED -> throw CloudSignInCancelledException("The Apple sign-in was closed")
            ProviderOutcome.FAILED -> throw CloudException("The Apple sign-in failed")
        }
        val idToken = result.idToken ?: throw CloudException("Apple returned no identity token")
        val rawNonce = result.rawNonce ?: throw CloudException("Apple returned no nonce")
        val authorizationCode = result.authorizationCode ?: throw CloudException("Apple returned no authorization code")
        return AppleSignIn(
            credential = OAuthProvider.credential(providerId = APPLE_PROVIDER_ID, idToken = idToken, rawNonce = rawNonce),
            authorizationCode = authorizationCode
        )
    }

    private class AppleSignIn(val credential: AuthCredential, val authorizationCode: String)
}
