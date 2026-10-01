package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.isDarkTheme

@Composable
fun CustomSummaryBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.isDarkTheme
    val border = if (isDarkTheme) null else BorderStroke(
        1.dp,
        MaterialTheme.appColors.chipSelectedBorderColor
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.appColors.chipSelectedContainerColor,
        shape = MaterialTheme.shapes.extraSmall,
        border = border
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.appColors.chipSelectedContentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@QuestLogPreviews
@Composable
private fun CustomSummaryBadgePreview() {
    QuestLogTheme(darkTheme = isSystemInDarkTheme()) {
        CustomSummaryBadge(text = "Playing")
    }
}
