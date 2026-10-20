package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.model.Game

/**
 * Newest release first, with the games that have no release date last. Games with the same date keep the order
 * they came in. [Game.releaseDate] is an ISO `yyyy-MM-dd` string, so comparing the strings compares the dates.
 */
internal fun List<Game>.newestFirst(): List<Game> = sortedWith(
    compareBy<Game> { it.releaseDate == null }.thenByDescending { it.releaseDate.orEmpty() }
)
