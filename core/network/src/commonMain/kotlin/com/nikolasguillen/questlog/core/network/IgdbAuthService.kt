package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbAuthResponse

/** Twitch's client-credentials endpoint, which issues the token IGDB expects. */
internal interface IgdbAuthService {
    suspend fun getAccessToken(
        clientId: String,
        clientSecret: String,
        grantType: String = "client_credentials"
    ): IgdbAuthResponse
}
