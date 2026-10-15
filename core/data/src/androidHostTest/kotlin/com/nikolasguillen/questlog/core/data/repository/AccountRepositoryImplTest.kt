package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException
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
}
