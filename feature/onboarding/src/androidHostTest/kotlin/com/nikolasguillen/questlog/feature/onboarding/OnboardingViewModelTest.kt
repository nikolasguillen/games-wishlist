package com.nikolasguillen.questlog.feature.onboarding

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.ToggleOwnedPlatformUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingPage
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEffect
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEvent
import com.nikolasguillen.questlog.feature.onboarding.model.ReminderStepState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the welcome flow's ViewModel: the page list it builds from the device facts the screen reports,
 * and finishing the flow — which must record completion exactly once however many times the user taps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val completeOnboardingUseCase = mockk<CompleteOnboardingUseCase>()
    private val getKnownPlatformsUseCase = mockk<GetKnownPlatformsUseCase>()
    private val getSelectedPlatformIdsUseCase = mockk<GetSelectedPlatformIdsUseCase>()
    private val toggleOwnedPlatformUseCase = mockk<ToggleOwnedPlatformUseCase>()
    private val syncPlatformCatalogUseCase = mockk<SyncPlatformCatalogUseCase>()

    // Stands in for the `owned_platforms` table, as in OwnedPlatformsViewModelTest.
    private val storedSelection = MutableStateFlow<Set<Int>>(emptySet())

    private val switch = Platform(id = 130, name = "Nintendo Switch", abbreviation = "Switch")
    private val pc = Platform(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC")
    private val ps5 = Platform(id = 167, name = "PlayStation 5", abbreviation = "PS5")
    private val catalogue = listOf(switch, pc, ps5)

    private val pagesWithoutReminders = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.Discover,
        OnboardingPage.Lists,
        OnboardingPage.Radar,
        OnboardingPage.Platforms
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { completeOnboardingUseCase() } just Runs
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
        every { getSelectedPlatformIdsUseCase() } returns storedSelection
        coEvery { toggleOwnedPlatformUseCase(any()) } answers {
            val platformId = firstArg<Int>()
            val current = storedSelection.value
            storedSelection.value = if (platformId in current) current - platformId else current + platformId
        }
        coEvery { syncPlatformCatalogUseCase() } returns AppResult.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(remindersAvailable: Boolean = true) = OnboardingViewModel(
        getKnownPlatformsUseCase = getKnownPlatformsUseCase,
        getSelectedPlatformIdsUseCase = getSelectedPlatformIdsUseCase,
        toggleOwnedPlatformUseCase = toggleOwnedPlatformUseCase,
        syncPlatformCatalogUseCase = syncPlatformCatalogUseCase,
        completeOnboardingUseCase = completeOnboardingUseCase,
        releaseRemindersAvailability = mockk<ReleaseRemindersAvailability> { every { isAvailable } returns remindersAvailable }
    )

    private fun OnboardingViewModel.platformNames(): List<String> =
        (uiState.value.platformPicker as PlatformPickerContentState.Success).platforms.map { it.name }

    private fun OnboardingViewModel.resolveFacts(
        requiresRuntimePermission: Boolean = true,
        canDeliver: Boolean = false
    ) {
        onEvent(OnboardingUiEvent.NotificationFactsResolved(requiresRuntimePermission, canDeliver))
    }

    @Test
    fun `the content state starts Loading`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(OnboardingContentState.Loading, viewModel.uiState.value.contentState)
    }

    @Test
    fun `resolving the device facts builds the tour and the platforms step`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.resolveFacts(requiresRuntimePermission = false)
        advanceUntilIdle()

        assertEquals(OnboardingContentState.Ready(pagesWithoutReminders), viewModel.uiState.value.contentState)
    }

    @Test
    fun `a second report of the device facts does not rebuild the page list`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.resolveFacts(requiresRuntimePermission = true, canDeliver = false)
        advanceUntilIdle()
        val first = viewModel.uiState.value.contentState

        viewModel.resolveFacts(requiresRuntimePermission = false, canDeliver = true)
        advanceUntilIdle()

        assertTrue(first === viewModel.uiState.value.contentState)
    }

    @Test
    fun `FinishClicked records completion once and emits Finished`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.FinishClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { completeOnboardingUseCase() }
        assertEquals(listOf(OnboardingUiEffect.Finished), effects)
        effectJob.cancel()
    }

    @Test
    fun `SkipClicked records completion once and emits Finished`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.SkipClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { completeOnboardingUseCase() }
        assertEquals(listOf(OnboardingUiEffect.Finished), effects)
        effectJob.cancel()
    }

    @Test
    fun `SkipClicked followed by FinishClicked still emits a single Finished`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.SkipClicked)
        viewModel.onEvent(OnboardingUiEvent.FinishClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { completeOnboardingUseCase() }
        assertEquals(listOf(OnboardingUiEffect.Finished), effects)
        effectJob.cancel()
    }

    @Test
    fun `tapping FinishClicked twice emits Finished only once`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.FinishClicked)
        viewModel.onEvent(OnboardingUiEvent.FinishClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { completeOnboardingUseCase() }
        assertEquals(listOf(OnboardingUiEffect.Finished), effects)
        effectJob.cancel()
    }

    @Test
    fun `refreshes the platform catalogue on open`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { syncPlatformCatalogUseCase() }
    }

    @Test
    fun `retrying shows Loading until the sync has finished, then Empty again`() = runTest(testDispatcher) {
        every { getKnownPlatformsUseCase() } returns flowOf(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(PlatformPickerContentState.Empty, viewModel.uiState.value.platformPicker)

        val gate = CompletableDeferred<AppResult<Unit>>()
        coEvery { syncPlatformCatalogUseCase() } coAnswers { gate.await() }
        viewModel.onEvent(OnboardingUiEvent.RetryPlatformSync)
        advanceUntilIdle()
        assertEquals(PlatformPickerContentState.Loading, viewModel.uiState.value.platformPicker)

        gate.complete(AppResult.success(Unit))
        advanceUntilIdle()
        assertEquals(PlatformPickerContentState.Empty, viewModel.uiState.value.platformPicker)
    }

    @Test
    fun `a retry that fails at once still shows Loading for the minimum feedback time`() =
        runTest(testDispatcher) {
            every { getKnownPlatformsUseCase() } returns flowOf(emptyList())
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(OnboardingUiEvent.RetryPlatformSync)
            runCurrent()

            assertEquals(PlatformPickerContentState.Loading, viewModel.uiState.value.platformPicker)
            advanceUntilIdle()
            assertEquals(PlatformPickerContentState.Empty, viewModel.uiState.value.platformPicker)
        }

    @Test
    fun `RetryPlatformSync syncs the catalogue again`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.RetryPlatformSync)
        advanceUntilIdle()

        coVerify(exactly = 2) { syncPlatformCatalogUseCase() }
    }

    @Test
    fun `PlatformToggled calls the toggle use case with that platform and checks the row`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(OnboardingUiEvent.PlatformToggled(switch.id))
            advanceUntilIdle()

            coVerify(exactly = 1) { toggleOwnedPlatformUseCase(switch.id) }
            val selected = (viewModel.uiState.value.platformPicker as PlatformPickerContentState.Success)
                .platforms.filter { it.isSelected }.map { it.id }
            assertEquals(listOf(switch.id), selected)
        }

    @Test
    fun `the selected platform count follows the stored selection`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.selectedPlatformCount)

        storedSelection.value = setOf(ps5.id, pc.id)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.selectedPlatformCount)
    }

    @Test
    fun `the picker stays Loading until the entry-time selection has been read`() = runTest(testDispatcher) {
        every { getSelectedPlatformIdsUseCase() } returns MutableSharedFlow()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(PlatformPickerContentState.Loading, viewModel.uiState.value.platformPicker)
    }

    @Test
    fun `a replay lists the platforms already owned first`() = runTest(testDispatcher) {
        storedSelection.value = setOf(ps5.id)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            listOf("PlayStation 5", "Nintendo Switch", "PC (Microsoft Windows)"),
            viewModel.platformNames()
        )
    }

    @Test
    fun `typing in the search field narrows the list and ClearPlatformQuery restores it`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.textFieldState.setTextAndPlaceCursorAtEnd("nintendo")
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()
            assertEquals(listOf("Nintendo Switch"), viewModel.platformNames())

            viewModel.onEvent(OnboardingUiEvent.ClearPlatformQuery)
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()
            assertEquals(3, viewModel.platformNames().size)
        }

    @Test
    fun `the reminders page is last when the device needs a permission that is not granted`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.resolveFacts(requiresRuntimePermission = true, canDeliver = false)
            advanceUntilIdle()

            assertEquals(
                OnboardingContentState.Ready(pagesWithoutReminders + OnboardingPage.Reminders),
                viewModel.uiState.value.contentState
            )
        }

    @Test
    fun `the reminders page is absent on a platform without release reminders`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(remindersAvailable = false)

            viewModel.resolveFacts(requiresRuntimePermission = true, canDeliver = false)
            advanceUntilIdle()

            val radarWithoutReminders = pagesWithoutReminders.map {
                if (it == OnboardingPage.Radar) OnboardingPage.RadarWithoutReminders else it
            }
            assertEquals(
                OnboardingContentState.Ready(radarWithoutReminders),
                viewModel.uiState.value.contentState
            )
        }

    @Test
    fun `the reminders page is absent when the device needs no runtime permission`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.resolveFacts(requiresRuntimePermission = false, canDeliver = false)
            advanceUntilIdle()

            assertEquals(
                OnboardingContentState.Ready(pagesWithoutReminders),
                viewModel.uiState.value.contentState
            )
        }

    @Test
    fun `the reminders page is absent when notifications can already be delivered`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.resolveFacts(requiresRuntimePermission = true, canDeliver = true)
            advanceUntilIdle()

            assertEquals(
                OnboardingContentState.Ready(pagesWithoutReminders),
                viewModel.uiState.value.contentState
            )
        }

    @Test
    fun `the reminders step starts Undecided`() = runTest(testDispatcher) {
        assertEquals(ReminderStepState.Undecided, createViewModel().uiState.value.reminderStep)
    }

    @Test
    fun `AllowNotificationsClicked asks the screen to show the system request`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.AllowNotificationsClicked)
        advanceUntilIdle()

        assertEquals(listOf(OnboardingUiEffect.RequestNotificationPermission), effects)
        assertEquals(ReminderStepState.Undecided, viewModel.uiState.value.reminderStep)
        effectJob.cancel()
    }

    @Test
    fun `a granted system result moves the step to Granted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(OnboardingUiEvent.NotificationPermissionResult(granted = true))
        advanceUntilIdle()

        assertEquals(ReminderStepState.Granted, viewModel.uiState.value.reminderStep)
    }

    @Test
    fun `a denied system result moves the step to Declined`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(OnboardingUiEvent.NotificationPermissionResult(granted = false))
        advanceUntilIdle()

        assertEquals(ReminderStepState.Declined, viewModel.uiState.value.reminderStep)
    }

    @Test
    fun `NotNowClicked declines without ever showing the system request`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.NotNowClicked)
        advanceUntilIdle()

        assertEquals(ReminderStepState.Declined, viewModel.uiState.value.reminderStep)
        assertTrue(effects.isEmpty())
        effectJob.cancel()
    }

    @Test
    fun `granting from system settings after declining moves the step to Granted`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(OnboardingUiEvent.NotNowClicked)

            viewModel.onEvent(OnboardingUiEvent.PermissionStateChanged(canDeliver = false))
            assertEquals(ReminderStepState.Declined, viewModel.uiState.value.reminderStep)

            viewModel.onEvent(OnboardingUiEvent.PermissionStateChanged(canDeliver = true))
            assertEquals(ReminderStepState.Granted, viewModel.uiState.value.reminderStep)
        }

    @Test
    fun `a permission state change does not touch an undecided step`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(OnboardingUiEvent.PermissionStateChanged(canDeliver = true))

        assertEquals(ReminderStepState.Undecided, viewModel.uiState.value.reminderStep)
    }

    @Test
    fun `finishing from a declined step still completes the flow`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<OnboardingUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        viewModel.onEvent(OnboardingUiEvent.NotNowClicked)
        advanceUntilIdle()

        viewModel.onEvent(OnboardingUiEvent.FinishClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { completeOnboardingUseCase() }
        assertEquals(listOf(OnboardingUiEffect.Finished), effects)
        effectJob.cancel()
    }
}
