package com.nikolasguillen.questlog.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.radar.GetRadarTimelineUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.radar.mapper.toUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarContentState
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEffect
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEvent
import com.nikolasguillen.questlog.feature.radar.model.RadarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    getRadarTimelineUseCase: GetRadarTimelineUseCase,
    getReleaseNotificationGameIdsUseCase: GetReleaseNotificationGameIdsUseCase,
    private val setReleaseNotificationEnabledUseCase: SetReleaseNotificationEnabledUseCase
) : ViewModel() {

    // Tracks the latest known opt-in set so onEvent can flip a toggle without waiting for the next
    // emission -- the source of truth for isNotificationEnabled always stays this use case's Flow.
    private var notificationEnabledGameIds: Set<Int> = emptySet()

    private val _uiEffect = Channel<RadarUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // Single source of truth: reactively observes local storage (plus whatever the periodic refresh
    // worker last wrote), no manually mirrored copy.
    internal val uiState: StateFlow<RadarUiState> = combine(
        getRadarTimelineUseCase(),
        getReleaseNotificationGameIdsUseCase()
    ) { sections, notificationIds ->
        notificationEnabledGameIds = notificationIds
        val uiSections = sections.toUiModel(notificationIds)
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

    internal fun onEvent(event: RadarUiEvent) {
        when (event) {
            is RadarUiEvent.ToggleReleaseNotification -> toggleReleaseNotification(event.gameId)
        }
    }

    private fun toggleReleaseNotification(gameId: Int) {
        val wasEnabled = gameId in notificationEnabledGameIds
        val gameTitle = findGameTitle(gameId)
        viewModelScope.launch {
            setReleaseNotificationEnabledUseCase(gameId, !wasEnabled)
            if (!wasEnabled) {
                _uiEffect.send(RadarUiEffect.RequestNotificationPermission)
            }
            val messageRes = if (wasEnabled) {
                R.string.release_notification_disabled_message
            } else {
                R.string.release_notification_enabled_message
            }
            _uiEffect.send(RadarUiEffect.ShowSnackbar(UiText.StringResource(messageRes, gameTitle)))
        }
    }

    private fun findGameTitle(gameId: Int): String {
        val content = uiState.value.contentState as? RadarContentState.Success ?: return ""
        return content.sections
            .firstNotNullOfOrNull { section -> section.entries.firstOrNull { it.id == gameId } }
            ?.title
            .orEmpty()
    }
}
