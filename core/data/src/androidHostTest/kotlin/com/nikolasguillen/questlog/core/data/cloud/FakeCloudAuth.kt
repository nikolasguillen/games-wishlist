package com.nikolasguillen.questlog.core.data.cloud

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stands in for the Firebase-backed [CloudAuth]. [signInFailure], when set, is thrown by every [signIn]; [provider] is
 * the method the pretend platform offers. [deleteUserFailures] are thrown by the successive [deleteUser] calls, a `null`
 * letting that call succeed, and [calls] records the order of the other members.
 */
class FakeCloudAuth(
    initial: CloudUser? = null,
    override val provider: CloudProvider = CloudProvider.GOOGLE
) : CloudAuth {
    val user = MutableStateFlow(initial)
    var signInFailure: Throwable? = null
    var signInCalls = 0
    var accountPickedCalls = 0

    val calls = mutableListOf<String>()
    var reauthenticateFailure: Throwable? = null
    var signOutFailure: Throwable? = null
    val deleteUserFailures = ArrayDeque<Throwable?>()

    override val currentUser = user

    override suspend fun signIn(onAccountPicked: () -> Unit): CloudUser {
        signInCalls++
        signInFailure?.let { throw it }
        accountPickedCalls++
        onAccountPicked()
        return CloudUser(uid = "uid-1", email = "ada@example.com", providers = setOf(provider)).also { user.value = it }
    }

    override suspend fun reauthenticate() {
        calls += "reauthenticate"
        reauthenticateFailure?.let { throw it }
    }

    override suspend fun signOut() {
        calls += "signOut"
        signOutFailure?.let { throw it }
        user.value = null
    }

    override suspend fun deleteUser() {
        calls += "deleteUser"
        deleteUserFailures.removeFirstOrNull()?.let { throw it }
        user.value = null
    }
}
