package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.SearchType
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.search_placeholder
import com.nikolasguillen.questlog.feature.search.resources.search_placeholder_characters
import com.nikolasguillen.questlog.feature.search.resources.search_placeholder_developers
import com.nikolasguillen.questlog.feature.search.resources.search_type_characters
import com.nikolasguillen.questlog.feature.search.resources.search_type_developers
import com.nikolasguillen.questlog.feature.search.resources.search_type_games

/** The chip label of this type. */
internal fun SearchType.toLabelUiText(): UiText = UiText.StringResource(
    when (this) {
        SearchType.GAMES -> Res.string.search_type_games
        SearchType.CHARACTERS -> Res.string.search_type_characters
        SearchType.DEVELOPERS -> Res.string.search_type_developers
    }
)

/** The hint the search field shows while it is empty and this type is selected. */
internal fun SearchType.toPlaceholderUiText(): UiText = UiText.StringResource(
    when (this) {
        SearchType.GAMES -> Res.string.search_placeholder
        SearchType.CHARACTERS -> Res.string.search_placeholder_characters
        SearchType.DEVELOPERS -> Res.string.search_placeholder_developers
    }
)
