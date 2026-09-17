package com.example.gameswishlist.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.settings.components.SettingsGroup
import com.example.gameswishlist.feature.settings.components.SettingsRow
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import com.example.gameswishlist.feature.settings.model.TranslationModelRowState
import com.example.gameswishlist.core.ui.R as CoreUiR

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onOwnedPlatformsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        onOwnedPlatformsClick = onOwnedPlatformsClick,
        modifier = modifier
    )
}

/**
 * Settings hub, reached from the profile icon on the top-level screens.
 *
 * Rows are grouped by theme rather than listed flat, so a new setting joins the group it belongs to
 * instead of lengthening a single list. Only settings with something behind them are listed: the ones
 * the mockups also showed — notifications, appearance, region — arrive with their feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBackClick: () -> Unit,
    onOwnedPlatformsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium
                )
        ) {
            SettingsGroup(title = stringResource(R.string.settings_group_game_profile)) {
                SettingsRow(
                    icon = Icons.Default.SportsEsports,
                    title = stringResource(R.string.settings_owned_platforms),
                    subtitle = state.ownedPlatformsSummary?.asString(),
                    onClick = onOwnedPlatformsClick
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_group_app)) {
                TranslationModelRow(
                    rowState = state.translationModel,
                    onDownloadClick = { onEvent(SettingsUiEvent.DownloadTranslationModel) }
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_about),
                    trailingText = state.appVersion
                )
            }
        }
    }
}

@Composable
private fun TranslationModelRow(rowState: TranslationModelRowState, onDownloadClick: () -> Unit) {
    val subtitle = when (rowState) {
        TranslationModelRowState.Hidden -> null
        TranslationModelRowState.Downloadable -> stringResource(R.string.settings_translation_model_downloadable)
        is TranslationModelRowState.Downloading -> stringResource(R.string.settings_translation_model_downloading)
        TranslationModelRowState.Ready -> stringResource(R.string.settings_translation_model_ready)
        TranslationModelRowState.Failed -> stringResource(R.string.settings_translation_model_failed)
    } ?: return

    SettingsRow(
        icon = Icons.Outlined.Translate,
        title = stringResource(R.string.settings_translation_model),
        subtitle = subtitle,
        trailingContent = {
            when (rowState) {
                TranslationModelRowState.Hidden -> Unit
                TranslationModelRowState.Downloadable -> IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.settings_translation_model_download_action)
                    )
                }

                is TranslationModelRowState.Downloading -> {
                    val fraction = rowState.fraction
                    if (fraction != null) {
                        CircularWavyProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                TranslationModelRowState.Ready -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                TranslationModelRowState.Failed -> IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.settings_translation_model_download_action)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.DynamicString("PS5, PC, Switch"),
                appVersion = "1.0"
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentNoFilterPreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.StringResource(R.string.settings_owned_platforms_all),
                appVersion = "1.0"
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentTranslationDownloadablePreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.DynamicString("PS5, PC, Switch"),
                appVersion = "1.0",
                translationModel = TranslationModelRowState.Downloadable
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentTranslationDownloadingPreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.DynamicString("PS5, PC, Switch"),
                appVersion = "1.0",
                translationModel = TranslationModelRowState.Downloading(fraction = 0.4f)
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentTranslationReadyPreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.DynamicString("PS5, PC, Switch"),
                appVersion = "1.0",
                translationModel = TranslationModelRowState.Ready
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentTranslationFailedPreview() {
    GamesWishlistTheme {
        SettingsContent(
            state = SettingsUiState(
                ownedPlatformsSummary = UiText.DynamicString("PS5, PC, Switch"),
                appVersion = "1.0",
                translationModel = TranslationModelRowState.Failed
            ),
            onEvent = {},
            onBackClick = {},
            onOwnedPlatformsClick = {}
        )
    }
}
