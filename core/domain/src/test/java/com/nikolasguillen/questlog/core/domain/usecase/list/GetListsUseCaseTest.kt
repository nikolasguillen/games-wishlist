package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.WishlistSummary
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.WishlistList
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GetListsUseCase]: each list reaches the overview with whether it is the current default, the
 * flag follows the default when it moves, and the order `getAllLists()` emits is left alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetListsUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetListsUseCase(repository)

    private val first = WishlistList(id = 1, name = "Wishlist")
    private val second = WishlistList(id = 3, name = "Co-op Picks")

    private fun given(lists: List<WishlistList>, defaultListId: Flow<Long>) {
        every { repository.getAllLists() } returns flowOf(lists)
        every { repository.observeDefaultListId() } returns defaultListId
    }

    @Test
    fun `only the default list is flagged, and the order is kept`() = runTest {
        given(listOf(first, second), defaultListId = flowOf(3L))

        assertEquals(
            listOf(
                WishlistSummary(first, isDefault = false),
                WishlistSummary(second, isDefault = true)
            ),
            useCase().first()
        )
    }

    @Test
    fun `the flag moves when the default moves`() = runTest {
        val defaultListId = MutableStateFlow(1L)
        given(listOf(first, second), defaultListId)
        val emissions = mutableListOf<List<WishlistSummary>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(emissions) }
        runCurrent()

        defaultListId.value = 3L
        runCurrent()

        assertEquals(
            listOf(listOf(true, false), listOf(false, true)),
            emissions.map { summaries -> summaries.map { it.isDefault } }
        )
    }

    @Test
    fun `no lists stays an empty list`() = runTest {
        given(emptyList(), defaultListId = flowOf(1L))

        assertEquals(emptyList<WishlistSummary>(), useCase().first())
    }
}
