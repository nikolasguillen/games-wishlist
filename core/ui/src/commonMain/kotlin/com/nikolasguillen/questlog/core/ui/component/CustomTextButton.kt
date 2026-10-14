package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors

/**
 * A [TextButton] whose label uses `textOnSurface`. Material's default is `primary`, a gold that vanishes
 * against the light background.
 */
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
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.textOnSurface),
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
