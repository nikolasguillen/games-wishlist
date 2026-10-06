package com.nikolasguillen.questlog.feature.wishlist

import com.nikolasguillen.questlog.core.domain.model.CoverImageUpdate
import com.nikolasguillen.questlog.core.domain.model.WishlistDetail
import com.nikolasguillen.questlog.core.domain.usecase.list.DeleteListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetDefaultListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.UpdateListUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.model.WishlistList
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.model.WishlistFormUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val LIST_ID = 3L

/**
 * Covers how [WishlistViewModel] turns a [WishlistDetail] into the header fields (description, cover, game
 * count), the flags the screen renders from (the "Default" badge, the options menu and the edit action), what
 * choosing "Set as default" does, how a refused or successful delete is reported, and how editing a list
 * turns the form's cover into a keep/remove/replace update and reports a failure to save it.
 *
 * [WishlistViewModel.uiState] is shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so
 * [createViewModel] collects it in the background, the same way the screen does via
 * `collectAsStateWithLifecycle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getWishlistDetailUseCase = mockk<GetWishlistDetailUseCase>()
    private val deleteListUseCase = mockk<DeleteListUseCase>()
    private val removeGameFromListUseCase = mockk<RemoveGameFromListUseCase>(relaxed = true)
    private val setDefaultListUseCase = mockk<SetDefaultListUseCase>(relaxed = true)
    private val updateListUseCase = mockk<UpdateListUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(isDefault: Boolean) = WishlistDetail(
        list = WishlistList(id = LIST_ID, name = "Co-op Picks"),
        games = emptyList(),
        isDefault = isDefault
    )

    private fun detailWithCover(isDefault: Boolean, coverImagePath: String? = "/covers/old.jpg") = WishlistDetail(
        list = WishlistList(
            id = LIST_ID,
            name = "Co-op Picks",
            description = "Controllers in hand.",
            icon = WishlistIcon.MULTIPLAYER,
            coverImagePath = coverImagePath
        ),
        games = emptyList(),
        isDefault = isDefault
    )

    private fun stubUpdate(result: AppResult<Unit> = AppResult.success(Unit)) {
        coEvery { updateListUseCase(any(), any(), any(), any(), any()) } returns result
    }

    private fun newViewModel() = WishlistViewModel(
        listId = LIST_ID,
        getWishlistDetailUseCase = getWishlistDetailUseCase,
        deleteListUseCase = deleteListUseCase,
        removeGameFromListUseCase = removeGameFromListUseCase,
        setDefaultListUseCase = setDefaultListUseCase,
        updateListUseCase = updateListUseCase
    )

    private fun TestScope.createViewModel(detail: WishlistDetail): WishlistViewModel =
        createViewModel(flowOf(detail))

    private fun TestScope.createViewModel(details: Flow<WishlistDetail?>): WishlistViewModel {
        every { getWishlistDetailUseCase(LIST_ID) } returns details
        return newViewModel().also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    private fun TestScope.newViewModelWith(details: Flow<WishlistDetail?>): WishlistViewModel {
        every { getWishlistDetailUseCase(LIST_ID) } returns details
        return newViewModel()
    }

    private fun TestScope.collectEffects(viewModel: WishlistViewModel): List<WishlistUiEffect> {
        val effects = mutableListOf<WishlistUiEffect>()
        // Unconfined, because advanceUntilIdle() stops once only background work is left, which would
        // leave the collector's resumption after a send unrun.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }
        return effects
    }

    @Test
    fun `before the detail loads neither the badge nor the menu shows`() = runTest(testDispatcher) {
        every { getWishlistDetailUseCase(LIST_ID) } returns MutableSharedFlow()

        val state = newViewModel().uiState.value

        assertEquals(WishlistUiState(), state)
        assertFalse(state.isDefaultList)
        assertFalse(state.showListOptions)
    }

    @Test
    fun `the header carries the list's description, cover and game count`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            detail(isDefault = false).copy(
                list = WishlistList(
                    id = LIST_ID,
                    name = "Co-op Picks",
                    description = "Controllers in hand.",
                    icon = WishlistIcon.MULTIPLAYER,
                    coverImagePath = "/covers/co-op.jpg"
                )
            )
        )

        val state = viewModel.uiState.value
        assertEquals("Controllers in hand.", state.description)
        assertEquals(WishlistIcon.MULTIPLAYER.toDrawableRes(), state.iconRes)
        assertEquals("/covers/co-op.jpg", state.coverImagePath)
        assertEquals(UiText.PluralResource(R.plurals.game_count, 0, 0), state.gameCountText)
    }

    @Test
    fun `a blank description is reported as none`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))

        assertNull(viewModel.uiState.value.description)
    }

    @Test
    fun `the default list shows the badge and no options menu`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = true))

        assertTrue(viewModel.uiState.value.isDefaultList)
        assertFalse(viewModel.uiState.value.showListOptions)
    }

    @Test
    fun `a non-default list shows the options menu and no badge`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))

        assertFalse(viewModel.uiState.value.isDefaultList)
        assertTrue(viewModel.uiState.value.showListOptions)
    }

    @Test
    fun `OnSetAsDefault makes the list the default and confirms with a snackbar`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))
        val effects = collectEffects(viewModel)

        viewModel.onEvent(WishlistUiEvent.OnSetAsDefault)
        advanceUntilIdle()

        coVerify(exactly = 1) { setDefaultListUseCase(LIST_ID) }
        assertEquals(
            listOf<WishlistUiEffect>(
                WishlistUiEffect.ShowSnackbar(UiText.StringResource(R.string.default_list_set_message))
            ),
            effects
        )
    }

    @Test
    fun `OnSetAsDefault on the list that is already the default does nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = true))
        val effects = collectEffects(viewModel)

        viewModel.onEvent(WishlistUiEvent.OnSetAsDefault)
        advanceUntilIdle()

        coVerify(exactly = 0) { setDefaultListUseCase(any()) }
        assertEquals(emptyList<WishlistUiEffect>(), effects)
    }

    @Test
    fun `a refused delete shows a snackbar and stays on the screen`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))
        val effects = collectEffects(viewModel)
        coEvery { deleteListUseCase(LIST_ID) } returns false

        viewModel.onEvent(WishlistUiEvent.OnWishlistDeleted)
        advanceUntilIdle()

        assertEquals(
            listOf<WishlistUiEffect>(
                WishlistUiEffect.ShowSnackbar(UiText.StringResource(R.string.unable_to_delete_wishlist))
            ),
            effects
        )
    }

    /**
     * Leaving the screen follows the list disappearing from storage, not the delete call itself: sending
     * `NavigateBack` from both made one delete pop two screens (see the next test).
     */
    @Test
    fun `a successful delete does not navigate by itself`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))
        val effects = collectEffects(viewModel)
        coEvery { deleteListUseCase(LIST_ID) } returns true

        viewModel.onEvent(WishlistUiEvent.OnWishlistDeleted)
        advanceUntilIdle()

        assertEquals(emptyList<WishlistUiEffect>(), effects)
    }

    /**
     * Deleting a list that holds games invalidates more than one query, so the detail flow can report the
     * list as gone several times in a row. Each report used to pop a screen.
     */
    @Test
    fun `a list that vanishes navigates back once, however often it is reported gone`() =
        runTest(testDispatcher) {
            val viewModel = newViewModelWith(flowOf(detail(isDefault = false), null, null))
            val effects = collectEffects(viewModel)
            backgroundScope.launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            assertEquals(listOf<WishlistUiEffect>(WishlistUiEffect.NavigateBack), effects)
        }

    @Test
    fun `updates to the list keep reaching the state after it was reported gone once or never`() =
        runTest(testDispatcher) {
            val details = MutableSharedFlow<WishlistDetail?>()
            val viewModel = createViewModel(details)

            details.emit(detail(isDefault = false))
            advanceUntilIdle()
            details.emit(detail(isDefault = true))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isDefaultList)
        }

    @Test
    fun `before the detail loads the edit action is hidden and the form is empty`() = runTest(testDispatcher) {
        every { getWishlistDetailUseCase(LIST_ID) } returns MutableSharedFlow()

        val state = newViewModel().uiState.value

        assertFalse(state.showEditAction)
        assertEquals(WishlistFormUiModel(), state.formValues)
    }

    @Test
    fun `the form is pre-filled with the list's stored values`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))

        assertEquals(
            WishlistFormUiModel(
                name = "Co-op Picks",
                description = "Controllers in hand.",
                icon = WishlistIcon.MULTIPLAYER,
                coverImage = "/covers/old.jpg"
            ),
            viewModel.uiState.value.formValues
        )
    }

    @Test
    fun `a non-default list shows the edit action next to the options menu`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))

        assertTrue(viewModel.uiState.value.showEditAction)
        assertTrue(viewModel.uiState.value.showListOptions)
    }

    /** The default list hides "Set as default" and "Delete", but it stays editable. */
    @Test
    fun `the default list shows the edit action although it has no options menu`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = true))

        assertTrue(viewModel.uiState.value.showEditAction)
        assertFalse(viewModel.uiState.value.showListOptions)
        assertTrue(viewModel.uiState.value.isDefaultList)
    }

    @Test
    fun `OnListEdited with an untouched cover keeps it and passes the edited fields`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))
        stubUpdate()

        viewModel.onEvent(
            WishlistUiEvent.OnListEdited(
                viewModel.uiState.value.formValues.copy(
                    name = "Renamed",
                    description = "",
                    icon = null
                )
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { updateListUseCase(LIST_ID, "Renamed", "", null, CoverImageUpdate.Keep) }
    }

    @Test
    fun `OnListEdited with the cover cleared removes it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))
        stubUpdate()

        viewModel.onEvent(
            WishlistUiEvent.OnListEdited(viewModel.uiState.value.formValues.copy(coverImage = null))
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updateListUseCase(LIST_ID, "Co-op Picks", "Controllers in hand.", WishlistIcon.MULTIPLAYER, CoverImageUpdate.Remove)
        }
    }

    @Test
    fun `OnListEdited with a newly picked cover replaces it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))
        stubUpdate()

        viewModel.onEvent(
            WishlistUiEvent.OnListEdited(
                viewModel.uiState.value.formValues.copy(coverImage = "content://picked/1")
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updateListUseCase(
                LIST_ID,
                "Co-op Picks",
                "Controllers in hand.",
                WishlistIcon.MULTIPLAYER,
                CoverImageUpdate.Replace("content://picked/1")
            )
        }
    }

    @Test
    fun `OnListEdited on a list without a cover that still has none keeps it`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false, coverImagePath = null))
        stubUpdate()

        viewModel.onEvent(WishlistUiEvent.OnListEdited(viewModel.uiState.value.formValues))
        advanceUntilIdle()

        coVerify(exactly = 1) { updateListUseCase(LIST_ID, any(), any(), any(), CoverImageUpdate.Keep) }
    }

    @Test
    fun `OnListEdited that succeeds shows nothing`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))
        val effects = collectEffects(viewModel)
        stubUpdate()

        viewModel.onEvent(WishlistUiEvent.OnListEdited(viewModel.uiState.value.formValues))
        advanceUntilIdle()

        assertEquals(emptyList<WishlistUiEffect>(), effects)
    }

    @Test
    fun `OnListEdited whose cover fails to save shows the storage error`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detailWithCover(isDefault = false))
        val effects = collectEffects(viewModel)
        stubUpdate(AppResult.failure(RepositoryError.FileStorage))

        viewModel.onEvent(
            WishlistUiEvent.OnListEdited(
                viewModel.uiState.value.formValues.copy(coverImage = "content://picked/1")
            )
        )
        advanceUntilIdle()

        assertEquals(
            listOf<WishlistUiEffect>(WishlistUiEffect.ShowSnackbar(RepositoryError.FileStorage.toUiText())),
            effects
        )
    }

    @Test
    fun `editing the default list reaches the use case and does not touch the default flag`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(detailWithCover(isDefault = true))
            stubUpdate()

            viewModel.onEvent(
                WishlistUiEvent.OnListEdited(viewModel.uiState.value.formValues.copy(name = "Renamed"))
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { updateListUseCase(LIST_ID, "Renamed", any(), any(), any()) }
            coVerify(exactly = 0) { setDefaultListUseCase(any()) }
            assertTrue(viewModel.uiState.value.isDefaultList)
        }
}
