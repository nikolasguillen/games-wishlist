package com.nikolasguillen.questlog.feature.wishlist

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFab
import com.nikolasguillen.questlog.core.ui.component.WishlistFormSheet
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.empty_list_message
import com.nikolasguillen.questlog.core.ui.resources.save_label
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.feature.wishlist.components.DeleteWishlistDialog
import com.nikolasguillen.questlog.feature.wishlist.components.RemoveGameDialog
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistDetailHeader
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistGamesList
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistTopBar
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistViewOptionsRow
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistContentState
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistFilterChipUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistStatusFilter
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.edit_wishlist_sheet_title
import com.nikolasguillen.questlog.feature.wishlist.resources.filtered_empty_message
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * Scrolls smoothly back to the first item in a single animation.
 *
 * `animateScrollToItem` is not used because it estimates the distance to an off-screen item as if every grid
 * line held two items, while the header, the options row, the section headers and every list-view row take a
 * whole line. The estimate falls short, so the scroll settles, re-estimates and starts again.
 *
 * Here the real distance is measured first: scrolling inside one [scroll] session lays items out without
 * drawing a frame in between, and each `scrollBy` returns how far it actually went. A top within one viewport
 * is animated all the way; a farther one is first jumped to one viewport below it, as `animateScrollToItem`
 * itself teleports over long distances.
 */
private suspend fun LazyGridState.animateScrollToTop() {
    val maxDistance = layoutInfo.viewportSize.height.toFloat()
    var distance = 0f
    scroll {
        val distanceToTop = -scrollBy(-maxDistance)
        distance = if (distanceToTop < maxDistance) {
            scrollBy(distanceToTop)
            distanceToTop
        } else {
            LazyLayoutScrollScope(this@animateScrollToTop, this).snapToItem(0)
            scrollBy(maxDistance)
        }
    }
    animateScrollBy(-distance)
    // The measured distance can be off by a fraction of a pixel, which would leave the header a sliver short.
    scrollToItem(0)
}

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    WishlistUiEffect.NavigateBack -> onBackClick()
                    is WishlistUiEffect.ShowSnackbar ->
                        snackbarHostState.showSnackbar(effect.message.resolve())
                }
            }
        }
    }

    WishlistContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
internal fun WishlistContent(
    state: WishlistUiState,
    onEvent: (WishlistUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Saveable so a rotation keeps the sheet open; its own fields are saveable too.
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var revealedGameId by remember { mutableStateOf<Int?>(null) }
    var gamePendingRemoval by remember { mutableStateOf<GameItemUiModel?>(null) }
    // Hoisted so one scroll state serves both views (and whatever needs to observe it later).
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // The same point Search shows its button: the first visible item is past the header and the options row.
    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX }
    }

    // A row left revealed in the list view must not survive a switch to the grid and back.
    LaunchedEffect(state.viewMode) { revealedGameId = null }

    Scaffold(
        topBar = {
            WishlistTopBar(
                showEditAction = state.showEditAction,
                showListOptions = state.showListOptions,
                onBackClick = onBackClick,
                onEditClick = { showEditSheet = true },
                onSetAsDefaultClick = { onEvent(WishlistUiEvent.OnSetAsDefault) },
                onDeleteClick = { showDeleteDialog = true }
            )
        },
        floatingActionButton = {
            // The grid state outlives the empty and filtered-empty content, where it may still hold a stale
            // position, so those states are excluded here rather than left to the scroll test alone.
            ScrollToTopFab(
                visible = state.contentState is WishlistContentState.Success && showScrollToTop,
                onClick = { scope.launch { gridState.animateScrollToTop() } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        val header: @Composable () -> Unit = {
            WishlistDetailHeader(
                title = state.listName.asString(),
                description = state.description,
                iconRes = state.iconRes,
                coverImagePath = state.coverImagePath,
                gameCountText = state.gameCountText.asString(),
                isDefaultList = state.isDefaultList
            )
        }
        val viewOptionsRow: @Composable () -> Unit = {
            WishlistViewOptionsRow(
                viewMode = state.viewMode,
                chips = state.filterChips,
                onChipClick = { onEvent(WishlistUiEvent.OnStatusFilterSelected(it)) },
                onToggleClick = { onEvent(WishlistUiEvent.OnViewModeToggled) }
            )
        }
        when (val content = state.contentState) {
            WishlistContentState.Loading -> LoadingPage(modifier = contentModifier)

            WishlistContentState.Empty -> Column(modifier = contentModifier) {
                header()
                EmptyPage(
                    message = stringResource(CoreUiRes.string.empty_list_message),
                    icon = Icons.Outlined.Inventory2,
                    modifier = Modifier.weight(1f)
                )
            }

            // The list has games but the active filter matches none: keep the row on screen, so the selected
            // chip explains the empty area and one tap on "All" brings the games back.
            WishlistContentState.FilteredEmpty -> Column(modifier = contentModifier) {
                header()
                viewOptionsRow()
                EmptyPage(
                    message = stringResource(Res.string.filtered_empty_message),
                    icon = Icons.Outlined.FilterAltOff,
                    modifier = Modifier.weight(1f)
                )
            }

            is WishlistContentState.Success -> WishlistGamesList(
                sections = content.sections,
                viewMode = state.viewMode,
                gridState = gridState,
                revealedGameId = revealedGameId,
                onRevealedGameIdChange = { revealedGameId = it },
                onGameClick = onGameClick,
                onGameRemoveClick = { game -> gamePendingRemoval = game },
                header = header,
                viewOptionsRow = viewOptionsRow,
                modifier = contentModifier
            )
        }

        if (showEditSheet) {
            WishlistFormSheet(
                title = stringResource(Res.string.edit_wishlist_sheet_title),
                confirmLabel = stringResource(CoreUiRes.string.save_label),
                initialValues = state.formValues,
                onDismiss = { showEditSheet = false },
                onConfirm = { values ->
                    onEvent(WishlistUiEvent.OnListEdited(values))
                    showEditSheet = false
                }
            )
        }

        if (showDeleteDialog) {
            DeleteWishlistDialog(
                listName = state.listName.asString(),
                onConfirm = {
                    showDeleteDialog = false
                    onEvent(WishlistUiEvent.OnWishlistDeleted)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        gamePendingRemoval?.let { game ->
            RemoveGameDialog(
                gameName = game.name,
                onConfirm = {
                    gamePendingRemoval = null
                    revealedGameId = null
                    onEvent(WishlistUiEvent.OnGameRemoved(game.id))
                },
                onDismiss = {
                    gamePendingRemoval = null
                    revealedGameId = null
                }
            )
        }
    }
}

@Composable
private fun WishlistContentPreview(
    contentState: WishlistContentState,
    viewMode: WishlistViewMode = WishlistViewMode.LIST,
    filterChips: List<WishlistFilterChipUiModel> = emptyList()
) {
    QuestLogTheme {
        WishlistContent(
            state = WishlistUiState(
                listName = UiText.DynamicString("Couch Co-op"),
                description = "Games worth playing together, controllers in hand.",
                gameCountText = UiText.DynamicString("12 games"),
                showListOptions = true,
                showEditAction = true,
                viewMode = viewMode,
                filterChips = filterChips,
                contentState = contentState
            ),
            onEvent = {},
            onGameClick = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

private fun previewSections() = listOf(
    WishlistSectionUiModel(
        status = GameStatus.PLAYING,
        label = UiText.DynamicString("PLAYING"),
        games = listOf(GameItemUiModel.getDummy())
    ),
    WishlistSectionUiModel(
        status = null,
        label = UiText.DynamicString("NO STATUS"),
        games = listOf(
            GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
        )
    )
)

@QuestLogPreviews
@Composable
private fun WishlistContentSuccessPreview() {
    WishlistContentPreview(WishlistContentState.Success(sections = previewSections()))
}

@QuestLogPreviews
@Composable
private fun WishlistContentGridPreview() {
    WishlistContentPreview(
        contentState = WishlistContentState.Success(sections = previewSections()),
        viewMode = WishlistViewMode.GRID
    )
}

private fun previewChips(selected: WishlistStatusFilter) = listOf(
    WishlistFilterChipUiModel(WishlistStatusFilter.All, UiText.DynamicString("All"), selected == WishlistStatusFilter.All),
    WishlistFilterChipUiModel(
        WishlistStatusFilter.Only(GameStatus.PLAYING),
        UiText.DynamicString("Playing"),
        selected == WishlistStatusFilter.Only(GameStatus.PLAYING)
    ),
    WishlistFilterChipUiModel(
        WishlistStatusFilter.Only(null),
        UiText.DynamicString("No status"),
        selected == WishlistStatusFilter.Only(null)
    )
)

@QuestLogPreviews
@Composable
private fun WishlistContentFilteredPreview() {
    val filter = WishlistStatusFilter.Only(GameStatus.PLAYING)
    WishlistContentPreview(
        contentState = WishlistContentState.Success(sections = previewSections().take(1)),
        filterChips = previewChips(selected = filter)
    )
}

@QuestLogPreviews
@Composable
private fun WishlistContentFilteredEmptyPreview() {
    WishlistContentPreview(
        contentState = WishlistContentState.FilteredEmpty,
        filterChips = previewChips(selected = WishlistStatusFilter.Only(GameStatus.PLAYING))
    )
}

@QuestLogPreviews
@Composable
private fun WishlistContentEmptyPreview() {
    WishlistContentPreview(WishlistContentState.Empty)
}

@QuestLogPreviews
@Composable
private fun WishlistContentLoadingPreview() {
    WishlistContentPreview(WishlistContentState.Loading)
}
