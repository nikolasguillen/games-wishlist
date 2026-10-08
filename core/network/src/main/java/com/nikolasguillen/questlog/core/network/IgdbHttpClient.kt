package com.nikolasguillen.questlog.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

internal const val IGDB_API_BASE_URL = "https://api.igdb.com/v4/"
internal const val TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token"

private const val CLIENT_ID_HEADER = "Client-ID"

/** The client that calls IGDB: JSON in and out, the credentials on every request, and non-2xx turned into an exception. */
internal fun createIgdbHttpClient(
    engine: HttpClientEngine,
    authManager: IgdbAuthManager,
    clientId: String,
    logBodies: Boolean
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(IgdbJson) }
    install(IgdbAuth) {
        this.authManager = authManager
        this.clientId = clientId
    }
    // Registered after the auth step so the dump shows what was really sent. The headers that carry the
    // credentials are masked, which the old interceptor-based logging could not do. On release builds the
    // plugin stays installed but silent.
    install(Logging) {
        level = if (logBodies) LogLevel.BODY else LogLevel.NONE
        sanitizeHeader { header -> header == HttpHeaders.Authorization || header == CLIENT_ID_HEADER }
    }
    throwIgdbHttpExceptionOnError()
}

/** The client for Twitch's token endpoint: the same JSON handling, but no credentials step of its own. */
internal fun createAuthHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(IgdbJson) }
    throwIgdbHttpExceptionOnError()
}

/**
 * Turns every non-2xx response into an [IgdbHttpException].
 *
 * Translating here, rather than letting the client throw its own `ResponseException`, is what keeps `:core:data`
 * free of an HTTP client dependency while still being able to tell a 404 from a 429. It is a translation, not
 * error handling: nothing is retried, logged or recovered, and the failure still lands in `:core:data`.
 *
 * The error body is not read into the message.
 */
private fun io.ktor.client.HttpClientConfig<*>.throwIgdbHttpExceptionOnError() {
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                throw IgdbHttpException(
                    code = response.status.value,
                    message = "HTTP ${response.status.value} ${response.status.description}"
                )
            }
        }
    }
}

internal class IgdbAuthConfig {
    lateinit var authManager: IgdbAuthManager
    var clientId: String = ""
}

/**
 * Adds the `Client-ID` and, when a token could be had, the bearer token to every request.
 *
 * A request still goes out without the token when fetching one failed: it comes back as a 401, which
 * `:core:data` - the error boundary - already maps to a typed error. See [IgdbAuthManager.getAccessToken].
 */
private val IgdbAuth: HttpClientPlugin<IgdbAuthConfig, *> = createClientPlugin("IgdbAuth", ::IgdbAuthConfig) {
    val authManager = pluginConfig.authManager
    val clientId = pluginConfig.clientId
    onRequest { request, _ ->
        request.header(CLIENT_ID_HEADER, clientId)
        authManager.getAccessToken()?.let { request.header(HttpHeaders.Authorization, "Bearer $it") }
    }
}
