package com.example.gameswishlist.feature.wishlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.EmptyPage
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.wishlist.components.StatusSectionHeader
import com.example.gameswishlist.feature.wishlist.components.WishlistGameRow
import com.example.gameswishlist.feature.wishlist.model.WishlistSectionUiModel
import com.example.gameswishlist.feature.wishlist.model.WishlistUiEffect
import com.example.gameswishlist.feature.wishlist.model.WishlistUiEvent
import com.example.gameswishlist.feature.wishlist.model.WishlistUiState
import com.example.gameswishlist.core.ui.R as CoreUiR

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    WishlistUiEffect.NavigateBack -> onBackClick()
                    is WishlistUiEffect.ShowSnackbar -> {
                        // TODO implement snackbar
                    }
                }
            }
        }
    }

    WishlistContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WishlistContent(
    state: WishlistUiState,
    onEvent: (WishlistUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.listName.asString(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.back_content_description)
                        )
                    }
                },
                actions = {
                    if (state.canDeleteList) {
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
        if (state.sections.isEmpty()) {
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
                state.sections.forEach { section ->
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
                        } else {
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            CustomAlertDialog(
                title = stringResource(
                    R.string.delete_list_dialog_title,
                    state.listName.asString()
                ),
                message = stringResource(R.string.delete_list_dialog_message),
                confirmButtonText = stringResource(R.string.delete_action),
                onConfirm = {
                    showDeleteDialog = false
                    onEvent(WishlistUiEvent.OnWishlistDeleted)
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

@Preview(showBackground = true)
@Composable
private fun WishlistContentPreview() {
    GamesWishlistTheme {
        WishlistContent(
            state = WishlistUiState(
                listName = UiText.DynamicString("My Wishlist"),
                sections = listOf(
                    WishlistSectionUiModel(
                        status = GameStatus.PLAYING,
                        label = UiText.DynamicString("PLAYING"),
                        games = listOf(GameItemUiModel.getDummy())
                    ),
                    WishlistSectionUiModel(
                        status = null,
                        label = null,
                        games = listOf(
                            GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
                        )
                    )
                ),
                canDeleteList = true
            ),
            onEvent = {},
            onGameClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistContentEmptyPreview() {
    GamesWishlistTheme {
        WishlistContent(
            state = WishlistUiState(listName = UiText.DynamicString("My Wishlist")),
            onEvent = {},
            onGameClick = {},
            onBackClick = {}
        )
    }
}
