package com.example.gameswishlist.feature.wishlist.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.feature.wishlist.R
import java.util.Locale

@Composable
fun StatusSectionHeader(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.wishlist_section_header, label.uppercase(Locale.getDefault()), count),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            top = MaterialTheme.spacing.large,
            bottom = MaterialTheme.spacing.small
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusSectionHeaderPreview() {
    GamesWishlistTheme {
        StatusSectionHeader(label = "Playing", count = 2)
    }
}
