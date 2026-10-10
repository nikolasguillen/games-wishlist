package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.got_it
import com.nikolasguillen.questlog.core.ui.util.modifiers.rainbowMetallicBorder
import com.nikolasguillen.questlog.feature.gamedetail.resources.Res
import com.nikolasguillen.questlog.feature.gamedetail.resources.description_translation_info_action
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

internal val TranslationBadgeIconSize = 14.dp

/**
 * The chip and info dialog shared by every platform's [TranslationEngineBadge]: the `actual`s only decide which
 * on-device model they credit, through [title], [message] and [icon].
 */
@Composable
internal fun TranslationBadge(
    title: String,
    message: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .clickable(
                onClickLabel = stringResource(Res.string.description_translation_info_action),
                role = Role.Button,
                onClick = { showInfoDialog = true }
            )
            .rainbowMetallicBorder(width = 2.dp, shape = shape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(MaterialTheme.spacing.medium)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.size(MaterialTheme.spacing.smallMedium))
        icon()
    }

    if (showInfoDialog) {
        CustomAlertDialog(
            title = title,
            message = message,
            onConfirm = { showInfoDialog = false },
            onDismiss = { showInfoDialog = false },
            confirmButtonText = stringResource(CoreUiRes.string.got_it)
        )
    }
}
