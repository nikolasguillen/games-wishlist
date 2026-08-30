package com.example.gameswishlist.feature.settings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.SetOwnedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.example.gameswishlist.feature.settings.mapper.toContentState
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiEvent
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class OwnedPlatformsViewModel @Inject constructor(
    getKnownPlatformsUseCase: GetKnownPlatformsUseCase,
    private val getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    private val setOwnedPlatformsUseCase: SetOwnedPlatformsUseCase,
    private val syncPlatformCatalogUseCase: SyncPlatformCatalogUseCase
) : ViewModel() {

    internal val textFieldState = TextFieldState()

    /**
     * Serialises the read-modify-write below. Each tap stores the whole set, so two taps in quick
     * succession would otherwise both read the pre-first-tap selection and the second would drop the
     * first.
     */
    private val selectionWriteLock = Mutex()

    /**
     * The selection as it stood when the screen opened, kept only to float those rows to the top.
     * `null` until the first read lands, which is what holds the list on `Loading` instead of
     * rendering once unordered and then jumping.
     */
    private val pinnedPlatformIds = MutableStateFlow<Set<Int>?>(null)

    internal val uiState: StateFlow<OwnedPlatformsUiState> = combine(
        getKnownPlatformsUseCase(),
        getSelectedPlatformIdsUseCase(),
        snapshotFlow { textFieldState.text.toString() }.distinctUntilChanged(),
        pinnedPlatformIds
    ) { known, selected, query, pinned ->
        OwnedPlatformsUiState(
            contentState = known.toContentState(
                selectedIds = selected,
                query = query,
                pinnedIds = pinned
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
        // Fire and forget: the list renders from Room either way, so a failed sync leaves whatever is
        // already cached rather than blanking the screen. Only an empty cache reaches the user, as the
        // Empty state.
        viewModelScope.launch {
            syncPlatformCatalogUseCase()
        }
    }

    internal fun onEvent(event: OwnedPlatformsUiEvent) {
        when (event) {
            is OwnedPlatformsUiEvent.OnPlatformToggled -> togglePlatform(event.platformId)
            is OwnedPlatformsUiEvent.OnClearQuery -> textFieldState.clearText()
        }
    }

    /**
     * Persists on every tap: the picker has no confirm step, so there is nothing to commit later.
     * The stored selection is re-read rather than taken from [uiState], so the write is applied to
     * what is actually saved instead of to what was last rendered — which a search query narrows.
     */
    private fun togglePlatform(platformId: Int) {
        viewModelScope.launch {
            selectionWriteLock.withLock {
                val current = getSelectedPlatformIdsUseCase().first()
                val updated = if (platformId in current) {
                    current - platformId
                } else {
                    current + platformId
                }
                setOwnedPlatformsUseCase(updated)
            }
        }
    }
}
