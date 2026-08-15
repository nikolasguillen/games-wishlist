package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

/**
 * One image of a game's gallery. Unlike platforms or genres, artworks belong to a single game and are
 * never shared, so this is a child table rather than a cross-ref.
 *
 * [position] carries the order IGDB returned the images in, which the detail gallery renders as-is.
 * `@Relation` cannot sort, so the order is restored when mapping to the domain model — that is also why
 * the primary key is the position and not the URL, which IGDB does not promise to be unique per game.
 */
@Entity(
    tableName = "game_artworks",
    primaryKeys = ["gameId", "position"]
)
data class GameArtworkEntity(
    val gameId: Int,
    val position: Int,
    val url: String
)
