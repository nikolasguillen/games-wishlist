package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.ReleaseInfoUiModel

/**
 * A card displaying the main release date of a game, with an option to show detailed dates in a dialog.
 */
@Composable
internal fun GameReleaseInfoCard(
    releaseInfo: ReleaseInfoUiModel,
    modifier: Modifier = Modifier
) {
    var showReleaseDatesDialog by rememberSaveable { mutableStateOf(false) }

    CustomContentCard(
        title = stringResource(R.string.main_release_date_title),
        modifier = modifier.then(
            if (releaseInfo.isExpandable) {
                Modifier.clickable { showReleaseDatesDialog = true }
            } else {
                Modifier
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = releaseInfo.mainDate.asString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (releaseInfo.isExpandable) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.spacing.large)
                )
            }
        }
    }

    if (showReleaseDatesDialog && releaseInfo.detailedMessage != null) {
        CustomAlertDialog(
            title = stringResource(R.string.release_dates_title),
            message = releaseInfo.detailedMessage.asString(),
            confirmButtonText = stringResource(android.R.string.ok),
            onConfirm = { showReleaseDatesDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameReleaseInfoCardPreview() {
    GamesWishlistTheme {
        GameReleaseInfoCard(
            releaseInfo = ReleaseInfoUiModel(
                mainDate = UiText.DynamicString("May 20th, 2026"),
                detailedMessage = UiText.DynamicString("PC: May 20th, 2026\nPS5: May 22nd, 2026"),
                isExpandable = true
            ),
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}
