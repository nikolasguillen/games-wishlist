package com.nikolasguillen.questlog.feature.onboarding

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.ToggleOwnedPlatformUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.nikolasguillen.questlog.core.ui.mapper.toPlatformPickerContentState
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.feature.onboarding.mapper.buildOnboardingPages
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEffect
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEvent
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiState
import com.nikolasguillen.questlog.feature.onboarding.model.ReminderStepState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class OnboardingViewModel(
    getKnownPlatformsUseCase: GetKnownPlatformsUseCase,
    private val getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase,
    private val toggleOwnedPlatformUseCase: ToggleOwnedPlatformUseCase,
    private val syncPlatformCatalogUseCase: SyncPlatformCatalogUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val releaseRemindersAvailability: ReleaseRemindersAvailability
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    internal val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    internal val textFieldState = TextFieldState()

    /**
     * The selection as it stood when the flow opened, kept only to float those rows to the top — empty on a
     * first launch, the current picks on a replay. `null` until the first read lands, which is what holds
     * the picker on `Loading` instead of rendering once unordered and then jumping.
     */
    private val pinnedPlatformIds = MutableStateFlow<Set<Int>?>(null)

    /** True while the catalogue is being fetched, so an empty cache reads as loading rather than empty. */
    private val isSyncing = MutableStateFlow(false)

    // Completing is guarded rather than idempotent-by-effect: a second tap would otherwise send a second
    // Finished, and the host would pop two screens.
    private var isCompleting = false

    init {
        viewModelScope.launch {
            pinnedPlatformIds.value = getSelectedPlatformIdsUseCase().first()
        }
        syncCatalog()

        // The picker is derived from continuously-observed sources, but the rest of the state is changed
        // by event handlers, so it is folded into the same MutableStateFlow rather than exposed separately.
        viewModelScope.launch {
            combine(
                getKnownPlatformsUseCase(),
                getSelectedPlatformIdsUseCase(),
                snapshotFlow { textFieldState.text.toString() }.distinctUntilChanged(),
                pinnedPlatformIds,
                isSyncing
            ) { known, selected, query, pinned, syncing ->
                known.toPlatformPickerContentState(
                    selectedIds = selected,
                    query = query,
                    pinnedIds = pinned,
                    isSyncing = syncing
                ) to selected.size
            }.collect { (picker, selectedCount) ->
                _uiState.update {
                    it.copy(platformPicker = picker, selectedPlatformCount = selectedCount)
                }
            }
        }
    }

    internal fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NotificationFactsResolved -> resolveFacts(event)
            is OnboardingUiEvent.PlatformToggled -> togglePlatform(event.platformId)
            OnboardingUiEvent.ClearPlatformQuery -> textFieldState.clearText()
            OnboardingUiEvent.RetryPlatformSync -> syncCatalog()
            OnboardingUiEvent.AllowNotificationsClicked -> {
                viewModelScope.launch { _uiEffect.send(OnboardingUiEffect.RequestNotificationPermission) }
            }

            OnboardingUiEvent.NotNowClicked -> setReminderStep(ReminderStepState.Declined)
            is OnboardingUiEvent.NotificationPermissionResult -> setReminderStep(
                if (event.granted) ReminderStepState.Granted else ReminderStepState.Declined
            )

            is OnboardingUiEvent.PermissionStateChanged -> onPermissionStateChanged(event.canDeliver)
            OnboardingUiEvent.FinishClicked, OnboardingUiEvent.SkipClicked -> complete()
        }
    }

    // Built once: the facts can change while the flow is open (the user grants the permission from system
    // settings), but a page count that shifts under the user's thumb is worse than a stale page.
    private fun resolveFacts(event: OnboardingUiEvent.NotificationFactsResolved) {
        _uiState.update { state ->
            if (state.contentState is OnboardingContentState.Ready) {
                state
            } else {
                state.copy(
                    contentState = OnboardingContentState.Ready(
                        buildOnboardingPages(
                            remindersAvailable = releaseRemindersAvailability.isAvailable,
                            requiresRuntimePermission = event.requiresRuntimePermission,
                            canDeliver = event.canDeliver
                        )
                    )
                )
            }
        }
    }

    private fun setReminderStep(step: ReminderStepState) {
        _uiState.update { it.copy(reminderStep = step) }
    }

    // Only a declined step can be overturned from outside the flow: the user can switch notifications on
    // in system settings and come back. Anything else changing underneath an undecided page would be
    // the page deciding for the user.
    private fun onPermissionStateChanged(canDeliver: Boolean) {
        _uiState.update { state ->
            if (canDeliver && state.reminderStep == ReminderStepState.Declined) {
                state.copy(reminderStep = ReminderStepState.Granted)
            } else {
                state
            }
        }
    }

    // The list renders from Room either way, so a failed sync leaves whatever is already cached. Only an
    // empty cache reaches the user, as the picker's Empty state, which is also what RetryPlatformSync
    // retries. While the sync runs that empty cache reads as Loading, held for at least
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

    // Saved the moment it is tapped: the step has no confirm button, so skipping the flow or closing the
    // app part-way never discards a pick. The flip itself is atomic in the data layer.
    private fun togglePlatform(platformId: Int) {
        viewModelScope.launch {
            toggleOwnedPlatformUseCase(platformId)
        }
    }

    private fun complete() {
        if (isCompleting) return
        isCompleting = true
        viewModelScope.launch {
            completeOnboardingUseCase()
            _uiEffect.send(OnboardingUiEffect.Finished)
        }
    }
}
