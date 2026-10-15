package com.nikolasguillen.questlog.core.data.cloud

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for the Firebase-backed [CloudAuth]. [signInFailure], when set, is thrown by every [signIn]; [provider] is
 * the method the pretend platform offers.
 */
class FakeCloudAuth(
    initial: CloudUser? = null,
    override val provider: CloudProvider = CloudProvider.GOOGLE
) : CloudAuth {
    val user = MutableStateFlow(initial)
    var signInFailure: Throwable? = null
    var signInCalls = 0

    override val currentUser = user

    override suspend fun signIn(): CloudUser {
        signInCalls++
        signInFailure?.let { throw it }
        return CloudUser(uid = "uid-1", email = "ada@example.com", providers = setOf(provider)).also { user.value = it }
    }

    override suspend fun reauthenticate() = error("not used by these tests")

    override suspend fun signOut() = error("not used by these tests")

    override suspend fun deleteUser() = error("not used by these tests")
}
