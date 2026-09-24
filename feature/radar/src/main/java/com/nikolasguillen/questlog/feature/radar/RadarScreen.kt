package com.nikolasguillen.questlog.feature.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.ReleaseBucket
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.component.MainScreenHeader
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.radar.components.RadarEmptyState
import com.nikolasguillen.questlog.feature.radar.components.RadarGameRow
import com.nikolasguillen.questlog.feature.radar.components.RadarSectionHeader
import com.nikolasguillen.questlog.feature.radar.model.RadarContentState
import com.nikolasguillen.questlog.feature.radar.model.RadarEntryUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarSectionUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarUiState

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RadarContent(
        state = state,
        onGameClick = onGameClick,
        onProfileClick = onProfileClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RadarContent(
    state: RadarUiState,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MainScreenHeader(
                title = stringResource(R.string.radar_title),
                onProfileClick = onProfileClick
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (val content = state.contentState) {
            RadarContentState.Loading -> LoadingPage(modifier = contentModifier)

            RadarContentState.Empty -> RadarEmptyState(modifier = contentModifier)

            is RadarContentState.Success -> LazyColumn(modifier = contentModifier) {
                content.sections.forEach { section ->
                    stickyHeader(key = "header_${section.bucket}", contentType = "header") {
                        RadarSectionHeader(
                            label = section.label.asString(),
                            bucket = section.bucket,
                            modifier = Modifier
                                .padding(horizontal = MaterialTheme.spacing.large)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.appColors.appBackground
                                )
                        )
                    }
                    items(
                        items = section.entries,
                        key = { "${section.bucket}_${it.id}_${it.platform.id}" },
                        contentType = { "game" }
                    ) { entry ->
                        RadarGameRow(
                            entry = entry,
                            onClick = { onGameClick(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarContentPreview(contentState: RadarContentState) {
    QuestLogTheme {
        RadarContent(
            state = RadarUiState(contentState = contentState),
            onGameClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarContentSuccessPreview() {
    RadarContentPreview(
        RadarContentState.Success(
            sections = listOf(
                RadarSectionUiModel(
                    bucket = ReleaseBucket.THIS_WEEK,
                    label = UiText.DynamicString("This week"),
                    // Same game, two owned platforms: one row per platform, same as GetRadarTimelineUseCase emits.
                    entries = listOf(
                        RadarEntryUiModel.getDummy(),
                        RadarEntryUiModel.getDummy().copy(
                            platform = RadarEntryUiModel.getDummy().platform.copy(
                                id = 6,
                                code = UiText.DynamicString("PC"),
                                color = Color(0xFF5E5E5E)
                            )
                        )
                    )
                ),
                RadarSectionUiModel(
                    bucket = ReleaseBucket.LATER,
                    label = UiText.DynamicString("Later"),
                    entries = listOf(
                        RadarEntryUiModel.getDummy().copy(
                            id = 2,
                            title = "Grand Theft Auto VI",
                            studio = "Rockstar Games",
                            dateLabel = UiText.DynamicString("2027"),
                            dateSubLabel = null,
                            dateStyle = RadarEntryUiModel.DateLabelStyle.PILL_MUTED
                        )
                    )
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun RadarContentEmptyPreview() {
    RadarContentPreview(RadarContentState.Empty)
}

@Preview(showBackground = true)
@Composable
private fun RadarContentLoadingPreview() {
    RadarContentPreview(RadarContentState.Loading)
}
