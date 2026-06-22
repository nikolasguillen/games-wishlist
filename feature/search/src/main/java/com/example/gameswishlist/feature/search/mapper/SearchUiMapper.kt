package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.Platform
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
            val abbreviation = platform.abbreviation
            val label =
                if (platform.name.length > UiConstants.MAX_PLATFORM_NAME_LENGTH && abbreviation != null) {
                    abbreviation
                } else {
                    platform.name
                }

            GameFilterUiModel.Platform(
                id = platform.id,
                label = UiText.DynamicString(label),
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
        val stringResId = when (type) {
            GameType.MAIN_GAME -> UiR.string.gametype_main_game
            GameType.REMAKE -> UiR.string.gametype_remake
            GameType.REMASTER -> UiR.string.gametype_remaster
            GameType.EXPANSION -> UiR.string.gametype_expansion
            GameType.DLC_ADDON -> UiR.string.gametype_dlc_addon
            GameType.STANDALONE_EXPANSION -> UiR.string.gametype_standalone_expansion
            else -> UiR.string.gametype_main_game
        }
        GameFilterUiModel.GameType(
            id = type.id,
            label = UiText.StringResource(stringResId),
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
