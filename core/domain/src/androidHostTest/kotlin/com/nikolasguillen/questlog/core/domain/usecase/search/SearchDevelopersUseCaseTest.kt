package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Developer
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [SearchDevelopersUseCase]: an exact name first, then names starting with the query, then the rest; the
 * studio with more games first inside a group, then by name; case ignored; at most 100 results; and a
 * repository failure passed through untouched.
 */
class SearchDevelopersUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = SearchDevelopersUseCase(repository)

    private fun developer(id: Int, name: String, games: Int = 1) =
        Developer(id = id, name = name, logoUrl = null, developedGameCount = games)

    private suspend fun search(query: String, vararg found: Developer): List<String> {
        coEvery { repository.searchDevelopers(query) } returns AppResult.success(found.toList())
        return (useCase(query) as AppResult.Success).data.map { it.name }
    }

    @Test
    fun `an exact match comes before a prefix match before a contains match`() = runTest {
        val names = search(
            "capcom",
            developer(1, "Not Capcom Games", games = 900),
            developer(2, "Capcom Vancouver", games = 50),
            developer(3, "Capcom", games = 5)
        )

        assertEquals(listOf("Capcom", "Capcom Vancouver", "Not Capcom Games"), names)
    }

    @Test
    fun `inside a group the studio with more games comes first and ties go by name`() = runTest {
        val names = search(
            "square",
            developer(1, "Square B", games = 10),
            developer(2, "Square Enix", games = 300),
            developer(3, "Square A", games = 10)
        )

        assertEquals(listOf("Square Enix", "Square A", "Square B"), names)
    }

    @Test
    fun `case is ignored when matching`() = runTest {
        val names = search(
            "CAPCOM",
            developer(1, "Mega Capcom", games = 99),
            developer(2, "capcom", games = 1)
        )

        assertEquals(listOf("capcom", "Mega Capcom"), names)
    }

    @Test
    fun `at most one hundred developers are returned`() = runTest {
        val many = Array(150) { developer(it, "Studio $it", games = it) }

        val names = search("studio", *many)

        assertEquals(100, names.size)
        assertEquals("Studio 149", names.first())
    }

    @Test
    fun `a failure passes through`() = runTest {
        coEvery { repository.searchDevelopers("x") } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase("x"))
    }
}
