package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nikolasguillen.questlog.core.model.AppearanceMode
import kotlinx.coroutines.CoroutineScope
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
 * Covers [AppearancePreferenceStoreImpl] against a real, temp-file-backed `DataStore<Preferences>` — no
 * Android `Context`/Robolectric involved, since the instance is injected rather than built from a
 * `Context` inside the class. Proves the persistence-layer half of FR-002 (defaults to [AppearanceMode.SYSTEM])
 * and FR-009/SC-003 (a value survives a fresh instance over the same file, the same guarantee a process
 * restart gives).
 */
class AppearancePreferenceStoreImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun newStore(): AppearancePreferenceStoreImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { temporaryFolder.newFile("test.preferences_pb") }
        )
        return AppearancePreferenceStoreImpl(dataStore)
    }

    @Test
    fun `reading before any write returns SYSTEM`() = runTest {
        val store = newStore()

        assertEquals(AppearanceMode.SYSTEM, store.observeAppearanceMode().first())
    }

    @Test
    fun `setAppearanceMode then observeAppearanceMode emits the new value`() = runTest {
        val store = newStore()

        store.setAppearanceMode(AppearanceMode.LIGHT)

        assertEquals(AppearanceMode.LIGHT, store.observeAppearanceMode().first())
    }

    @Test
    fun `a written value survives a fresh instance over the same file`() = runTest {
        val file = temporaryFolder.newFile("restart.preferences_pb")

        // DataStore refuses two live instances over the same file at once (it tracks this per-file, not
        // per-object), so the first instance's scope must be cancelled -- simulating the process exiting --
        // before a second one opens the same file, the same way a real restart would free it.
        val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val firstStore = AppearancePreferenceStoreImpl(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
        )
        firstStore.setAppearanceMode(AppearanceMode.DARK)
        firstScope.cancel()

        val secondStore = AppearancePreferenceStoreImpl(
            PreferenceDataStoreFactory.create(produceFile = { file })
        )

        assertEquals(AppearanceMode.DARK, secondStore.observeAppearanceMode().first())
    }
}
