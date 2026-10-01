package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.feature.settings.R

/**
 * Narrows the platform list. The catalogue runs to hundreds of entries once synced, most of them
 * hardware nobody is looking for, so the field is how the list stays usable rather than a nicety.
 */
@Composable
internal fun PlatformSearchField(
    state: TextFieldState,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { Text(text = stringResource(R.string.owned_platforms_search_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (state.text.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(
                            R.string.owned_platforms_search_clear_content_description
                        )
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    )
}

@QuestLogPreviews
@Composable
private fun PlatformSearchFieldEmptyPreview() {
    QuestLogTheme(darkTheme = isSystemInDarkTheme()) {
        PlatformSearchField(state = TextFieldState(), onClearQuery = {})
    }
}

@QuestLogPreviews
@Composable
private fun PlatformSearchFieldTypedPreview() {
    QuestLogTheme(darkTheme = isSystemInDarkTheme()) {
        PlatformSearchField(state = TextFieldState("PlayStation"), onClearQuery = {})
    }
}
