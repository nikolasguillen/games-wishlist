package com.nikolasguillen.questlog.feature.wishlist.mapper

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.mapper.toLabelUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistFilterChipUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistStatusFilter
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.filter_all
import com.nikolasguillen.questlog.feature.wishlist.resources.no_status_section_label

/** Section order: active statuses first, finished/dropped last. */
private val STATUS_ORDER = listOf(
    GameStatus.PLAYING,
    GameStatus.BOUGHT,
    GameStatus.WANT_TO_BUY,
    GameStatus.COMPLETED,
    GameStatus.DROPPED
)

/** The "No status" section (and its chip) goes after every real status. */
private fun GameStatus?.sectionPosition(): Int =
    this?.let(STATUS_ORDER::indexOf) ?: STATUS_ORDER.size

/** The label of a status section, and of its filter chip. `null` is the "No status" section. */
internal fun GameStatus?.toSectionLabel(): UiText =
    this?.toLabelUiText() ?: UiText.StringResource(Res.string.no_status_section_label)

internal fun List<Game>.toWishlistSectionUiModel(): List<WishlistSectionUiModel> {
    val games = this.toGameItemList()
    val byStatus = games.groupBy { it.status }

    val statusSections = STATUS_ORDER.mapNotNull { status ->
        val filteredGames = byStatus[status]
        if (filteredGames.isNullOrEmpty()) return@mapNotNull null
        WishlistSectionUiModel(
            status = status,
            label = status.toSectionLabel(),
            games = filteredGames
        )
    }
    val unstatusedGames = byStatus[null]
    val unstatusedSection = if (unstatusedGames.isNullOrEmpty()) {
        null
    } else {
        WishlistSectionUiModel(
            status = null,
            label = null.toSectionLabel(),
            games = unstatusedGames
        )
    }
    return statusSections + listOfNotNull(unstatusedSection)
}

/** The sections [filter] keeps, compared by status, never by label. */
internal fun List<WishlistSectionUiModel>.filteredBy(filter: WishlistStatusFilter): List<WishlistSectionUiModel> =
    when (filter) {
        WishlistStatusFilter.All -> this
        is WishlistStatusFilter.Only -> filter { it.status == filter.status }
    }

/**
 * The chip strip for these (unfiltered) sections: "All" plus one chip per status, in section order.
 *
 * Empty when there is nothing to choose between: no games at all (the whole options row is hidden then,
 * whatever the filter was), or a single section while no filter is active. While a filter is active its chip
 * is kept even if its status has run out of games, so the user can see why nothing is listed.
 */
internal fun List<WishlistSectionUiModel>.toFilterChips(
    filter: WishlistStatusFilter
): List<WishlistFilterChipUiModel> {
    if (isEmpty()) return emptyList()

    val statuses = map { it.status }.toMutableSet()
    if (filter is WishlistStatusFilter.Only) statuses += filter.status

    val allChip = WishlistFilterChipUiModel(
        filter = WishlistStatusFilter.All,
        label = UiText.StringResource(Res.string.filter_all),
        isSelected = filter == WishlistStatusFilter.All
    )
    val statusChips = if (statuses.size < 2) {
        emptyList()
    } else {
        statuses.sortedBy { it.sectionPosition() }.map { status ->
            val statusFilter = WishlistStatusFilter.Only(status)
            WishlistFilterChipUiModel(
                filter = statusFilter,
                label = status.toSectionLabel(),
                isSelected = filter == statusFilter
            )
        }
    }
    return listOf(allChip) + statusChips
}
