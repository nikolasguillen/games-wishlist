package com.nikolasguillen.questlog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.GetSavedGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.feature.settings.mapper.toReleaseNotificationUiModels
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsContentState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEffect
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEvent
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class ReleaseNotificationsViewModel(
    getSavedGamesUseCase: GetSavedGamesUseCase,
    getReleaseNotificationGameIdsUseCase: GetReleaseNotificationGameIdsUseCase,
    getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    private val setReleaseNotificationEnabledUseCase: SetReleaseNotificationEnabledUseCase
) : ViewModel() {

    private val _uiEffect = Channel<ReleaseNotificationsUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // Single source of truth: reactively observes local storage, no manually mirrored copy. Every saved
    // game is listed -- not filtered to the opted-in ones -- so a row's own toggle never removes it.
    internal val uiState: StateFlow<ReleaseNotificationsUiState> = combine(
        getSavedGamesUseCase(),
        getReleaseNotificationGameIdsUseCase(),
        getSelectedPlatformIdsUseCase()
    ) { savedGames, enabledGameIds, ownedPlatformIds ->
        val contentState = if (savedGames.isEmpty()) {
            ReleaseNotificationsContentState.Empty
        } else {
            ReleaseNotificationsContentState.Success(
                savedGames.toReleaseNotificationUiModels(ownedPlatformIds, enabledGameIds)
            )
        }
        ReleaseNotificationsUiState(contentState = contentState)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReleaseNotificationsUiState()
        )

    internal fun onEvent(event: ReleaseNotificationsUiEvent) {
        when (event) {
            is ReleaseNotificationsUiEvent.SetEnabled -> viewModelScope.launch {
                setReleaseNotificationEnabledUseCase(event.gameId, event.enabled)
                if (event.enabled) {
                    _uiEffect.send(ReleaseNotificationsUiEffect.RequestNotificationPermission)
                }
            }
        }
    }
}
