package com.nikolasguillen.questlog.core.cloud

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider as AndroidOAuthProvider
import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.android
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.auth.AuthResult as AndroidAuthResult

private const val APPLE_PROVIDER_ID = "apple.com"
private const val WEB_CONTEXT_CANCELED = "ERROR_WEB_CONTEXT_CANCELED"

/**
 * Google through Credential Manager, whose ID token goes to GitLive; Apple through Firebase's own web flow, which
 * GitLive does not wrap, so those calls use the underlying Android objects.
 */
internal class AndroidProviderAuth(
    private val context: Context,
    private val activityTracker: CurrentActivityTracker,
    private val webClientId: String
) : PlatformProviderAuth {

    private val auth get() = Firebase.auth

    override suspend fun signIn(provider: CloudProvider) {
        when (provider) {
            CloudProvider.GOOGLE -> auth.signInWithCredential(googleCredential())
            CloudProvider.APPLE -> appleFlow(
                resumePending = true,
                start = { activity, apple -> auth.android.startActivityForSignInWithProvider(activity, apple) }
            )
        }
    }

    override suspend fun link(provider: CloudProvider) {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        when (provider) {
            CloudProvider.GOOGLE -> user.linkWithCredential(googleCredential())
            CloudProvider.APPLE -> appleFlow(
                resumePending = false,
                start = { activity, apple -> user.android.startActivityForLinkWithProvider(activity, apple) }
            )
        }
    }

    override suspend fun reauthenticate(provider: CloudProvider): AppleGrant? {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        return when (provider) {
            CloudProvider.GOOGLE -> {
                user.reauthenticate(googleCredential())
                null
            }

            CloudProvider.APPLE -> {
                val result = appleFlow(
                    resumePending = false,
                    start = { activity, apple -> user.android.startActivityForReauthenticateWithProvider(activity, apple) }
                )
                val accessToken = (result.credential as? OAuthCredential)?.accessToken
                accessToken?.let(::AppleGrant)
            }
        }
    }

    override suspend fun revokeApple(grant: AppleGrant) {
        auth.android.revokeAccessToken(grant.value).await()
    }

    override suspend fun clearProviderState() {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun googleCredential(): AuthCredential {
        val activity = requireActivity()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw CloudSignInCancelledException("The Google sign-in was closed", e)
        } catch (e: NoCredentialException) {
            // No Google account on the device: nothing to say, the user adds one and tries again.
            throw CloudSignInCancelledException("No Google account is available", e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw CloudException("Credential Manager returned an unexpected credential type")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return GoogleAuthProvider.credential(idToken, null)
    }

    /**
     * Runs Firebase's Apple web flow. With [resumePending], a result left by an Activity that was recreated
     * mid-flow is used instead of starting a second flow.
     */
    private suspend fun appleFlow(
        resumePending: Boolean,
        start: (Activity, AndroidOAuthProvider) -> Task<AndroidAuthResult>
    ): AndroidAuthResult {
        val activity = requireActivity()
        val pending = if (resumePending) auth.android.pendingAuthResult else null
        val task = pending ?: start(
            activity,
            AndroidOAuthProvider.newBuilder(APPLE_PROVIDER_ID).setScopes(listOf("email", "name")).build()
        )
        return try {
            task.await()
        } catch (e: FirebaseAuthWebException) {
            if (e.errorCode == WEB_CONTEXT_CANCELED) {
                throw CloudSignInCancelledException("The Apple sign-in was closed", e)
            }
            throw e
        }
    }

    // With no resumed Activity the user left the app mid-tap, which is a cancelled sign-in.
    private fun requireActivity(): Activity =
        activityTracker.current ?: throw CloudSignInCancelledException("The app is not in the foreground")
}

// `kotlinx-coroutines-play-services` is not a dependency of the project, and this is all the module needs from it.
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel(CancellationException("The Firebase task was cancelled")) }
}
