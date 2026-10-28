package com.nikolasguillen.questlog.desktop

/**
 * What a second launch tells the running instance before it exits: one UTF-8 line over the instance socket
 * (specs/020-desktop-app, data-model §6). Anything that is not one of these lines is ignored, because the socket is
 * reachable by every process of the same user.
 */
internal sealed interface SingleInstanceMessage {

    /** The line a sender writes, without its line break. */
    val line: String

    /** Bring the window forward. */
    data object Focus : SingleInstanceMessage {
        override val line: String = FOCUS
    }

    /** Bring the window forward and open the game with [gameId], a positive integer. */
    data class OpenGame(val gameId: Int) : SingleInstanceMessage {
        init {
            require(gameId > 0) { "A game id is a positive integer" }
        }

        override val line: String = "$OPEN_GAME $gameId"
    }

    companion object {
        private const val FOCUS = "FOCUS"
        private const val OPEN_GAME = "OPEN_GAME"

        /** The message [line] carries, or `null` for anything else. */
        fun parse(line: String): SingleInstanceMessage? {
            if (line == FOCUS) return Focus
            if (!line.startsWith("$OPEN_GAME ")) return null
            val digits = line.removePrefix("$OPEN_GAME ")
            // Only plain digits: `toIntOrNull` would also take a sign.
            if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
            val gameId = digits.toIntOrNull()?.takeIf { it > 0 } ?: return null
            return OpenGame(gameId)
        }
    }
}
