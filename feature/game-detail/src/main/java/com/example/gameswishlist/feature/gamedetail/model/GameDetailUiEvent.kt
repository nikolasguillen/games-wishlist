package com.example.gameswishlist.feature.gamedetail.model

internal sealed interface GameDetailUiEvent {

    /** The user tapped "Retry" on the error state, reloading the same game the screen was opened for. */
    data object Retry : GameDetailUiEvent
    data class UpdateNotes(val notes: String) : GameDetailUiEvent
    data class UpdatePriority(val priorityId: Int) : GameDetailUiEvent
    data class UpdateStatus(val statusId: Int) : GameDetailUiEvent
    data class ToggleGameInList(val listId: Long) : GameDetailUiEvent
    data object ConfirmListSelection : GameDetailUiEvent
    data object OpenListSelector : GameDetailUiEvent
    data object DismissListSelector : GameDetailUiEvent
    data object ToggleFavorite : GameDetailUiEvent
    data object ShareGame : GameDetailUiEvent
    data class NavigateToGame(val id: Int) : GameDetailUiEvent
    data object TranslateDescription : GameDetailUiEvent
    data object ShowOriginalDescription : GameDetailUiEvent
}
