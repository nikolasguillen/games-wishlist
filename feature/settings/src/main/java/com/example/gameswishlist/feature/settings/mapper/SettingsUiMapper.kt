package com.example.gameswishlist.feature.settings.mapper

import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.settings.R

private const val PLATFORM_SEPARATOR = ", "

/**
 * Condenses the platforms the filter runs on into the subtitle of the "Owned platforms" row.
 *
 * The names are data, but the empty case is a resource, which is what makes the result a [UiText]
 * rather than a `String`. Empty reads as "all platforms" rather than "not set", because that is what an
 * empty selection now does: it filters nothing.
 */
internal fun List<Platform>.toSummaryUiText(): UiText {
    return if (isEmpty()) {
        UiText.StringResource(R.string.settings_owned_platforms_all)
    } else {
        UiText.CompoundString(
            texts = map { UiText.DynamicString(it.abbreviation ?: it.name) },
            separator = PLATFORM_SEPARATOR
        )
    }
}
