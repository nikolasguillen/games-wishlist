package com.example.gameswishlist.feature.gamedetail

import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
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
import com.example.gameswishlist.core.model.WishlistConstants
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
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getGameDetailUseCase = mockk<GetGameDetailUseCase>()
    private val updateGameUseCase = mockk<UpdateGameUseCase>(relaxed = true)
    private val toggleWishlistUseCase = mockk<ToggleWishlistUseCase>(relaxed = true)
    private val getWishlistAssignmentsUseCase = mockk<GetWishlistAssignmentsUseCase>()
    private val addGameToListUseCase = mockk<AddGameToListUseCase>(relaxed = true)
    private val removeGameFromListUseCase = mockk<RemoveGameFromListUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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

    private fun TestScope.createViewModel(game: Game = testGame()): GameDetailViewModel {
        coEvery { getGameDetailUseCase(game.id) } returns AppResult.Success(game)
        return GameDetailViewModel(
            gameId = game.id,
            getGameDetailUseCase = getGameDetailUseCase,
            updateGameUseCase = updateGameUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        ).also { advanceUntilIdle() }
    }

    private fun GameDetailViewModel.successState(): GameDetailContentState.Success =
        uiState.value.contentState as GameDetailContentState.Success

    @Test
    fun `loadGame maps a successful result into Success content state`() = runTest(testDispatcher) {
        val game = testGame()

        val viewModel = createViewModel(game)

        val content = viewModel.successState()
        assertEquals(game.id, content.game.id)
        assertEquals("Test Game", (content.game.name as UiText.DynamicString).value)
    }

    @Test
    fun `loadGame maps a failure into Error content state`() = runTest(testDispatcher) {
        coEvery { getGameDetailUseCase(GAME_ID) } returns AppResult.Failure(RepositoryError.NoNetwork)

        val viewModel = GameDetailViewModel(
            gameId = GAME_ID,
            getGameDetailUseCase = getGameDetailUseCase,
            updateGameUseCase = updateGameUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contentState is GameDetailContentState.Error)
    }

    @Test
    fun `updatePriority sets the selected priority and persists it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(priority = null))

        viewModel.onEvent(GameDetailUiEvent.UpdatePriority(Priority.HIGH.id))
        advanceUntilIdle()

        val priorities = viewModel.successState().game.personalDetails.availablePriorities
        assertTrue(priorities.first { it.id == Priority.HIGH.id }.selected)
        coVerify { updateGameUseCase(match { it.priority == Priority.HIGH }) }
    }

    @Test
    fun `updatePriority toggles off when the same priority is selected again`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(priority = Priority.HIGH))

        viewModel.onEvent(GameDetailUiEvent.UpdatePriority(Priority.HIGH.id))
        advanceUntilIdle()

        val priorities = viewModel.successState().game.personalDetails.availablePriorities
        assertTrue(priorities.none { it.selected })
        coVerify { updateGameUseCase(match { it.priority == null }) }
    }

    @Test
    fun `updateStatus toggles off when the same status is selected again`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(status = GameStatus.PLAYING))

        viewModel.onEvent(GameDetailUiEvent.UpdateStatus(GameStatus.PLAYING.id))
        advanceUntilIdle()

        val statuses = viewModel.successState().game.personalDetails.availableStatuses
        assertTrue(statuses.none { it.selected })
        coVerify { updateGameUseCase(match { it.status == null }) }
    }

    @Test
    fun `toggleFavorite flips isWishlisted and calls the use case with the previous state`() = runTest(testDispatcher) {
        val viewModel = createViewModel(testGame(isWishlisted = false))

        viewModel.onEvent(GameDetailUiEvent.ToggleFavorite)
        advanceUntilIdle()

        assertTrue(viewModel.successState().game.isWishlisted)
        coVerify { toggleWishlistUseCase(match { !it.isWishlisted }) }
    }

    @Test
    fun `updateNotes debounces persistence and coalesces rapid edits`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(GameDetailUiEvent.UpdateNotes("h"))
        viewModel.onEvent(GameDetailUiEvent.UpdateNotes("he"))
        viewModel.onEvent(GameDetailUiEvent.UpdateNotes("hel"))
        advanceTimeBy(499)
        coVerify(exactly = 0) { updateGameUseCase(any()) }

        advanceTimeBy(2)
        advanceUntilIdle()
        coVerify(exactly = 1) { updateGameUseCase(match { it.notes == "hel" }) }
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
    fun `confirmListSelection syncs isWishlisted when the default list is toggled on`() = runTest(testDispatcher) {
        val defaultList = WishlistList(id = WishlistConstants.DEFAULT_WISHLIST_ID, name = "Wishlist")
        every { getWishlistAssignmentsUseCase(GAME_ID) } returns flowOf(
            listOf(WishlistAssignment(list = defaultList, isAssigned = false))
        )
        val viewModel = createViewModel(testGame(isWishlisted = false))

        viewModel.onEvent(GameDetailUiEvent.OpenListSelector)
        advanceUntilIdle()
        viewModel.onEvent(GameDetailUiEvent.ToggleGameInList(defaultList.id))
        viewModel.onEvent(GameDetailUiEvent.ConfirmListSelection)
        advanceUntilIdle()

        assertTrue(viewModel.successState().game.isWishlisted)
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
