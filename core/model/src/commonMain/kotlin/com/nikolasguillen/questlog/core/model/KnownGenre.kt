package com.nikolasguillen.questlog.core.model

/**
 * The IGDB genres the app has a label for. It is the app's table of known genres: a [Genre] whose id is
 * not listed here is still shown, under IGDB's own name.
 *
 * Code compares [Genre.id], never a name; the entry only exists so the UI can resolve a short, localisable
 * label from the id.
 *
 * @property id The IGDB genre id.
 * @property igdbName IGDB's own name for the genre. It is data, used as the [Genre.name] of a default genre;
 * the UI never displays it for a known genre.
 */
enum class KnownGenre(val id: Int, val igdbName: String) {
    POINT_AND_CLICK(2, "Point-and-click"),
    FIGHTING(4, "Fighting"),
    SHOOTER(5, "Shooter"),
    MUSIC(7, "Music"),
    PLATFORM(8, "Platform"),
    PUZZLE(9, "Puzzle"),
    RACING(10, "Racing"),
    REAL_TIME_STRATEGY(11, "Real Time Strategy (RTS)"),
    ROLE_PLAYING(12, "Role-playing (RPG)"),
    SIMULATOR(13, "Simulator"),
    SPORT(14, "Sport"),
    STRATEGY(15, "Strategy"),
    TURN_BASED_STRATEGY(16, "Turn-based strategy (TBS)"),
    TACTICAL(24, "Tactical"),
    HACK_AND_SLASH(25, "Hack and slash/Beat 'em up"),
    QUIZ_TRIVIA(26, "Quiz/Trivia"),
    PINBALL(30, "Pinball"),
    ADVENTURE(31, "Adventure"),
    INDIE(32, "Indie"),
    ARCADE(33, "Arcade"),
    VISUAL_NOVEL(34, "Visual Novel"),
    CARD_AND_BOARD_GAME(35, "Card & Board Game"),
    MOBA(36, "MOBA");

    companion object {
        /**
         * Returns the [KnownGenre] with the given IGDB [id], or `null` when the app has no entry for it.
         * There is no fallback entry: the caller decides what an unknown genre becomes.
         */
        fun fromId(id: Int): KnownGenre? = entries.find { it.id == id }

        /**
         * The genres that fill the browse row when the taste profile cannot, in the order they appear.
         */
        val BROWSE_DEFAULTS: List<KnownGenre> =
            listOf(ROLE_PLAYING, SHOOTER, ADVENTURE, STRATEGY, INDIE)
    }
}
