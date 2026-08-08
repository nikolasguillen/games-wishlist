package com.example.gameswishlist.feature.gamedetail

import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.RefreshGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.ToggleWishlistUseCase
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEffect
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [GameDetailViewModel] observes the game reactively from [GetGameDetailUseCase] (a Flow) and
 * only ever forwards mutations to the persistence use cases -- it does not mirror the game
 * locally. Since these mocks don't simulate a real reactive repository, the mutation tests below
 * verify the use case was called with the correctly computed argument, not a resulting uiState
 * change: that reactive wiring belongs to GameRepositoryImpl, tested separately at the data layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getGameDetailUseCase = mockk<GetGameDetailUseCase>()
    private val refreshGameDetailUseCase = mockk<RefreshGameDetailUseCase>()
    private val updateGameUseCase = mockk<UpdateGameUseCase>(relaxed = true)
    private val toggleWishlistUseCase = mockk<ToggleWishlistUseCase>(relaxed = true)
    private val getWishlistAssignmentsUseCase = mockk<GetWishlistAssignmentsUseCase>()
    private val addGameToListUseCase = mockk<AddGameToListUseCase>(relaxed = true)
    private val removeGameFromListUseCase = mockk<RemoveGameFromListUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { refreshGameDetailUseCase(any()) } returns AppResult.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testGame(
        id: Int = GAME_ID,
        priority: Priority? = null,
        status: GameStatus? = null,
        isWishlisted: Boolean = false,
        url: String? = null
    ) = Game(
        id = id,
        name = "Test Game",
        priority = priority,
        status = status,
        isWishlisted = isWishlisted,
        url = url
    )

    // uiState (and currentGameFlow) are shared with WhileSubscribed, so they only start collecting
    // once they have a subscriber -- mirrors the screen's collectAsStateWithLifecycle.
    private fun TestScope.createViewModel(game: Game = testGame()): GameDetailViewModel {
        every { getGameDetailUseCase(game.id) } returns flowOf(game)
        return GameDetailViewModel(
            gameId = game.id,
            getGameDetailUseCase = getGameDetailUseCase,
            refreshGameDetailUseCase = refreshGameDetailUseCase,
            updateGameUseCase = updateGameUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        ).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    private fun GameDetailViewModel.successState(): GameDetailContentState.Success =
        uiState.value.contentState as GameDetailContentState.Success

    @Test
    fun `observing the game maps a cached value into Success content state`() = runTest(testDispatcher) {
        val game = testGame()

        val viewModel = createViewModel(game)

        val content = viewModel.successState()
        assertEquals(game.id, content.game.id)
        assertEquals("Test Game", (content.game.name as UiText.DynamicString).value)
    }

    @Test
    fun `Error content state is shown when nothing is cached and refresh fails`() = runTest(testDispatcher) {
        every { getGameDetailUseCase(GAME_ID) } returns flowOf(null)
        coEvery { refreshGameDetailUseCase(GAME_ID) } returns AppResult.failure(RepositoryError.NoNetwork)

        val viewModel = GameDetailViewModel(
            gameId = GAME_ID,
            getGameDetailUseCase = getGameDetailUseCase,
            refreshGameDetailUseCase = refreshGameDetailUseCase,
            updateGameUseCase = updateGameUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contentState is GameDetailContentState.Error)
    }

    @Test
    fun `Success is kept when a refresh fails but the game is already cached`() = runTest(testDispatcher) {
        val game = testGame()
        coEvery { refreshGameDetailUseCase(game.id) } returns AppResult.failure(RepositoryError.NoNetwork)

        val viewModel = createViewModel(game)

        assertTrue(viewModel.uiState.value.contentState is GameDetailContentState.Success)
    }

    @Test
    fun `updatePriority persists the newly selected priority`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(priority = null))

        viewModel.onEvent(GameDetailUiEvent.UpdatePriority(Priority.HIGH.id))
        advanceUntilIdle()

        coVerify { updateGameUseCase(match { it.priority == Priority.HIGH }) }
    }

    @Test
    fun `updatePriority toggles off when the same priority is selected again`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(priority = Priority.HIGH))

        viewModel.onEvent(GameDetailUiEvent.UpdatePriority(Priority.HIGH.id))
        advanceUntilIdle()

        coVerify { updateGameUseCase(match { it.priority == null }) }
    }

    @Test
    fun `updateStatus toggles off when the same status is selected again`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(status = GameStatus.PLAYING))

        viewModel.onEvent(GameDetailUiEvent.UpdateStatus(GameStatus.PLAYING.id))
        advanceUntilIdle()

        coVerify { updateGameUseCase(match { it.status == null }) }
    }

    @Test
    fun `updateNotes persists the new notes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(GameDetailUiEvent.UpdateNotes("new notes"))
        advanceUntilIdle()

        coVerify { updateGameUseCase(match { it.notes == "new notes" }) }
    }

    @Test
    fun `toggleFavorite calls the use case with the current game`() = runTest(testDispatcher) {
        val game = testGame(isWishlisted = false)
        val viewModel = createViewModel(game)

        viewModel.onEvent(GameDetailUiEvent.ToggleFavorite)
        advanceUntilIdle()

        coVerify { toggleWishlistUseCase(match { !it.isWishlisted }) }
    }

    @Test
    fun `confirmListSelection adds and removes lists based on the selection delta`() = runTest(testDispatcher) {
        val listA = WishlistList(id = 10L, name = "Backlog")
        val listB = WishlistList(id = 20L, name = "Playing")
        every { getWishlistAssignmentsUseCase(GAME_ID) } returns flowOf(
            listOf(
                WishlistAssignment(list = listA, isAssigned = true),
                WishlistAssignment(list = listB, isAssigned = false)
            )
        )
        val viewModel = createViewModel()

        viewModel.onEvent(GameDetailUiEvent.OpenListSelector)
        advanceUntilIdle()
        viewModel.onEvent(GameDetailUiEvent.ToggleGameInList(listA.id))
        viewModel.onEvent(GameDetailUiEvent.ToggleGameInList(listB.id))
        viewModel.onEvent(GameDetailUiEvent.ConfirmListSelection)
        advanceUntilIdle()

        coVerify { removeGameFromListUseCase(GAME_ID, listA.id) }
        coVerify { addGameToListUseCase(GAME_ID, listB.id) }
        assertNull(viewModel.uiState.value.wishlistSelectorState)
    }

    @Test
    fun `ShareGame emits the url message when the game has a url`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(url = "https://example.com"))
        val effects = mutableListOf<GameDetailUiEffect>()
        val collectJob = launch { viewModel.uiEffect.toList(effects) }

        viewModel.onEvent(GameDetailUiEvent.ShareGame)
        advanceUntilIdle()

        val effect = effects.single() as GameDetailUiEffect.ShareGame
        assertTrue(effect.text is UiText.StringResource)
        collectJob.cancel()
    }

    @Test
    fun `NavigateToGame event emits a navigation effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val effects = mutableListOf<GameDetailUiEffect>()
        val collectJob = launch { viewModel.uiEffect.toList(effects) }

        viewModel.onEvent(GameDetailUiEvent.NavigateToGame(42))
        advanceUntilIdle()

        assertEquals(GameDetailUiEffect.NavigateToGame(42), effects.single())
        collectJob.cancel()
    }

    private companion object {
        const val GAME_ID = 1
    }
}
