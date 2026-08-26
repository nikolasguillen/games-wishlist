package com.example.gameswishlist.feature.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.component.ProfileIconButton
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.lists.components.CreateWishlistCard
import com.example.gameswishlist.feature.lists.components.CreateWishlistSheet
import com.example.gameswishlist.feature.lists.components.WishlistRow
import com.example.gameswishlist.feature.lists.model.ListsContentState
import com.example.gameswishlist.feature.lists.model.ListsUiEffect
import com.example.gameswishlist.feature.lists.model.ListsUiEvent
import com.example.gameswishlist.feature.lists.model.ListsUiState
import com.example.gameswishlist.feature.lists.model.WishlistListUiModel

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    onListClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is ListsUiEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                    }
                }
            }
        }
    }

    ListsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onListClick = onListClick,
        onProfileClick = onProfileClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
internal fun ListsContent(
    state: ListsUiState,
    onEvent: (ListsUiEvent) -> Unit,
    onListClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.wishlist_hub_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = { ProfileIconButton(onClick = onProfileClick) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (val content = state.contentState) {
            ListsContentState.Loading -> LoadingPage(modifier = contentModifier)

            is ListsContentState.Success -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumLarge)
            ) {
                item {
                    CreateWishlistCard(onClick = { showCreateSheet = true })
                }
                items(content.lists, key = { it.id }) { list ->
                    WishlistRow(
                        list = list,
                        onClick = { onListClick(list.id) }
                    )
                }
            }
        }

        if (showCreateSheet) {
            CreateWishlistSheet(
                onDismiss = { showCreateSheet = false },
                onCreate = { name, description, icon, coverImageUri ->
                    onEvent(
                        ListsUiEvent.OnListCreated(
                            name = name,
                            description = description,
                            icon = icon,
                            coverImageUri = coverImageUri
                        )
                    )
                    showCreateSheet = false
                }
            )
        }
    }
}

@Composable
private fun ListsContentPreview(contentState: ListsContentState) {
    GamesWishlistTheme {
        ListsContent(
            state = ListsUiState(contentState = contentState),
            onEvent = {},
            onListClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListsContentSuccessPreview() {
    ListsContentPreview(
        ListsContentState.Success(
            lists = listOf(
                WishlistListUiModel(
                    id = 1,
                    name = "My Wishlist",
                    description = "Everything I want to play",
                    iconRes = WishlistIcon.BACKLOG.toDrawableRes(),
                    coverImagePath = null,
                    gameCountText = UiText.DynamicString("12")
                ),
                WishlistListUiModel(
                    id = 2,
                    name = "RPGs to Try",
                    description = "One day",
                    iconRes = WishlistIcon.HEART.toDrawableRes(),
                    coverImagePath = null,
                    gameCountText = UiText.DynamicString("99+")
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun ListsContentNoListsPreview() {
    ListsContentPreview(ListsContentState.Success(lists = emptyList()))
}

@Preview(showBackground = true)
@Composable
private fun ListsContentLoadingPreview() {
    ListsContentPreview(ListsContentState.Loading)
}
