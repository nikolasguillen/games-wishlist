package com.example.gameswishlist.feature.gamedetail.model

sealed interface GameDetailUiEvent {
    data class LoadGame(val id: Int) : GameDetailUiEvent
    data class UpdateNotes(val notes: String) : GameDetailUiEvent
    data class UpdatePriority(val priorityId: Int) : GameDetailUiEvent
    data class UpdateStatus(val statusId: Int) : GameDetailUiEvent
    data class AddGameToList(val listId: Long) : GameDetailUiEvent
    data object OpenListSelector : GameDetailUiEvent
    data object DismissListSelector : GameDetailUiEvent
    data object ToggleFavorite : GameDetailUiEvent
    data object ShareGame : GameDetailUiEvent
}
