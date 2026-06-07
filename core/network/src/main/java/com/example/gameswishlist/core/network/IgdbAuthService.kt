package com.example.gameswishlist.core.network

import com.example.gameswishlist.core.network.model.IgdbAuthResponse
import retrofit2.http.POST
import retrofit2.http.Query

interface IgdbAuthService {
    @POST("https://id.twitch.tv/oauth2/token")
    suspend fun getAccessToken(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String = "client_credentials"
    ): IgdbAuthResponse
}
