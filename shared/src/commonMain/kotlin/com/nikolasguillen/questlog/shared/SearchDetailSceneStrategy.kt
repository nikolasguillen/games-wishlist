package com.nikolasguillen.questlog.shared

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

private const val SEARCH_PANE_KEY = "questlog.searchDetail.searchPane"
private const val SEARCH_DETAIL_KEY = "questlog.searchDetail.detail"

/** Metadata for the root Search entry, the one the left pane shows. */
internal fun searchPaneMetadata(): Map<String, Any> = mapOf(SEARCH_PANE_KEY to true)

/** Metadata for a game's detail opened from Search, the only kind the right pane can show. */
internal fun searchDetailMetadata(): Map<String, Any> = mapOf(SEARCH_DETAIL_KEY to true)

/**
 * Shows Search and a game opened from it side by side when the window is wide, and steps aside otherwise.
 *
 * It decides from the entries' metadata only, with the same rule the click handlers and back use ([searchDetailRun]),
 * so it knows nothing about routes. When there is no such game, or the window is narrow, it returns `null` and the
 * default single-pane strategy renders exactly as it did before this scene existed.
 */
internal data class SearchDetailSceneStrategy(private val twoPaneEnabled: Boolean) : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!twoPaneEnabled) return null
        val run = searchDetailRun(
            items = entries,
            isSearch = { SEARCH_PANE_KEY in it.metadata },
            isSearchDetail = { SEARCH_DETAIL_KEY in it.metadata }
        ) ?: return null
        return SearchDetailScene(
            searchEntry = entries.first(),
            detailEntry = entries[run.last],
            previousEntries = entries.take(run.first),
            onBack = onBack
        )
    }
}
