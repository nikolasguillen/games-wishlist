package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudConnectivityException
import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException
import com.nikolasguillen.questlog.core.cloud.error.CloudRecentLoginRequiredException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import com.nikolasguillen.questlog.core.data.cloud.FakeCloudAuth
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.model.RepositoryError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class AccountRepositoryImplTest {

    private val auth = FakeCloudAuth()
    private val repository = AccountRepositoryImpl(auth)

    @Test
    fun `observeAccount maps the cloud user to an account`() = runTest {
        auth.user.value = CloudUser(
            uid = "uid-9",
            email = "ada@example.com",
            providers = setOf(CloudProvider.GOOGLE, CloudProvider.APPLE)
        )

        val account = repository.observeAccount().first()

        assertEquals(
            Account(id = "uid-9", email = "ada@example.com", providers = setOf(AuthProvider.GOOGLE, AuthProvider.APPLE)),
            account
        )
    }

    @Test
    fun `observeAccount emits null when nobody is signed in`() = runTest {
        assertNull(repository.observeAccount().first())
    }

    @Test
    fun `the sign-in provider is the one the platform offers`() {
        assertEquals(AuthProvider.GOOGLE, AccountRepositoryImpl(FakeCloudAuth(provider = CloudProvider.GOOGLE)).signInProvider)
        assertEquals(AuthProvider.APPLE, AccountRepositoryImpl(FakeCloudAuth(provider = CloudProvider.APPLE)).signInProvider)
    }

    @Test
    fun `signIn returns the mapped account`() = runTest {
        val result = repository.signIn()

        assertEquals(1, auth.signInCalls)
        assertEquals(setOf(AuthProvider.GOOGLE), (result as AppResult.Success).data.providers)
        assertEquals("ada@example.com", result.data.email)
    }

    @Test
    fun `signIn reports the picked account to the caller, and not a closed sheet`() = runTest {
        var picked = 0
        repository.signIn(onAccountPicked = { picked++ })
        assertEquals(1, picked)

        auth.signInFailure = CloudSignInCancelledException()
        repository.signIn(onAccountPicked = { picked++ })
        assertEquals(1, picked)
    }

    @Test
    fun `signIn on an Apple platform returns an Apple account`() = runTest {
        val apple = FakeCloudAuth(provider = CloudProvider.APPLE)

        val result = AccountRepositoryImpl(apple).signIn()

        assertEquals(setOf(AuthProvider.APPLE), (result as AppResult.Success).data.providers)
    }

    @Test
    fun `a closed sign-in becomes sign-in cancelled`() = runTest {
        auth.signInFailure = CloudSignInCancelledException()

        val result = repository.signIn()

        assertEquals(RepositoryError.SignInCancelled, (result as AppResult.Failure).error)
    }

    @Test
    fun `a credential collision becomes account exists with other method`() = runTest {
        auth.signInFailure = CloudCredentialCollisionException()

        val result = repository.signIn()

        assertEquals(RepositoryError.AccountExistsWithOtherMethod, (result as AppResult.Failure).error)
    }

    @Test
    fun `cancellation is rethrown and not mapped`() = runTest {
        auth.signInFailure = CancellationException("scope closed")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.signIn() }
        }
    }

    @Test
    fun `signOut succeeds and the account goes away`() = runTest {
        val signedIn = FakeCloudAuth(
            initial = CloudUser(uid = "uid-1", email = "ada@example.com", providers = setOf(CloudProvider.GOOGLE))
        )

        val result = AccountRepositoryImpl(signedIn).signOut()

        assertEquals(AppResult.success(Unit), result)
        assertNull(AccountRepositoryImpl(signedIn).observeAccount().first())
    }

    @Test
    fun `signOut maps a failure`() = runTest {
        auth.signOutFailure = CloudConnectivityException()

        val result = repository.signOut()

        assertEquals(RepositoryError.NoNetwork, (result as AppResult.Failure).error)
    }

    @Test
    fun `a closed reauthentication becomes sign-in cancelled`() = runTest {
        auth.reauthenticateFailure = CloudSignInCancelledException()

        val result = repository.reauthenticate()

        assertEquals(RepositoryError.SignInCancelled, (result as AppResult.Failure).error)
    }

    @Test
    fun `reauthenticate succeeds`() = runTest {
        assertEquals(AppResult.success(Unit), repository.reauthenticate())
        assertEquals(listOf("reauthenticate"), auth.calls)
    }

    @Test
    fun `deleteAccount deletes the user without signing in again`() = runTest {
        val result = repository.deleteAccount()

        assertEquals(AppResult.success(Unit), result)
        assertEquals(listOf("deleteUser"), auth.calls)
    }

    @Test
    fun `deleteAccount signs in once more and retries once when a recent sign-in is asked for`() = runTest {
        auth.deleteUserFailures += CloudRecentLoginRequiredException()

        val result = repository.deleteAccount()

        assertEquals(AppResult.success(Unit), result)
        assertEquals(listOf("deleteUser", "reauthenticate", "deleteUser"), auth.calls)
    }

    @Test
    fun `a second recent-sign-in refusal is not-signed-in and never loops`() = runTest {
        auth.deleteUserFailures += CloudRecentLoginRequiredException()
        auth.deleteUserFailures += CloudRecentLoginRequiredException()

        val result = repository.deleteAccount()

        assertEquals(RepositoryError.NotSignedIn, (result as AppResult.Failure).error)
        assertEquals(listOf("deleteUser", "reauthenticate", "deleteUser"), auth.calls)
    }

    @Test
    fun `a closed sign-in during the retry is sign-in cancelled`() = runTest {
        auth.deleteUserFailures += CloudRecentLoginRequiredException()
        auth.reauthenticateFailure = CloudSignInCancelledException()

        val result = repository.deleteAccount()

        assertEquals(RepositoryError.SignInCancelled, (result as AppResult.Failure).error)
    }

    @Test
    fun `deleteAccount maps another failure`() = runTest {
        auth.deleteUserFailures += CloudConnectivityException()

        val result = repository.deleteAccount()

        assertEquals(RepositoryError.NoNetwork, (result as AppResult.Failure).error)
    }

    @Test
    fun `deleteAccount rethrows cancellation`() = runTest {
        auth.deleteUserFailures += CancellationException("scope closed")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { repository.deleteAccount() }
        }
    }
}
