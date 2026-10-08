package com.nikolasguillen.questlog.feature.radar.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.ReleaseBucket
import com.nikolasguillen.questlog.core.ui.model.UiText

/** One bucket of the Radar timeline (e.g. "This week"), with the games that fall into it. */
@Immutable
internal data class RadarSectionUiModel(
    val bucket: ReleaseBucket,
    val label: UiText,
    val entries: List<RadarEntryUiModel>
)
