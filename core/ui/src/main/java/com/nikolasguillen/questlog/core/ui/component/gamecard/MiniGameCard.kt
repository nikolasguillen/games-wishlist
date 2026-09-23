package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors

@Composable
fun MiniGameCard(coverImage: String?, modifier: Modifier = Modifier) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
    ) {
        GameCoverImage(coverImage = coverImage)
    }
}

@Preview
@Composable
private fun MiniGameCardPreview() {
    QuestLogTheme {
        MiniGameCard(coverImage = null)
    }
}