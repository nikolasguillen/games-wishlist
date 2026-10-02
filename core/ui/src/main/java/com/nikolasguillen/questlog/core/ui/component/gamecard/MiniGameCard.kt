package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme

@Composable
fun MiniGameCard(coverImage: String?, modifier: Modifier = Modifier) {
    GenericGameCardLayout(
        shape = MaterialTheme.shapes.small,
        modifier = modifier.size(48.dp)
    ) {
        GameCoverImage(coverImage = coverImage)
    }
}

@QuestLogPreviews
@Composable
private fun MiniGameCardPreview() {
    QuestLogTheme {
        MiniGameCard(coverImage = null)
    }
}