package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.network.model.NetworkGame
import com.example.gameswishlist.core.network.model.NetworkGameDetail
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus

fun NetworkGame.toGame(): Game {
    return Game(
        id = id,
        name = name,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating ?: 0.0,
        metacritic = metacritic,
        platforms = platforms?.map { it.platform.name } ?: emptyList(),
        genres = genres?.map { it.name } ?: emptyList()
    )
}

fun NetworkGameDetail.toGame(): Game {
    return Game(
        id = id,
        name = name,
        description = description ?: "",
        released = released,
        backgroundImage = backgroundImage,
        rating = rating ?: 0.0,
        metacritic = metacritic,
        platforms = platforms?.map { it.platform.name } ?: emptyList(),
        genres = genres?.map { it.name } ?: emptyList(),
        publishers = publishers?.map { it.name } ?: emptyList(),
        developers = developers?.map { it.name } ?: emptyList()
    )
}

fun GameEntity.toGame(): Game {
    return Game(
        id = id,
        name = name,
        description = description,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating,
        metacritic = metacritic,
        platforms = if (platforms.isEmpty()) emptyList() else platforms.split(","),
        genres = if (genres.isEmpty()) emptyList() else genres.split(","),
        publishers = if (publishers.isEmpty()) emptyList() else publishers.split(","),
        developers = if (developers.isEmpty()) emptyList() else developers.split(","),
        isWishlisted = isWishlisted,
        notes = notes,
        priority = priority,
        status = status
    )
}

fun Game.toEntity(): GameEntity {
    return GameEntity(
        id = id,
        name = name,
        description = description,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating,
        metacritic = metacritic,
        platforms = platforms.joinToString(","),
        genres = genres.joinToString(","),
        publishers = publishers.joinToString(","),
        developers = developers.joinToString(","),
        isWishlisted = isWishlisted,
        notes = notes,
        priority = priority,
        status = status
    )
}
