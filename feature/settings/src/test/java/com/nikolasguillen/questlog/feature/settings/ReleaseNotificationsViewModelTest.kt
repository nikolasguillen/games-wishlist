package com.nikolasguillen.questlog.feature.settings

import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetGamesWithReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.ReleaseDate
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsContentState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEvent
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
import org.junit.Before
import org.junit.Test

private const val PC = 6

/** Covers the release-notifications management list: rendering, the "no date yet" label, and turning a row off. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseNotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getGamesWithReleaseNotificationsUseCase = mockk<GetGamesWithReleaseNotificationsUseCase>()
    private val getSelectedPlatformIdsUseCase = mockk<GetSelectedPlatformIdsUseCase>()
    private val setReleaseNotificationEnabledUseCase = mockk<SetReleaseNotificationEnabledUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSelectedPlatformIdsUseCase() } returns flowOf(emptySet())
        coEvery { setReleaseNotificationEnabledUseCase(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(games: List<Game>): ReleaseNotificationsViewModel {
        every { getGamesWithReleaseNotificationsUseCase() } returns flowOf(games)
        return ReleaseNotificationsViewModel(
            getGamesWithReleaseNotificationsUseCase,
            getSelectedPlatformIdsUseCase,
            setReleaseNotificationEnabledUseCase
        ).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun `the list renders from the combined use-case flows`() = runTest(testDispatcher) {
        val game = Game(
            id = 1,
            name = "Hollow Knight: Silksong",
            releaseDates = listOf(ReleaseDate(date = 1_000L, platformId = PC, platformName = "PC"))
        )

        val viewModel = createViewModel(listOf(game))

        val content = viewModel.uiState.value.contentState as ReleaseNotificationsContentState.Success
        assertEquals(1, content.games.size)
        assertEquals("Hollow Knight: Silksong", content.games.single().title)
    }

    @Test
    fun `a game with an imprecise date shows the no-date-yet label`() = runTest(testDispatcher) {
        val game = Game(
            id = 1,
            name = "Grand Theft Auto VI",
            releaseDates = listOf(
                ReleaseDate(date = 1_000L, platformId = PC, platformName = "PC", precision = DatePrecision.QUARTER)
            )
        )

        val viewModel = createViewModel(listOf(game))

        val content = viewModel.uiState.value.contentState as ReleaseNotificationsContentState.Success
        assertEquals(
            UiText.StringResource(R.string.release_notifications_no_date_yet),
            content.games.single().dateLabel
        )
    }

    @Test
    fun `toggling a row off calls the use case with false`() = runTest(testDispatcher) {
        val game = Game(id = 1, name = "Silksong")
        val viewModel = createViewModel(listOf(game))

        viewModel.onEvent(ReleaseNotificationsUiEvent.ToggleOff(1))
        advanceUntilIdle()

        coVerify(exactly = 1) { setReleaseNotificationEnabledUseCase(1, false) }
    }

    @Test
    fun `an empty opt-in set produces Empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel(emptyList())

        assertEquals(ReleaseNotificationsContentState.Empty, viewModel.uiState.value.contentState)
    }
}
