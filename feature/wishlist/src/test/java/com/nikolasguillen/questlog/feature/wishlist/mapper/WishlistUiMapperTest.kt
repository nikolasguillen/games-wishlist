package com.nikolasguillen.questlog.feature.wishlist.mapper

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.ui.mapper.toLabelUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistFilterChipUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistStatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun game(id: Int, status: GameStatus?) = Game(id = id, name = "Game $id", status = status)

/** One game per given status, grouped the way the screen groups them. */
private fun sectionsOf(vararg statuses: GameStatus?): List<WishlistSectionUiModel> =
    statuses.mapIndexed { index, status -> game(index + 1, status) }.toWishlistSectionUiModel()

private val ALL_LABEL = UiText.StringResource(R.string.filter_all)
private val NO_STATUS_LABEL = UiText.StringResource(R.string.no_status_section_label)

private fun List<WishlistFilterChipUiModel>.filters() = map { it.filter }

/**
 * Covers how a wishlist's games are grouped into status sections, which both the list and the grid view render
 * unchanged: the section order, that a status without games gets no section (so no orphan header), the counts
 * each header shows, and the "No status" section that always comes last. Also covers the status filter: which
 * chips the strip offers (All always, per-status chips from two statuses on) and which sections each filter
 * keeps.
 */
class WishlistUiMapperTest {

    @Test
    fun `sections follow the active-first status order with no status last`() {
        val games = listOf(
            game(1, GameStatus.DROPPED),
            game(2, null),
            game(3, GameStatus.WANT_TO_BUY),
            game(4, GameStatus.PLAYING),
            game(5, GameStatus.COMPLETED),
            game(6, GameStatus.BOUGHT)
        )

        val statuses = games.toWishlistSectionUiModel().map { it.status }

        assertEquals(
            listOf(
                GameStatus.PLAYING,
                GameStatus.BOUGHT,
                GameStatus.WANT_TO_BUY,
                GameStatus.COMPLETED,
                GameStatus.DROPPED,
                null
            ),
            statuses
        )
    }

    @Test
    fun `a status without games gets no section`() {
        val games = listOf(game(1, GameStatus.PLAYING), game(2, GameStatus.DROPPED))

        val statuses = games.toWishlistSectionUiModel().map { it.status }

        assertEquals(listOf<GameStatus?>(GameStatus.PLAYING, GameStatus.DROPPED), statuses)
    }

    @Test
    fun `each section holds exactly the games of its status`() {
        val games = listOf(
            game(1, GameStatus.PLAYING),
            game(2, null),
            game(3, GameStatus.PLAYING),
            game(4, GameStatus.PLAYING)
        )

        val sections = games.toWishlistSectionUiModel()

        assertEquals(listOf(1, 3, 4), sections.single { it.status == GameStatus.PLAYING }.games.map { it.id })
        assertEquals(listOf(2), sections.single { it.status == null }.games.map { it.id })
    }

    @Test
    fun `games that all share one status make a single section`() {
        val games = listOf(game(1, GameStatus.BOUGHT), game(2, GameStatus.BOUGHT), game(3, GameStatus.BOUGHT))

        val sections = games.toWishlistSectionUiModel()

        assertEquals(1, sections.size)
        assertEquals(GameStatus.BOUGHT, sections.single().status)
        assertEquals(3, sections.single().games.size)
    }

    @Test
    fun `games that all lack a status make a single no-status section`() {
        val sections = listOf(game(1, null), game(2, null)).toWishlistSectionUiModel()

        assertEquals(1, sections.size)
        assertEquals(null, sections.single().status)
        assertEquals(UiText.StringResource(R.string.no_status_section_label), sections.single().label)
    }

    @Test
    fun `no games make no sections`() {
        assertTrue(emptyList<Game>().toWishlistSectionUiModel().isEmpty())
    }

    @Test
    fun `an empty list has no chips whatever the filter is`() {
        val none = emptyList<WishlistSectionUiModel>()

        assertTrue(none.toFilterChips(WishlistStatusFilter.All).isEmpty())
        assertTrue(none.toFilterChips(WishlistStatusFilter.Only(GameStatus.PLAYING)).isEmpty())
    }

    @Test
    fun `a single section offers only the All chip while no filter is active`() {
        val chips = sectionsOf(GameStatus.PLAYING).toFilterChips(WishlistStatusFilter.All)

        assertEquals(listOf<WishlistStatusFilter>(WishlistStatusFilter.All), chips.filters())
        assertEquals(listOf(true), chips.map { it.isSelected })
    }

    @Test
    fun `with several sections the chips are All then one per section in section order`() {
        val chips = sectionsOf(GameStatus.COMPLETED, null, GameStatus.PLAYING)
            .toFilterChips(WishlistStatusFilter.All)

        assertEquals(
            listOf(
                WishlistStatusFilter.All,
                WishlistStatusFilter.Only(GameStatus.PLAYING),
                WishlistStatusFilter.Only(GameStatus.COMPLETED),
                WishlistStatusFilter.Only(null)
            ),
            chips.filters()
        )
        assertEquals(
            listOf(ALL_LABEL, GameStatus.PLAYING.toLabelUiText(), GameStatus.COMPLETED.toLabelUiText(), NO_STATUS_LABEL),
            chips.map { it.label }
        )
    }

    @Test
    fun `All is the selected chip when no filter is active`() {
        val chips = sectionsOf(GameStatus.PLAYING, GameStatus.BOUGHT).toFilterChips(WishlistStatusFilter.All)

        assertEquals(listOf(true, false, false), chips.map { it.isSelected })
    }

    @Test
    fun `exactly the chip of the active filter is selected`() {
        val chips = sectionsOf(GameStatus.PLAYING, GameStatus.BOUGHT, null)
            .toFilterChips(WishlistStatusFilter.Only(GameStatus.BOUGHT))

        assertEquals(listOf(WishlistStatusFilter.Only(GameStatus.BOUGHT)), chips.filter { it.isSelected }.filters())
    }

    /**
     * Status chips only appear once there are two statuses to choose between, so a filter on the one status
     * left leaves just the (unselected) All chip. Documented here because nothing then marks the filter that is
     * still applied.
     */
    @Test
    fun `a filter on the only remaining status leaves just the All chip`() {
        val chips = sectionsOf(GameStatus.PLAYING).toFilterChips(WishlistStatusFilter.Only(GameStatus.PLAYING))

        assertEquals(listOf<WishlistStatusFilter>(WishlistStatusFilter.All), chips.filters())
        assertEquals(listOf(false), chips.map { it.isSelected })
    }

    @Test
    fun `a filtered status that lost its games keeps its selected chip in its ordered slot`() {
        val chips = sectionsOf(GameStatus.PLAYING, GameStatus.COMPLETED)
            .toFilterChips(WishlistStatusFilter.Only(GameStatus.BOUGHT))

        assertEquals(
            listOf(
                WishlistStatusFilter.All,
                WishlistStatusFilter.Only(GameStatus.PLAYING),
                WishlistStatusFilter.Only(GameStatus.BOUGHT),
                WishlistStatusFilter.Only(GameStatus.COMPLETED)
            ),
            chips.filters()
        )
        val bought = chips.single { it.filter == WishlistStatusFilter.Only(GameStatus.BOUGHT) }
        assertTrue(bought.isSelected)
        assertEquals(GameStatus.BOUGHT.toLabelUiText(), bought.label)
    }

    @Test
    fun `a filtered no-status section that lost its games keeps its chip last`() {
        val chips = sectionsOf(GameStatus.PLAYING, GameStatus.COMPLETED)
            .toFilterChips(WishlistStatusFilter.Only(null))

        val last = chips.last()
        assertEquals(WishlistStatusFilter.Only(null), last.filter)
        assertTrue(last.isSelected)
        assertEquals(NO_STATUS_LABEL, last.label)
        assertFalse(chips.first().isSelected)
    }

    @Test
    fun `All keeps every section`() {
        val sections = sectionsOf(GameStatus.PLAYING, GameStatus.COMPLETED, null)

        assertEquals(sections, sections.filteredBy(WishlistStatusFilter.All))
    }

    @Test
    fun `a status filter keeps only the section of that status`() {
        val sections = sectionsOf(GameStatus.PLAYING, GameStatus.COMPLETED, null)

        val filtered = sections.filteredBy(WishlistStatusFilter.Only(GameStatus.COMPLETED))

        assertEquals(listOf<GameStatus?>(GameStatus.COMPLETED), filtered.map { it.status })
    }

    @Test
    fun `the no-status filter keeps only the no-status section`() {
        val sections = sectionsOf(GameStatus.PLAYING, null)

        val filtered = sections.filteredBy(WishlistStatusFilter.Only(null))

        assertEquals(listOf<GameStatus?>(null), filtered.map { it.status })
    }

    @Test
    fun `a filter whose status has no section keeps nothing`() {
        val sections = sectionsOf(GameStatus.PLAYING, GameStatus.COMPLETED)

        assertTrue(sections.filteredBy(WishlistStatusFilter.Only(GameStatus.DROPPED)).isEmpty())
    }
}
