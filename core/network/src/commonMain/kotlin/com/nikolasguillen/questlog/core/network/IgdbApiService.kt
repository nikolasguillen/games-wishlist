package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbPlatform
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
import com.nikolasguillen.questlog.core.network.model.IgdbReleaseDateEntry

/**
 * IGDB's endpoints. Every one takes the apicalypse query as plain text, built by the caller, and returns the
 * decoded rows; a failure is thrown, never wrapped in a result type.
 */
interface IgdbApiService {
    suspend fun searchGames(query: String): List<IgdbGame>

    suspend fun getGameDetail(query: String): List<IgdbGame>

    suspend fun getPopularityPrimitives(query: String): List<IgdbPopularityPrimitive>

    suspend fun getPlatforms(query: String): List<IgdbPlatform>

    suspend fun getReleaseDates(query: String): List<IgdbReleaseDateEntry>
}
