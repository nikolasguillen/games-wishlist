package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbPlatform
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
import com.nikolasguillen.questlog.core.network.model.IgdbReleaseDateEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.network.sockets.SocketTimeoutException as KtorSocketTimeoutException

internal class IgdbApiServiceImpl(private val client: HttpClient) : IgdbApiService {

    override suspend fun searchGames(query: String): List<IgdbGame> = post("games", query)

    override suspend fun getGameDetail(query: String): List<IgdbGame> = post("games", query)

    override suspend fun getPopularityPrimitives(query: String): List<IgdbPopularityPrimitive> =
        post("popularity_primitives", query)

    override suspend fun getPlatforms(query: String): List<IgdbPlatform> = post("platforms", query)

    override suspend fun getReleaseDates(query: String): List<IgdbReleaseDateEntry> = post("release_dates", query)

    // The client's own failure types stop here: callers recognise IgdbTimeoutException and IgdbConnectivityException
    // instead, so no layer above this module needs to know which HTTP client is underneath.
    private suspend inline fun <reified T> post(endpoint: String, query: String): List<T> = try {
        client.post("$IGDB_API_BASE_URL$endpoint") {
            contentType(ContentType.Text.Plain)
            setBody(query)
        }.body()
    } catch (e: HttpRequestTimeoutException) {
        throw IgdbTimeoutException(e)
    } catch (e: ConnectTimeoutException) {
        throw IgdbTimeoutException(e)
    } catch (e: KtorSocketTimeoutException) {
        throw IgdbTimeoutException(e)
    } catch (e: Exception) {
        // Whatever this platform's engine reports in its own terms. Anything else, cancellation included, goes
        // on unchanged.
        throw e.toPlatformTransportFailure() ?: e
    }
}
