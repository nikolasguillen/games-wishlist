package com.nikolasguillen.questlog.feature.radar.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class RadarUiState(
    val contentState: RadarContentState = RadarContentState.Loading
)
