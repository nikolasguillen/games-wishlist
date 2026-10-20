package com.nikolasguillen.questlog.feature.search

import com.nikolasguillen.questlog.core.domain.model.WishlistAssignment
import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject
import com.nikolasguillen.questlog.core.domain.usecase.ToggleWishlistUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.AddGameToListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.profile.GetCharacterGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.profile.GetCharacterProfileUseCase
import com.nikolasguillen.questlog.core.domain.usecase.profile.GetDeveloperGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.profile.GetDeveloperProfileUseCase
import com.nikolasguillen.questlog.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.nikolasguillen.questlog.core.domain.usecase.translation.TranslateDescriptionUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.CharacterProfile
import com.nikolasguillen.questlog.core.model.DeveloperProfile
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import com.nikolasguillen.questlog.core.model.WishlistList
import com.nikolasguillen.questlog.core.ui.model.DescriptionTranslationState
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.EntityDetailSubject
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEffect
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEvent
import com.nikolasguillen.questlog.feature.search.model.EntityGamesContentState
import com.nikolasguillen.questlog.feature.search.model.EntityProfileContentState
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.added_to_wishlist
import com.nikolasguillen.questlog.feature.search.resources.entity_founded_year
import com.nikolasguillen.questlog.feature.search.resources.entity_game_count
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers [EntityDetailViewModel]: the order in which a character's and a developer's page load, the chip text, the
 * translation of the description, saving a game and the list selector. The use cases are mocked, so what is pinned
 * here is what the view model asks for and what it puts in the state, not the data behind them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntityDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getCharacterProfileUseCase = mockk<GetCharacterProfileUseCase>()
    private val getDeveloperProfileUseCase = mockk<GetDeveloperProfileUseCase>()
    private val getCharacterGamesUseCase = mockk<GetCharacterGamesUseCase>()
    private val getDeveloperGamesUseCase = mockk<GetDeveloperGamesUseCase>()
    private val translateDescriptionUseCase = mockk<TranslateDescriptionUseCase>()
    private val getTranslationModelStatusUseCase = mockk<GetTranslationModelStatusUseCase>()
    private val getWishlistedGameIdsUseCase = mockk<GetWishlistedGameIdsUseCase>()
    private val toggleWishlistUseCase = mockk<ToggleWishlistUseCase>(relaxed = true)
    private val getWishlistAssignmentsUseCase = mockk<GetWishlistAssignmentsUseCase>()
    private val addGameToListUseCase = mockk<AddGameToListUseCase>(relaxed = true)
    private val removeGameFromListUseCase = mockk<RemoveGameFromListUseCase>(relaxed = true)

    private val character = EntityDetailSubject.Character(7)
    private val developer = EntityDetailSubject.Developer(401)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getWishlistedGameIdsUseCase() } returns flowOf(emptySet())
        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.READY
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun game(id: Int, name: String = "Game $id") = Game(id = id, name = name)

    private fun characterProfile(description: String? = "The Ghost of Sparta.", gameIds: List<Int> = listOf(1, 2)) =
        CharacterProfile(id = 7, name = "Kratos", imageUrl = "https://img/k.jpg", description = description, gameIds = gameIds)

    private fun developerProfile(description: String? = "A studio.", foundedYear: Int? = 1984) =
        DeveloperProfile(id = 401, name = "Naughty Dog", logoUrl = null, description = description, foundedYear = foundedYear)

    private fun givenCharacter(
        profile: CharacterProfile = characterProfile(),
        games: List<Game> = listOf(game(1), game(2))
    ) {
        coEvery { getCharacterProfileUseCase(7) } returns AppResult.success(profile)
        coEvery { getCharacterGamesUseCase(profile.gameIds) } returns AppResult.success(games)
    }

    private fun givenDeveloper(
        profile: DeveloperProfile = developerProfile(),
        games: List<Game> = listOf(game(1), game(2))
    ) {
        coEvery { getDeveloperProfileUseCase(401) } returns AppResult.success(profile)
        coEvery { getDeveloperGamesUseCase(401) } returns AppResult.success(games)
    }

    private fun TestScope.createViewModel(subject: EntityDetailSubject): EntityDetailViewModel =
        EntityDetailViewModel(
            subject = subject,
            getCharacterProfileUseCase = getCharacterProfileUseCase,
            getDeveloperProfileUseCase = getDeveloperProfileUseCase,
            getCharacterGamesUseCase = getCharacterGamesUseCase,
            getDeveloperGamesUseCase = getDeveloperGamesUseCase,
            translateDescriptionUseCase = translateDescriptionUseCase,
            getTranslationModelStatusUseCase = getTranslationModelStatusUseCase,
            getWishlistedGameIdsUseCase = getWishlistedGameIdsUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        ).also { advanceUntilIdle() }

    private fun EntityDetailViewModel.profileSuccess() =
        uiState.value.profile as EntityProfileContentState.Success

    private fun EntityDetailViewModel.gamesSuccess() =
        uiState.value.games as EntityGamesContentState.Success

    // region Loading

    @Test
    fun `a character page loads the profile and then asks for the games of the ids on it`() =
        runTest(testDispatcher) {
            givenCharacter(profile = characterProfile(gameIds = listOf(5, 9)), games = listOf(game(5), game(9)))
            coEvery { getCharacterGamesUseCase(listOf(5, 9)) } returns AppResult.success(listOf(game(5), game(9)))

            val viewModel = createViewModel(character)

            assertEquals("Kratos", viewModel.profileSuccess().header.name)
            assertEquals(listOf(5, 9), viewModel.gamesSuccess().games.map { it.id })
            coVerify(exactly = 1) { getCharacterGamesUseCase(listOf(5, 9)) }
            coVerify(exactly = 0) { getDeveloperProfileUseCase(any()) }
            coVerify(exactly = 0) { getDeveloperGamesUseCase(any()) }
        }

    @Test
    fun `a developer page runs the profile and the games requests both`() = runTest(testDispatcher) {
        givenDeveloper()

        val viewModel = createViewModel(developer)

        assertEquals("Naughty Dog", viewModel.profileSuccess().header.name)
        assertEquals(listOf(1, 2), viewModel.gamesSuccess().games.map { it.id })
        coVerify(exactly = 1) { getDeveloperProfileUseCase(401) }
        coVerify(exactly = 1) { getDeveloperGamesUseCase(401) }
        coVerify(exactly = 0) { getCharacterProfileUseCase(any()) }
    }

    @Test
    fun `a developer's games do not wait for its profile`() = runTest(testDispatcher) {
        coEvery { getDeveloperProfileUseCase(401) } returns AppResult.success(developerProfile())
        coEvery { getDeveloperGamesUseCase(401) } returns AppResult.success(listOf(game(1)))

        val viewModel = createViewModel(developer)

        // Both use cases were called once the dispatcher drained: neither had to finish before the other started.
        coVerify(exactly = 1) { getDeveloperProfileUseCase(401) }
        coVerify(exactly = 1) { getDeveloperGamesUseCase(401) }
        assertTrue(viewModel.uiState.value.profile is EntityProfileContentState.Success)
        assertTrue(viewModel.uiState.value.games is EntityGamesContentState.Success)
    }

    @Test
    fun `the page starts with the profile and the games loading`() = runTest(testDispatcher) {
        givenCharacter()

        val viewModel = EntityDetailViewModel(
            subject = character,
            getCharacterProfileUseCase = getCharacterProfileUseCase,
            getDeveloperProfileUseCase = getDeveloperProfileUseCase,
            getCharacterGamesUseCase = getCharacterGamesUseCase,
            getDeveloperGamesUseCase = getDeveloperGamesUseCase,
            translateDescriptionUseCase = translateDescriptionUseCase,
            getTranslationModelStatusUseCase = getTranslationModelStatusUseCase,
            getWishlistedGameIdsUseCase = getWishlistedGameIdsUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
        )

        assertEquals(EntityProfileContentState.Loading, viewModel.uiState.value.profile)
        assertEquals(EntityGamesContentState.Loading, viewModel.uiState.value.games)
        assertNull(viewModel.uiState.value.gameCountText)
    }

    @Test
    fun `a developer's header carries the founding year chip and a character's does not`() =
        runTest(testDispatcher) {
            givenDeveloper(profile = developerProfile(foundedYear = 1984))
            val developerModel = createViewModel(developer)
            givenCharacter()
            val characterModel = createViewModel(character)

            assertEquals(
                UiText.StringResource(Res.string.entity_founded_year, 1984),
                developerModel.profileSuccess().header.foundedYearText
            )
            assertNull(characterModel.profileSuccess().header.foundedYearText)
        }

    @Test
    fun `a developer without a founding date has no founding chip`() = runTest(testDispatcher) {
        givenDeveloper(profile = developerProfile(foundedYear = null))

        val viewModel = createViewModel(developer)

        assertNull(viewModel.profileSuccess().header.foundedYearText)
    }

    // endregion

    // region Games count

    @Test
    fun `the game count chip follows the number of games, singular for one`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(gameIds = listOf(1)), games = listOf(game(1)))
        val one = createViewModel(character)
        givenDeveloper(games = listOf(game(1), game(2)))
        val two = createViewModel(developer)

        assertEquals(UiText.PluralResource(Res.plurals.entity_game_count, 1, 1), one.uiState.value.gameCountText)
        assertEquals(UiText.PluralResource(Res.plurals.entity_game_count, 2, 2), two.uiState.value.gameCountText)
    }

    @Test
    fun `no games is the empty state with a count of zero`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(gameIds = emptyList()), games = emptyList())

        val viewModel = createViewModel(character)

        assertEquals(EntityGamesContentState.Empty, viewModel.uiState.value.games)
        assertEquals(UiText.PluralResource(Res.plurals.entity_game_count, 0, 0), viewModel.uiState.value.gameCountText)
    }

    // endregion

    // region Description and translation

    @Test
    fun `the description can be translated only when it exists and the model is ready`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(description = "A hero."))
        assertEquals(DescriptionTranslationState.Available, createViewModel(character).uiState.value.descriptionTranslation)

        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.DOWNLOADABLE
        assertEquals(DescriptionTranslationState.Unavailable, createViewModel(character).uiState.value.descriptionTranslation)

        coEvery { getTranslationModelStatusUseCase() } returns TranslationModelStatus.READY
        givenCharacter(profile = characterProfile(description = null))
        val noDescription = createViewModel(character)
        assertEquals(DescriptionTranslationState.Unavailable, noDescription.uiState.value.descriptionTranslation)
        assertNull(noDescription.profileSuccess().description)
    }

    @Test
    fun `translating goes through in progress to the translated text`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(description = "A hero."))
        val translation = CompletableDeferred<String?>()
        coEvery { translateDescriptionUseCase(TranslationSubject.Character(7), "A hero.") } coAnswers { translation.await() }
        val viewModel = createViewModel(character)

        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()
        assertEquals(DescriptionTranslationState.InProgress, viewModel.uiState.value.descriptionTranslation)

        translation.complete("Un eroe.")
        advanceUntilIdle()

        assertEquals(DescriptionTranslationState.Ready("Un eroe."), viewModel.uiState.value.descriptionTranslation)
    }

    @Test
    fun `a developer's description is translated under the developer subject`() = runTest(testDispatcher) {
        givenDeveloper(profile = developerProfile(description = "A studio."))
        coEvery { translateDescriptionUseCase(TranslationSubject.Developer(401), "A studio.") } returns "Uno studio."
        val viewModel = createViewModel(developer)

        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()

        coVerify(exactly = 1) { translateDescriptionUseCase(TranslationSubject.Developer(401), "A studio.") }
        assertEquals(DescriptionTranslationState.Ready("Uno studio."), viewModel.uiState.value.descriptionTranslation)
    }

    @Test
    fun `a translation that returns nothing is a failure, and asking again retries`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(description = "A hero."))
        coEvery { translateDescriptionUseCase(any(), any()) } returns null andThen "Un eroe."
        val viewModel = createViewModel(character)

        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()
        assertEquals(DescriptionTranslationState.Failed, viewModel.uiState.value.descriptionTranslation)

        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()
        assertEquals(DescriptionTranslationState.Ready("Un eroe."), viewModel.uiState.value.descriptionTranslation)
    }

    @Test
    fun `a second translate while one is in progress is ignored`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(description = "A hero."))
        val translation = CompletableDeferred<String?>()
        coEvery { translateDescriptionUseCase(any(), any()) } coAnswers { translation.await() }
        val viewModel = createViewModel(character)

        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()
        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        translation.complete("Un eroe.")
        advanceUntilIdle()

        coVerify(exactly = 1) { translateDescriptionUseCase(any(), any()) }
    }

    @Test
    fun `show original goes back to the translate action`() = runTest(testDispatcher) {
        givenCharacter(profile = characterProfile(description = "A hero."))
        coEvery { translateDescriptionUseCase(any(), any()) } returns "Un eroe."
        val viewModel = createViewModel(character)
        viewModel.onEvent(EntityDetailUiEvent.TranslateDescription)
        advanceUntilIdle()

        viewModel.onEvent(EntityDetailUiEvent.ShowOriginalDescription)

        assertEquals(DescriptionTranslationState.Available, viewModel.uiState.value.descriptionTranslation)
    }

    // endregion

    // region Saving

    @Test
    fun `saving a game toggles it and the snackbar names the default list`() = runTest(testDispatcher) {
        val saved = game(1, name = "God of War")
        givenCharacter(games = listOf(saved, game(2)))
        every { getWishlistAssignmentsUseCase(1) } returns flowOf(
            listOf(WishlistAssignment(WishlistList(id = 10, name = "Backlog"), isAssigned = false, isDefault = true))
        )
        val viewModel = createViewModel(character)
        val effects = mutableListOf<EntityDetailUiEffect>()
        val collectJob = launch { viewModel.uiEffect.toList(effects) }

        viewModel.onEvent(EntityDetailUiEvent.OnToggleSave(1))
        advanceUntilIdle()

        coVerify { toggleWishlistUseCase(saved) }
        val effect = effects.single() as EntityDetailUiEffect.ShowSnackbar
        assertEquals(UiText.StringResource(Res.string.added_to_wishlist, "God of War", "Backlog"), effect.message)
        collectJob.cancel()
    }

    @Test
    fun `saving a game the page does not hold does nothing`() = runTest(testDispatcher) {
        givenCharacter()
        val viewModel = createViewModel(character)

        viewModel.onEvent(EntityDetailUiEvent.OnToggleSave(999))
        advanceUntilIdle()

        coVerify(exactly = 0) { toggleWishlistUseCase(any()) }
    }

    @Test
    fun `a wishlisted-ids emission marks the cards saved in place`() = runTest(testDispatcher) {
        val ids = MutableStateFlow<Set<Int>>(emptySet())
        every { getWishlistedGameIdsUseCase() } returns ids
        givenCharacter()
        val viewModel = createViewModel(character)
        assertFalse(viewModel.gamesSuccess().games.first { it.id == 1 }.isSaved)

        ids.value = setOf(1)
        advanceUntilIdle()

        assertTrue(viewModel.gamesSuccess().games.first { it.id == 1 }.isSaved)
        assertFalse(viewModel.gamesSuccess().games.first { it.id == 2 }.isSaved)
    }

    @Test
    fun `games that arrive after the saved ids are already marked`() = runTest(testDispatcher) {
        every { getWishlistedGameIdsUseCase() } returns flowOf(setOf(2))
        givenCharacter()

        val viewModel = createViewModel(character)

        assertTrue(viewModel.gamesSuccess().games.first { it.id == 2 }.isSaved)
    }

    @Test
    fun `the list selector opens with the lists and confirms only what changed`() = runTest(testDispatcher) {
        val listA = WishlistList(id = 10, name = "Wishlist")
        val listB = WishlistList(id = 20, name = "Backlog")
        val listC = WishlistList(id = 30, name = "Completed")
        givenCharacter(games = listOf(game(1), game(2)))
        every { getWishlistAssignmentsUseCase(1) } returns flowOf(
            listOf(
                WishlistAssignment(listA, isAssigned = true, isDefault = false),
                WishlistAssignment(listB, isAssigned = false, isDefault = false),
                WishlistAssignment(listC, isAssigned = false, isDefault = false)
            )
        )
        val viewModel = createViewModel(character)

        viewModel.onEvent(EntityDetailUiEvent.OnOpenListSelector(1))
        advanceUntilIdle()
        assertEquals(listOf(10L, 20L, 30L), viewModel.uiState.value.listSelectorState!!.availableLists.map { it.id })

        viewModel.onEvent(EntityDetailUiEvent.OnToggleListSelection(20))
        viewModel.onEvent(EntityDetailUiEvent.OnToggleListSelection(10))
        viewModel.onEvent(EntityDetailUiEvent.OnConfirmListSelection)
        advanceUntilIdle()

        coVerify(exactly = 1) { addGameToListUseCase(game(1), 20) }
        coVerify(exactly = 1) { removeGameFromListUseCase(1, 10) }
        coVerify(exactly = 0) { addGameToListUseCase(any(), 30) }
        assertNull(viewModel.uiState.value.listSelectorState)
    }

    @Test
    fun `dismissing the list selector closes it without changing anything`() = runTest(testDispatcher) {
        givenCharacter()
        every { getWishlistAssignmentsUseCase(1) } returns flowOf(
            listOf(WishlistAssignment(WishlistList(id = 10, name = "Wishlist"), isAssigned = false, isDefault = true))
        )
        val viewModel = createViewModel(character)
        viewModel.onEvent(EntityDetailUiEvent.OnOpenListSelector(1))
        advanceUntilIdle()

        viewModel.onEvent(EntityDetailUiEvent.OnDismissListSelector)

        assertNull(viewModel.uiState.value.listSelectorState)
        coVerify(exactly = 0) { addGameToListUseCase(any(), any()) }
        coVerify(exactly = 0) { removeGameFromListUseCase(any(), any()) }
    }

    // endregion
}
