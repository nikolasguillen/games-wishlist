package com.example.gameswishlist.core.model

/**
 * Represents the type of game as defined by IGDB.
 *
 * @property id The raw integer ID from the IGDB API.
 */
enum class GameType(val id: Int) {
    MAIN_GAME(0),
    DLC_ADDON(1),
    EXPANSION(2),
    BUNDLE(3),
    STANDALONE_EXPANSION(4),
    MOD(5),
    EPISODE(6),
    SEASON(7),
    REMAKE(8),
    REMASTER(9),
    EXPANDED_GAME(10),
    PORT(11),
    FORK(12),
    PACK(13),
    UPDATE(14);

    companion object {
        /**
         * Returns the [GameType] corresponding to the given IGDB ID.
         * Defaults to [MAIN_GAME] if the ID is null or unknown.
         */
        fun fromId(id: Int?): GameType = entries.find { it.id == id } ?: MAIN_GAME

        /**
         * Types that are typically excluded from search results to reduce noise.
         */
        val noisyTypes =
            listOf(BUNDLE, FORK, PACK, UPDATE, MOD, EPISODE, SEASON)
    }
}
