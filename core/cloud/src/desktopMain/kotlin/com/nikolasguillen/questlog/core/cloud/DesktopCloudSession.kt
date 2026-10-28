package com.nikolasguillen.questlog.core.cloud

import kotlinx.serialization.Serializable

/**
 * What stays on the machine between runs of a signed-in desktop app (research R5). The ID token is not here: it lives
 * for an hour and is fetched again from [refreshToken] when it runs short.
 *
 * @property providers the sign-in methods linked to the account, as Firebase names them (`google.com`).
 * @property refreshToken a secret: whoever has it is signed in. It is never logged.
 */
@Serializable
internal data class DesktopCloudSession(
    val uid: String,
    val email: String?,
    val providers: List<String>,
    val refreshToken: String
) {
    // Kept out of logs and exception messages by hand: a data class would print every property.
    override fun toString(): String = "DesktopCloudSession(uid=$uid)"
}
