package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.KnownGenre
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.genre_point_and_click
import com.nikolasguillen.questlog.core.ui.resources.genre_fighting
import com.nikolasguillen.questlog.core.ui.resources.genre_shooter
import com.nikolasguillen.questlog.core.ui.resources.genre_music
import com.nikolasguillen.questlog.core.ui.resources.genre_platform
import com.nikolasguillen.questlog.core.ui.resources.genre_puzzle
import com.nikolasguillen.questlog.core.ui.resources.genre_racing
import com.nikolasguillen.questlog.core.ui.resources.genre_real_time_strategy
import com.nikolasguillen.questlog.core.ui.resources.genre_role_playing
import com.nikolasguillen.questlog.core.ui.resources.genre_simulator
import com.nikolasguillen.questlog.core.ui.resources.genre_sport
import com.nikolasguillen.questlog.core.ui.resources.genre_strategy
import com.nikolasguillen.questlog.core.ui.resources.genre_turn_based_strategy
import com.nikolasguillen.questlog.core.ui.resources.genre_tactical
import com.nikolasguillen.questlog.core.ui.resources.genre_hack_and_slash
import com.nikolasguillen.questlog.core.ui.resources.genre_quiz_trivia
import com.nikolasguillen.questlog.core.ui.resources.genre_pinball
import com.nikolasguillen.questlog.core.ui.resources.genre_adventure
import com.nikolasguillen.questlog.core.ui.resources.genre_indie
import com.nikolasguillen.questlog.core.ui.resources.genre_arcade
import com.nikolasguillen.questlog.core.ui.resources.genre_visual_novel
import com.nikolasguillen.questlog.core.ui.resources.genre_card_and_board_game
import com.nikolasguillen.questlog.core.ui.resources.genre_moba

/**
 * Maps a [Genre] to the short label the UI shows for it.
 *
 * A known genre (see [KnownGenre]) resolves by id to a localisable `genre_*` resource; any other falls back to
 * the name IGDB gave it. The id decides, never the name, so a rename on IGDB's side cannot change a label.
 */
fun Genre.toLabelUiText(): UiText {
    val known = KnownGenre.fromId(id) ?: return UiText.DynamicString(name)
    val stringRes = when (known) {
        KnownGenre.POINT_AND_CLICK -> Res.string.genre_point_and_click
        KnownGenre.FIGHTING -> Res.string.genre_fighting
        KnownGenre.SHOOTER -> Res.string.genre_shooter
        KnownGenre.MUSIC -> Res.string.genre_music
        KnownGenre.PLATFORM -> Res.string.genre_platform
        KnownGenre.PUZZLE -> Res.string.genre_puzzle
        KnownGenre.RACING -> Res.string.genre_racing
        KnownGenre.REAL_TIME_STRATEGY -> Res.string.genre_real_time_strategy
        KnownGenre.ROLE_PLAYING -> Res.string.genre_role_playing
        KnownGenre.SIMULATOR -> Res.string.genre_simulator
        KnownGenre.SPORT -> Res.string.genre_sport
        KnownGenre.STRATEGY -> Res.string.genre_strategy
        KnownGenre.TURN_BASED_STRATEGY -> Res.string.genre_turn_based_strategy
        KnownGenre.TACTICAL -> Res.string.genre_tactical
        KnownGenre.HACK_AND_SLASH -> Res.string.genre_hack_and_slash
        KnownGenre.QUIZ_TRIVIA -> Res.string.genre_quiz_trivia
        KnownGenre.PINBALL -> Res.string.genre_pinball
        KnownGenre.ADVENTURE -> Res.string.genre_adventure
        KnownGenre.INDIE -> Res.string.genre_indie
        KnownGenre.ARCADE -> Res.string.genre_arcade
        KnownGenre.VISUAL_NOVEL -> Res.string.genre_visual_novel
        KnownGenre.CARD_AND_BOARD_GAME -> Res.string.genre_card_and_board_game
        KnownGenre.MOBA -> Res.string.genre_moba
    }
    return UiText.StringResource(stringRes)
}
