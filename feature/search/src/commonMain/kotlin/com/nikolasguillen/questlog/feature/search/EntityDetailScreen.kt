package com.nikolasguillen.questlog.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ControllerLoadingAnimation
import com.nikolasguillen.questlog.core.ui.component.CustomInfoChip
import com.nikolasguillen.questlog.core.ui.component.DescriptionCard
import com.nikolasguillen.questlog.core.ui.component.DetailHeader
import com.nikolasguillen.questlog.core.ui.component.DetailHeaderDefaults
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.component.ErrorPage
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFab
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.component.gamecard.VerticalGameCard
import com.nikolasguillen.questlog.core.ui.model.DescriptionTranslationState
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.choose_list_content_description
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.core.ui.util.isLandscapeShape
import com.nikolasguillen.questlog.feature.search.components.CharacterPortrait
import com.nikolasguillen.questlog.feature.search.components.DeveloperLogo
import com.nikolasguillen.questlog.feature.search.components.EntityDetailTopBar
import com.nikolasguillen.questlog.feature.search.components.SearchResultGridCells
import com.nikolasguillen.questlog.feature.search.components.resultGridColumnCount
import com.nikolasguillen.questlog.feature.search.model.EntityDetailSubject
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEffect
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEvent
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiState
import com.nikolasguillen.questlog.feature.search.model.EntityGamesContentState
import com.nikolasguillen.questlog.feature.search.model.EntityHeaderUiModel
import com.nikolasguillen.questlog.feature.search.model.EntityProfileContentState
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.entity_games_title
import com.nikolasguillen.questlog.feature.search.resources.entity_no_games
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * The page of a character or a developer: a header, the translatable description, then a grid of its games.
 * It is the wishlist detail's sibling and is never reached from the bottom bar, so the navigation rail is hidden
 * while it is open.
 */
// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun EntityDetailScreen(
    viewModel: EntityDetailViewModel,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is EntityDetailUiEffect.ShowSnackbar -> {
                        this@repeatOnLifecycle.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(effect.message.resolve())
                        }
                    }
                }
            }
        }
    }

    EntityDetailContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
internal fun EntityDetailContent(
    state: EntityDetailUiState,
    onEvent: (EntityDetailUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX }
    }

    Scaffold(
        topBar = { EntityDetailTopBar(onBackClick = onBackClick) },
        floatingActionButton = {
            ScrollToTopFab(
                visible = state.profile is EntityProfileContentState.Success && showScrollToTop,
                onClick = { scope.launch { gridState.animateScrollToItem(0) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (val profile = state.profile) {
            EntityProfileContentState.Loading -> LoadingPage(modifier = contentModifier)

            is EntityProfileContentState.Error -> ErrorPage(
                message = profile.message,
                onRetryClick = { onEvent(EntityDetailUiEvent.OnRetry) },
                modifier = contentModifier
            )

            is EntityProfileContentState.Success -> EntityDetailGrid(
                state = state,
                header = profile.header,
                description = profile.description,
                onEvent = onEvent,
                onGameClick = onGameClick,
                gridState = gridState,
                modifier = contentModifier
            )
        }

        state.listSelectorState?.let { selectorState ->
            ListSelectorSheet(
                gameName = UiText.DynamicString(selectorState.gameName),
                list = selectorState.availableLists,
                onDismiss = { onEvent(EntityDetailUiEvent.OnDismissListSelector) },
                onConfirm = { onEvent(EntityDetailUiEvent.OnConfirmListSelection) },
                onToggleList = { listId -> onEvent(EntityDetailUiEvent.OnToggleListSelection(listId)) }
            )
        }
    }
}

/**
 * One scrolling grid for the whole page, so the header and the description scroll away with the games. The
 * header, the description, the title and the state of the games take a whole line each; only the game cards share
 * one, with the same columns and edges as the results grid they were reached from.
 */
@Composable
private fun EntityDetailGrid(
    state: EntityDetailUiState,
    header: EntityHeaderUiModel,
    description: String?,
    onEvent: (EntityDetailUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val chooseListLabel = stringResource(CoreUiRes.string.choose_list_content_description)

    BoxWithConstraints(modifier = modifier) {
        val edge = MaterialTheme.spacing.large
        val columnCount = resultGridColumnCount(
            availableWidth = maxWidth,
            spacing = MaterialTheme.spacing.large,
            edge = edge,
            isLandscape = isLandscapeShape(maxWidth, maxHeight)
        )

        LazyVerticalGrid(
            columns = SearchResultGridCells(columnCount, edge),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            contentPadding = PaddingValues(bottom = ScrollToTopFabDefaults.ContentBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                EntityHeader(state = state, header = header)
            }

            if (description != null) {
                item(key = "description", span = { GridItemSpan(maxLineSpan) }) {
                    DescriptionCard(
                        description = description,
                        translation = state.descriptionTranslation,
                        onTranslateClick = { onEvent(EntityDetailUiEvent.TranslateDescription) },
                        onShowOriginalClick = { onEvent(EntityDetailUiEvent.ShowOriginalDescription) },
                        modifier = Modifier.padding(horizontal = edge)
                    )
                }
            }

            item(key = "games_title", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.entity_games_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = edge)
                )
            }

            when (val games = state.games) {
                EntityGamesContentState.Loading -> item(key = "games_loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.extraLarge)
                    ) {
                        ControllerLoadingAnimation(modifier = Modifier.size(MaterialTheme.spacing.doubleLarge * 4))
                    }
                }

                EntityGamesContentState.Empty -> item(key = "games_empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyPage(
                        message = stringResource(Res.string.entity_no_games),
                        icon = Icons.Outlined.SportsEsports,
                        modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraLarge)
                    )
                }

                is EntityGamesContentState.Error -> item(key = "games_error", span = { GridItemSpan(maxLineSpan) }) {
                    ErrorPage(
                        message = games.message,
                        onRetryClick = { onEvent(EntityDetailUiEvent.OnRetry) },
                        modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraLarge)
                    )
                }

                is EntityGamesContentState.Success -> itemsIndexed(
                    items = games.games,
                    key = { _, game -> game.id }
                ) { index, game ->
                    val column = index % columnCount

                    VerticalGameCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        onLongClickLabel = chooseListLabel,
                        onSaveClick = { onEvent(EntityDetailUiEvent.OnToggleSave(game.id)) },
                        onLongClick = { onEvent(EntityDetailUiEvent.OnOpenListSelector(game.id)) },
                        modifier = Modifier
                            .animateItem(fadeOutSpec = null)
                            .padding(
                                start = if (column == 0) edge else MaterialTheme.spacing.default,
                                end = if (column == columnCount - 1) edge else MaterialTheme.spacing.default
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityHeader(
    state: EntityDetailUiState,
    header: EntityHeaderUiModel,
    modifier: Modifier = Modifier
) {
    DetailHeader(
        title = header.name,
        titleMaxLines = 2,
        cover = {
            val coverModifier = Modifier.size(DetailHeaderDefaults.CoverWidth)
            when (state.subject) {
                is EntityDetailSubject.Character -> CharacterPortrait(header.imageUrl, coverModifier)
                is EntityDetailSubject.Developer -> DeveloperLogo(header.imageUrl, coverModifier)
            }
        },
        modifier = modifier.fillMaxWidth(),
        chips = {
            val chips = listOfNotNull(state.gameCountText, header.foundedYearText)
            chips.forEach { text ->
                CustomInfoChip(
                    text = text.asString(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isLarge = false
                )
            }
        }
    )
}

private fun previewState(
    subject: EntityDetailSubject = EntityDetailSubject.Character(1),
    profile: EntityProfileContentState = EntityProfileContentState.Success(
        header = EntityHeaderUiModel(name = "Kratos", imageUrl = null, foundedYearText = null),
        description = "The Ghost of Sparta, who set out to avenge his family and ended up fighting the gods."
    ),
    games: EntityGamesContentState = EntityGamesContentState.Success(
        listOf(
            GameItemUiModel.getDummy(),
            GameItemUiModel.getDummy().copy(id = 2, name = "God of War Ragnarök"),
            GameItemUiModel.getDummy().copy(id = 3, name = "God of War III")
        )
    )
) = EntityDetailUiState(
    subject = subject,
    profile = profile,
    games = games,
    gameCountText = UiText.DynamicString("3 games"),
    descriptionTranslation = DescriptionTranslationState.Available
)

@Composable
private fun EntityDetailPreview(state: EntityDetailUiState) {
    QuestLogTheme {
        EntityDetailContent(
            state = state,
            onEvent = {},
            onGameClick = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun EntityDetailCharacterPreview() {
    EntityDetailPreview(previewState())
}

@QuestLogPreviews
@Composable
private fun EntityDetailDeveloperPreview() {
    EntityDetailPreview(
        previewState(
            subject = EntityDetailSubject.Developer(1),
            profile = EntityProfileContentState.Success(
                header = EntityHeaderUiModel(
                    name = "Naughty Dog",
                    imageUrl = null,
                    foundedYearText = UiText.DynamicString("Founded 1984")
                ),
                description = "An American first-party video game developer based in Santa Monica, California."
            )
        )
    )
}

@QuestLogPreviews
@Composable
private fun EntityDetailNoDescriptionPreview() {
    EntityDetailPreview(
        previewState(
            profile = EntityProfileContentState.Success(
                header = EntityHeaderUiModel(name = "Young Kratos", imageUrl = null, foundedYearText = null),
                description = null
            )
        )
    )
}

@QuestLogPreviews
@Composable
private fun EntityDetailLoadingGamesPreview() {
    EntityDetailPreview(previewState(games = EntityGamesContentState.Loading))
}

@QuestLogPreviews
@Composable
private fun EntityDetailGamesErrorPreview() {
    EntityDetailPreview(
        previewState(games = EntityGamesContentState.Error(UiText.DynamicString("No internet connection.")))
    )
}

@QuestLogPreviews
@Composable
private fun EntityDetailNoGamesPreview() {
    EntityDetailPreview(previewState(games = EntityGamesContentState.Empty))
}

@QuestLogPreviews
@Composable
private fun EntityDetailLoadingPreview() {
    EntityDetailPreview(previewState(profile = EntityProfileContentState.Loading))
}

@QuestLogPreviews
@Composable
private fun EntityDetailErrorPreview() {
    EntityDetailPreview(
        previewState(profile = EntityProfileContentState.Error(UiText.DynamicString("No internet connection.")))
    )
}
