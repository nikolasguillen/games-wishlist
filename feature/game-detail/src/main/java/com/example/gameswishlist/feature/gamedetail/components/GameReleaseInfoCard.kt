package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.PlatformTileUiModel
import com.example.gameswishlist.feature.gamedetail.model.ReleaseInfoUiModel

private const val MAX_VISIBLE_PLATFORM_TILES = 4

/**
 * A card displaying the main release date of a game alongside a compact strip of platform
 * tiles, with an option to show detailed per-platform dates in a dialog.
 */
@Composable
internal fun GameReleaseInfoCard(
    releaseInfo: ReleaseInfoUiModel,
    platforms: List<PlatformTileUiModel>,
    modifier: Modifier = Modifier
) {
    var showReleaseDatesDialog by rememberSaveable { mutableStateOf(false) }

    CustomContentCard(
        modifier = modifier.then(
            if (releaseInfo.isExpandable) {
                Modifier.clickable { showReleaseDatesDialog = true }
            } else {
                Modifier
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    stringResource(R.string.main_release_date_title).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = releaseInfo.mainDate.asString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (platforms.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                    PlatformTileRow(platforms = platforms)
                }
                if (releaseInfo.isExpandable) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(MaterialTheme.spacing.large)
                    )
                }
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

/**
 * A row of small colored platform tiles, capped at [MAX_VISIBLE_PLATFORM_TILES] with a
 * "+N" overflow tile so the strip never wraps or overruns the card.
 */
@Composable
private fun PlatformTileRow(
    platforms: List<PlatformTileUiModel>,
    modifier: Modifier = Modifier
) {
    val visiblePlatforms = platforms.take(MAX_VISIBLE_PLATFORM_TILES)
    val overflowCount = platforms.size - visiblePlatforms.size

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smallMedium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        visiblePlatforms.forEach { platform ->
            PlatformTile(code = platform.code, color = platform.color)
        }
        if (overflowCount > 0) {
            PlatformTile(
                code = UiText.StringResource(R.string.platform_overflow_format, overflowCount),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PlatformTile(
    code: UiText,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
            .background(color)
    ) {
        Text(
            text = code.asString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
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
            platforms = listOf(
                PlatformTileUiModel(
                    id = 1,
                    code = UiText.DynamicString("PC"),
                    color = Color(0xFF5E5E5E)
                ),
                PlatformTileUiModel(
                    id = 2,
                    code = UiText.DynamicString("PS5"),
                    color = Color(0xFF2E4EA6)
                ),
                PlatformTileUiModel(
                    id = 3,
                    code = UiText.DynamicString("XSX"),
                    color = Color(0xFF107C10)
                ),
                PlatformTileUiModel(
                    id = 4,
                    code = UiText.DynamicString("SWI"),
                    color = Color(0xFFE60012)
                ),
                PlatformTileUiModel(
                    id = 5,
                    code = UiText.DynamicString("MAC"),
                    color = Color(0xFF8E8E93)
                )
            ),
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameReleaseInfoCardNoDatePreview() {
    GamesWishlistTheme {
        GameReleaseInfoCard(
            releaseInfo = ReleaseInfoUiModel(
                mainDate = UiText.DynamicString("TBA"),
                detailedMessage = null,
                isExpandable = false
            ),
            platforms = listOf(
                PlatformTileUiModel(
                    id = 1,
                    code = UiText.DynamicString("PC"),
                    color = Color(0xFF5E5E5E)
                ),
                PlatformTileUiModel(
                    id = 2,
                    code = UiText.DynamicString("PS5"),
                    color = Color(0xFF2E4EA6)
                )
            ),
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}
