package com.example.gameswishlist.feature.radar.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

/**
 * One row of the Radar timeline.
 *
 * [dateLabel] is the primary date text; [dateSubLabel] is only non-null for [DateLabelStyle.THIS_WEEK]
 * (the weekday goes in [dateLabel], the short date underneath in [dateSubLabel]). [dateStyle] tells the
 * row how to render the pair — plain text, the this-week weekday stack, or a pill for an imprecise date.
 */
@Immutable
internal data class RadarEntryUiModel(
    val id: Int,
    val coverImage: String?,
    val title: String,
    val studio: String?,
    val dateLabel: UiText,
    val dateSubLabel: UiText? = null,
    val dateStyle: DateLabelStyle = DateLabelStyle.PLAIN
) {
    internal enum class DateLabelStyle { PLAIN, THIS_WEEK, PILL_ACCENT, PILL_MUTED }

    companion object {
        fun getDummy() = RadarEntryUiModel(
            id = 1,
            coverImage = "https://media.rawg.io/media/games/618/618c49a64e2f469d6107ba9357d812d6.jpg",
            title = "Hollow Knight: Silksong",
            studio = "Team Cherry",
            dateLabel = UiText.DynamicString("Thu"),
            dateSubLabel = UiText.DynamicString("Sep 25"),
            dateStyle = DateLabelStyle.THIS_WEEK
        )
    }
}
