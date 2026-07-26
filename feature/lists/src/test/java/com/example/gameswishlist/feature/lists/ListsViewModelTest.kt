package com.example.gameswishlist.feature.lists

import com.example.gameswishlist.core.domain.usecase.list.CreateListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.model.WishlistList
import io.mockk.coVerify
import io.mockk.every
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

/**
 * [ListsViewModel.lists] is shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed],
 * so it only starts collecting the underlying [GetListsUseCase] flow once it has a subscriber --
 * [createViewModel] mirrors that by collecting it in the background, the same way the screen does
 * via `collectAsStateWithLifecycle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getListsUseCase = mockk<GetListsUseCase>()
    private val createListUseCase = mockk<CreateListUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testList(id: Long, name: String, gameCount: Int = 0) = WishlistList(
        id = id,
        name = name,
        icon = WishlistIcon.BACKLOG,
        gameCount = gameCount
    )

    private fun TestScope.createViewModel(lists: List<WishlistList> = emptyList()): ListsViewModel {
        every { getListsUseCase() } returns flowOf(lists)
        return ListsViewModel(
            getListsUseCase = getListsUseCase,
            createListUseCase = createListUseCase
        ).also { viewModel ->
            backgroundScope.launch { viewModel.lists.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun `lists maps the repository lists into UI models`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            lists = listOf(testList(id = 1, name = "RPGs to Try", gameCount = 8))
        )

        val uiModel = viewModel.lists.value.single()
        assertEquals(1L, uiModel.id)
        assertEquals("RPGs to Try", uiModel.name)
    }

    @Test
    fun `lists reflects an empty repository as an empty list`() = runTest(testDispatcher) {
        val viewModel = createViewModel(lists = emptyList())

        assertEquals(emptyList<Any>(), viewModel.lists.value)
    }

    @Test
    fun `createList delegates to the use case with the given parameters`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.createList("New List", "Description", WishlistIcon.HEART)
        advanceUntilIdle()

        coVerify { createListUseCase("New List", "Description", WishlistIcon.HEART) }
    }
}
