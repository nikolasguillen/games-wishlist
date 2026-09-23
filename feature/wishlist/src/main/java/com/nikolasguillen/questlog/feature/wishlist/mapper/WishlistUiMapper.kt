package com.nikolasguillen.questlog.feature.wishlist.mapper

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.mapper.toLabelUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel

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
    val unstatusedGames = byStatus[null]
    val unstatusedSection = if (unstatusedGames.isNullOrEmpty()) {
        null
    } else {
        WishlistSectionUiModel(
            status = null,
            label = UiText.StringResource(R.string.no_status_section_label),
            games = unstatusedGames
        )
    }
    return statusSections + listOfNotNull(unstatusedSection)
}