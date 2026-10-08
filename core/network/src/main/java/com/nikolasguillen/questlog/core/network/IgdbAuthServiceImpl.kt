package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbAuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post

/** Runs on a client of its own, with no auth step: it is the call that fetches the token that step needs. */
internal class IgdbAuthServiceImpl(private val client: HttpClient) : IgdbAuthService {

    override suspend fun getAccessToken(
        clientId: String,
        clientSecret: String,
        grantType: String
    ): IgdbAuthResponse = client.post(TWITCH_TOKEN_URL) {
        parameter("client_id", clientId)
        parameter("client_secret", clientSecret)
        parameter("grant_type", grantType)
    }.body()
}
