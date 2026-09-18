package com.example.gameswishlist.feature.settings

import com.example.gameswishlist.core.common.AppVersionProvider
import com.example.gameswishlist.core.common.NetworkStatusProvider
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.translation.DownloadTranslationModelUseCase
import com.example.gameswishlist.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus
import com.example.gameswishlist.feature.settings.model.SettingsUiEffect
import com.example.gameswishlist.feature.settings.model.SettingsUiEvent
import com.example.gameswishlist.feature.settings.model.SettingsUiState
import com.example.gameswishlist.feature.settings.model.TranslationModelRowState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Covers the on-device translation model row: the state seeded from [GetTranslationModelStatusUseCase]
 * per [TranslationModelStatus] — including a DOWNLOADING status resuming live progress rather than a
 * static indeterminate spinner — the download progressing the row from Downloading to Ready, a failed
 * download landing on Failed, a second tap while a download is already running being ignored, and a tap
 * off an unmetered network being refused with [SettingsUiEffect.ShowWifiRequiredDialog] instead of
 * starting a download AICore would silently never progress.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val appVersionProvider = mockk<AppVersionProvider> { every { versionName } returns "1.0" }
    private val getSelectedPlatformsUseCase = mockk<GetSelectedPlatformsUseCase>()
    private val downloadTranslationModelUseCase = mockk<DownloadTranslationModelUseCase>()
    private val getTranslationModelStatusUseCase = mockk<GetTranslationModelStatusUseCase>()
    private val networkStatusProvider =
        mockk<NetworkStatusProvider> { every { isUnmeteredNetworkAvailable } returns true }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSelectedPlatformsUseCase() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        appVersionProvider = appVersionProvider,
        getSelectedPlatformsUseCase = getSelectedPlatformsUseCase,
        downloadTranslationModelUseCase = downloadTranslationModelUseCase,
        getTranslationModelStatusUseCase = getTranslationModelStatusUseCase,
        networkStatusProvider = networkStatusProvider
    )

    // uiState is WhileSubscribed(5000): nothing upstream runs until something collects it.
    private fun CoroutineScope.collectStates(
        viewModel: SettingsViewModel,
        states: MutableList<SettingsUiState>
    ) = launch { viewModel.uiState.collect { states.add(it) } }

    @Test
    fun `row state is seeded Hidden when the model status is UNSUPPORTED`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.UNSUPPORTED

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertEquals(TranslationModelRowState.Hidden, states.last().translationModel)
        job.cancel()
    }

    @Test
    fun `row state is seeded Downloadable when the model status is DOWNLOADABLE`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertEquals(TranslationModelRowState.Downloadable, states.last().translationModel)
        job.cancel()
    }

    @Test
    fun `a DOWNLOADING status resumes reporting progress instead of seeding a static Downloading`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADING
        coEvery { downloadTranslationModelUseCase() } returns flow {
            emit(TranslationModelDownload.InProgress(fraction = 0.3f))
            yield()
            emit(TranslationModelDownload.InProgress(fraction = 0.7f))
        }

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertEquals(
            listOf(
                TranslationModelRowState.Downloading(0.3f),
                TranslationModelRowState.Downloading(0.7f)
            ),
            states.map { it.translationModel }.distinct()
        )
        job.cancel()
    }

    @Test
    fun `row state is seeded Ready when the model status is READY`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.READY

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertEquals(TranslationModelRowState.Ready, states.last().translationModel)
        job.cancel()
    }

    @Test
    fun `DownloadTranslationModel drives the row from Downloading to Ready`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
        coEvery { downloadTranslationModelUseCase() } returns flow {
            emit(TranslationModelDownload.InProgress(fraction = null))
            yield()
            emit(TranslationModelDownload.InProgress(fraction = 0.5f))
            yield()
            emit(TranslationModelDownload.Completed)
        }

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
        advanceUntilIdle()

        // Hidden, the state's default, is not asserted here: unlike the old stateIn(WhileSubscribed)
        // pipeline, a plain MutableStateFlow gives no guarantee that a new collector observes it before
        // the two init { } launches (which run on the same test dispatcher) have already moved it on.
        assertEquals(
            listOf(
                TranslationModelRowState.Downloadable,
                TranslationModelRowState.Downloading(null),
                TranslationModelRowState.Downloading(0.5f),
                TranslationModelRowState.Ready
            ),
            states.map { it.translationModel }.distinct()
        )
        job.cancel()
    }

    @Test
    fun `a download failure yields Failed`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
        coEvery { downloadTranslationModelUseCase() } returns flowOf(TranslationModelDownload.Failed)

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
        advanceUntilIdle()

        assertEquals(TranslationModelRowState.Failed, states.last().translationModel)
        job.cancel()
    }

    @Test
    fun `a second download press while one is already in progress is ignored`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
        // An unlimited channel, not flowOf/flow{}: the test needs to land exactly one emission and
        // then hold the flow open, so the row is provably still Downloading when the second press lands.
        val downloadEvents = Channel<TranslationModelDownload>(Channel.UNLIMITED)
        coEvery { downloadTranslationModelUseCase() } returns downloadEvents.receiveAsFlow()

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
        downloadEvents.trySend(TranslationModelDownload.InProgress(fraction = null))
        advanceUntilIdle()
        assertEquals(TranslationModelRowState.Downloading(null), states.last().translationModel)

        viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
        advanceUntilIdle()

        downloadEvents.trySend(TranslationModelDownload.Completed)
        downloadEvents.close()
        advanceUntilIdle()

        coVerify(exactly = 1) { downloadTranslationModelUseCase() }
        job.cancel()
    }

    @Test
    fun `DownloadTranslationModel off an unmetered network shows the Wi-Fi required effect instead of starting`() =
        runTest {
            coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
            every { networkStatusProvider.isUnmeteredNetworkAvailable } returns false

            val viewModel = viewModel()
            val effects = mutableListOf<SettingsUiEffect>()
            val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
            val stateJob = collectStates(viewModel, mutableListOf())
            advanceUntilIdle()

            viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
            advanceUntilIdle()

            assertEquals(listOf(SettingsUiEffect.ShowWifiRequiredDialog), effects)
            coVerify(exactly = 0) { downloadTranslationModelUseCase() }
            effectJob.cancel()
            stateJob.cancel()
        }

    @Test
    fun `DownloadTranslationModel on an unmetered network starts the download as usual`() = runTest {
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
        coEvery { downloadTranslationModelUseCase() } returns flowOf(TranslationModelDownload.Completed)

        val viewModel = viewModel()
        val states = mutableListOf<SettingsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.DownloadTranslationModel)
        advanceUntilIdle()

        assertEquals(TranslationModelRowState.Ready, states.last().translationModel)
        coVerify(exactly = 1) { downloadTranslationModelUseCase() }
        job.cancel()
    }
}
