package com.nikolasguillen.questlog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetGamesWithReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.feature.settings.mapper.toReleaseNotificationUiModels
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsContentState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEvent
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReleaseNotificationsViewModel @Inject constructor(
    getGamesWithReleaseNotificationsUseCase: GetGamesWithReleaseNotificationsUseCase,
    getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    private val setReleaseNotificationEnabledUseCase: SetReleaseNotificationEnabledUseCase
) : ViewModel() {

    // Single source of truth: reactively observes local storage, no manually mirrored copy.
    internal val uiState: StateFlow<ReleaseNotificationsUiState> = combine(
        getGamesWithReleaseNotificationsUseCase(),
        getSelectedPlatformIdsUseCase()
    ) { games, ownedPlatformIds ->
        val contentState = if (games.isEmpty()) {
            ReleaseNotificationsContentState.Empty
        } else {
            ReleaseNotificationsContentState.Success(games.toReleaseNotificationUiModels(ownedPlatformIds))
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
            is ReleaseNotificationsUiEvent.ToggleOff ->
                viewModelScope.launch { setReleaseNotificationEnabledUseCase(event.gameId, false) }
        }
    }
}
