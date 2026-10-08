package com.nikolasguillen.questlog.feature.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.radar.GetRadarTimelineUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.radar.mapper.toUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarContentState
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEffect
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEvent
import com.nikolasguillen.questlog.feature.radar.model.RadarUiState
import com.nikolasguillen.questlog.feature.radar.resources.Res
import com.nikolasguillen.questlog.feature.radar.resources.release_notification_disabled_message
import com.nikolasguillen.questlog.feature.radar.resources.release_notification_enabled_message
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class RadarViewModel(
    getRadarTimelineUseCase: GetRadarTimelineUseCase,
    getReleaseNotificationGameIdsUseCase: GetReleaseNotificationGameIdsUseCase,
    private val setReleaseNotificationEnabledUseCase: SetReleaseNotificationEnabledUseCase,
    releaseRemindersAvailability: ReleaseRemindersAvailability
) : ViewModel() {

    // Fixed for the life of the process: a platform without reminders never shows a bell or asks for permission.
    private val remindersAvailable = releaseRemindersAvailability.isAvailable

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
            },
            releaseRemindersAvailable = remindersAvailable
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RadarUiState(releaseRemindersAvailable = remindersAvailable)
        )

    internal fun onEvent(event: RadarUiEvent) {
        when (event) {
            is RadarUiEvent.ToggleReleaseNotification -> toggleReleaseNotification(event.gameId)
        }
    }

    private fun toggleReleaseNotification(gameId: Int) {
        if (!remindersAvailable) return
        val wasEnabled = gameId in notificationEnabledGameIds
        val gameTitle = findGameTitle(gameId)
        viewModelScope.launch {
            setReleaseNotificationEnabledUseCase(gameId, !wasEnabled)
            if (!wasEnabled) {
                _uiEffect.send(RadarUiEffect.RequestNotificationPermission)
            }
            val messageRes = if (wasEnabled) {
                Res.string.release_notification_disabled_message
            } else {
                Res.string.release_notification_enabled_message
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
