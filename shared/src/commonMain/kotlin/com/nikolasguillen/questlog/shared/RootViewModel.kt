package com.nikolasguillen.questlog.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetAppearanceModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetOnboardingCompletedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The state of the app root, shared by every platform: the user's appearance choice, which follows every
 * change, and the onboarding flag, which is read once.
 *
 * The flag is deliberately not observed. It picks the back stack's first entry, and finishing the welcome flow
 * must not rebuild the stack under the user.
 */
class RootViewModel(
    getAppearanceModeUseCase: GetAppearanceModeUseCase,
    getOnboardingCompletedUseCase: GetOnboardingCompletedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAppearanceModeUseCase().collect { mode ->
                _uiState.update { it.copy(appearanceMode = mode) }
            }
        }
        viewModelScope.launch {
            val completed = getOnboardingCompletedUseCase().first()
            _uiState.update { it.copy(onboardingCompleted = completed) }
        }
    }
}
