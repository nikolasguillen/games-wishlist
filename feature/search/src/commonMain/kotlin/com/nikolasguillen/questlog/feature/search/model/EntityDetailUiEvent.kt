package com.nikolasguillen.questlog.feature.search.model

internal sealed interface EntityDetailUiEvent {
    /** The user tapped "Retry" on whichever area failed: the page (header and description) or the games. */
    data object OnRetry : EntityDetailUiEvent

    data object TranslateDescription : EntityDetailUiEvent
    data object ShowOriginalDescription : EntityDetailUiEvent

    /** The user tapped a game's save button, toggling its membership in the default wishlist. */
    data class OnToggleSave(val gameId: Int) : EntityDetailUiEvent

    /** The user long-pressed a game, opening the list selector for it. */
    data class OnOpenListSelector(val gameId: Int) : EntityDetailUiEvent
    data class OnToggleListSelection(val listId: Long) : EntityDetailUiEvent
    data object OnConfirmListSelection : EntityDetailUiEvent
    data object OnDismissListSelector : EntityDetailUiEvent
}
