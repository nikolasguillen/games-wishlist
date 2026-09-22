package com.example.gameswishlist.feature.radar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.ReleaseBucket

/** [bucket] only affects rendering: [ReleaseBucket.THIS_WEEK] gets a leading dot, every other bucket doesn't. */
@Composable
internal fun RadarSectionHeader(
    label: String,
    bucket: ReleaseBucket,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = MaterialTheme.spacing.large, bottom = MaterialTheme.spacing.small)
    ) {
        if (bucket == ReleaseBucket.THIS_WEEK) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarSectionHeaderThisWeekPreview() {
    GamesWishlistTheme {
        RadarSectionHeader(label = "This week", bucket = ReleaseBucket.THIS_WEEK)
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarSectionHeaderThisMonthPreview() {
    GamesWishlistTheme {
        RadarSectionHeader(label = "This month", bucket = ReleaseBucket.THIS_MONTH)
    }
}
