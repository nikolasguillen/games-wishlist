package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkGame(
    val id: Int,
    val name: String,
    val released: String?,
    @Json(name = "background_image") val backgroundImage: String?,
    val rating: Double?,
    val metacritic: Int?,
    val platforms: List<NetworkPlatformEntry>?,
    val genres: List<NetworkGenre>?
)

@JsonClass(generateAdapter = true)
data class NetworkPlatformEntry(
    val platform: NetworkPlatform
)

@JsonClass(generateAdapter = true)
data class NetworkPlatform(
    val id: Int,
    val name: String,
    val slug: String
)

@JsonClass(generateAdapter = true)
data class NetworkGenre(
    val id: Int,
    val name: String,
    val slug: String
)

@JsonClass(generateAdapter = true)
data class NetworkGameDetail(
    val id: Int,
    val name: String,
    val description: String?,
    val released: String?,
    @Json(name = "background_image") val backgroundImage: String?,
    val rating: Double?,
    val metacritic: Int?,
    val platforms: List<NetworkPlatformEntry>?,
    val genres: List<NetworkGenre>?,
    val publishers: List<NetworkPublisher>?,
    val developers: List<NetworkDeveloper>?
)

@JsonClass(generateAdapter = true)
data class NetworkPublisher(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class NetworkDeveloper(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class NetworkResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)
