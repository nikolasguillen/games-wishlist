package com.nikolasguillen.questlog.feature.search.model

/**
 * What a search looks up. One search asks for exactly one type, so a results grid never mixes them.
 *
 * - [GAMES]: games by title. Also the only type with game suggestions while typing.
 * - [CHARACTERS]: game characters by name.
 * - [DEVELOPERS]: companies credited as the developer of at least one game, by name.
 */
internal enum class SearchType {
    GAMES,
    CHARACTERS,
    DEVELOPERS
}
