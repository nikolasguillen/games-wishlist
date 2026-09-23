package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbPlatform
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
import com.nikolasguillen.questlog.core.network.model.IgdbReleaseDateEntry
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

    @POST("popularity_primitives")
    suspend fun getPopularityPrimitives(
        @Body body: RequestBody
    ): List<IgdbPopularityPrimitive>

    @POST("platforms")
    suspend fun getPlatforms(
        @Body body: RequestBody
    ): List<IgdbPlatform>

    @POST("release_dates")
    suspend fun getReleaseDates(
        @Body body: RequestBody
    ): List<IgdbReleaseDateEntry>
}
