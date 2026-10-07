package com.nikolasguillen.questlog.feature.onboarding

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

    private fun createViewModel() = OnboardingViewModel(
        getKnownPlatformsUseCase = getKnownPlatformsUseCase,
        getSelectedPlatformIdsUseCase = getSelectedPlatformIdsUseCase,
        toggleOwnedPlatformUseCase = toggleOwnedPlatformUseCase,
        syncPlatformCatalogUseCase = syncPlatformCatalogUseCase,
        completeOnboardingUseCase = completeOnboardingUseCase
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
    fun `resolving the device facts builds the page list ending with the platforms step`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.resolveFacts()
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
}
