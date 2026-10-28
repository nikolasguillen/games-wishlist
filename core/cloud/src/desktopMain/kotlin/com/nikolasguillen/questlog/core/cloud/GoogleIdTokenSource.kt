package com.nikolasguillen.questlog.core.cloud

/** Where a Google ID token comes from: the user choosing an account in the system browser. */
internal fun interface GoogleIdTokenSource {

    /**
     * Opens Google's sign-in and returns the ID token it ends with. [onAccountPicked] runs once the user has chosen an
     * account, before the token is fetched. Throws only `CloudException`, and `CancellationException`.
     */
    suspend fun obtainIdToken(onAccountPicked: () -> Unit): String
}
