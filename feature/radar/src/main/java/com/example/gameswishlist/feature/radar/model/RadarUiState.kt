package com.example.gameswishlist.feature.radar.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class RadarUiState(
    val contentState: RadarContentState = RadarContentState.Loading
)
