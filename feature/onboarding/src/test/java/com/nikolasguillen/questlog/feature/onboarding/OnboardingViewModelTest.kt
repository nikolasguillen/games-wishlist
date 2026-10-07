package com.nikolasguillen.questlog.feature.onboarding

import com.nikolasguillen.questlog.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingContentState
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingPage
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEffect
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingUiEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val infoPages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.Discover,
        OnboardingPage.Lists,
        OnboardingPage.Radar
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { completeOnboardingUseCase() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = OnboardingViewModel(completeOnboardingUseCase)

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
    fun `resolving the device facts builds the informational pages`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.resolveFacts()
        advanceUntilIdle()

        assertEquals(OnboardingContentState.Ready(infoPages), viewModel.uiState.value.contentState)
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
}
