package com.example.gameswishlist.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.common.AppVersionProvider
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.translation.DownloadTranslationModelUseCase
import com.example.gameswishlist.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus
import com.example.gameswishlist.feature.settings.mapper.toSummaryUiText
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import com.example.gameswishlist.feature.settings.model.TranslationModelRowState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    appVersionProvider: AppVersionProvider,
    private val getSelectedPlatformsUseCase: GetSelectedPlatformsUseCase,
    private val downloadTranslationModelUseCase: DownloadTranslationModelUseCase,
    private val getTranslationModelStatusUseCase: GetTranslationModelStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = appVersionProvider.versionName))
    internal val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeOwnedPlatforms()
        loadTranslationModelStatus()
    }

    internal fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.DownloadTranslationModel -> downloadTranslationModel()
        }
    }

    // Collected for the ViewModel's whole life rather than gated behind WhileSubscribed, the same way
    // GameDetailViewModel.observeContentState folds a continuously-observed source into a single
    // MutableStateFlow instead of combine()-ing it with the locally driven parts of the state.
    private fun observeOwnedPlatforms() {
        viewModelScope.launch {
            getSelectedPlatformsUseCase().collect { platforms ->
                _uiState.update { it.copy(ownedPlatformsSummary = platforms.toSummaryUiText()) }
            }
        }
    }

    private fun loadTranslationModelStatus() {
        viewModelScope.launch {
            val rowState = when (getTranslationModelStatusUseCase()) {
                TranslationModelStatus.UNSUPPORTED -> TranslationModelRowState.Hidden
                TranslationModelStatus.DOWNLOADABLE -> TranslationModelRowState.Downloadable
                TranslationModelStatus.DOWNLOADING -> TranslationModelRowState.Downloading(null)
                TranslationModelStatus.READY -> TranslationModelRowState.Ready
            }
            _uiState.update { it.copy(translationModel = rowState) }
        }
    }

    private fun downloadTranslationModel() {
        if (_uiState.value.translationModel is TranslationModelRowState.Downloading) return
        viewModelScope.launch {
            downloadTranslationModelUseCase().collect { download ->
                val rowState = when (download) {
                    is TranslationModelDownload.InProgress -> TranslationModelRowState.Downloading(download.fraction)
                    TranslationModelDownload.Completed -> TranslationModelRowState.Ready
                    TranslationModelDownload.Failed -> TranslationModelRowState.Failed
                }
                _uiState.update { it.copy(translationModel = rowState) }
            }
        }
    }
}
