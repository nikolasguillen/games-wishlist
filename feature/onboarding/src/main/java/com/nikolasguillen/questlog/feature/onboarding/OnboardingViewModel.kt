package com.nikolasguillen.questlog.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.nikolasguillen.questlog.feature.onboarding.mapper.buildOnboardingPages
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEffect
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEvent
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    internal val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // Completing is guarded rather than idempotent-by-effect: a second tap would otherwise send a second
    // Finished, and the host would pop two screens.
    private var isCompleting = false

    internal fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NotificationFactsResolved -> resolveFacts(event)
            OnboardingUiEvent.FinishClicked -> complete()
        }
    }

    // Built once: the facts can change while the flow is open (the user grants the permission from system
    // settings), but a page count that shifts under the user's thumb is worse than a stale page.
    private fun resolveFacts(event: OnboardingUiEvent.NotificationFactsResolved) {
        _uiState.update { state ->
            if (state.contentState is OnboardingContentState.Ready) {
                state
            } else {
                state.copy(
                    contentState = OnboardingContentState.Ready(
                        buildOnboardingPages(
                            requiresRuntimePermission = event.requiresRuntimePermission,
                            canDeliver = event.canDeliver
                        )
                    )
                )
            }
        }
    }

    private fun complete() {
        if (isCompleting) return
        isCompleting = true
        viewModelScope.launch {
            completeOnboardingUseCase()
            _uiEffect.send(OnboardingUiEffect.Finished)
        }
    }
}
