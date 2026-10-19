package com.nikolasguillen.questlog.shared

import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.navigation.GameDetailRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute

/*
 * The back stack rules behind the two-pane Search layout: which entries are "a game opened from Search", and how
 * opening one and going back behave on a wide window. They are plain functions over the back stack so they can be
 * tested without Compose, and the click handlers, the system back and the scene all use these same definitions.
 *
 * A "Search-detail run" is the trailing run of details opened from Search (`GameDetailRoute.fromSearch`) sitting
 * directly on the root `SearchRoute`. A detail with `fromSearch` can only be created on top of `SearchRoute` or of
 * another one, and every tab switch pops back to the root, so such a detail only ever exists inside a run. A detail
 * from anywhere else (Radar, a wishlist, a release reminder) is not part of it, and one pushed over a run covers the
 * run, which then no longer ends the stack.
 */

/**
 * The indices of the trailing run of details (`isSearchDetail`) sitting directly on the first item, which has to be
 * Search (`isSearch`), or `null` when there is none.
 *
 * Generic so that the scene strategy can apply the same rule to navigation entries (through their metadata) as the
 * click handlers apply to keys.
 */
internal fun <T> searchDetailRun(
    items: List<T>,
    isSearch: (T) -> Boolean,
    isSearchDetail: (T) -> Boolean
): IntRange? {
    if (items.isEmpty() || !isSearch(items.first())) return null
    var first = items.size
    while (first > 1 && isSearchDetail(items[first - 1])) first--
    return if (first < items.size) first..items.lastIndex else null
}

/** [searchDetailRun] over a back stack's keys. */
internal fun searchDetailRun(keys: List<NavKey>): IntRange? = searchDetailRun(
    items = keys,
    isSearch = { it is SearchRoute },
    isSearchDetail = { it is GameDetailRoute && it.fromSearch }
)

/**
 * Opens a game from the Search flow.
 *
 * Below the two-pane threshold it is a plain push, as before. On a wide window the new detail replaces whatever
 * detail the right pane showed, so the stack never holds more than one; opening the game that is already the only
 * one changes nothing.
 */
internal fun openSearchDetail(backStack: MutableList<NavKey>, gameId: Int, twoPaneEnabled: Boolean) {
    val detail = GameDetailRoute(gameId, fromSearch = true)
    if (!twoPaneEnabled) {
        if (backStack.lastOrNull() != detail) backStack.add(detail)
        return
    }
    val run = searchDetailRun(backStack)
    if (run != null) {
        if (run.first == run.last && backStack[run.first] == detail) return
        removeRun(backStack, run)
    }
    backStack.add(detail)
}

/**
 * The system back action, and the one place a screen's back button is wired to when it has one.
 *
 * On a wide window with a game open beside the results, back closes the whole run in one step: Search returns to full
 * width, however many games were opened before the window grew. Otherwise it pops the last entry, and the stack's
 * root is never popped.
 */
internal fun navigateBack(backStack: MutableList<NavKey>, twoPaneEnabled: Boolean) {
    val run = if (twoPaneEnabled) searchDetailRun(backStack) else null
    if (run != null) {
        removeRun(backStack, run)
    } else if (backStack.size > 1) {
        backStack.removeLastOrNull()
    }
}

private fun removeRun(backStack: MutableList<NavKey>, run: IntRange) {
    for (index in run.last downTo run.first) backStack.removeAt(index)
}
