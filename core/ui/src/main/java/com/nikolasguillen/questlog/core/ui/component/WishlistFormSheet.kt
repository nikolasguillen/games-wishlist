package com.nikolasguillen.questlog.core.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.R
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.WishlistFormUiModel
import java.io.File

/**
 * Bottom sheet with the form shared by creating and editing a wishlist. It has no create/edit mode of its
 * own: the caller supplies the [title], the [confirmLabel] and the [initialValues].
 *
 * Each field is seeded from [initialValues] once, on first composition, and survives rotation and process
 * death. Dismissing the sheet drops that state, so reopening it starts again from the caller's values.
 *
 * @param onConfirm Receives the trimmed values. Only reachable while the name is not blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistFormSheet(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (WishlistFormUiModel) -> Unit,
    initialValues: WishlistFormUiModel = WishlistFormUiModel()
) {
    var name by rememberSaveable { mutableStateOf(initialValues.name) }
    var description by rememberSaveable { mutableStateOf(initialValues.description) }
    var selectedIcon by rememberSaveable { mutableStateOf(initialValues.icon) }
    var selectedCoverImage by rememberSaveable { mutableStateOf(initialValues.coverImage) }
    val descriptionFocusRequester = remember { FocusRequester() }
    val pickCoverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedCoverImage = uri.toString() }

    CustomModalBottomSheet(
        onDismiss = onDismiss,
        title = title
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumLarge),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(all = MaterialTheme.spacing.large)
        ) {
            CoverImagePicker(
                coverImage = selectedCoverImage,
                onPickClick = {
                    pickCoverImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { selectedCoverImage = null }
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.list_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { descriptionFocusRequester.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.appColors.textOnSurface,
                    focusedLabelColor = MaterialTheme.appColors.textOnSurface,
                    cursorColor = MaterialTheme.appColors.textOnSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional_label)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.appColors.textOnSurface,
                    focusedLabelColor = MaterialTheme.appColors.textOnSurface,
                    cursorColor = MaterialTheme.appColors.textOnSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester)
            )

            Text(
                text = stringResource(R.string.icon_optional_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                items(WishlistIcon.entries) { icon ->
                    IconOption(
                        icon = icon,
                        isSelected = selectedIcon == icon,
                        onClick = { selectedIcon = if (selectedIcon == icon) null else icon }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.appColors.textOnSurface
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
                Button(
                    onClick = {
                        onConfirm(
                            WishlistFormUiModel(
                                name = name.trim(),
                                description = description.trim(),
                                icon = selectedIcon,
                                coverImage = selectedCoverImage
                            )
                        )
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(text = confirmLabel)
                }
            }
        }
    }
}

@Composable
private fun CoverImagePicker(
    coverImage: String?,
    onPickClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPickClick)
        ) {
            if (coverImage != null) {
                AsyncImage(
                    // A stored cover is an absolute file path and a freshly picked one a content URI.
                    // Wrapping the path keeps it from depending on how Coil parses a scheme-less string.
                    model = if (coverImage.startsWith("/")) File(coverImage) else coverImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.placeholder),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = stringResource(R.string.add_cover_image_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (coverImage != null) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove_cover_image_action)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.cover_image_optional_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IconOption(
    icon: WishlistIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(icon.toDrawableRes()),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(MaterialTheme.spacing.extraLarge)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistFormSheetPreview() {
    QuestLogTheme {
        WishlistFormSheet(
            title = "Edit Wishlist",
            confirmLabel = "Save",
            onDismiss = {},
            onConfirm = {},
            initialValues = WishlistFormUiModel(
                name = "Couch Co-op",
                description = "Games worth playing together.",
                icon = WishlistIcon.HEART
            )
        )
    }
}
