package com.nikolasguillen.questlog.feature.settings

import com.nikolasguillen.questlog.core.domain.usecase.GetSavedGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.GetReleaseNotificationGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SetReleaseNotificationEnabledUseCase
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.ReleaseDate
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsContentState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEffect
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEvent
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.release_notifications_no_date_yet
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

private const val PC = 6

/**
 * Covers the release-notifications management list: it renders *every* saved game (not only the opted-in
 * ones), each row's [ReleaseNotificationsContentState.Success]'s `isEnabled` reflects the opt-in set, the
 * "no date yet" label, and that setting a row's state calls the use case directly with no side effect on
 * which games are listed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseNotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getSavedGamesUseCase = mockk<GetSavedGamesUseCase>()
    private val getReleaseNotificationGameIdsUseCase = mockk<GetReleaseNotificationGameIdsUseCase>()
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

    private fun TestScope.createViewModel(
        savedGames: List<Game>,
        enabledGameIds: Set<Int> = emptySet()
    ): ReleaseNotificationsViewModel {
        every { getSavedGamesUseCase() } returns flowOf(savedGames)
        every { getReleaseNotificationGameIdsUseCase() } returns flowOf(enabledGameIds)
        return ReleaseNotificationsViewModel(
            getSavedGamesUseCase,
            getReleaseNotificationGameIdsUseCase,
            getSelectedPlatformIdsUseCase,
            setReleaseNotificationEnabledUseCase
        ).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun `the list renders every saved game, not only the opted-in ones`() = runTest(testDispatcher) {
        val enabled = Game(id = 1, name = "Hollow Knight: Silksong")
        val notEnabled = Game(id = 2, name = "Elden Ring")

        val viewModel = createViewModel(savedGames = listOf(enabled, notEnabled), enabledGameIds = setOf(1))

        val content = viewModel.uiState.value.contentState as ReleaseNotificationsContentState.Success
        assertEquals(2, content.games.size)
        assertTrue(content.games.single { it.gameId == 1 }.isEnabled)
        assertFalse(content.games.single { it.gameId == 2 }.isEnabled)
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

        val viewModel = createViewModel(savedGames = listOf(game))

        val content = viewModel.uiState.value.contentState as ReleaseNotificationsContentState.Success
        assertEquals(
            UiText.StringResource(Res.string.release_notifications_no_date_yet),
            content.games.single().dateLabel
        )
    }

    @Test
    fun `SetEnabled true calls the use case with true`() = runTest(testDispatcher) {
        val game = Game(id = 1, name = "Silksong")
        val viewModel = createViewModel(savedGames = listOf(game), enabledGameIds = emptySet())

        viewModel.onEvent(ReleaseNotificationsUiEvent.SetEnabled(1, true))
        advanceUntilIdle()

        coVerify(exactly = 1) { setReleaseNotificationEnabledUseCase(1, true) }
    }

    @Test
    fun `SetEnabled false calls the use case with false`() = runTest(testDispatcher) {
        val game = Game(id = 1, name = "Silksong")
        val viewModel = createViewModel(savedGames = listOf(game), enabledGameIds = setOf(1))

        viewModel.onEvent(ReleaseNotificationsUiEvent.SetEnabled(1, false))
        advanceUntilIdle()

        coVerify(exactly = 1) { setReleaseNotificationEnabledUseCase(1, false) }
    }

    @Test
    fun `enabling a row requests notification permission`() = runTest(testDispatcher) {
        val game = Game(id = 1, name = "Silksong")
        val viewModel = createViewModel(savedGames = listOf(game), enabledGameIds = emptySet())
        val effects = mutableListOf<ReleaseNotificationsUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(ReleaseNotificationsUiEvent.SetEnabled(1, true))
        advanceUntilIdle()

        assertEquals(listOf(ReleaseNotificationsUiEffect.RequestNotificationPermission), effects)
        effectJob.cancel()
    }

    @Test
    fun `disabling a row does not request notification permission`() = runTest(testDispatcher) {
        val game = Game(id = 1, name = "Silksong")
        val viewModel = createViewModel(savedGames = listOf(game), enabledGameIds = setOf(1))
        val effects = mutableListOf<ReleaseNotificationsUiEffect>()
        val effectJob = launch { viewModel.uiEffect.collect { effects.add(it) } }
        advanceUntilIdle()

        viewModel.onEvent(ReleaseNotificationsUiEvent.SetEnabled(1, false))
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        effectJob.cancel()
    }

    @Test
    fun `no saved games at all produces Empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel(savedGames = emptyList())

        assertEquals(ReleaseNotificationsContentState.Empty, viewModel.uiState.value.contentState)
    }
}
