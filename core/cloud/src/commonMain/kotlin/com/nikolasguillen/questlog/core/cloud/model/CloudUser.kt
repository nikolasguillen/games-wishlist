package com.nikolasguillen.questlog.core.cloud.model

/**
 * The signed-in cloud identity.
 *
 * @property uid the backend's user id.
 * @property email null when no linked method shares one.
 * @property providers the linked sign-in methods.
 */
data class CloudUser(
    val uid: String,
    val email: String?,
    val providers: Set<CloudProvider>
)
