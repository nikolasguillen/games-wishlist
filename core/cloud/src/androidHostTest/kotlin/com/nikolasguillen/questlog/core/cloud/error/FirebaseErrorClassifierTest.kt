package com.nikolasguillen.questlog.core.cloud.error

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseErrorClassifierTest {

    @Test
    fun `firestore unavailable is a connectivity failure`() {
        assertTrue(classifyFirestoreCode("UNAVAILABLE") is CloudException.Connectivity)
    }

    @Test
    fun `firestore deadline exceeded is a timeout`() {
        assertTrue(classifyFirestoreCode("DEADLINE_EXCEEDED") is CloudException.Timeout)
    }

    @Test
    fun `firestore unauthenticated and permission denied are unauthenticated`() {
        assertTrue(classifyFirestoreCode("UNAUTHENTICATED") is CloudException.Unauthenticated)
        assertTrue(classifyFirestoreCode("PERMISSION_DENIED") is CloudException.Unauthenticated)
    }

    @Test
    fun `any other firestore code is left for the caller to rethrow`() {
        assertNull(classifyFirestoreCode("NOT_FOUND"))
        assertNull(classifyFirestoreCode("INTERNAL"))
    }

    @Test
    fun `android codes for an account that exists under another method are a collision`() {
        assertTrue(
            classifyAuthCode("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") is CloudException.CredentialCollision
        )
        assertTrue(classifyAuthCode("ERROR_EMAIL_ALREADY_IN_USE") is CloudException.CredentialCollision)
    }

    @Test
    fun `android codes for a credential that belongs elsewhere are already linked`() {
        assertTrue(classifyAuthCode("ERROR_CREDENTIAL_ALREADY_IN_USE") is CloudException.CredentialAlreadyLinked)
        assertTrue(classifyAuthCode("ERROR_PROVIDER_ALREADY_LINKED") is CloudException.CredentialAlreadyLinked)
    }

    @Test
    fun `android recent login code asks for a fresh sign in`() {
        assertTrue(classifyAuthCode("ERROR_REQUIRES_RECENT_LOGIN") is CloudException.RecentLoginRequired)
    }

    @Test
    fun `android network code is a connectivity failure`() {
        assertTrue(classifyAuthCode("ERROR_NETWORK_REQUEST_FAILED") is CloudException.Connectivity)
    }

    @Test
    fun `android codes for a dead session are unauthenticated`() {
        listOf(
            "ERROR_USER_TOKEN_EXPIRED",
            "ERROR_INVALID_USER_TOKEN",
            "ERROR_USER_NOT_FOUND",
            "ERROR_USER_DISABLED"
        ).forEach { code ->
            assertTrue(code, classifyAuthCode(code) is CloudException.Unauthenticated)
        }
    }

    // GitLive hands iOS callers the NSError code as a number in a string, not an "ERROR_*" name (spike T010).
    @Test
    fun `ios numeric codes are classified like their android names`() {
        assertTrue(classifyAuthCode("17012") is CloudException.CredentialCollision) // accountExistsWithDifferentCredential
        assertTrue(classifyAuthCode("17007") is CloudException.CredentialCollision) // emailAlreadyInUse
        assertTrue(classifyAuthCode("17025") is CloudException.CredentialAlreadyLinked) // credentialAlreadyInUse
        assertTrue(classifyAuthCode("17015") is CloudException.CredentialAlreadyLinked) // providerAlreadyLinked
        assertTrue(classifyAuthCode("17014") is CloudException.RecentLoginRequired) // requiresRecentLogin
        assertTrue(classifyAuthCode("17020") is CloudException.Connectivity) // networkError
        assertTrue(classifyAuthCode("17021") is CloudException.Unauthenticated) // userTokenExpired
        assertTrue(classifyAuthCode("17017") is CloudException.Unauthenticated) // invalidUserToken
        assertTrue(classifyAuthCode("17011") is CloudException.Unauthenticated) // userNotFound
        assertTrue(classifyAuthCode("17005") is CloudException.Unauthenticated) // userDisabled
    }

    @Test
    fun `an unknown or missing auth code is left for the caller to rethrow`() {
        assertNull(classifyAuthCode("ERROR_SOMETHING_NEW"))
        assertNull(classifyAuthCode("17999"))
        assertNull(classifyAuthCode(null))
    }
}
