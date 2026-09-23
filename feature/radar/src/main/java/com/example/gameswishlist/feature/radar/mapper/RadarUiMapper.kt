package com.example.gameswishlist.feature.radar.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.RadarEntry
import com.example.gameswishlist.core.model.RadarTimelineSection
import com.example.gameswishlist.core.model.ReleaseBucket
import com.example.gameswishlist.core.ui.model.PlatformTileUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.PlatformVisuals
import com.example.gameswishlist.feature.radar.R
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel.DateLabelStyle
import com.example.gameswishlist.feature.radar.model.RadarSectionUiModel
import java.util.Locale

internal fun List<RadarTimelineSection>.toUiModel(): List<RadarSectionUiModel> {
    return map { it.toUiModel() }
}

private fun RadarTimelineSection.toUiModel(): RadarSectionUiModel {
    return RadarSectionUiModel(
        bucket = bucket,
        label = bucket.toLabelUiText(),
        entries = entries.map { it.toUiModel(bucket) }
    )
}

private fun RadarEntry.toUiModel(bucket: ReleaseBucket): RadarEntryUiModel {
    val label = resolveDateLabel(bucket)
    val style = PlatformVisuals.styleFor(
        Platform(id = releaseDate.platformId, name = releaseDate.platformName)
    )
    return RadarEntryUiModel(
        id = game.id,
        coverImage = game.backgroundImage,
        title = game.name,
        studio = game.developers.takeIf { it.isNotEmpty() }?.joinToString { it.name },
        platform = PlatformTileUiModel(id = releaseDate.platformId, code = style.code, color = style.color),
        dateLabel = label.primary,
        dateSubLabel = label.secondary,
        dateStyle = label.style
    )
}

private data class DateLabel(val primary: UiText, val secondary: UiText? = null, val style: DateLabelStyle)

private val TbaLabel = DateLabel(UiText.StringResource(R.string.radar_date_tba), style = DateLabelStyle.PILL_MUTED)

/**
 * Formats a [RadarEntry]'s date for display. [ReleaseBucket.THIS_WEEK] only affects [DatePrecision.EXACT_DATE]
 * entries, which get the weekday-plus-date stack; every other precision renders the same regardless of bucket.
 */
private fun RadarEntry.resolveDateLabel(bucket: ReleaseBucket): DateLabel {
    val date = releaseDate.date ?: return TbaLabel
    return when (releaseDate.precision) {
        DatePrecision.TBD -> TbaLabel

        DatePrecision.YEAR_ONLY -> DateLabel(
            primary = UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "yyyy")),
            style = DateLabelStyle.PILL_MUTED
        )

        DatePrecision.QUARTER -> {
            val localDate = DateUtils.timestampToLocalDate(date)
            val quarter = (localDate.monthValue - 1) / 3 + 1
            DateLabel(
                primary = UiText.StringResource(R.string.radar_quarter_format, quarter, localDate.year),
                style = DateLabelStyle.PILL_ACCENT
            )
        }

        DatePrecision.YEAR_MONTH -> DateLabel(
            primary = UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "MMM yyyy").capitalizedFirst()),
            style = DateLabelStyle.PLAIN
        )

        DatePrecision.EXACT_DATE -> if (bucket == ReleaseBucket.THIS_WEEK) {
            DateLabel(
                primary = UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "EEE").capitalizedFirst()),
                secondary = UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "MMM d").capitalizedFirst()),
                style = DateLabelStyle.THIS_WEEK
            )
        } else {
            DateLabel(
                primary = UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "MMM d").capitalizedFirst()),
                style = DateLabelStyle.PLAIN
            )
        }
    }
}

/**
 * Some locales' short month/weekday patterns ("EEE", "MMM") come back lowercase from
 * [java.time.format.DateTimeFormatter] (Italian's "set", "lun"). Capitalized to read consistently with the
 * rest of the Radar UI regardless of device locale.
 */
private fun String.capitalizedFirst(): String = replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun ReleaseBucket.toLabelUiText(): UiText = when (this) {
    ReleaseBucket.RECENTLY_RELEASED -> UiText.StringResource(R.string.radar_bucket_recently_released)
    ReleaseBucket.THIS_WEEK -> UiText.StringResource(R.string.radar_bucket_this_week)
    ReleaseBucket.THIS_MONTH -> UiText.StringResource(R.string.radar_bucket_this_month)
    ReleaseBucket.NEXT_3_MONTHS -> UiText.StringResource(R.string.radar_bucket_next_3_months)
    ReleaseBucket.LATER -> UiText.StringResource(R.string.radar_bucket_later)
    ReleaseBucket.TBA -> UiText.StringResource(R.string.radar_bucket_tba)
}
