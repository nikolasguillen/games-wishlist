package com.example.gameswishlist.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.common.AppVersionProvider
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.translation.IsDescriptionTranslationSupportedUseCase
import com.example.gameswishlist.core.domain.usecase.translation.ObserveDescriptionTranslationEnabledUseCase
import com.example.gameswishlist.core.domain.usecase.translation.SetDescriptionTranslationEnabledUseCase
import com.example.gameswishlist.feature.settings.mapper.toSummaryUiText
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    appVersionProvider: AppVersionProvider,
    getSelectedPlatformsUseCase: GetSelectedPlatformsUseCase,
    observeDescriptionTranslationEnabledUseCase: ObserveDescriptionTranslationEnabledUseCase,
    private val setDescriptionTranslationEnabledUseCase: SetDescriptionTranslationEnabledUseCase,
    isDescriptionTranslationSupportedUseCase: IsDescriptionTranslationSupportedUseCase
) : ViewModel() {

    private val appVersion: String = appVersionProvider.versionName

    // Checked once: readiness does not change while the app is open, unlike the toggle itself.
    private val isTranslationSupported = MutableStateFlow(false)

    internal val uiState: StateFlow<SettingsUiState> = combine(
        getSelectedPlatformsUseCase(),
        observeDescriptionTranslationEnabledUseCase(),
        isTranslationSupported
    ) { platforms, isTranslationEnabled, isTranslationSupported ->
        SettingsUiState(
            ownedPlatformsSummary = platforms.toSummaryUiText(),
            appVersion = appVersion,
            isTranslationSupported = isTranslationSupported,
            isTranslationEnabled = isTranslationEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(appVersion = appVersion)
    )

    init {
        viewModelScope.launch {
            isTranslationSupported.value = isDescriptionTranslationSupportedUseCase()
        }
    }

    internal fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetDescriptionTranslation ->
                viewModelScope.launch { setDescriptionTranslationEnabledUseCase(event.enabled) }
        }
    }
}
