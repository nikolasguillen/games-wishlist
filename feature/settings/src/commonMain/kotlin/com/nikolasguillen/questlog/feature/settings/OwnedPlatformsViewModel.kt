package com.nikolasguillen.questlog.feature.settings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.ToggleOwnedPlatformUseCase
import com.nikolasguillen.questlog.core.ui.mapper.toPlatformPickerContentState
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.feature.settings.model.OwnedPlatformsUiEvent
import com.nikolasguillen.questlog.feature.settings.model.OwnedPlatformsUiState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class OwnedPlatformsViewModel(
    getKnownPlatformsUseCase: GetKnownPlatformsUseCase,
    private val getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    private val toggleOwnedPlatformUseCase: ToggleOwnedPlatformUseCase,
    private val syncPlatformCatalogUseCase: SyncPlatformCatalogUseCase
) : ViewModel() {

    internal val textFieldState = TextFieldState()

    /**
     * The selection as it stood when the screen opened, kept only to float those rows to the top.
     * `null` until the first read lands, which is what holds the list on `Loading` instead of
     * rendering once unordered and then jumping.
     */
    private val pinnedPlatformIds = MutableStateFlow<Set<Int>?>(null)

    /** True while the catalogue is being fetched, so an empty cache reads as loading rather than empty. */
    private val isSyncing = MutableStateFlow(false)

    internal val uiState: StateFlow<OwnedPlatformsUiState> = combine(
        getKnownPlatformsUseCase(),
        getSelectedPlatformIdsUseCase(),
        snapshotFlow { textFieldState.text.toString() }.distinctUntilChanged(),
        pinnedPlatformIds,
        isSyncing
    ) { known, selected, query, pinned, syncing ->
        OwnedPlatformsUiState(
            contentState = known.toPlatformPickerContentState(
                selectedIds = selected,
                query = query,
                pinnedIds = pinned,
                isSyncing = syncing
            ),
            selectedCount = selected.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OwnedPlatformsUiState()
    )

    init {
        viewModelScope.launch {
            pinnedPlatformIds.value = getSelectedPlatformIdsUseCase().first()
        }
        syncCatalog()
    }

    internal fun onEvent(event: OwnedPlatformsUiEvent) {
        when (event) {
            is OwnedPlatformsUiEvent.OnPlatformToggled -> togglePlatform(event.platformId)
            is OwnedPlatformsUiEvent.OnClearQuery -> textFieldState.clearText()
            OwnedPlatformsUiEvent.OnRetrySync -> syncCatalog()
        }
    }

    // The list renders from Room either way, so a failed sync leaves whatever is already cached rather
    // than blanking the screen. Only an empty cache reaches the user, as the Empty state -- which is also
    // what OnRetrySync retries. While the sync runs that empty cache reads as Loading, held for at least
    // MIN_LOADING_FEEDBACK_MILLIS so a retry that fails instantly still visibly did something.
    private fun syncCatalog() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                coroutineScope {
                    launch { delay(UiConstants.MIN_LOADING_FEEDBACK_MILLIS) }
                    syncPlatformCatalogUseCase()
                }
            } finally {
                isSyncing.value = false
            }
        }
    }

    /**
     * Persists on every tap: the picker has no confirm step, so there is nothing to commit later. The
     * flip happens atomically in the data layer, so the ViewModel needs no lock of its own and a tap
     * never depends on what was last rendered — which a search query narrows.
     */
    private fun togglePlatform(platformId: Int) {
        viewModelScope.launch {
            toggleOwnedPlatformUseCase(platformId)
        }
    }
}
