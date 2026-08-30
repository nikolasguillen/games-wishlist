package com.example.gameswishlist.feature.settings

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.example.gameswishlist.core.domain.usecase.discover.GetKnownPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.SetOwnedPlatformsUseCase
import com.example.gameswishlist.core.domain.usecase.discover.SyncPlatformCatalogUseCase
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsContentState
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiEvent
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the picker's selection round trip, the search filter and the entry-time ordering.
 *
 * [storedSelection] stands in for the `owned_platforms` table: the fake [SetOwnedPlatformsUseCase]
 * writes into it and [GetSelectedPlatformIdsUseCase] reads back out of it, so a tap travels the same
 * store-then-observe path it takes in the app. That round trip is the point — the picker holds no
 * copy of the selection, which is what lets deselecting the last platform leave every row unchecked
 * instead of the screen and the store disagreeing about what was saved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnedPlatformsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getKnownPlatformsUseCase = mockk<GetKnownPlatformsUseCase>()
    private val getSelectedPlatformIdsUseCase = mockk<GetSelectedPlatformIdsUseCase>()
    private val setOwnedPlatformsUseCase = mockk<SetOwnedPlatformsUseCase>()
    private val syncPlatformCatalogUseCase = mockk<SyncPlatformCatalogUseCase>()

    private val storedSelection = MutableStateFlow<Set<Int>>(emptySet())

    // Declared in the order Room returns them, i.e. sorted by name.
    private val switch = Platform(id = 130, name = "Nintendo Switch", abbreviation = "Switch")
    private val pc = Platform(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC")
    private val ps5 = Platform(id = 167, name = "PlayStation 5", abbreviation = "PS5")
    private val catalogue = listOf(switch, pc, ps5)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getSelectedPlatformIdsUseCase() } returns storedSelection
        coEvery { setOwnedPlatformsUseCase(any()) } answers { storedSelection.value = firstArg() }
        coEvery { syncPlatformCatalogUseCase() } returns AppResult.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OwnedPlatformsViewModel(
        getKnownPlatformsUseCase = getKnownPlatformsUseCase,
        getSelectedPlatformIdsUseCase = getSelectedPlatformIdsUseCase,
        setOwnedPlatformsUseCase = setOwnedPlatformsUseCase,
        syncPlatformCatalogUseCase = syncPlatformCatalogUseCase
    )

    private fun CoroutineScope.collectStates(
        viewModel: OwnedPlatformsViewModel,
        states: MutableList<OwnedPlatformsUiState>
    ) = launch { viewModel.uiState.collect { states.add(it) } }

    /** Mutates the search field and forces the snapshot system to notify observers. */
    private fun OwnedPlatformsViewModel.setQuery(query: String) {
        textFieldState.setTextAndPlaceCursorAtEnd(query)
        Snapshot.sendApplyNotifications()
    }

    private fun List<OwnedPlatformsUiState>.lastSuccess(): OwnedPlatformsContentState.Success =
        last().contentState as OwnedPlatformsContentState.Success

    @Test
    fun `marks the stored platforms as selected and leaves the rest unselected`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
        storedSelection.value = setOf(ps5.id, pc.id)

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        val selected = states.lastSuccess().platforms.filter { it.isSelected }.map { it.name }
        assertEquals(listOf("PC (Microsoft Windows)", "PlayStation 5"), selected.sorted())
        assertEquals(2, states.last().selectedCount)
        job.cancel()
    }

    @Test
    fun `stays on Loading until the catalogue, the selection and the entry order have arrived`() =
        runTest {
            every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
            every { getSelectedPlatformIdsUseCase() } returns MutableSharedFlow()

            val viewModel = viewModel()
            val states = mutableListOf<OwnedPlatformsUiState>()
            val job = collectStates(viewModel, states)
            advanceUntilIdle()

            assertTrue(states.all { it.contentState is OwnedPlatformsContentState.Loading })
            job.cancel()
        }

    @Test
    fun `reports Empty when nothing is cached to pick from`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(emptyList())

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        assertTrue(states.last().contentState is OwnedPlatformsContentState.Empty)
        job.cancel()
    }

    @Test
    fun `refreshes the platform catalogue on open`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)

        viewModel()
        advanceUntilIdle()

        coVerify { syncPlatformCatalogUseCase() }
    }

    @Test
    fun `selecting a platform persists the whole new set and checks the row`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
        storedSelection.value = setOf(ps5.id)

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(switch.id))
        advanceUntilIdle()

        coVerify { setOwnedPlatformsUseCase(setOf(ps5.id, switch.id)) }
        val selected = states.lastSuccess().platforms.filter { it.isSelected }.map { it.id }
        assertEquals(setOf(ps5.id, switch.id), selected.toSet())
        job.cancel()
    }

    @Test
    fun `deselecting the last platform stores an empty set and leaves every row unchecked`() =
        runTest {
            every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
            storedSelection.value = setOf(ps5.id)

            val viewModel = viewModel()
            val states = mutableListOf<OwnedPlatformsUiState>()
            val job = collectStates(viewModel, states)
            advanceUntilIdle()

            viewModel.onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(ps5.id))
            advanceUntilIdle()

            coVerify { setOwnedPlatformsUseCase(emptySet()) }
            assertTrue(states.lastSuccess().platforms.none { it.isSelected })
            assertEquals(0, states.last().selectedCount)
            job.cancel()
        }

    @Test
    fun `two taps in quick succession both survive`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
        storedSelection.value = emptySet()

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(ps5.id))
        viewModel.onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(switch.id))
        advanceUntilIdle()

        assertEquals(setOf(ps5.id, switch.id), storedSelection.value)
        job.cancel()
    }

    @Test
    fun `the search field matches on name and on abbreviation`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.setQuery("nintendo")
        advanceUntilIdle()
        assertEquals(listOf("Nintendo Switch"), states.lastSuccess().platforms.map { it.name })

        viewModel.setQuery("ps5")
        advanceUntilIdle()
        assertEquals(listOf("PlayStation 5"), states.lastSuccess().platforms.map { it.name })

        job.cancel()
    }

    @Test
    fun `reports NoSearchResults when the query matches nothing`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.setQuery("Dreamcast")
        advanceUntilIdle()

        assertTrue(states.last().contentState is OwnedPlatformsContentState.NoSearchResults)
        job.cancel()
    }

    @Test
    fun `floats the platforms selected on entry to the top and keeps that order while tapping`() =
        runTest {
            every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
            storedSelection.value = setOf(ps5.id)

            val viewModel = viewModel()
            val states = mutableListOf<OwnedPlatformsUiState>()
            val job = collectStates(viewModel, states)
            advanceUntilIdle()

            val onEntry = listOf("PlayStation 5", "Nintendo Switch", "PC (Microsoft Windows)")
            assertEquals(onEntry, states.lastSuccess().platforms.map { it.name })

            // Selecting another platform must not pull it up under the finger.
            viewModel.onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(pc.id))
            advanceUntilIdle()

            assertEquals(onEntry, states.lastSuccess().platforms.map { it.name })
            job.cancel()
        }

    @Test
    fun `a query drops the entry ordering and shows plain catalogue order`() = runTest {
        every { getKnownPlatformsUseCase() } returns flowOf(catalogue)
        storedSelection.value = setOf(ps5.id)

        val viewModel = viewModel()
        val states = mutableListOf<OwnedPlatformsUiState>()
        val job = collectStates(viewModel, states)
        advanceUntilIdle()

        viewModel.setQuery("p")
        advanceUntilIdle()

        assertEquals(
            listOf("PC (Microsoft Windows)", "PlayStation 5"),
            states.lastSuccess().platforms.map { it.name }
        )
        job.cancel()
    }
}
