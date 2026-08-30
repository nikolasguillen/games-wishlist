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

class GetSelectedPlatformsUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetSelectedPlatformsUseCase(repository)

    private fun given(selectedIds: Set<Int>, known: List<Platform>) {
        every { repository.getOwnedPlatformIds() } returns flowOf(selectedIds)
        every { repository.getKnownPlatforms() } returns flowOf(known)
    }

    @Test
    fun `a selection resolves against the cached platforms`() = runTest {
        given(selectedIds = setOf(6, 167), known = listOf(PC, SWITCH, PS5))

        assertEquals(listOf(PC, PS5), useCase().first())
    }

    /**
     * Nothing is substituted here. An empty selection means the user asked for no platform filter, so
     * guessing one from their saved games would apply a rule they never chose and cannot see.
     */
    @Test
    fun `no selection resolves to nothing rather than to a guess`() = runTest {
        given(selectedIds = emptySet(), known = listOf(PC, SWITCH, PS5))

        assertEquals(emptyList<Platform>(), useCase().first())
    }

    @Test
    fun `a selection naming an uncached platform ignores it`() = runTest {
        given(selectedIds = setOf(6, 999), known = listOf(PC, PS5))

        assertEquals(listOf(PC), useCase().first())
    }
}
