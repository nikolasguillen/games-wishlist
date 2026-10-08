package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.GameType
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.gametype_bundle
import com.nikolasguillen.questlog.core.ui.resources.gametype_dlc_addon
import com.nikolasguillen.questlog.core.ui.resources.gametype_episode
import com.nikolasguillen.questlog.core.ui.resources.gametype_expanded_game
import com.nikolasguillen.questlog.core.ui.resources.gametype_expansion
import com.nikolasguillen.questlog.core.ui.resources.gametype_fork
import com.nikolasguillen.questlog.core.ui.resources.gametype_main_game
import com.nikolasguillen.questlog.core.ui.resources.gametype_mod
import com.nikolasguillen.questlog.core.ui.resources.gametype_pack
import com.nikolasguillen.questlog.core.ui.resources.gametype_port
import com.nikolasguillen.questlog.core.ui.resources.gametype_remake
import com.nikolasguillen.questlog.core.ui.resources.gametype_remaster
import com.nikolasguillen.questlog.core.ui.resources.gametype_season
import com.nikolasguillen.questlog.core.ui.resources.gametype_standalone_expansion
import com.nikolasguillen.questlog.core.ui.resources.gametype_update

/**
 * Maps a [GameType] to its corresponding [UiText] representation.
 */
fun GameType.toUiText(): UiText {
    val stringResId = when (this) {
        GameType.MAIN_GAME -> Res.string.gametype_main_game
        GameType.DLC_ADDON -> Res.string.gametype_dlc_addon
        GameType.EXPANSION -> Res.string.gametype_expansion
        GameType.BUNDLE -> Res.string.gametype_bundle
        GameType.STANDALONE_EXPANSION -> Res.string.gametype_standalone_expansion
        GameType.MOD -> Res.string.gametype_mod
        GameType.EPISODE -> Res.string.gametype_episode
        GameType.SEASON -> Res.string.gametype_season
        GameType.REMAKE -> Res.string.gametype_remake
        GameType.REMASTER -> Res.string.gametype_remaster
        GameType.EXPANDED_GAME -> Res.string.gametype_expanded_game
        GameType.PORT -> Res.string.gametype_port
        GameType.FORK -> Res.string.gametype_fork
        GameType.PACK -> Res.string.gametype_pack
        GameType.UPDATE -> Res.string.gametype_update
    }
    return UiText.StringResource(stringResId)
}
