package com.nikolasguillen.questlog.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.DescriptionTranslationState
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.mapper.toGameCountText
import com.nikolasguillen.questlog.feature.search.mapper.toHeader
import com.nikolasguillen.questlog.feature.search.mapper.toSelectorItem
import com.nikolasguillen.questlog.feature.search.mapper.toTranslationSubject
import com.nikolasguillen.questlog.feature.search.model.EntityDetailSubject
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEffect
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiEvent
import com.nikolasguillen.questlog.feature.search.model.EntityDetailUiState
import com.nikolasguillen.questlog.feature.search.model.EntityGamesContentState
import com.nikolasguillen.questlog.feature.search.model.EntityHeaderUiModel
import com.nikolasguillen.questlog.feature.search.model.EntityProfileContentState
import com.nikolasguillen.questlog.feature.search.model.ListSelectorState
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.added_to_wishlist
import com.nikolasguillen.questlog.feature.search.resources.removed_from_wishlist
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The page of one character or one developer: its header and description, then every game it is linked to.
 *
 * The two areas load on their own. A character's games are known only from its profile, so they load after it; a
 * developer's games are asked for by its id, so they load beside the profile. A failure of the games leaves the
 * header and description on screen and is retried on its own.
 *
 * @param subject Whose page this is; the route's argument.
 */
class EntityDetailViewModel(
    private val subject: EntityDetailSubject,
    private val getCharacterProfileUseCase: GetCharacterProfileUseCase,
    private val getDeveloperProfileUseCase: GetDeveloperProfileUseCase,
    private val getCharacterGamesUseCase: GetCharacterGamesUseCase,
    private val getDeveloperGamesUseCase: GetDeveloperGamesUseCase,
    private val translateDescriptionUseCase: TranslateDescriptionUseCase,
    private val getTranslationModelStatusUseCase: GetTranslationModelStatusUseCase,
    private val getWishlistedGameIdsUseCase: GetWishlistedGameIdsUseCase,
    private val toggleWishlistUseCase: ToggleWishlistUseCase,
    private val getWishlistAssignmentsUseCase: GetWishlistAssignmentsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntityDetailUiState(subject = subject))
    internal val uiState: StateFlow<EntityDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<EntityDetailUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // The games as the domain holds them, so a save or a long press can find the game behind a card by its id.
    private var loadedGames: List<Game> = emptyList()

    // Membership of the default wishlist, kept beside the collector below so a freshly built grid can be stamped
    // with it synchronously instead of showing every card unsaved for one frame.
    private val wishlistedGameIds = MutableStateFlow<Set<Int>>(emptySet())

    // The raw description, which is what gets translated; the UI state only carries it inside the content state.
    private var description: String? = null

    // A character's games are looked up from the ids on its profile.
    private var characterGameIds: List<Int> = emptyList()

    private var profileJob: Job? = null
    private var gamesJob: Job? = null

    init {
        observeWishlistedGameIds()
        loadProfile()
        // A developer's games do not depend on its profile, so both requests run at once.
        if (subject is EntityDetailSubject.Developer) loadGames()
    }

    internal fun onEvent(event: EntityDetailUiEvent) {
        when (event) {
            EntityDetailUiEvent.OnRetry -> retry()
            EntityDetailUiEvent.TranslateDescription -> translateDescription()
            EntityDetailUiEvent.ShowOriginalDescription -> showOriginalDescription()
            is EntityDetailUiEvent.OnToggleSave -> toggleSave(event.gameId)
            is EntityDetailUiEvent.OnOpenListSelector -> openListSelector(event.gameId)
            is EntityDetailUiEvent.OnToggleListSelection -> toggleListSelection(event.listId)
            EntityDetailUiEvent.OnConfirmListSelection -> confirmListSelection()
            EntityDetailUiEvent.OnDismissListSelector -> _uiState.update { it.copy(listSelectorState = null) }
        }
    }

    private fun loadProfile() {
        profileJob?.cancel()
        _uiState.update { it.copy(profile = EntityProfileContentState.Loading) }
        profileJob = viewModelScope.launch {
            when (subject) {
                is EntityDetailSubject.Character -> getCharacterProfileUseCase(subject.id)
                    .onSuccess { profile ->
                        characterGameIds = profile.gameIds
                        showProfile(profile.toHeader(), profile.description)
                        loadGames()
                    }
                    .onFailure(::showProfileError)

                is EntityDetailSubject.Developer -> getDeveloperProfileUseCase(subject.id)
                    .onSuccess { profile -> showProfile(profile.toHeader(), profile.description) }
                    .onFailure(::showProfileError)
            }
        }
    }

    private suspend fun showProfile(header: EntityHeaderUiModel, description: String?) {
        this.description = description
        // The same rule as a game's description: translate only when there is text and the device can do it.
        val translation = if (description.isNullOrBlank() ||
            getTranslationModelStatusUseCase() != TranslationModelStatus.READY
        ) {
            DescriptionTranslationState.Unavailable
        } else {
            DescriptionTranslationState.Available
        }
        _uiState.update {
            it.copy(
                profile = EntityProfileContentState.Success(header = header, description = description),
                descriptionTranslation = translation
            )
        }
    }

    private fun showProfileError(error: RepositoryError) {
        _uiState.update { it.copy(profile = EntityProfileContentState.Error(error.toUiText())) }
    }

    private fun loadGames() {
        gamesJob?.cancel()
        _uiState.update { it.copy(games = EntityGamesContentState.Loading, gameCountText = null) }
        gamesJob = viewModelScope.launch {
            val result = when (subject) {
                is EntityDetailSubject.Character -> getCharacterGamesUseCase(characterGameIds)
                is EntityDetailSubject.Developer -> getDeveloperGamesUseCase(subject.id)
            }
            result
                .onSuccess(::showGames)
                .onFailure { error ->
                    _uiState.update { it.copy(games = EntityGamesContentState.Error(error.toUiText())) }
                }
        }
    }

    private fun showGames(games: List<Game>) {
        loadedGames = games
        _uiState.update {
            it.copy(
                games = if (games.isEmpty()) {
                    EntityGamesContentState.Empty
                } else {
                    EntityGamesContentState.Success(games.toGameItemList(wishlistedGameIds.value))
                },
                gameCountText = games.size.toGameCountText()
            )
        }
    }

    /**
     * Reloads whatever failed. A failed profile covers the whole page, so it is retried first; for a character
     * its success loads the games after it. A developer's games are a request of their own, retried beside it.
     */
    private fun retry() {
        val state = _uiState.value
        if (state.profile is EntityProfileContentState.Error) loadProfile()
        if (state.games is EntityGamesContentState.Error &&
            (subject is EntityDetailSubject.Developer || state.profile !is EntityProfileContentState.Error)
        ) {
            loadGames()
        }
    }

    private fun translateDescription() {
        if (_uiState.value.descriptionTranslation is DescriptionTranslationState.InProgress) return
        val description = description ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(descriptionTranslation = DescriptionTranslationState.InProgress) }
            val translation = translateDescriptionUseCase(subject.toTranslationSubject(), description)
                ?.let { DescriptionTranslationState.Ready(it) }
                ?: DescriptionTranslationState.Failed
            _uiState.update { it.copy(descriptionTranslation = translation) }
        }
    }

    private fun showOriginalDescription() {
        _uiState.update { it.copy(descriptionTranslation = DescriptionTranslationState.Available) }
    }

    /**
     * Patches each card's saved flag in place, for the whole life of the view model: a save from this page, or
     * from anywhere else, has to reach the cards here. Nothing is re-filtered or re-sorted.
     */
    private fun observeWishlistedGameIds() {
        viewModelScope.launch {
            getWishlistedGameIdsUseCase().collect { ids ->
                wishlistedGameIds.value = ids
                _uiState.update { current ->
                    val games = current.games
                    if (games is EntityGamesContentState.Success) {
                        current.copy(
                            games = games.copy(games = games.games.map { it.copy(isSaved = it.id in ids) })
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun findGame(gameId: Int): Game? = loadedGames.find { it.id == gameId }

    /**
     * No optimistic update: the toggle writes to Room, which [observeWishlistedGameIds] re-emits from, and that
     * patches the card. The confirmation names the default wishlist the toggle targets, since the user can
     * rename it.
     */
    private fun toggleSave(gameId: Int) {
        val game = findGame(gameId) ?: return
        val wasSaved = gameId in wishlistedGameIds.value

        viewModelScope.launch {
            val defaultListName = getWishlistAssignmentsUseCase(game.id).first()
                .firstOrNull { it.isDefault }?.list?.name
            toggleWishlistUseCase(game)

            defaultListName?.let {
                val messageRes =
                    if (wasSaved) Res.string.removed_from_wishlist else Res.string.added_to_wishlist
                _uiEffect.trySend(
                    EntityDetailUiEffect.ShowSnackbar(message = UiText.StringResource(messageRes, game.name, it))
                )
            }
        }
    }

    private fun openListSelector(gameId: Int) {
        val game = findGame(gameId) ?: return

        viewModelScope.launch {
            val assignments = getWishlistAssignmentsUseCase(game.id).first()
            _uiState.update {
                it.copy(
                    listSelectorState = ListSelectorState(
                        gameId = game.id,
                        gameName = game.name,
                        availableLists = assignments.map { assignment -> assignment.toSelectorItem() }
                    )
                )
            }
        }
    }

    private fun toggleListSelection(listId: Long) {
        _uiState.update { current ->
            val selectorState = current.listSelectorState ?: return@update current
            current.copy(
                listSelectorState = selectorState.copy(
                    availableLists = selectorState.availableLists.map {
                        if (it.id == listId) it.copy(isSelected = !it.isSelected) else it
                    }
                )
            )
        }
    }

    private fun confirmListSelection() {
        val selectorState = _uiState.value.listSelectorState ?: return
        // A catalogue game may not be stored yet; the use case stores it so the list can show it.
        val game = findGame(selectorState.gameId)
        if (game == null) {
            _uiState.update { it.copy(listSelectorState = null) }
            return
        }

        viewModelScope.launch {
            val originalAssignments = getWishlistAssignmentsUseCase(selectorState.gameId).first()
            val initialSelectedIds =
                originalAssignments.filter { it.isAssigned }.map { it.list.id }.toSet()
            val finalSelectedIds =
                selectorState.availableLists.filter { it.isSelected }.map { it.id }.toSet()

            (finalSelectedIds - initialSelectedIds).forEach { listId -> addGameToListUseCase(game, listId) }
            (initialSelectedIds - finalSelectedIds).forEach { listId ->
                removeGameFromListUseCase(selectorState.gameId, listId)
            }

            _uiState.update { it.copy(listSelectorState = null) }
        }
    }
}
