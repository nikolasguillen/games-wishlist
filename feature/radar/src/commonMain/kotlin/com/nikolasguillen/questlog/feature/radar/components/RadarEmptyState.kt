package com.nikolasguillen.questlog.feature.radar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.feature.radar.resources.Res
import com.nikolasguillen.questlog.feature.radar.resources.radar_empty_subtitle
import com.nikolasguillen.questlog.feature.radar.resources.radar_empty_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RadarEmptyState(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(Res.string.radar_empty_title),
        subtitle = stringResource(Res.string.radar_empty_subtitle),
        icon = Icons.Default.Radar,
        modifier = modifier
    )
}

@QuestLogPreviews
@Composable
private fun RadarEmptyStatePreview() {
    QuestLogTheme {
        RadarEmptyState()
    }
}
