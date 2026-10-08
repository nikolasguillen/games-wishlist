package com.nikolasguillen.questlog.feature.radar

import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.radar.GetRadarTimelineUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RadarEntry
import com.nikolasguillen.questlog.core.model.RadarTimelineSection
import com.nikolasguillen.questlog.core.model.ReleaseBucket
import com.nikolasguillen.questlog.core.model.ReleaseDate
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.radar.model.RadarContentState
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEffect
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEvent
import com.nikolasguillen.questlog.feature.radar.resources.Res
import com.nikolasguillen.questlog.feature.radar.resources.release_notification_disabled_message
import com.nikolasguillen.questlog.feature.radar.resources.release_notification_enabled_message
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [RadarViewModel.uiState] is shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so
 * [createViewModel] collects it in the background the way the screen does via
 * `collectAsStateWithLifecycle` -- the same setup as `ListsViewModelTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getRadarTimelineUseCase = mockk<GetRadarTimelineUseCase>()
    private val getReleaseNotificationGameIdsUseCase = mockk<GetReleaseNotificationGameIdsUseCase>()
    private val setReleaseNotificationEnabledUseCase = mockk<SetReleaseNotificationEnabledUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getReleaseNotificationGameIdsUseCase() } returns flowOf(emptySet())
        coEvery { setReleaseNotificationEnabledUseCase(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(remindersAvailable: Boolean = true) = RadarViewModel(
        getRadarTimelineUseCase,
        getReleaseNotificationGameIdsUseCase,
        setReleaseNotificationEnabledUseCase,
        mockk<ReleaseRemindersAvailability> { every { isAvailable } returns remindersAvailable }
    )

    private fun TestScope.createViewModel(
        sections: List<RadarTimelineSection>,
        notificationEnabledGameIds: Set<Int> = emptySet(),
        remindersAvailable: Boolean = true
    ): RadarViewModel {
        every { getRadarTimelineUseCase() } returns flowOf(sections)
        every { getReleaseNotificationGameIdsUseCase() } returns flowOf(notificationEnabledGameIds)
        return buildViewModel(remindersAvailable).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    private fun radarEntry(gameId: Int = 1, platformId: Int = 6) = RadarEntry(
        game = Game(id = gameId, name = "Hollow Knight: Silksong"),
        releaseDate = ReleaseDate(date = 1_000L, platformId = platformId, platformName = "Platform $platformId")
    )

    @Test
    fun `uiState starts in Loading before the first emission`() {
        every { getRadarTimelineUseCase() } returns flowOf(emptyList())

        val viewModel = buildViewModel()

        assertEquals(RadarContentState.Loading, viewModel.uiState.value.contentState)
    }

    @Test
    fun `uiState maps a populated timeline into a Success section list`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry()))

        val viewModel = createViewModel(listOf(section))

        val content = viewModel.uiState.value.contentState as RadarContentState.Success
        assertEquals(1, content.sections.size)
        assertEquals("Hollow Knight: Silksong", content.sections.single().entries.single().title)
    }

    @Test
    fun `uiState reflects an empty timeline as Empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel(emptyList())

        assertEquals(RadarContentState.Empty, viewModel.uiState.value.contentState)
    }

    @Test
    fun `isNotificationEnabled reaches both rows of a multi-platform game`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(
            bucket = ReleaseBucket.THIS_WEEK,
            entries = listOf(radarEntry(gameId = 1, platformId = 6), radarEntry(gameId = 1, platformId = 167))
        )

        val viewModel = createViewModel(listOf(section), notificationEnabledGameIds = setOf(1))

        val entries = (viewModel.uiState.value.contentState as RadarContentState.Success).sections.single().entries
        assertEquals(2, entries.size)
        assertTrue(entries.all { it.isNotificationEnabled })
    }

    @Test
    fun `ToggleReleaseNotification calls the use case with the toggled value`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry(gameId = 1)))
        val viewModel = createViewModel(listOf(section), notificationEnabledGameIds = emptySet())

        viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(1))
        advanceUntilIdle()

        coVerify(exactly = 1) { setReleaseNotificationEnabledUseCase(1, true) }
    }

    @Test
    fun `enabling a toggle requests notification permission and shows a snackbar`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry(gameId = 1)))
        val viewModel = createViewModel(listOf(section), notificationEnabledGameIds = emptySet())

        val effects = mutableListOf<RadarUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(1))
        advanceUntilIdle()

        assertEquals(
            listOf(
                RadarUiEffect.RequestNotificationPermission,
                RadarUiEffect.ShowSnackbar(
                    UiText.StringResource(Res.string.release_notification_enabled_message, "Hollow Knight: Silksong")
                )
            ),
            effects
        )
        effectJob.cancel()
    }

    @Test
    fun `disabling a toggle does not request notification permission but shows a snackbar`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry(gameId = 1)))
        val viewModel = createViewModel(listOf(section), notificationEnabledGameIds = setOf(1))

        val effects = mutableListOf<RadarUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(1))
        advanceUntilIdle()

        coVerify(exactly = 1) { setReleaseNotificationEnabledUseCase(1, false) }
        assertEquals(
            listOf(
                RadarUiEffect.ShowSnackbar(
                    UiText.StringResource(Res.string.release_notification_disabled_message, "Hollow Knight: Silksong")
                )
            ),
            effects
        )
        effectJob.cancel()
    }

    @Test
    fun `a platform without release reminders says so in the state and never shows the bell`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry(gameId = 1)))
        val viewModel = createViewModel(listOf(section), remindersAvailable = false)

        assertFalse(viewModel.uiState.value.releaseRemindersAvailable)
    }

    @Test
    fun `ToggleReleaseNotification does nothing on a platform without release reminders`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(bucket = ReleaseBucket.THIS_WEEK, entries = listOf(radarEntry(gameId = 1)))
        val viewModel = createViewModel(listOf(section), remindersAvailable = false)

        val effects = mutableListOf<RadarUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(1))
        advanceUntilIdle()

        coVerify(exactly = 0) { setReleaseNotificationEnabledUseCase(any(), any()) }
        assertEquals(emptyList<RadarUiEffect>(), effects)
        effectJob.cancel()
    }
}
