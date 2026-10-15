package com.nikolasguillen.questlog.core.data.cloud

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import kotlinx.coroutines.flow.MutableStateFlow

/** Stands in for the Firebase-backed [CloudAuth]. [signInFailure], when set, is thrown by every [signIn]. */
class FakeCloudAuth(initial: CloudUser? = null) : CloudAuth {
    val user = MutableStateFlow(initial)
    var signInFailure: Throwable? = null
    val signInCalls = mutableListOf<CloudProvider>()

    override val currentUser = user

    override suspend fun signIn(provider: CloudProvider): CloudUser {
        signInCalls += provider
        signInFailure?.let { throw it }
        return CloudUser(uid = "uid-1", email = "ada@example.com", providers = setOf(provider)).also { user.value = it }
    }

    override suspend fun link(provider: CloudProvider): CloudUser = error("not used by these tests")

    override suspend fun reauthenticate() = error("not used by these tests")

    override suspend fun signOut() = error("not used by these tests")

    override suspend fun deleteUser() = error("not used by these tests")
}
