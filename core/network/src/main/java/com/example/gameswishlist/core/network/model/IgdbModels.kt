package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IgdbGame(
    val id: Int,
    val name: String,
    val summary: String?,
    @Json(name = "first_release_date") val firstReleaseDate: Long?,
    val cover: IgdbCover?,
    @Json(name = "total_rating") val totalRating: Double?,
    val platforms: List<IgdbPlatform>?,
    val genres: List<IgdbGenre>?,
    @Json(name = "involved_companies") val involvedCompanies: List<IgdbInvolvedCompany>?
)

@JsonClass(generateAdapter = true)
data class IgdbCover(
    val id: Int,
    val url: String?
)

@JsonClass(generateAdapter = true)
data class IgdbPlatform(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class IgdbGenre(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class IgdbInvolvedCompany(
    val id: Int,
    val company: IgdbCompany,
    val developer: Boolean,
    val publisher: Boolean
)

@JsonClass(generateAdapter = true)
data class IgdbCompany(
    val id: Int,
    val name: String
)
