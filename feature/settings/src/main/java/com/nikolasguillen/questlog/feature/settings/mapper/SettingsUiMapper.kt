package com.nikolasguillen.questlog.feature.settings.mapper

import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.R

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
