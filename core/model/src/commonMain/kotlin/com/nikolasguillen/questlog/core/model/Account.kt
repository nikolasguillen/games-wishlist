package com.nikolasguillen.questlog.core.model

/**
 * The signed-in identity the cloud backup belongs to.
 *
 * @property id the backend's user id; never shown to the user.
 * @property email null when no linked method shares one, which is rare.
 * @property providers the linked sign-in methods; never empty while signed in.
 */
data class Account(
    val id: String,
    val email: String?,
    val providers: Set<AuthProvider>
)
