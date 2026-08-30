package com.example.gameswishlist.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.feature.settings.mapper.toSummaryUiText
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext context: Context,
    getSelectedPlatformsUseCase: GetSelectedPlatformsUseCase
) : ViewModel() {

    private val appVersion: String = context.packageManager.versionNameOf(context.packageName)

    internal val uiState: StateFlow<SettingsUiState> = getSelectedPlatformsUseCase()
        .map { platforms ->
            SettingsUiState(
                ownedPlatformsSummary = platforms.toSummaryUiText(),
                appVersion = appVersion
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState(appVersion = appVersion)
        )
}

/**
 * Reads the installed version name. The lookup cannot fail for the app's own package, but the platform
 * declares it as throwing, so a miss degrades to an empty version rather than taking down Settings.
 */
private fun PackageManager.versionNameOf(packageName: String): String = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }.versionName
}.getOrNull().orEmpty()
