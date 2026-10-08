package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.domain.usecase.settings.GetAppearanceModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetOnboardingCompletedUseCase
import com.nikolasguillen.questlog.core.model.AppearanceMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * [RootViewModel] is what the app root reads before it can draw anything: the theme, and whether the welcome
 * flow comes first. Its one subtle promise is that "not read yet" is `null`, never `false` - a `false` would
 * flash the welcome flow at a returning user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAppearanceModeUseCase = mockk<GetAppearanceModeUseCase>()
    private val getOnboardingCompletedUseCase = mockk<GetOnboardingCompletedUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = RootViewModel(getAppearanceModeUseCase, getOnboardingCompletedUseCase)

    @Test
    fun `nothing is known before the stored values have been read`() = runTest(testDispatcher) {
        every { getAppearanceModeUseCase() } returns flowOf(AppearanceMode.DARK)
        every { getOnboardingCompletedUseCase() } returns flowOf(true)

        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.onboardingCompleted)
        assertEquals(AppearanceMode.SYSTEM, viewModel.uiState.value.appearanceMode)
    }

    @Test
    fun `the stored onboarding flag is exposed once it has been read`() = runTest(testDispatcher) {
        every { getAppearanceModeUseCase() } returns flowOf(AppearanceMode.SYSTEM)
        every { getOnboardingCompletedUseCase() } returns flowOf(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun `a flag that is not completed is false, not null`() = runTest(testDispatcher) {
        every { getAppearanceModeUseCase() } returns flowOf(AppearanceMode.SYSTEM)
        every { getOnboardingCompletedUseCase() } returns flowOf(false)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun `the onboarding flag is read once and later changes do not move the start destination`() =
        runTest(testDispatcher) {
            val flag = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(false) }
            every { getAppearanceModeUseCase() } returns flowOf(AppearanceMode.SYSTEM)
            every { getOnboardingCompletedUseCase() } returns flag

            val viewModel = createViewModel()
            advanceUntilIdle()
            flag.emit(true)
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.onboardingCompleted)
        }

    @Test
    fun `the stored appearance is exposed and every change passes through`() = runTest(testDispatcher) {
        val appearance = MutableSharedFlow<AppearanceMode>(replay = 1).apply { tryEmit(AppearanceMode.LIGHT) }
        every { getAppearanceModeUseCase() } returns appearance
        every { getOnboardingCompletedUseCase() } returns flowOf(true)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(AppearanceMode.LIGHT, viewModel.uiState.value.appearanceMode)

        appearance.emit(AppearanceMode.DARK)
        advanceUntilIdle()
        assertEquals(AppearanceMode.DARK, viewModel.uiState.value.appearanceMode)
    }
}
