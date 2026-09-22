package com.example.gameswishlist.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.radar.GetRadarTimelineUseCase
import com.example.gameswishlist.feature.radar.mapper.toUiModel
import com.example.gameswishlist.feature.radar.model.RadarContentState
import com.example.gameswishlist.feature.radar.model.RadarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    getRadarTimelineUseCase: GetRadarTimelineUseCase
) : ViewModel() {

    // Single source of truth: reactively observes local storage (plus whatever the periodic refresh
    // worker last wrote), no manually mirrored copy.
    internal val uiState: StateFlow<RadarUiState> = getRadarTimelineUseCase()
        .map { sections ->
            val uiSections = sections.toUiModel()
            RadarUiState(
                contentState = if (uiSections.isEmpty()) {
                    RadarContentState.Empty
                } else {
                    RadarContentState.Success(uiSections)
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RadarUiState()
        )
}
