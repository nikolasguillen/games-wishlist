package com.nikolasguillen.questlog.feature.settings.mapper

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.domain.radar.resolveReleaseDates
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.R
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationUiModel

/**
 * Every saved game, each carrying its own [ReleaseNotificationUiModel.isEnabled] rather than being
 * filtered to the currently-enabled ones -- the list is meant to stay put while a row's toggle changes.
 */
internal fun List<Game>.toReleaseNotificationUiModels(
    ownedPlatformIds: Set<Int>,
    enabledGameIds: Set<Int>
): List<ReleaseNotificationUiModel> {
    return map { it.toReleaseNotificationUiModel(ownedPlatformIds, enabledGameIds) }
}

private fun Game.toReleaseNotificationUiModel(
    ownedPlatformIds: Set<Int>,
    enabledGameIds: Set<Int>
): ReleaseNotificationUiModel {
    val earliestDate = resolveReleaseDates(ownedPlatformIds).minByOrNull { it.date ?: Long.MAX_VALUE }
    val date = earliestDate?.date
    val dateLabel = if (date != null && earliestDate.precision == DatePrecision.EXACT_DATE) {
        UiText.DynamicString(DateUtils.formatUnixTimestamp(date))
    } else {
        UiText.StringResource(R.string.release_notifications_no_date_yet)
    }
    return ReleaseNotificationUiModel(
        gameId = id,
        coverImage = backgroundImage,
        title = name,
        dateLabel = dateLabel,
        isEnabled = id in enabledGameIds
    )
}
