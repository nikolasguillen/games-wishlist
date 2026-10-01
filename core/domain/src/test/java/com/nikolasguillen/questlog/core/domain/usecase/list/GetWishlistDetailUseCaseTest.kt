package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.WishlistDetail
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LIST_ID = 3L

/**
 * Covers [GetWishlistDetailUseCase]: the detail carries whether the list is the current default, that
 * flag follows the default when it moves, and a list that no longer exists still emits `null`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetWishlistDetailUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetWishlistDetailUseCase(repository)

    private val list = WishlistList(id = LIST_ID, name = "Co-op Picks")

    private fun given(list: WishlistList?, defaultListId: Flow<Long>) {
        every { repository.observeListById(LIST_ID) } returns flowOf(list)
        every { repository.getGamesByList(LIST_ID) } returns flowOf(listOf(Game(id = 1, name = "Cindergate")))
        every { repository.observeDefaultListId() } returns defaultListId
    }

    @Test
    fun `a list that is the default is flagged as default`() = runTest {
        given(list, defaultListId = flowOf(LIST_ID))

        assertTrue(useCase(LIST_ID).first()!!.isDefault)
    }

    @Test
    fun `a list that is not the default is not flagged`() = runTest {
        given(list, defaultListId = flowOf(1L))

        assertFalse(useCase(LIST_ID).first()!!.isDefault)
    }

    @Test
    fun `the flag follows the default when it moves`() = runTest {
        val defaultListId = MutableStateFlow(1L)
        given(list, defaultListId)
        val emissions = mutableListOf<WishlistDetail?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase(LIST_ID).toList(emissions)
        }
        runCurrent()

        defaultListId.value = LIST_ID
        runCurrent()

        assertEquals(listOf(false, true), emissions.map { it!!.isDefault })
    }

    @Test
    fun `a list that no longer exists emits null`() = runTest {
        given(list = null, defaultListId = flowOf(1L))

        assertNull(useCase(LIST_ID).first())
    }
}
