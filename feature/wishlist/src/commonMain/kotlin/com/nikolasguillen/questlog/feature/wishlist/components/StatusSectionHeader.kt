package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.wishlist_section_header
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatusSectionHeader(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(
            Res.string.wishlist_section_header,
            label.uppercase(),
            count
        ),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.appColors.textOnSurface,
        modifier = modifier.padding(
            top = MaterialTheme.spacing.large,
            bottom = MaterialTheme.spacing.small
        )
    )
}

@QuestLogPreviews
@Composable
private fun StatusSectionHeaderPreview() {
    QuestLogTheme {
        StatusSectionHeader(label = "Playing", count = 2)
    }
}
