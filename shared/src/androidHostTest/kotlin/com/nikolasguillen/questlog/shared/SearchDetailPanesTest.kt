package com.nikolasguillen.questlog.shared

import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.navigation.GameDetailRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The back stack rules of the two-pane Search layout: which entries form the run of games opened from Search, how
 * opening one replaces or stacks depending on the window, and how back closes the run. They are pure functions over
 * a plain list, so no Compose or navigation runtime is involved.
 */
class SearchDetailPanesTest {

    private fun fromSearch(id: Int) = GameDetailRoute(id, fromSearch = true)

    private fun elsewhere(id: Int) = GameDetailRoute(id)

    private fun stack(vararg keys: NavKey): MutableList<NavKey> = mutableListOf(*keys)

    // searchDetailRun

    @Test
    fun `a single game opened from Search is a run on Search`() {
        assertEquals(1..1, searchDetailRun(stack(SearchRoute, fromSearch(1))))
    }

    @Test
    fun `several games opened from Search are one run`() {
        assertEquals(1..2, searchDetailRun(stack(SearchRoute, fromSearch(1), fromSearch(2))))
    }

    @Test
    fun `Search alone has no run`() {
        assertNull(searchDetailRun(stack(SearchRoute)))
    }

    @Test
    fun `a game opened from elsewhere is not a run`() {
        assertNull(searchDetailRun(stack(SearchRoute, elsewhere(9))))
    }

    @Test
    fun `a game opened over the run covers it`() {
        assertNull(searchDetailRun(stack(SearchRoute, fromSearch(1), elsewhere(9))))
    }

    @Test
    fun `a stack that does not start on Search has no run`() {
        assertNull(searchDetailRun(stack(RadarRoute, elsewhere(4))))
    }

    @Test
    fun `an empty stack has no run`() {
        assertNull(searchDetailRun(stack()))
    }

    // openSearchDetail

    @Test
    fun `below the threshold a game opened from Search is a push`() {
        val backStack = stack(SearchRoute)

        openSearchDetail(backStack, gameId = 1, twoPaneEnabled = false)
        openSearchDetail(backStack, gameId = 2, twoPaneEnabled = false)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1), fromSearch(2)), backStack)
    }

    @Test
    fun `below the threshold opening the game on top again changes nothing`() {
        val backStack = stack(SearchRoute, fromSearch(1))

        openSearchDetail(backStack, gameId = 1, twoPaneEnabled = false)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
    }

    @Test
    fun `on a wide window the first game opens beside the results`() {
        val backStack = stack(SearchRoute)

        openSearchDetail(backStack, gameId = 1, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
    }

    @Test
    fun `on a wide window another game replaces the open one`() {
        val backStack = stack(SearchRoute, fromSearch(1))

        openSearchDetail(backStack, gameId = 2, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(2)), backStack)
    }

    @Test
    fun `on a wide window opening the open game again changes nothing`() {
        val backStack = stack(SearchRoute, fromSearch(1))

        openSearchDetail(backStack, gameId = 1, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
    }

    @Test
    fun `on a wide window a run stacked while narrow collapses to the new game`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2), fromSearch(3))

        openSearchDetail(backStack, gameId = 4, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(4)), backStack)
    }

    @Test
    fun `on a wide window a run stacked while narrow collapses even to its own top game`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2))

        openSearchDetail(backStack, gameId = 2, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(2)), backStack)
    }

    // navigateBack

    @Test
    fun `on a wide window back closes the whole run`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2), fromSearch(3))

        navigateBack(backStack, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute), backStack)
    }

    @Test
    fun `below the threshold back pops one entry`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2))

        navigateBack(backStack, twoPaneEnabled = false)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
    }

    @Test
    fun `back on a wide window pops a single non-run entry`() {
        val backStack = stack(SearchRoute, elsewhere(9))

        navigateBack(backStack, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute), backStack)
    }

    // Resizing across the threshold only changes how the same stack is read

    @Test
    fun `a run stacked while narrow is walked one entry at a time until the window grows`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2), fromSearch(3))

        navigateBack(backStack, twoPaneEnabled = false)
        navigateBack(backStack, twoPaneEnabled = false)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
    }

    @Test
    fun `a window that grows mid-walk closes what is left of the run in one back`() {
        val backStack = stack(SearchRoute, fromSearch(1), fromSearch(2), fromSearch(3))

        navigateBack(backStack, twoPaneEnabled = false)
        navigateBack(backStack, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute), backStack)
    }

    // A game opened from elsewhere is never part of the run

    @Test
    fun `a game opened over the run is popped alone and the run is back after it`() {
        val backStack = stack(SearchRoute, fromSearch(1), elsewhere(9))

        navigateBack(backStack, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, fromSearch(1)), backStack)
        assertEquals(1..1, searchDetailRun(backStack))
    }

    @Test
    fun `a game from elsewhere is not the same game as one opened from Search`() {
        val backStack = stack(SearchRoute, elsewhere(1))

        openSearchDetail(backStack, gameId = 1, twoPaneEnabled = true)

        assertEquals(listOf<NavKey>(SearchRoute, elsewhere(1), fromSearch(1)), backStack)
    }

    @Test
    fun `back never pops the root`() {
        val backStack = stack(SearchRoute)

        navigateBack(backStack, twoPaneEnabled = true)
        navigateBack(backStack, twoPaneEnabled = false)

        assertEquals(listOf<NavKey>(SearchRoute), backStack)
    }
}
