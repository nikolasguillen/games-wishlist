package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val IDENTITY_TOOLKIT = "https://identitytoolkit.googleapis.com/v1/accounts"
private const val SECURE_TOKEN = "https://securetoken.googleapis.com/v1/token"

/**
 * The Firebase Auth REST calls the desktop app makes (contracts/firestore-rest.md): signing in with a Google ID token,
 * refreshing the Firebase ID token, reading the account and deleting it. Every failure is a `CloudException`.
 *
 * @param apiKey The Firebase project's web API key. It identifies the project and is not a secret.
 */
internal class IdentityToolkitClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {

    /** Trades a Google [googleIdToken] for a Firebase session; the account is created on the first sign-in. */
    suspend fun signInWithIdp(googleIdToken: String): IdpSignInResult {
        val body = postJson(
            "$IDENTITY_TOOLKIT:signInWithIdp",
            buildJsonObject {
                put("requestUri", "http://localhost")
                put("postBody", "id_token=$googleIdToken&providerId=google.com")
                put("returnSecureToken", true)
                put("returnIdpCredential", true)
            }
        )
        // The email already belongs to an account that uses another method: Firebase wants the user to confirm first.
        if (body["needConfirmation"]?.jsonPrimitive?.boolean == true) {
            throw CloudException.CredentialCollision("needConfirmation")
        }
        return parse {
            IdpSignInResult(
                uid = body.string("localId"),
                email = body["email"]?.jsonPrimitive?.content,
                idToken = body.string("idToken"),
                refreshToken = body.string("refreshToken"),
                expiresInSeconds = body.string("expiresIn").toLong()
            )
        }
    }

    /** Gets a new ID token for [refreshToken]. */
    suspend fun refresh(refreshToken: String): TokenRefreshResult {
        val body = call {
            httpClient.submitForm(
                url = "$SECURE_TOKEN?key=$apiKey",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }
            ).jsonBody()
        }
        return parse {
            TokenRefreshResult(
                uid = body.string("user_id"),
                idToken = body.string("id_token"),
                refreshToken = body.string("refresh_token"),
                expiresInSeconds = body.string("expires_in").toLong()
            )
        }
    }

    /** Reads the email and the linked sign-in methods of the account [idToken] belongs to. */
    suspend fun lookup(idToken: String): AccountLookupResult {
        val body = postJson("$IDENTITY_TOOLKIT:lookup", buildJsonObject { put("idToken", idToken) })
        return parse {
            val user = body.getValue("users").jsonArray.first().jsonObject
            AccountLookupResult(
                email = user["email"]?.jsonPrimitive?.content,
                providerIds = user["providerUserInfo"]?.jsonArray.orEmpty()
                    .map { it.jsonObject.getValue("providerId").jsonPrimitive.content }
            )
        }
    }

    /** Deletes the account [idToken] belongs to. Firebase wants a recent sign-in for it. */
    suspend fun delete(idToken: String) {
        postJson("$IDENTITY_TOOLKIT:delete", buildJsonObject { put("idToken", idToken) })
    }

    private suspend fun postJson(url: String, body: JsonObject): JsonObject = call {
        httpClient.post("$url?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }.jsonBody()
    }

    private suspend fun HttpResponse.jsonBody(): JsonObject {
        val text = bodyAsText()
        if (!status.isSuccess()) throw RestErrorMapper.fromIdentityToolkitBody(text)
        return try {
            Json.parseToJsonElement(text) as? JsonObject ?: JsonObject(emptyMap())
        } catch (e: Exception) {
            throw CloudException.Unknown("Firebase answered with something that is not JSON", e)
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T = RestErrorMapper.call(block)

    private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content

    // A well-formed answer missing a field the call needs is Firebase's problem, not something the user can act on.
    private inline fun <T> parse(read: () -> T): T = try {
        read()
    } catch (e: CloudException) {
        throw e
    } catch (e: Exception) {
        throw CloudException.Unknown("Firebase answered with an unexpected shape", e)
    }
}
