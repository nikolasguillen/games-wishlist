package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A full-screen error state. [onRetryClick] is optional -- when null, no action is rendered, which is
 * the right call for an error a caller cannot meaningfully retry (see [EmptyPage] for the non-error
 * counterpart, whose action label varies per caller instead of being fixed to "Retry").
 */
@Composable
fun ErrorPage(
    message: UiText,
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.mediumLarge))
            Text(
                text = message.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            if (onRetryClick != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                Button(onClick = onRetryClick) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPagePreview() {
    GamesWishlistTheme {
        ErrorPage(message = UiText.DynamicString("No internet connection."))
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorPageWithRetryPreview() {
    GamesWishlistTheme {
        ErrorPage(
            message = UiText.DynamicString("No internet connection."),
            onRetryClick = {}
        )
    }
}
