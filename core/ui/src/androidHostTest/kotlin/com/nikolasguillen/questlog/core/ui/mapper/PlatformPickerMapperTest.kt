package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [toPlatformPickerContentState], the rules the owned-platforms picker shares between Settings and
 * the welcome flow: which rows are checked, what the search box matches, and the order — curated
 * platforms first, the platforms selected on entry floated above them, and neither reshuffled by a tap.
 */
class PlatformPickerMapperTest {

    // Declared in the order Room returns them, i.e. sorted by name.
    private val switch = Platform(id = 130, name = "Nintendo Switch", abbreviation = "Switch")
    private val pc = Platform(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC")
    private val ps5 = Platform(id = 167, name = "PlayStation 5", abbreviation = "PS5")
    private val catalogue = listOf(switch, pc, ps5)

    private fun List<Platform>.state(
        selected: Set<Int> = emptySet(),
        query: String = "",
        pinned: Set<Int>? = emptySet(),
        isSyncing: Boolean = false
    ) = toPlatformPickerContentState(
        selectedIds = selected,
        query = query,
        pinnedIds = pinned,
        isSyncing = isSyncing
    )

    private fun PlatformPickerContentState.names(): List<String> =
        (this as PlatformPickerContentState.Success).platforms.map { it.name }

    @Test
    fun `marks the selected ids as selected and leaves the rest unselected`() {
        val state = catalogue.state(selected = setOf(ps5.id, pc.id))

        val selected = (state as PlatformPickerContentState.Success).platforms
            .filter { it.isSelected }
            .map { it.id }
        assertEquals(setOf(ps5.id, pc.id), selected.toSet())
    }

    @Test
    fun `stays Loading until the entry-time selection has been read`() {
        assertEquals(PlatformPickerContentState.Loading, catalogue.state(pinned = null))
    }

    @Test
    fun `reports Empty when nothing is cached to pick from`() {
        assertEquals(PlatformPickerContentState.Empty, emptyList<Platform>().state())
    }

    @Test
    fun `an empty catalogue reads as Loading while it is being synced`() {
        assertEquals(PlatformPickerContentState.Loading, emptyList<Platform>().state(isSyncing = true))
    }

    @Test
    fun `a cached catalogue is shown even while a sync is running`() {
        val state = catalogue.state(isSyncing = true)

        assertTrue(state is PlatformPickerContentState.Success)
    }

    @Test
    fun `the search matches on name and on abbreviation`() {
        assertEquals(listOf("Nintendo Switch"), catalogue.state(query = "nintendo").names())
        assertEquals(listOf("PlayStation 5"), catalogue.state(query = "ps5").names())
    }

    @Test
    fun `reports NoSearchResults when the query matches nothing`() {
        assertEquals(PlatformPickerContentState.NoSearchResults, catalogue.state(query = "Dreamcast"))
    }

    @Test
    fun `floats the platforms pinned on entry to the top`() {
        val state = catalogue.state(selected = setOf(ps5.id), pinned = setOf(ps5.id))

        assertEquals(listOf("PlayStation 5", "Nintendo Switch", "PC (Microsoft Windows)"), state.names())
    }

    @Test
    fun `selecting another platform does not move it while the pinned set is unchanged`() {
        val onEntry = catalogue.state(selected = setOf(ps5.id), pinned = setOf(ps5.id)).names()

        val afterTap = catalogue.state(selected = setOf(ps5.id, pc.id), pinned = setOf(ps5.id)).names()

        assertEquals(onEntry, afterTap)
    }

    @Test
    fun `a query drops the entry pinning but keeps the catalogue ranking`() {
        val state = catalogue.state(selected = setOf(ps5.id), query = "p", pinned = setOf(ps5.id))

        // PS5 outranks PC in the curated order, so it leads even though PC sorts first by name.
        assertEquals(listOf("PlayStation 5", "PC (Microsoft Windows)"), state.names())
    }

    /**
     * IGDB returns the catalogue in no useful order, and `generation` alone would bury PC and every
     * headset at the bottom. The curated list is what keeps the head of the list recognisable.
     */
    @Test
    fun `ranks curated platforms first and the rest newest hardware first`() {
        val saturn = Platform(id = 32, name = "Sega Saturn", abbreviation = "Saturn", generation = 5)
        val quest = Platform(id = 471, name = "Meta Quest 3", abbreviation = null, generation = null)

        val state = listOf(quest, saturn, ps5).state()

        assertEquals(listOf("PlayStation 5", "Sega Saturn", "Meta Quest 3"), state.names())
    }

    @Test
    fun `a platform without an abbreviation is still listed`() {
        val quest = Platform(id = 471, name = "Meta Quest 3", abbreviation = null)

        val state = listOf(quest).state()

        assertTrue(state is PlatformPickerContentState.Success)
        assertEquals(listOf("Meta Quest 3"), state.names())
    }
}
