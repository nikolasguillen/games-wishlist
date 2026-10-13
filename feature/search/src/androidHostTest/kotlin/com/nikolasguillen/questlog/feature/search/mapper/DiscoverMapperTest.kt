package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.Company
import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.RecommendedShelf
import com.nikolasguillen.questlog.core.model.ShelfReason
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_because_you_like
import com.nikolasguillen.questlog.feature.search.resources.discover_more_from
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers how the Discover feed's domain shelves become the rows on screen: the six-game cap, whether a shelf
 * holds more than the row shows (the only case that gets an arrow), the shelf identity and titles, the hero
 * split, and [shelfGames], the single definition of what a shelf holds that both the capped row and the
 * uncapped grid read.
 */
class DiscoverMapperTest {

    private fun games(count: Int, firstId: Int = 1): List<Game> =
        (firstId until firstId + count).map { Game(id = it, name = "Game $it") }

    private fun feed(
        popular: List<Game> = emptyList(),
        upcoming: List<Game> = emptyList(),
        recommended: List<RecommendedShelf> = emptyList()
    ) = DiscoverFeed(popular = popular, upcoming = upcoming, recommended = recommended)

    private fun genreShelf(genreId: Int, name: String, games: List<Game>) =
        RecommendedShelf(ShelfReason.ByGenre(Genre(genreId, name)), games)

    private fun developerShelf(companyId: Int, name: String, games: List<Game>) =
        RecommendedShelf(ShelfReason.ByDeveloper(Company(companyId, name)), games)

    @Test
    fun `the row holds six games`() {
        assertEquals(6, DISCOVER_SHELF_ROW_SIZE)
    }

    @Test
    fun `an empty lane has no shelf at all`() {
        val content = feed(popular = emptyList()).toDiscoverContentState(savedIds = emptySet())

        assertNull(content.popular)
    }

    @Test
    fun `a lane of one game shows it and has no more`() {
        val shelf = feed(popular = games(1)).toDiscoverContentState(emptySet()).popular!!

        assertEquals(listOf(1), shelf.games.map { it.id })
        assertFalse(shelf.hasMore)
    }

    @Test
    fun `a lane of exactly six shows all six and has no more`() {
        val shelf = feed(popular = games(6)).toDiscoverContentState(emptySet()).popular!!

        assertEquals((1..6).toList(), shelf.games.map { it.id })
        assertFalse(shelf.hasMore)
    }

    @Test
    fun `a lane of seven shows the first six in order and has more`() {
        val shelf = feed(popular = games(7)).toDiscoverContentState(emptySet()).popular!!

        assertEquals((1..6).toList(), shelf.games.map { it.id })
        assertTrue(shelf.hasMore)
    }

    @Test
    fun `a long lane keeps its order and never reorders the row`() {
        val shuffled = listOf(40, 7, 23, 3, 99, 12, 1, 56).map { Game(id = it, name = "Game $it") }

        val shelf = feed(popular = shuffled).toDiscoverContentState(emptySet()).popular!!

        assertEquals(listOf(40, 7, 23, 3, 99, 12), shelf.games.map { it.id })
    }

    @Test
    fun `most anticipated drops the hero before it is capped`() {
        // The hero plus six leaves exactly six for the shelf: nothing more to see.
        val six = feed(upcoming = games(7)).toDiscoverContentState(emptySet())
        assertEquals(1, six.hero?.id)
        assertEquals((2..7).toList(), six.upcoming!!.games.map { it.id })
        assertFalse(six.upcoming!!.hasMore)

        // The hero plus seven leaves seven: six in the row and an arrow.
        val seven = feed(upcoming = games(8)).toDiscoverContentState(emptySet())
        assertEquals(1, seven.hero?.id)
        assertEquals((2..7).toList(), seven.upcoming!!.games.map { it.id })
        assertTrue(seven.upcoming!!.hasMore)
    }

    @Test
    fun `a lane holding only the hero has no most anticipated shelf`() {
        val content = feed(upcoming = games(1)).toDiscoverContentState(emptySet())

        assertEquals(1, content.hero?.id)
        assertNull(content.upcoming)
    }

    @Test
    fun `the lanes carry their lane key and their own title`() {
        val content = feed(popular = games(2), upcoming = games(3, firstId = 10)).toDiscoverContentState(emptySet())

        val popular = content.popular!!
        assertEquals(DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH), popular.key)
        assertEquals(UiText.StringResource(Res.string.discover_popular_this_month), popular.title)

        val upcoming = content.upcoming!!
        assertEquals(DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED), upcoming.key)
        assertEquals(UiText.StringResource(Res.string.discover_most_anticipated), upcoming.title)
    }

    @Test
    fun `personalised shelves are keyed by the id of their signal and titled by its name`() {
        val content = feed(
            recommended = listOf(
                developerShelf(companyId = 7, name = "Larian Studios", games = games(8)),
                genreShelf(genreId = 12, name = "Role-playing (RPG)", games = games(3, firstId = 100))
            )
        ).toDiscoverContentState(emptySet())

        val (developer, genre) = content.recommended
        assertEquals(DiscoverShelfKey.ByDeveloper(developerId = 7), developer.key)
        assertEquals(UiText.StringResource(Res.string.discover_more_from, "Larian Studios"), developer.title)
        assertEquals((1..6).toList(), developer.games.map { it.id })
        assertTrue(developer.hasMore)

        assertEquals(DiscoverShelfKey.ByGenre(genreId = 12), genre.key)
        assertEquals(UiText.StringResource(Res.string.discover_because_you_like, "Role-playing (RPG)"), genre.title)
        assertEquals(listOf(100, 101, 102), genre.games.map { it.id })
        assertFalse(genre.hasMore)
    }

    @Test
    fun `two shelves with the same visible title still have different keys`() {
        val content = feed(
            recommended = listOf(
                genreShelf(genreId = 12, name = "Same name", games = games(3)),
                genreShelf(genreId = 31, name = "Same name", games = games(3, firstId = 50))
            )
        ).toDiscoverContentState(emptySet())

        val (first, second) = content.recommended
        assertEquals(first.title, second.title)
        assertTrue(first.key != second.key)
    }

    @Test
    fun `cards are marked saved from the saved ids`() {
        val content = feed(popular = games(3)).toDiscoverContentState(savedIds = setOf(2))

        assertEquals(listOf(false, true, false), content.popular!!.games.map { it.isSaved })
    }

    @Test
    fun `shelfGames returns the whole shelf for every key, and the row is its prefix`() {
        val popular = games(9)
        val upcoming = games(8, firstId = 100)
        val byGenre = games(10, firstId = 200)
        val byDeveloper = games(7, firstId = 300)
        val feed = feed(
            popular = popular,
            upcoming = upcoming,
            recommended = listOf(
                genreShelf(genreId = 12, name = "RPG", games = byGenre),
                developerShelf(companyId = 7, name = "Larian", games = byDeveloper)
            )
        )
        val content = feed.toDiscoverContentState(emptySet())

        assertEquals(popular, feed.shelfGames(DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH)))
        assertEquals(upcoming.drop(1), feed.shelfGames(DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED)))
        assertEquals(byGenre, feed.shelfGames(DiscoverShelfKey.ByGenre(12)))
        assertEquals(byDeveloper, feed.shelfGames(DiscoverShelfKey.ByDeveloper(7)))

        (listOfNotNull(content.popular, content.upcoming) + content.recommended).forEach { shelf ->
            val whole = feed.shelfGames(shelf.key)
            assertNotNull(whole)
            assertEquals(whole!!.take(DISCOVER_SHELF_ROW_SIZE).map { it.id }, shelf.games.map { it.id })
            assertEquals(whole.size > DISCOVER_SHELF_ROW_SIZE, shelf.hasMore)
        }
    }

    @Test
    fun `shelfGames is null for a shelf the feed does not hold`() {
        val feed = feed(recommended = listOf(genreShelf(genreId = 12, name = "RPG", games = games(5))))

        assertNull(feed.shelfGames(DiscoverShelfKey.ByGenre(31)))
        assertNull(feed.shelfGames(DiscoverShelfKey.ByDeveloper(12)))
    }

    @Test
    fun `a genre key and a developer key with the same number are different shelves`() {
        val feed = feed(
            recommended = listOf(
                genreShelf(genreId = 5, name = "Shooter", games = games(5)),
                developerShelf(companyId = 5, name = "Studio", games = games(5, firstId = 50))
            )
        )

        assertEquals(1, feed.shelfGames(DiscoverShelfKey.ByGenre(5))!!.first().id)
        assertEquals(50, feed.shelfGames(DiscoverShelfKey.ByDeveloper(5))!!.first().id)
    }
}
