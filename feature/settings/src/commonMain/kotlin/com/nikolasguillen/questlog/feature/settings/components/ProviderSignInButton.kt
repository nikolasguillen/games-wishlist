package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * A full-width button that starts one provider's sign-in. The label is the provider's own wording ("Continue with
 * Google", "Sign in with Apple"), passed in so the button holds no text of its own.
 *
 * While [loading] the label gives way to a spinner, and the label stays as the button's description so a screen reader
 * still names it. [enabled] is false while any cloud action runs, so a second tap cannot start a second one.
 */
@Composable
internal fun ProviderSignInButton(
    label: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = label }
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                strokeWidth = MaterialTheme.spacing.extraSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text = label)
        }
    }
}

@QuestLogPreviews
@Composable
private fun ProviderSignInButtonPreview() {
    QuestLogTheme {
        ProviderSignInButton(label = "Continue with Google", loading = false, enabled = true, onClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun ProviderSignInButtonLoadingPreview() {
    QuestLogTheme {
        ProviderSignInButton(label = "Sign in with Apple", loading = true, enabled = false, onClick = {})
    }
}
