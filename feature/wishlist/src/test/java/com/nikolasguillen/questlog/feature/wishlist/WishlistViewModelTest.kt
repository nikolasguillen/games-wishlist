package com.nikolasguillen.questlog.feature.wishlist

import com.nikolasguillen.questlog.core.domain.model.WishlistDetail
import com.nikolasguillen.questlog.core.domain.usecase.list.DeleteListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetDefaultListUseCase
import com.nikolasguillen.questlog.core.model.WishlistList
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val LIST_ID = 3L

/**
 * Covers how [WishlistViewModel] turns a [WishlistDetail] into the two flags the top bar renders from
 * (the "Default" badge and the options menu), what choosing "Set as default" does, and how a refused or
 * successful delete is reported.
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

    private fun newViewModel() = WishlistViewModel(
        listId = LIST_ID,
        getWishlistDetailUseCase = getWishlistDetailUseCase,
        deleteListUseCase = deleteListUseCase,
        removeGameFromListUseCase = removeGameFromListUseCase,
        setDefaultListUseCase = setDefaultListUseCase
    )

    private fun TestScope.createViewModel(detail: WishlistDetail): WishlistViewModel {
        every { getWishlistDetailUseCase(LIST_ID) } returns flowOf(detail)
        return newViewModel().also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
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

    @Test
    fun `a successful delete navigates back`() = runTest(testDispatcher) {
        val viewModel = createViewModel(detail(isDefault = false))
        val effects = collectEffects(viewModel)
        coEvery { deleteListUseCase(LIST_ID) } returns true

        viewModel.onEvent(WishlistUiEvent.OnWishlistDeleted)
        advanceUntilIdle()

        assertEquals(listOf<WishlistUiEffect>(WishlistUiEffect.NavigateBack), effects)
    }
}
