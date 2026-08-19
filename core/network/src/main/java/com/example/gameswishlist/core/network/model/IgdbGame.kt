package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Main game model returned by the IGDB API.
 *
 * This class represents a video game with its associated metadata.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The official name of the game.
 * @property summary A brief overview or description of the game's plot and mechanics.
 * @property firstReleaseDate Unix timestamp (seconds) of the game's first official release date.
 * @property cover Reference to the game's main cover image.
 * @property gameType The numerical ID representing the game's type (e.g., 0 for Main Game).
 * @property totalRating Average rating based on both external critics and IGDB users (0-100).
 * @property totalRatingCount Total number of ratings across all platforms. Used as a proxy for popularity.
 * @property aggregatedRating Average rating based on both external critics and IGDB users (0-10).
 * @property hypes Number of users who added this to their "want to play" list. Useful for identifying trending games.
 * @property url The official IGDB page URL for the game.
 * @property platforms List of platforms the game is available on (e.g., PS5, PC, Xbox).
 * @property releaseDates Detailed release dates for each platform.
 * @property genres List of genres the game belongs to (e.g., RPG, Shooter).
 * @property involvedCompanies List of companies involved in the game, categorized by their role (Developer/Publisher).
 * @property gameEngines List of game engines used to develop the game (e.g., Unreal Engine, Unity).
 * @property parentGame The main game if this is a DLC, Remake, etc.
 * @property dlcList List of DLCs for this game.
 * @property expansions List of expansions for this game.
 * @property remakes List of remakes of this game.
 * @property remasters List of remasters of this game.
 */
@JsonClass(generateAdapter = true)
data class IgdbGame(
    val id: Int,
    val name: String,
    val summary: String?,
    @Json(name = "game_type") val gameType: Int?,
    @Json(name = "first_release_date") val firstReleaseDate: Long?,
    val cover: IgdbCover?,
    @Json(name = "total_rating") val totalRating: Double?,
    @Json(name = "total_rating_count") val totalRatingCount: Int?,
    @Json(name = "aggregated_rating") val aggregatedRating: Double?,
    val hypes: Int?,
    val url: String?,
    val platforms: List<IgdbPlatform>?,
    @Json(name = "release_dates") val releaseDates: List<IgdbReleaseDate>?,
    val genres: List<IgdbGenre>?,
    @Json(name = "involved_companies") val involvedCompanies: List<IgdbInvolvedCompany>?,
    @Json(name = "game_engines") val gameEngines: List<IgdbGameEngine>?,
    @Json(name = "parent_game") val parentGame: IgdbGame? = null,
    @Json(name = "dlcs") val dlcList: List<IgdbGame>? = null,
    val expansions: List<IgdbGame>? = null,
    val remakes: List<IgdbGame>? = null,
    val remasters: List<IgdbGame>? = null,
    val artworks: List<IgdbArtwork>? = null,
    val screenshots: List<IgdbArtwork>? = null
)
