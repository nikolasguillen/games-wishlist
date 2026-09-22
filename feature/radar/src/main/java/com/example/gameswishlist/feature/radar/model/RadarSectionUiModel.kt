package com.example.gameswishlist.feature.radar.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.model.ReleaseBucket
import com.example.gameswishlist.core.ui.model.UiText

/** One bucket of the Radar timeline (e.g. "This week"), with the games that fall into it. */
@Immutable
internal data class RadarSectionUiModel(
    val bucket: ReleaseBucket,
    val label: UiText,
    val entries: List<RadarEntryUiModel>
)
