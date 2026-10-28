package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
private const val SCOPES = "openid email profile"

// RFC 7636 asks for 43 to 128 characters of the unreserved set; 32 random bytes in unpadded base64url is exactly 43.
private const val VERIFIER_BYTES = 32
private const val STATE_BYTES = 16

private const val RETURN_PAGE =
    "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>QuestLog</title></head>" +
        "<body style=\"font-family: sans-serif; text-align: center; margin-top: 4em\">" +
        "<h2>You can return to QuestLog</h2><p>You can close this tab.</p></body></html>"

/**
 * Google's installed-app sign-in with PKCE and a loopback redirect (contracts/firestore-rest.md): the system browser
 * shows Google's account chooser, Google redirects to a one-shot server on `127.0.0.1`, and the code it carries is
 * traded for an ID token. Firebase takes that token in `accounts:signInWithIdp`.
 *
 * @param openBrowser Opens [AUTH_ENDPOINT]'s URL for the user; replaced in tests.
 * @param redirectTimeout How long the user has to finish in the browser before it counts as cancelled.
 */
internal class GoogleInstalledAppSignIn(
    private val httpClient: HttpClient,
    private val clientId: String,
    private val clientSecret: String,
    private val openBrowser: (url: String) -> Unit = ::openInSystemBrowser,
    private val redirectTimeout: Duration = 5.minutes
) : GoogleIdTokenSource {

    override suspend fun obtainIdToken(onAccountPicked: () -> Unit): String {
        if (clientId.isBlank()) throw CloudException.Unknown("The Google desktop client is not configured")

        val verifier = randomUrlSafe(VERIFIER_BYTES)
        val state = randomUrlSafe(STATE_BYTES)
        val redirect = CompletableDeferred<String?>()
        val server = startLoopbackServer(redirect)
        try {
            val redirectUri = "http://127.0.0.1:${server.address.port}"
            try {
                val url = authorizationUrl(clientId, redirectUri, codeChallenge(verifier), state)
                withContext(Dispatchers.IO) { openBrowser(url) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw CloudException.Unknown("The browser could not be opened", e)
            }

            val query = withTimeoutOrNull(redirectTimeout) { redirect.await() }
                ?: throw CloudException.SignInCancelled("No answer from the browser within $redirectTimeout")
            val code = codeFromRedirect(query, state)
            onAccountPicked()
            return exchangeCode(code, redirectUri, verifier)
        } finally {
            // Stopping waits for an answer still being written, so it must not run on a cancelled coroutine's thread.
            withContext(NonCancellable + Dispatchers.IO) { server.stop(1) }
        }
    }

    private fun startLoopbackServer(redirect: CompletableDeferred<String?>): HttpServer {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            val query = exchange.requestURI.rawQuery.orEmpty()
            // Browsers also ask for /favicon.ico and the like: only a request that carries an answer is the redirect.
            val isAnswer = query.split('&').any { it.startsWith("code=") || it.startsWith("error=") }
            if (isAnswer) {
                exchange.respond(200, RETURN_PAGE)
                redirect.complete(query)
            } else {
                exchange.respond(404, "")
            }
        }
        server.start()
        return server
    }

    private fun HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) }
        close()
    }

    private suspend fun exchangeCode(code: String, redirectUri: String, verifier: String): String =
        RestErrorMapper.call {
            val response = httpClient.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = parameters {
                    append("code", code)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("redirect_uri", redirectUri)
                    append("grant_type", "authorization_code")
                    append("code_verifier", verifier)
                }
            )
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) throw CloudException.Unknown("Google refused the code: ${response.status}")
            try {
                Json.parseToJsonElement(body).jsonObject.getValue("id_token").jsonPrimitive.content
            } catch (e: Exception) {
                throw CloudException.Unknown("Google returned no ID token", e)
            }
        }
}

/** The PKCE `code_challenge` for [verifier]: its SHA-256, base64url without padding. */
internal fun codeChallenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII)))

/** The page the browser opens: Google's account chooser, asking for an authorization code. */
internal fun authorizationUrl(clientId: String, redirectUri: String, challenge: String, state: String): String {
    val parameters = listOf(
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
        "response_type" to "code",
        "scope" to SCOPES,
        "code_challenge" to challenge,
        "code_challenge_method" to "S256",
        "state" to state,
        "prompt" to "select_account"
    )
    return AUTH_ENDPOINT + "?" + parameters.joinToString("&") { (name, value) ->
        "$name=${URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")}"
    }
}

/**
 * The authorization code in the redirect's [query]. A refusal in the browser is a cancellation, and a `state` that is
 * not the one that was sent means the answer is not for this sign-in.
 */
internal fun codeFromRedirect(query: String, expectedState: String): String {
    val parameters = query.split('&').filter { it.isNotEmpty() }.associate {
        val name = it.substringBefore('=')
        val value = it.substringAfter('=', "")
        URLDecoder.decode(name, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
    parameters["error"]?.let { error ->
        if (error == "access_denied") throw CloudException.SignInCancelled("The user refused the sign-in")
        throw CloudException.Unknown("Google answered $error")
    }
    if (parameters["state"] != expectedState) throw CloudException.Unknown("The sign-in answer does not match the request")
    return parameters["code"]?.takeIf { it.isNotEmpty() } ?: throw CloudException.Unknown("The sign-in answer has no code")
}

/** [byteCount] random bytes as unpadded base64url, which is only unreserved characters. */
internal fun randomUrlSafe(byteCount: Int): String {
    val bytes = ByteArray(byteCount).also { SecureRandom().nextBytes(it) }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun openInSystemBrowser(url: String) {
    val uri = URI(url)
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri)
        return
    }
    val os = System.getProperty("os.name").orEmpty()
    val command = when {
        os.startsWith("Mac", ignoreCase = true) -> listOf("open", url)
        os.startsWith("Windows", ignoreCase = true) -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
        else -> throw IOException("No way to open a browser on $os")
    }
    ProcessBuilder(command).start()
}
