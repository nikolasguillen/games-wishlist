package com.nikolasguillen.questlog.feature.search.components

import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the pure rules behind the arrow keys in the wide search panel: Down walks forward and stops on the last item,
 * Up walks back and leaves the first item for the text field (`null`), a highlight that is not in the list counts as
 * none, and a highlight survives a change of the list only while its item is still in it.
 */
class SearchDropdownNavigationTest {

    private val first = SearchDropdownItemUiModel.TypeChip(SearchType.GAMES)
    private val second = SearchDropdownItemUiModel.TypeChip(SearchType.CHARACTERS)
    private val middle = SearchDropdownItemUiModel.Genre(1)
    private val fourth = SearchDropdownItemUiModel.RecentQuery("a")
    private val last = SearchDropdownItemUiModel.RecentGame(7)
    private val items = listOf(first, second, middle, fourth, last)

    @Test
    fun `down from nothing highlights the first item`() {
        assertEquals(first, highlightNext(items, null))
    }

    @Test
    fun `down from an item goes one forward`() {
        assertEquals(fourth, highlightNext(items, middle))
    }

    @Test
    fun `down from the last item stays on it`() {
        assertEquals(last, highlightNext(items, last))
    }

    @Test
    fun `down on an empty list highlights nothing`() {
        assertNull(highlightNext(emptyList(), null))
        assertNull(highlightNext(emptyList(), first))
    }

    @Test
    fun `up from an item goes one back`() {
        assertEquals(middle, highlightPrevious(items, fourth))
    }

    @Test
    fun `up from the first item returns to the text field`() {
        assertNull(highlightPrevious(items, first))
    }

    @Test
    fun `up from nothing stays on nothing`() {
        assertNull(highlightPrevious(items, null))
    }

    @Test
    fun `a highlight that is not in the list counts as nothing`() {
        val gone = SearchDropdownItemUiModel.RecentQuery("gone")

        assertEquals(first, highlightNext(items, gone))
        assertNull(highlightPrevious(items, gone))
    }

    @Test
    fun `reconciling keeps an item that is still in the list`() {
        assertEquals(middle, reconcileHighlight(items, middle))
    }

    @Test
    fun `reconciling clears an item that is gone`() {
        assertNull(reconcileHighlight(listOf(first, second), middle))
    }

    @Test
    fun `reconciling keeps nothing as nothing`() {
        assertNull(reconcileHighlight(items, null))
    }
}
