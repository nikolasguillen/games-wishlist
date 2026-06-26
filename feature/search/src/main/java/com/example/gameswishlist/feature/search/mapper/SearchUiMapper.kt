package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.mapper.getShortLabel
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.UiConstants
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SortingUiModel
import com.example.gameswishlist.core.ui.R as UiR

fun List<Platform>.toPlatformFilters(): List<GameFilterUiModel> {
    return this
        .sortedWith(
            compareByDescending<Platform> { platform ->
                val generation = platform.generation ?: 0
                val isRecent = generation >= UiConstants.RECENT_GENERATION_THRESHOLD
                val isMajorFamily =
                    platform.platformFamily == UiConstants.PLATFORM_FAMILY_PLAYSTATION ||
                            platform.platformFamily == UiConstants.PLATFORM_FAMILY_XBOX ||
                            platform.platformFamily == UiConstants.PLATFORM_FAMILY_NINTENDO

                val isPc = platform.category == UiConstants.PLATFORM_CATEGORY_OPERATING_SYSTEM ||
                        platform.name.contains(Regex("\\b(pc)\\b", RegexOption.IGNORE_CASE))

                when {
                    // Tier 1: Modern major consoles (Last 2 generations: Gen 8 & 9)
                    isMajorFamily && isRecent -> {
                        when (platform.platformFamily) {
                            UiConstants.PLATFORM_FAMILY_PLAYSTATION -> 100
                            UiConstants.PLATFORM_FAMILY_XBOX -> 95
                            else -> 90 // Nintendo
                        }
                    }
                    // Tier 2: PC / Operating Systems
                    isPc -> 80
                    // Tier 3: Other major consoles (Vintage/Retro: Gen 7 and older)
                    isMajorFamily -> 70
                    // Tier 4: Everything else
                    else -> 0
                }
            }.thenByDescending { platform ->
                // Secondary sort: Still prefer Consoles to other categories within tiers
                if (platform.category == UiConstants.PLATFORM_CATEGORY_CONSOLE) 10 else 0
            }.thenByDescending {
                // Tertiary sort: Most recent generation first
                it.generation ?: 0
            }
        )
        .map { platform ->
            GameFilterUiModel.Platform(
                id = platform.id,
                label = UiText.DynamicString(platform.getShortLabel()),
                selected = false
            )
        }
}

fun List<Genre>.toGenreFilters(): List<GameFilterUiModel> {
    return this.map { genre ->
        GameFilterUiModel.Genre(
            id = genre.id,
            label = UiText.DynamicString(genre.name),
            selected = false
        )
    }
}

fun getInitialGameTypeFilters(): List<GameFilterUiModel> {
    return listOf(
        GameType.MAIN_GAME,
        GameType.REMAKE,
        GameType.REMASTER,
        GameType.EXPANSION,
        GameType.DLC_ADDON,
        GameType.STANDALONE_EXPANSION
    ).map { type ->
        GameFilterUiModel.GameType(
            id = type.id,
            label = type.toUiText(),
            selected = false
        )
    }
}

fun getInitialSortFilters(): List<SortingUiModel> {
    return SearchSort.entries.map { sort ->
        val labelResId = when (sort) {
            SearchSort.RELEVANCE -> UiR.string.sort_relevance
            SearchSort.NAME -> UiR.string.sort_name
            SearchSort.RATING -> UiR.string.sort_rating
            SearchSort.RELEASE_DATE -> UiR.string.sort_release_date
        }
        SortingUiModel(
            sortType = sort,
            label = UiText.StringResource(labelResId),
            selected = sort == SearchSort.RELEVANCE,
            descending = true
        )
    }
}

/**
 * Business logic to determine if the current sorting is different from the default.
 * Default is [SearchSort.RELEVANCE] with [SortingUiModel.descending] = true.
 */
fun List<SortingUiModel>.isSortActive(): Boolean {
    val selected = this.find { it.selected } ?: return false
    return selected.sortType != SearchSort.RELEVANCE || !selected.descending
}
