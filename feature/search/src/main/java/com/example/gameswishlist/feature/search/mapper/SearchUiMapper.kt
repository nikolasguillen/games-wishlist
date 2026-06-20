package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.UiConstants
import com.example.gameswishlist.feature.search.model.GameFilterUiModel

fun List<Platform>.toPlatformFilters(): List<GameFilterUiModel> {
    return this.map { platform ->
        val abbreviation = platform.abbreviation
        val label = if (platform.name.length > UiConstants.MAX_PLATFORM_NAME_LENGTH && abbreviation != null) {
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
