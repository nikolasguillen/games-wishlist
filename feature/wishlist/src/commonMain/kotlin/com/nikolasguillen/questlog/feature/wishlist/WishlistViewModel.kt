package com.nikolasguillen.questlog.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.model.CoverImageUpdate
import com.nikolasguillen.questlog.core.domain.usecase.list.DeleteListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistViewModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetDefaultListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetWishlistViewModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.UpdateListUseCase
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.model.WishlistFormUiModel
import com.nikolasguillen.questlog.feature.wishlist.mapper.filteredBy
import com.nikolasguillen.questlog.feature.wishlist.mapper.toFilterChips
import com.nikolasguillen.questlog.feature.wishlist.mapper.toWishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistContentState
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistStatusFilter
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.default_list_set_message
import com.nikolasguillen.questlog.feature.wishlist.resources.game_count
import com.nikolasguillen.questlog.feature.wishlist.resources.unable_to_delete_wishlist
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class WishlistViewModel(
    private val listId: Long,
    getWishlistDetailUseCase: GetWishlistDetailUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase,
    private val setDefaultListUseCase: SetDefaultListUseCase,
    private val updateListUseCase: UpdateListUseCase,
    getWishlistViewModeUseCase: GetWishlistViewModeUseCase,
    private val setWishlistViewModeUseCase: SetWishlistViewModeUseCase,
) : ViewModel() {
    private val _uiEffect = Channel<WishlistUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // Screen-lifetime only: a new ViewModel (the screen re-entered) starts unfiltered, and the filter is not
    // persisted. Declared before uiState, which combines it.
    private val selectedFilter = MutableStateFlow<WishlistStatusFilter>(WishlistStatusFilter.All)

    // The game the remove confirmation is open for. Only its id is kept; the state derives the game from the
    // loaded list, so the dialog closes by itself when that game is no longer in it. Declared before uiState.
    private val gameIdPendingRemoval = MutableStateFlow<Int?>(null)

    // Single source of truth: reactively observes local storage, no manually mirrored copy.
    internal val uiState: StateFlow<WishlistUiState> = combine(
        getWishlistDetailUseCase(listId)
            // Deleting a list that holds games invalidates several queries, so the flow can report it gone more
            // than once. Only the first report may navigate; every other emission has to get through.
            .distinctUntilChanged { old, new -> old == null && new == null }
            .onEach { detail ->
                // The list is gone: deleted from this screen, or from under it. This is the only place that
                // leaves the screen after a delete, so one delete pops exactly one screen.
                if (detail == null) _uiEffect.send(WishlistUiEffect.NavigateBack)
                // With no game left the filter row is gone, so a leftover filter would hide the next game
                // added with nothing on screen to explain or clear it. A filter whose status merely ran out
                // of games is kept on purpose: the user sees the filtered-empty message instead.
                if (detail != null && detail.games.isEmpty()) selectedFilter.value = WishlistStatusFilter.All
                // A game that left the list while its confirmation was open must not bring the dialog back if
                // it is saved again later.
                val pendingId = gameIdPendingRemoval.value
                val stillInList = detail?.games?.any { it.id == pendingId } == true
                if (pendingId != null && !stillInList) gameIdPendingRemoval.value = null
            },
        getWishlistViewModeUseCase(),
        selectedFilter,
        gameIdPendingRemoval
    ) { detail, viewMode, filter, pendingRemovalId ->
        if (detail == null) {
            WishlistUiState(viewMode = viewMode)
        } else {
            val allSections = detail.games.toWishlistSectionUiModel()
            val visibleSections = allSections.filteredBy(filter)
            WishlistUiState(
                listName = UiText.DynamicString(detail.list.name),
                description = detail.list.description.ifBlank { null },
                iconRes = detail.list.icon.toDrawableRes(),
                coverImagePath = detail.list.coverImagePath,
                gameCountText = UiText.PluralResource(
                    Res.plurals.game_count,
                    detail.games.size,
                    detail.games.size
                ),
                isDefaultList = detail.isDefault,
                showListOptions = !detail.isDefault,
                // Independent of showListOptions: the default list is editable too.
                showEditAction = true,
                formValues = WishlistFormUiModel(
                    name = detail.list.name,
                    description = detail.list.description,
                    icon = detail.list.icon,
                    coverImage = detail.list.coverImagePath
                ),
                viewMode = viewMode,
                filterChips = allSections.toFilterChips(filter),
                gamePendingRemoval = allSections.firstNotNullOfOrNull { section ->
                    section.games.firstOrNull { it.id == pendingRemovalId }
                },
                contentState = when {
                    allSections.isEmpty() -> WishlistContentState.Empty
                    visibleSections.isEmpty() -> WishlistContentState.FilteredEmpty
                    else -> WishlistContentState.Success(visibleSections)
                }
            )
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WishlistUiState())

    internal fun onEvent(event: WishlistUiEvent) {
        return when (event) {
            is WishlistUiEvent.OnSetAsDefault -> setAsDefault()
            is WishlistUiEvent.OnWishlistDeleted -> deleteList()
            is WishlistUiEvent.OnGameRemovalRequested -> gameIdPendingRemoval.value = event.gameId
            is WishlistUiEvent.OnGameRemovalDismissed -> gameIdPendingRemoval.value = null
            is WishlistUiEvent.OnGameRemoved -> removeGame(event.gameId)
            is WishlistUiEvent.OnListEdited -> editList(event.values)
            is WishlistUiEvent.OnViewModeToggled -> toggleViewMode()
            is WishlistUiEvent.OnStatusFilterSelected -> selectFilter(event.filter)
        }
    }

    private fun selectFilter(filter: WishlistStatusFilter) {
        selectedFilter.value = filter
    }

    private fun toggleViewMode() {
        val toggledMode = when (uiState.value.viewMode) {
            WishlistViewMode.LIST -> WishlistViewMode.GRID
            WishlistViewMode.GRID -> WishlistViewMode.LIST
        }
        viewModelScope.launch { setWishlistViewModeUseCase(toggledMode) }
    }

    private fun setAsDefault() {
        if (uiState.value.isDefaultList) return
        viewModelScope.launch {
            setDefaultListUseCase(listId)
            _uiEffect.send(
                WishlistUiEffect.ShowSnackbar(
                    message = UiText.StringResource(Res.string.default_list_set_message)
                )
            )
        }
    }

    private fun deleteList() {
        viewModelScope.launch {
            if (!deleteListUseCase(listId)) {
                _uiEffect.send(
                    WishlistUiEffect.ShowSnackbar(
                        message = UiText.StringResource(Res.string.unable_to_delete_wishlist)
                    )
                )
            }
        }
    }

    private fun editList(values: WishlistFormUiModel) {
        val currentCover = uiState.value.formValues.coverImage
        // Only the form's own cover string can tell the three cases apart: untouched, cleared, or a freshly
        // picked image (the sheet starts from the stored path, so anything else is a new pick).
        val coverImage = when (val pickedCover = values.coverImage) {
            currentCover -> CoverImageUpdate.Keep
            null -> CoverImageUpdate.Remove
            else -> CoverImageUpdate.Replace(pickedCover)
        }
        viewModelScope.launch {
            updateListUseCase(listId, values.name, values.description, values.icon, coverImage)
                .onFailure { error ->
                    _uiEffect.send(WishlistUiEffect.ShowSnackbar(error.toUiText()))
                }
        }
    }

    private fun removeGame(gameId: Int) {
        gameIdPendingRemoval.value = null
        viewModelScope.launch {
            removeGameFromListUseCase(gameId, listId)
        }
    }
}
