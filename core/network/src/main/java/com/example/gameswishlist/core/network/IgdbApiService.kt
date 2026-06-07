package com.example.gameswishlist.core.network

import com.example.gameswishlist.core.network.model.IgdbGame
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface IgdbApiService {
    @POST("games")
    suspend fun searchGames(
        @Body body: RequestBody
    ): List<IgdbGame>

    @POST("games")
    suspend fun getGameDetail(
        @Body body: RequestBody
    ): List<IgdbGame>
}
