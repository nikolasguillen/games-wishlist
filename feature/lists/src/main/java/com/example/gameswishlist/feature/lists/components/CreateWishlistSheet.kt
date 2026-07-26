package com.example.gameswishlist.feature.lists.components

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
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.ui.component.CustomModalBottomSheet
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.feature.lists.R
import com.example.gameswishlist.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateWishlistSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, icon: WishlistIcon?, coverImageUri: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedIcon by rememberSaveable { mutableStateOf<WishlistIcon?>(null) }
    var selectedCoverImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val descriptionFocusRequester = remember { FocusRequester() }
    val pickCoverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedCoverImageUri = uri.toString() }

    CustomModalBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.new_wishlist_sheet_title)
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
                coverImageUri = selectedCoverImageUri,
                onPickClick = {
                    pickCoverImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveClick = { selectedCoverImageUri = null }
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.list_name_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { descriptionFocusRequester.requestFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional_label)) },
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
                    Text(text = stringResource(CoreUiR.string.cancel))
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
                Button(
                    onClick = {
                        onCreate(name.trim(), description.trim(), selectedIcon, selectedCoverImageUri)
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.create_action))
                }
            }
        }
    }
}

@Composable
private fun CoverImagePicker(
    coverImageUri: String?,
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
            if (coverImageUri != null) {
                AsyncImage(
                    model = coverImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    error = painterResource(CoreUiR.drawable.placeholder),
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
        if (coverImageUri != null) {
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
            modifier = Modifier.size(24.dp)
        )
    }
}
