package com.nikolasguillen.questlog.feature.radar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.feature.radar.R

@Composable
internal fun RadarEmptyState(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.radar_empty_title),
        subtitle = stringResource(R.string.radar_empty_subtitle),
        icon = Icons.Default.Radar,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun RadarEmptyStatePreview() {
    QuestLogTheme {
        RadarEmptyState()
    }
}
