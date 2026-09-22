package com.example.gameswishlist.feature.radar

import com.example.gameswishlist.core.domain.radar.GetRadarTimelineUseCase
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RadarEntry
import com.example.gameswishlist.core.model.RadarTimelineSection
import com.example.gameswishlist.core.model.ReleaseBucket
import com.example.gameswishlist.core.model.ReleaseDate
import com.example.gameswishlist.feature.radar.model.RadarContentState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * [RadarViewModel.uiState] is shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so
 * [createViewModel] collects it in the background the way the screen does via
 * `collectAsStateWithLifecycle` -- the same setup as `ListsViewModelTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getRadarTimelineUseCase = mockk<GetRadarTimelineUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(sections: List<RadarTimelineSection>): RadarViewModel {
        every { getRadarTimelineUseCase() } returns flowOf(sections)
        return RadarViewModel(getRadarTimelineUseCase).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
        }
    }

    @Test
    fun `uiState starts in Loading before the first emission`() {
        every { getRadarTimelineUseCase() } returns flowOf(emptyList())

        val viewModel = RadarViewModel(getRadarTimelineUseCase)

        assertEquals(RadarContentState.Loading, viewModel.uiState.value.contentState)
    }

    @Test
    fun `uiState maps a populated timeline into a Success section list`() = runTest(testDispatcher) {
        val section = RadarTimelineSection(
            bucket = ReleaseBucket.THIS_WEEK,
            entries = listOf(
                RadarEntry(
                    game = Game(id = 1, name = "Hollow Knight: Silksong"),
                    releaseDate = ReleaseDate(date = 1_000L, platformId = 6, platformName = "PC")
                )
            )
        )

        val viewModel = createViewModel(listOf(section))

        val content = viewModel.uiState.value.contentState as RadarContentState.Success
        assertEquals(1, content.sections.size)
        assertEquals("Hollow Knight: Silksong", content.sections.single().entries.single().title)
    }

    @Test
    fun `uiState reflects an empty timeline as Empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel(emptyList())

        assertEquals(RadarContentState.Empty, viewModel.uiState.value.contentState)
    }
}
