package com.nikolasguillen.questlog.core.common.desktop.windows

private const val GAME_ID_PREFIX = "gameId="

/** The game a notification click asks for, from the toast's `launch` payload `gameId=<id>`. Positive ids only. */
internal fun parseGameId(arguments: String): Int? =
    arguments.takeIf { it.startsWith(GAME_ID_PREFIX) }
        ?.removePrefix(GAME_ID_PREFIX)
        ?.takeIf { text -> text.all { it in '0'..'9' } }
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
