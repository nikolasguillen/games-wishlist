package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers [WishlistViewModePreferenceStoreImpl] against a real, temp-file-backed `DataStore<Preferences>` — no
 * Android `Context`/Robolectric involved, since the instance is injected rather than built from a `Context`
 * inside the class. Proves the persistence half of the wishlist view toggle: it defaults to the list view, a
 * choice is read back, it survives a fresh instance over the same file (what a process restart gives), and an
 * id this version does not know falls back to the list view instead of failing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModePreferenceStoreImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun newStore(): WishlistViewModePreferenceStoreImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { temporaryFolder.newFile("test.preferences_pb") }
        )
        return WishlistViewModePreferenceStoreImpl(dataStore)
    }

    @Test
    fun `reading before any write returns LIST`() = runTest {
        val store = newStore()

        assertEquals(WishlistViewMode.LIST, store.observeWishlistViewMode().first())
    }

    @Test
    fun `setWishlistViewMode then observeWishlistViewMode emits the new value`() = runTest {
        val store = newStore()

        store.setWishlistViewMode(WishlistViewMode.GRID)

        assertEquals(WishlistViewMode.GRID, store.observeWishlistViewMode().first())
    }

    @Test
    fun `switching back to the list overwrites the grid`() = runTest {
        val store = newStore()
        store.setWishlistViewMode(WishlistViewMode.GRID)

        store.setWishlistViewMode(WishlistViewMode.LIST)

        assertEquals(WishlistViewMode.LIST, store.observeWishlistViewMode().first())
    }

    @Test
    fun `a written value survives a fresh instance over the same file`() = runTest {
        val file = temporaryFolder.newFile("restart.preferences_pb")

        // DataStore refuses two live instances over the same file at once (it tracks this per-file, not
        // per-object), so the first instance's scope must be cancelled -- simulating the process exiting --
        // before a second one opens the same file, the same way a real restart would free it.
        val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val firstStore = WishlistViewModePreferenceStoreImpl(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
        )
        firstStore.setWishlistViewMode(WishlistViewMode.GRID)
        firstScope.cancel()

        val secondStore = WishlistViewModePreferenceStoreImpl(
            PreferenceDataStoreFactory.create(produceFile = { file })
        )

        assertEquals(WishlistViewMode.GRID, secondStore.observeWishlistViewMode().first())
    }

    @Test
    fun `an id this version does not know reads as LIST`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { temporaryFolder.newFile("unknown.preferences_pb") }
        )
        dataStore.edit { prefs -> prefs[intPreferencesKey("wishlist_view_mode")] = 99 }

        val store = WishlistViewModePreferenceStoreImpl(dataStore)

        assertEquals(WishlistViewMode.LIST, store.observeWishlistViewMode().first())
    }
}
