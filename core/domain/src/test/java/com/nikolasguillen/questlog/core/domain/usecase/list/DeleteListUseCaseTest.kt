package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [DeleteListUseCase]: the list currently set as the default is never deleted, and nothing else is
 * special — in particular not the id the app was seeded with, once the user has moved the default away.
 */
class DeleteListUseCaseTest {

    private val repository = mockk<GameRepository>(relaxed = true)

    private val useCase = DeleteListUseCase(repository)

    @Test
    fun `the current default list is refused and nothing is deleted`() = runTest {
        coEvery { repository.getDefaultListId() } returns 7L

        val deleted = useCase(7L)

        assertFalse(deleted)
        coVerify(exactly = 0) { repository.deleteList(any()) }
    }

    @Test
    fun `a list that is not the default is deleted`() = runTest {
        coEvery { repository.getDefaultListId() } returns 7L

        val deleted = useCase(3L)

        assertTrue(deleted)
        coVerify(exactly = 1) { repository.deleteList(3L) }
    }

    @Test
    fun `the seeded list is deletable once it is no longer the default`() = runTest {
        coEvery { repository.getDefaultListId() } returns 3L

        val deleted = useCase(1L)

        assertTrue(deleted)
        coVerify(exactly = 1) { repository.deleteList(1L) }
    }
}
