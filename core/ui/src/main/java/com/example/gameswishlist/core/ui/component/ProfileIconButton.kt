package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R

/**
 * Entry point to the Settings screen. Shared by every top-level screen (Search, Radar, Lists) so it stays
 * in one consistent place instead of each screen re-implementing it.
 */
@Composable
fun ProfileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = stringResource(R.string.settings_content_description),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.extraSmall)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFFF)
@Composable
private fun ProfileIconButtonPreview() {
    GamesWishlistTheme {
        ProfileIconButton(onClick = {})
    }
}
