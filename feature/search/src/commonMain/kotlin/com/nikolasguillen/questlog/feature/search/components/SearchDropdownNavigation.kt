package com.nikolasguillen.questlog.feature.search.components

import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel

/**
 * Where the highlight goes when the user presses Down: the first item when nothing is highlighted, otherwise the next
 * one. It stops on the last item instead of wrapping, and it is `null` when there are no items. A [current] that is
 * not in [items] counts as nothing highlighted.
 *
 * `null` stands for the text field itself, so the highlight is never "lost": it either is an item or is the field.
 */
internal fun highlightNext(
    items: List<SearchDropdownItemUiModel>,
    current: SearchDropdownItemUiModel?
): SearchDropdownItemUiModel? {
    if (items.isEmpty()) return null
    val index = indexOfHighlight(items, current)
    return if (index < 0) items.first() else items[minOf(index + 1, items.lastIndex)]
}

/**
 * Where the highlight goes when the user presses Up: the previous item, or back to the text field (`null`) from the
 * first item and from nothing. A [current] that is not in [items] counts as nothing highlighted.
 */
internal fun highlightPrevious(
    items: List<SearchDropdownItemUiModel>,
    current: SearchDropdownItemUiModel?
): SearchDropdownItemUiModel? {
    val index = indexOfHighlight(items, current)
    return if (index <= 0) null else items[index - 1]
}

/**
 * The highlight after the list changed: [current] if its item is still there, otherwise nothing. The highlight is the
 * item, not its position, so it follows its row when rows are added or removed around it and clears when the row goes.
 */
internal fun reconcileHighlight(
    items: List<SearchDropdownItemUiModel>,
    current: SearchDropdownItemUiModel?
): SearchDropdownItemUiModel? = current?.takeIf { it in items }

private fun indexOfHighlight(
    items: List<SearchDropdownItemUiModel>,
    current: SearchDropdownItemUiModel?
): Int = if (current == null) -1 else items.indexOf(current)
