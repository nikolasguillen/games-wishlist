package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.WishlistAssignment
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.WishlistList
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GetWishlistAssignmentsUseCase]: each list says whether the game is in it and whether it is the
 * current default, independently of each other.
 */
class GetWishlistAssignmentsUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetWishlistAssignmentsUseCase(repository)

    private val first = WishlistList(id = 1, name = "Wishlist")
    private val second = WishlistList(id = 3, name = "Co-op Picks")

    @Test
    fun `assignment and default are reported independently, in the repository order`() = runTest {
        every { repository.getAllLists() } returns flowOf(listOf(first, second))
        every { repository.getListIdsForGame(GAME_ID) } returns flowOf(listOf(3L))
        every { repository.observeDefaultListId() } returns flowOf(1L)

        assertEquals(
            listOf(
                WishlistAssignment(first, isAssigned = false, isDefault = true),
                WishlistAssignment(second, isAssigned = true, isDefault = false)
            ),
            useCase(GAME_ID).first()
        )
    }

    private companion object {
        const val GAME_ID = 42
    }
}
