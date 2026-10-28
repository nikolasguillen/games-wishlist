package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Maps what the Firebase REST APIs answer to this module's own [CloudException] (contracts/firestore-rest.md), the way
 * `FirebaseErrorClassifier` does for the mobile SDKs.
 */
internal object RestErrorMapper {

    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403

    /**
     * The failure for an Identity Toolkit error [message], such as `INVALID_ID_TOKEN` or
     * `TOKEN_EXPIRED : the token is stale`. Only the code before ` : ` counts.
     */
    fun fromIdentityToolkitMessage(message: String?): CloudException {
        val code = message?.substringBefore(" : ")?.trim().orEmpty()
        return when (code) {
            "FEDERATED_USER_ID_ALREADY_LINKED" -> CloudException.CredentialCollision(code)
            "CREDENTIAL_TOO_OLD_LOGIN_AGAIN" -> CloudException.RecentLoginRequired(code)
            "TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN", "INVALID_ID_TOKEN", "USER_DISABLED", "USER_NOT_FOUND" ->
                CloudException.Unauthenticated(code)

            else -> CloudException.Unknown(code.ifEmpty { "Identity Toolkit failed" })
        }
    }

    /** The failure for a non-2xx Identity Toolkit answer whose [body] is `{"error": {"message": …}}`. */
    fun fromIdentityToolkitBody(body: String): CloudException = fromIdentityToolkitMessage(
        try {
            (Json.parseToJsonElement(body) as? JsonObject)
                ?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.content
        } catch (_: Exception) {
            // Not JSON, or not shaped like an error: nothing to name.
            null
        }
    )

    /** The failure for a non-2xx Firestore answer. */
    fun fromFirestoreStatus(status: Int): CloudException = when (status) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> CloudException.Unauthenticated("Firestore answered $status")
        else -> CloudException.Unknown("Firestore answered $status")
    }

    /**
     * Runs one REST call within [CLOUD_CALL_TIMEOUT] and lets only [CloudException] and `CancellationException` out.
     * Cancellation is always rethrown as it is; the call's own timeout is told apart from it, because it is a
     * `CancellationException` too.
     */
    suspend fun <T> call(block: suspend () -> T): T = try {
        withTimeout(CLOUD_CALL_TIMEOUT) { block() }
    } catch (e: TimeoutCancellationException) {
        throw CloudException.Timeout("A call took longer than $CLOUD_CALL_TIMEOUT", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: CloudException) {
        throw e
    } catch (e: SocketTimeoutException) {
        throw CloudException.Timeout(cause = e)
    } catch (e: IOException) {
        // UnknownHostException, ConnectException, SocketException and the HTTP client's own connect failures.
        throw CloudException.Connectivity(cause = e)
    } catch (e: Exception) {
        throw CloudException.Unknown(cause = e)
    }
}
