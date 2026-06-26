package com.example.gameswishlist.core.ui.mapper

import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText

/**
 * Maps a [GameType] to its corresponding [UiText] representation.
 */
fun GameType.toUiText(): UiText {
    val stringResId = when (this) {
        GameType.MAIN_GAME -> R.string.gametype_main_game
        GameType.DLC_ADDON -> R.string.gametype_dlc_addon
        GameType.EXPANSION -> R.string.gametype_expansion
        GameType.BUNDLE -> R.string.gametype_bundle
        GameType.STANDALONE_EXPANSION -> R.string.gametype_standalone_expansion
        GameType.MOD -> R.string.gametype_mod
        GameType.EPISODE -> R.string.gametype_episode
        GameType.SEASON -> R.string.gametype_season
        GameType.REMAKE -> R.string.gametype_remake
        GameType.REMASTER -> R.string.gametype_remaster
        GameType.EXPANDED_GAME -> R.string.gametype_expanded_game
        GameType.PORT -> R.string.gametype_port
        GameType.FORK -> R.string.gametype_fork
        GameType.PACK -> R.string.gametype_pack
        GameType.UPDATE -> R.string.gametype_update
    }
    return UiText.StringResource(stringResId)
}
