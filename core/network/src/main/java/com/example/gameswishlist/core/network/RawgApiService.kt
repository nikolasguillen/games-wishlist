package com.example.gameswishlist.core.network

import com.example.gameswishlist.core.network.model.NetworkGame
import com.example.gameswishlist.core.network.model.NetworkGameDetail
import com.example.gameswishlist.core.network.model.NetworkResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {
    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): NetworkResponse<NetworkGame>

    @GET("games/{id}")
    suspend fun getGameDetail(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): NetworkGameDetail
}
