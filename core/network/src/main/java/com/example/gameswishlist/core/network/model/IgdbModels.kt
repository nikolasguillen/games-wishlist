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
 * @property genres List of genres the game belongs to (e.g., RPG, Shooter).
 * @property involvedCompanies List of companies involved in the game, categorized by their role (Developer/Publisher).
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
    val genres: List<IgdbGenre>?,
    @Json(name = "involved_companies") val involvedCompanies: List<IgdbInvolvedCompany>?,
    @Json(name = "parent_game") val parentGame: IgdbGame? = null,
    @Json(name = "dlcs") val dlcList: List<IgdbGame>? = null,
    val expansions: List<IgdbGame>? = null,
    val remakes: List<IgdbGame>? = null,
    val remasters: List<IgdbGame>? = null,
    val artworks: List<IgdbArtwork>? = null,
    val screenshots: List<IgdbArtwork>? = null
)

/**
 * Represents a game's artwork or screenshot image reference.
 *
 * @property id Internal IGDB unique identifier.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@JsonClass(generateAdapter = true)
data class IgdbArtwork(
    val id: Int,
    val url: String?
)

/**
 * Represents a game's cover image reference.
 *
 * @property id Internal IGDB unique identifier for the cover.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@JsonClass(generateAdapter = true)
data class IgdbCover(
    val id: Int,
    val url: String?
)

/**
 * Represents a gaming platform.
 *
 * @property id Internal IGDB unique identifier for the platform.
 * @property abbreviation Short form of the platform name (e.g., "PS5", "PC").
 * @property name Full official name of the platform (e.g., "PlayStation 5").
 * @property generation The numerical generation of the platform (e.g., 9 for PS5).
 * @property category Numerical category of the platform (1: Console, 2: Arcade, etc).
 * @property platformFamily Numerical ID of the platform family (1: PlayStation, 2: Xbox, 5: Nintendo).
 */
@JsonClass(generateAdapter = true)
data class IgdbPlatform(
    val id: Int,
    val abbreviation: String?,
    val name: String,
    val generation: Int?,
    val category: Int?,
    @Json(name = "platform_family") val platformFamily: Int?
)

/**
 * Represents a game genre.
 *
 * @property id Internal IGDB unique identifier for the genre.
 * @property name The name of the genre (e.g., "Adventure", "Strategy").
 */
@JsonClass(generateAdapter = true)
data class IgdbGenre(
    val id: Int,
    val name: String
)

/**
 * Junction model for companies involved in a game's production.
 *
 * @property id Internal IGDB unique identifier for the involved company record.
 * @property company The specific company details.
 * @property developer True if the company acted as a developer for this game.
 * @property publisher True if the company acted as a publisher for this game.
 */
@JsonClass(generateAdapter = true)
data class IgdbInvolvedCompany(
    val id: Int,
    val company: IgdbCompany,
    val developer: Boolean,
    val publisher: Boolean
)

/**
 * Represents a company in the gaming industry.
 *
 * @property id Internal IGDB unique identifier for the company.
 * @property name The official name of the company.
 */
@JsonClass(generateAdapter = true)
data class IgdbCompany(
    val id: Int,
    val name: String
)
