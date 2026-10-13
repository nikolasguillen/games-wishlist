package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.sort_relevance
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.SearchBrowseUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSort
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_because_you_like
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import com.nikolasguillen.questlog.feature.search.resources.genre_browse_placeholder
import com.nikolasguillen.questlog.feature.search.resources.sort_top_picks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * Covers the two mapper functions that decide what a browse looks like to the user: the text the search bar
 * shows in place of a typed query ([toPlaceholder]), and the "Relevance" / "Top picks" relabelling
 * ([withRelevanceLabel]) that must change a label and nothing else.
 */
class SearchUiMapperTest {

    @Test
    fun `a genre browse's placeholder is the genre sentence around the genre's own label`() {
        val label = UiText.DynamicString("RPG")
        val browse = SearchBrowseUiModel.Genre(BrowseGenreUiModel(id = 12, label = label))

        assertEquals(UiText.StringResource(Res.string.genre_browse_placeholder, label), browse.toPlaceholder())
    }

    @Test
    fun `a lane shelf's placeholder is its own title`() {
        val title = UiText.StringResource(Res.string.discover_popular_this_month)
        val browse = SearchBrowseUiModel.Shelf(
            key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
            title = title
        )

        assertSame(title, browse.toPlaceholder())
    }

    @Test
    fun `a personalised shelf's placeholder is its formatted title`() {
        val title = UiText.StringResource(Res.string.discover_because_you_like, "Role-playing (RPG)")
        val browse = SearchBrowseUiModel.Shelf(key = DiscoverShelfKey.ByGenre(genreId = 12), title = title)

        assertEquals(title, browse.toPlaceholder())
    }

    @Test
    fun `relabelling for a browse changes only the Relevance label to Top picks`() {
        val before = getInitialSortFilters()

        val after = before.withRelevanceLabel(isBrowse = true)

        assertEquals(before.size, after.size)
        after.zip(before).forEach { (new, old) ->
            if (new.sortType == SearchSort.RELEVANCE) {
                assertEquals(UiText.StringResource(Res.string.sort_top_picks), new.label)
                // The user's choice is not the label's to change.
                assertEquals(old.copy(label = new.label), new)
            } else {
                assertEquals(old, new)
            }
        }
    }

    @Test
    fun `relabelling away from a browse restores the Relevance label and keeps the selection`() {
        val selected = getInitialSortFilters().map {
            if (it.sortType == SearchSort.RATING) it.copy(selected = true, descending = false)
            else it.copy(selected = false)
        }

        val restored = selected.withRelevanceLabel(isBrowse = true).withRelevanceLabel(isBrowse = false)

        val relevance = restored.first { it.sortType == SearchSort.RELEVANCE }
        assertEquals(UiText.StringResource(CoreUiRes.string.sort_relevance), relevance.label)
        assertEquals(selected, restored)
    }
}
