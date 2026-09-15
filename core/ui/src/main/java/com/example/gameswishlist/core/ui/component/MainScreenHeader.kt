package com.example.gameswishlist.core.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing

/**
 * Shared top-of-screen header for every top-level section (Search, Lists, and Radar once it exists).
 * Fixes the content height and the profile-entry slot so sections line up regardless of what they put
 * in [content] — a title or a search bar. See [MainScreenHeaderDefaults.Height] for why that height
 * was chosen.
 *
 * [containerColor] paints [content] and [bottomContent] as one surface, so a caller that animates its
 * background on scroll (Search) only has to pass the color once instead of syncing two surfaces.
 * [bottomContent] is for anything that scrolls with the header but isn't part of its fixed-height row
 * (Search's result count / sort-and-filter row).
 *
 * [leadingContent] is an optional slot to the left of [content] (Search's back-to-feed arrow). It is
 * always laid out, empty included, and animates its width, so a caller that shows or hides an icon there
 * gets [content] resized instead of jumped.
 */
@Composable
fun MainScreenHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    leadingContent: @Composable () -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Surface(color = containerColor, modifier = modifier) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .height(MainScreenHeaderDefaults.Height)
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                // Always emitted, even when empty: this slot's animated width is what resizes [content],
                // and a slot that entered and left the composition would snap instead of animating.
                // alignment = Center makes the resize expand/collapse outward from the icon's own
                // center instead of animateContentSize's default TopStart, which — combined with this
                // Row's CenterVertically — read as the icon sliding up from the bottom.
                Box(
                    modifier = Modifier
                        .clipToBounds()
                        .animateContentSize(alignment = Alignment.Center)
                ) {
                    leadingContent()
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                // The gap to [content] rides on the button rather than on Arrangement.spacedBy, which
                // would also open a gap in front of an empty leading slot.
                ProfileIconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
                )
            }
            bottomContent()
        }
    }
}

/**
 * A title-only [MainScreenHeader] for sections that show plain text instead of a search bar (Lists,
 * and Radar once it exists).
 */
@Composable
fun MainScreenHeader(
    title: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    MainScreenHeader(
        onProfileClick = onProfileClick,
        modifier = modifier,
        containerColor = containerColor,
        bottomContent = bottomContent,
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

object MainScreenHeaderDefaults {
    /**
     * [content] is vertically centered in the header row, so this is [SearchBarDefaults.InputFieldHeight]
     * (the collapsed search bar's own fixed height, 56.dp) plus two margins of `spacing.medium` (8.dp,
     * not referenced directly — this is a plain object, outside composition) — the same margin a
     * title-only header (Lists) gets around its text for free by sharing this constant. Raise it if a
     * header ever feels cramped again; both header shapes grow in step.
     */
    val Height: Dp = SearchBarDefaults.InputFieldHeight + (12.dp * 2)
}

@Preview(showBackground = true)
@Composable
private fun MainScreenHeaderPreview() {
    GamesWishlistTheme {
        MainScreenHeader(
            onProfileClick = {},
            content = {
                Text(text = "Games Wishlist")
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenHeaderWithLeadingContentPreview() {
    GamesWishlistTheme {
        MainScreenHeader(
            onProfileClick = {},
            leadingContent = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            content = {
                Text(text = "Games Wishlist")
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenHeaderTitlePreview() {
    GamesWishlistTheme {
        MainScreenHeader(
            title = "My Wishlists",
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenHeaderWithBottomContentPreview() {
    GamesWishlistTheme {
        MainScreenHeader(
            title = "Search",
            onProfileClick = {},
            bottomContent = {
                Text(
                    text = "12 results",
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
            }
        )
    }
}
