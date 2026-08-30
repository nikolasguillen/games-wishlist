package com.example.gameswishlist.feature.settings.mapper

import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.util.PlatformVisuals
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsContentState
import com.example.gameswishlist.feature.settings.model.PlatformUiModel

private val CURATED_RANK: Map<Int, Int> =
    PlatformVisuals.curatedPlatformIds.withIndex().associate { (index, id) -> id to index }

/**
 * Base order for the picker.
 *
 * The curated platforms come first in their curated order, then everything else newest hardware first.
 * IGDB has no popularity metric for platforms, so `generation` is the closest proxy for the long tail —
 * and a poor one on its own, because PC, mobile and headsets carry no generation at all and would sink
 * to the bottom. That is exactly what the curated head is for.
 */
private val PLATFORM_ORDER: Comparator<Platform> =
    compareBy<Platform> { CURATED_RANK[it.id] ?: Int.MAX_VALUE }
        .thenByDescending { it.generation ?: 0 }
        .thenBy { it.name }

internal fun Platform.toUiModel(selectedIds: Set<Int>): PlatformUiModel {
    return PlatformUiModel(
        id = id,
        name = name,
        abbreviation = abbreviation,
        isSelected = id in selectedIds
    )
}

/**
 * Resolves what the picker shows.
 *
 * @param selectedIds The stored selection, which decides the checkboxes.
 * @param query Free text from the search field. Matching is on name and abbreviation because that is
 * what a search box is for; every decision downstream of it still runs on [Platform.id].
 * @param pinnedIds The selection as it was when the screen opened, used only to float those rows to
 * the top. It is deliberately not [selectedIds]: ordering on the live selection would make the list
 * jump under the finger on every tap. `null` means the entry-time snapshot has not been taken yet,
 * which is the difference between "still loading" and "nothing is selected".
 */
internal fun List<Platform>.toContentState(
    selectedIds: Set<Int>,
    query: String,
    pinnedIds: Set<Int>?
): OwnedPlatformsContentState {
    if (pinnedIds == null) return OwnedPlatformsContentState.Loading
    if (isEmpty()) return OwnedPlatformsContentState.Empty

    val matches = if (query.isBlank()) this else filter { it.matches(query) }
    if (matches.isEmpty()) return OwnedPlatformsContentState.NoSearchResults

    // Partitioning is stable, so the ranking survives inside both halves.
    val ranked = matches.sortedWith(PLATFORM_ORDER)
    val ordered = if (query.isBlank()) {
        val (pinned, rest) = ranked.partition { it.id in pinnedIds }
        pinned + rest
    } else {
        ranked
    }
    return OwnedPlatformsContentState.Success(ordered.map { it.toUiModel(selectedIds) })
}

private fun Platform.matches(query: String): Boolean {
    val trimmed = query.trim()
    return name.contains(trimmed, ignoreCase = true) ||
            abbreviation?.contains(trimmed, ignoreCase = true) == true
}
