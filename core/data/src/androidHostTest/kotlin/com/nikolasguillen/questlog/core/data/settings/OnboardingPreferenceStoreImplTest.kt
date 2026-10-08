package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers [OnboardingPreferenceStoreImpl] against a real, temp-file-backed `DataStore<Preferences>` — no
 * Android `Context`/Robolectric involved, since the instance is injected. Proves the persistence half of
 * FR-002: the flag defaults to `false`, and a value survives a fresh instance over the same file, the same
 * guarantee a process restart gives.
 */
class OnboardingPreferenceStoreImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun newStore(): OnboardingPreferenceStoreImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { temporaryFolder.newFile("test.preferences_pb") }
        )
        return OnboardingPreferenceStoreImpl(dataStore)
    }

    @Test
    fun `reading before any write returns false`() = runTest {
        val store = newStore()

        assertFalse(store.observeOnboardingCompleted().first())
    }

    @Test
    fun `setOnboardingCompleted then observeOnboardingCompleted emits true`() = runTest {
        val store = newStore()

        store.setOnboardingCompleted()

        assertTrue(store.observeOnboardingCompleted().first())
    }

    @Test
    fun `setting the flag twice is harmless`() = runTest {
        val store = newStore()

        store.setOnboardingCompleted()
        store.setOnboardingCompleted()

        assertTrue(store.observeOnboardingCompleted().first())
    }

    @Test
    fun `a written value survives a fresh instance over the same file`() = runTest {
        val file = temporaryFolder.newFile("restart.preferences_pb")

        // DataStore refuses two live instances over the same file at once (it tracks this per-file, not
        // per-object), so the first instance's scope must be cancelled -- simulating the process exiting --
        // before a second one opens the same file, the same way a real restart would free it.
        val firstScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob())
        val firstStore = OnboardingPreferenceStoreImpl(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
        )
        firstStore.setOnboardingCompleted()
        firstScope.cancel()

        val secondStore = OnboardingPreferenceStoreImpl(
            PreferenceDataStoreFactory.create(produceFile = { file })
        )

        assertTrue(secondStore.observeOnboardingCompleted().first())
    }
}
