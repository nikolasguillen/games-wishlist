package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Platform
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val PC = Platform(id = 6, name = "PC")
private val PS5 = Platform(id = 167, name = "PlayStation 5")
private val SWITCH = Platform(id = 130, name = "Nintendo Switch")

class GetOwnedPlatformsUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetOwnedPlatformsUseCase(repository)

    private fun given(ownedIds: Set<Int>, known: List<Platform>, inferred: List<Platform>) {
        every { repository.getOwnedPlatformIds() } returns flowOf(ownedIds)
        every { repository.getKnownPlatforms() } returns flowOf(known)
        every { repository.getInferredPlatforms() } returns flowOf(inferred)
    }

    @Test
    fun `an explicit selection filters the known platforms`() = runTest {
        given(ownedIds = setOf(6, 167), known = listOf(PC, SWITCH, PS5), inferred = listOf(SWITCH))

        assertEquals(listOf(PC, PS5), useCase().first())
    }

    /**
     * The user has never opened the picker, so the platforms their saved games run on stand in.
     * Returning nothing here would empty every feed with no way for them to tell why.
     */
    @Test
    fun `no selection falls back to the inferred platforms`() = runTest {
        given(ownedIds = emptySet(), known = listOf(PC, SWITCH, PS5), inferred = listOf(SWITCH, PS5))

        assertEquals(listOf(SWITCH, PS5), useCase().first())
    }

    @Test
    fun `a selection naming an unknown platform ignores it`() = runTest {
        given(ownedIds = setOf(6, 999), known = listOf(PC, PS5), inferred = emptyList())

        assertEquals(listOf(PC), useCase().first())
    }

    @Test
    fun `a cold start with nothing saved and nothing chosen is empty`() = runTest {
        given(ownedIds = emptySet(), known = emptyList(), inferred = emptyList())

        assertEquals(emptyList<Platform>(), useCase().first())
    }
}
