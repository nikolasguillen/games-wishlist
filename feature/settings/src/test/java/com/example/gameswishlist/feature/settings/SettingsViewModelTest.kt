package com.example.gameswishlist.feature.settings

import com.example.gameswishlist.core.common.AppVersionProvider
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.example.gameswishlist.core.domain.usecase.translation.ObserveDescriptionTranslationEnabledUseCase
import com.example.gameswishlist.core.domain.usecase.translation.SetDescriptionTranslationEnabledUseCase
import com.example.gameswishlist.core.model.TranslationModelStatus
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the on-device translation row: hidden until the async support check resolves, reflects the
 * stored toggle once supported, and writes the flipped value through on tap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val appVersionProvider = mockk<AppVersionProvider> { every { versionName } returns "1.0" }
    private val getSelectedPlatformsUseCase = mockk<GetSelectedPlatformsUseCase>()
    private val observeDescriptionTranslationEnabledUseCase =
        mockk<ObserveDescriptionTranslationEnabledUseCase>()
    private val setDescriptionTranslationEnabledUseCase = mockk<SetDescriptionTranslationEnabledUseCase>()
    private val getTranslationModelStatusUseCase = mockk<GetTranslationModelStatusUseCase>()

    private val isTranslationEnabled = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSelectedPlatformsUseCase() } returns flowOf(emptyList())
        every { observeDescriptionTranslationEnabledUseCase() } returns isTranslationEnabled
        coEvery { setDescriptionTranslationEnabledUseCase(any()) } answers {
            isTranslationEnabled.value = firstArg()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        appVersionProvider = appVersionProvider,
        getSelectedPlatformsUseCase = getSelectedPlatformsUseCase,
        observeDescriptionTranslationEnabledUseCase = observeDescriptionTranslationEnabledUseCase,
        setDescriptionTranslationEnabledUseCase = setDescriptionTranslationEnabledUseCase,
        getTranslationModelStatusUseCase = getTranslationModelStatusUseCase
    )

    // uiState is WhileSubscribed(5000): nothing upstream runs until something collects it.
    private fun CoroutineScope.collectStates(
        viewModel: SettingsViewModel,
        states: MutableList<SettingsUiState>
    ) = launch { viewModel.uiState.collect { states.add(it) } }

    @Test
    fun `row stays hidden when the device does not support translation`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.UNSUPPORTED

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertFalse(states.last().isTranslationSupported)
        job.cancel()
    }

    @Test
    fun `row reflects the stored toggle once the device is confirmed supported`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.READY
        isTranslationEnabled.value = true

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertTrue(states.last().isTranslationSupported)
        assertTrue(states.last().isTranslationEnabled)
        job.cancel()
    }

    @Test
    fun `toggling the row writes the flipped value through`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.READY
        isTranslationEnabled.value = false

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.SetDescriptionTranslation(true))
        advanceUntilIdle()

        coVerify { setDescriptionTranslationEnabledUseCase(true) }
        assertTrue(states.last().isTranslationEnabled)
        job.cancel()
    }
}
