package com.example.gameswishlist.feature.wishlist.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.mapper.toLabelUiText
import com.example.gameswishlist.feature.wishlist.model.WishlistSectionUiModel

/** Section order: active statuses first, finished/dropped last. */
private val STATUS_ORDER = listOf(
    GameStatus.PLAYING,
    GameStatus.BOUGHT,
    GameStatus.WANT_TO_BUY,
    GameStatus.COMPLETED,
    GameStatus.DROPPED
)

internal fun List<Game>.toWishlistSectionUiModel(): List<WishlistSectionUiModel> {
    val games = this.toGameItemList()
    val byStatus = games.groupBy { it.status }

    val statusSections = STATUS_ORDER.mapNotNull { status ->
        val filteredGames = byStatus[status]
        if (filteredGames.isNullOrEmpty()) return@mapNotNull null
        WishlistSectionUiModel(status = status, label = status.toLabelUiText(), games = filteredGames)
    }
    // Games without a personal status yet are shown without a section header.
    val unstatusedGames = byStatus[null]
    val unstatusedSection = if (unstatusedGames.isNullOrEmpty()) {
        null
    } else {
        WishlistSectionUiModel(status = null, label = null, games = unstatusedGames)
    }
    return statusSections + listOfNotNull(unstatusedSection)
}