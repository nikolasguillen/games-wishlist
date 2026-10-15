package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AuthProvider

internal fun CloudUser.toAccount() = Account(
    id = uid,
    email = email,
    providers = providers.mapTo(mutableSetOf()) { it.toAuthProvider() }
)

internal fun CloudProvider.toAuthProvider(): AuthProvider = when (this) {
    CloudProvider.GOOGLE -> AuthProvider.GOOGLE
    CloudProvider.APPLE -> AuthProvider.APPLE
}
