package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme

/** The app's [TextButton]: a label in `primary`, which is readable on both light and dark surfaces. */
@Composable
fun CustomTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@QuestLogPreviews
@Composable
private fun CustomTextButtonPreview() {
    QuestLogTheme {
        CustomTextButton(onClick = {}) {
            Text(text = "Cancel")
        }
    }
}
