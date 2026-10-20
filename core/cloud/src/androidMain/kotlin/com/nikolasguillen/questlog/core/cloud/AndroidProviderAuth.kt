package com.nikolasguillen.questlog.core.cloud

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.error.CloudNoAccountException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth

/**
 * Google through Credential Manager: the ID token it returns goes to GitLive. Google is the only method Android
 * offers, so Apple's web flow is not here.
 */
internal class AndroidProviderAuth(
    private val context: Context,
    private val activityTracker: CurrentActivityTracker,
    private val webClientId: String
) : PlatformProviderAuth {

    override val provider = CloudProvider.GOOGLE

    private val auth get() = Firebase.auth

    override suspend fun signIn(onAccountPicked: () -> Unit) {
        val credential = googleCredential()
        onAccountPicked()
        auth.signInWithCredential(credential)
    }

    override suspend fun reauthenticate(onAccountPicked: () -> Unit): AppleGrant? {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        val credential = googleCredential()
        onAccountPicked()
        user.reauthenticate(credential)
        return null
    }

    // Google keeps no server-side grant for this app to revoke, and reauthenticate never returns one here.
    override suspend fun revokeApple(grant: AppleGrant) = Unit

    override suspend fun clearProviderState() {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun googleCredential(): AuthCredential {
        // With no resumed Activity the user left the app mid-tap, which is a cancelled sign-in.
        val activity = activityTracker.current
            ?: throw CloudSignInCancelledException("The app is not in the foreground")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw CloudSignInCancelledException("The Google sign-in was closed", e)
        } catch (e: NoCredentialException) {
            // Not a cancel: the user never saw a sheet, so the screen has to say why nothing happened.
            throw CloudNoAccountException("No Google account is available", e)
        } catch (e: GetCredentialException) {
            // Credential Manager's other failures (provider misconfigured, interrupted, unknown) are not ours to
            // classify; wrapping them keeps this module's contract and the cause for the log.
            throw CloudException("Credential Manager failed: ${e.type}", e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw CloudException("Credential Manager returned an unexpected credential type")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return GoogleAuthProvider.credential(idToken, null)
    }
}
