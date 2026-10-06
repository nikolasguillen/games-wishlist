package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.R
import com.nikolasguillen.questlog.core.ui.util.modifiers.metallicBorder

/**
 * Entry point to the Settings screen. Shared by every top-level screen (Search, Radar, Lists) so it stays
 * in one consistent place instead of each screen re-implementing it. Placed by [MainScreenHeader], which
 * is how every top-level screen should reach it rather than calling it directly.
 */
@Composable
fun ProfileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .metallicBorder(
                width = 2.dp,
                shape = CircleShape
            )
            .size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = stringResource(R.string.settings_content_description),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

@QuestLogPreviews
@Composable
private fun ProfileIconButtonPreview() {
    QuestLogTheme {
        ProfileIconButton(onClick = {})
    }
}
