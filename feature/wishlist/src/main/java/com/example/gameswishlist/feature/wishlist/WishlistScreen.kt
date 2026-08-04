package com.example.gameswishlist.feature.wishlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.EmptyPage
import com.example.gameswishlist.feature.wishlist.components.StatusSectionHeader
import com.example.gameswishlist.feature.wishlist.components.WishlistGameRow
import com.example.gameswishlist.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    listName: String,
    viewModel: WishlistViewModel,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    onListDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.listDeleted.collect { onListDeleted() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.back_content_description)
                        )
                    }
                },
                actions = {
                    if (viewModel.canDeleteList) {
                        ListOptionsMenu(onDeleteClick = { showDeleteDialog = true })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        if (sections.isEmpty()) {
            EmptyPage(
                message = stringResource(CoreUiR.string.empty_list_message),
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                sections.forEach { section ->
                    val label = section.label
                    if (label != null) {
                        item(key = "header_${section.status}") {
                            StatusSectionHeader(
                                label = label.asString(),
                                count = section.games.size,
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                            )
                        }
                    }
                    itemsIndexed(
                        items = section.games,
                        key = { _, game -> game.id }
                    ) { index, game ->
                        WishlistGameRow(
                            game = game,
                            onClick = { onGameClick(game.id) }
                        )
                        if (index < section.games.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                ),
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            CustomAlertDialog(
                title = stringResource(R.string.delete_list_dialog_title, listName),
                message = stringResource(R.string.delete_list_dialog_message),
                confirmButtonText = stringResource(R.string.delete_action),
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteList()
                },
                dismissButtonText = stringResource(CoreUiR.string.cancel),
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun ListOptionsMenu(onDeleteClick: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.list_options_content_description)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_list_action)) },
                onClick = {
                    expanded = false
                    onDeleteClick()
                }
            )
        }
    }
}
