package com.example.gameswishlist.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.common.AppVersionProvider
import com.example.gameswishlist.core.common.NetworkStatusProvider
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.translation.DownloadTranslationModelUseCase
import com.example.gameswishlist.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus
import com.example.gameswishlist.feature.settings.mapper.toSummaryUiText
import com.example.gameswishlist.feature.settings.model.SettingsUiEffect
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import com.example.gameswishlist.feature.settings.model.TranslationModelRowState
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
class SettingsViewModel @Inject constructor(
    appVersionProvider: AppVersionProvider,
    private val getSelectedPlatformsUseCase: GetSelectedPlatformsUseCase,
    private val downloadTranslationModelUseCase: DownloadTranslationModelUseCase,
    private val getTranslationModelStatusUseCase: GetTranslationModelStatusUseCase,
    private val networkStatusProvider: NetworkStatusProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(appVersion = appVersionProvider.versionName))
    internal val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    init {
        observeOwnedPlatforms()
        loadTranslationModelStatus()
    }

    internal fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.DownloadTranslationModel -> requestTranslationModelDownload()
            SettingsUiEvent.ConfirmDownloadTranslationModel ->
                viewModelScope.launch { observeTranslationModelDownload() }
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
            when (getTranslationModelStatusUseCase()) {
                TranslationModelStatus.UNSUPPORTED ->
                    _uiState.update { it.copy(translationModel = TranslationModelRowState.Hidden) }
                TranslationModelStatus.DOWNLOADABLE ->
                    _uiState.update { it.copy(translationModel = TranslationModelRowState.Downloadable) }
                // Safe to resume unconditionally: GameDescriptionTranslatorImpl only re-enters
                // GeminiNanoClient.download() when it is itself already tracking an active job in this
                // process. Otherwise it reports the indeterminate state without touching the SDK again,
                // so this never risks the false-Completed re-invoking download() directly would cause.
                TranslationModelStatus.DOWNLOADING -> observeTranslationModelDownload()
                TranslationModelStatus.READY ->
                    _uiState.update { it.copy(translationModel = TranslationModelRowState.Ready) }
            }
        }
    }

    private fun requestTranslationModelDownload() {
        if (_uiState.value.translationModel is TranslationModelRowState.Downloading) return
        if (!networkStatusProvider.isUnmeteredNetworkAvailable) {
            // AICore only transfers the model's several GB over an unmetered connection; on mobile data
            // it accepts the request and then silently sits at 0% forever, with no error the SDK
            // surfaces — so this is refused up front instead of leaving the user staring at a stuck bar.
            viewModelScope.launch { _uiEffect.send(SettingsUiEffect.ShowWifiRequiredDialog) }
            return
        }
        viewModelScope.launch { _uiEffect.send(SettingsUiEffect.ShowDownloadConfirmDialog) }
    }

    private suspend fun observeTranslationModelDownload() {
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
