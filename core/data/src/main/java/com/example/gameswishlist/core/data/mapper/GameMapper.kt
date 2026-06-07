package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.network.model.NetworkGame
import com.example.gameswishlist.core.network.model.NetworkGameDetail

fun NetworkGame.toGame(): Game {
    return Game(
        id = id,
        name = name,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating ?: 0.0,
        metaCritic = metacritic,
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
        metaCritic = metacritic,
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
        metaCritic = metacritic,
        platforms = if (platforms.isEmpty()) emptyList() else platforms.split(","),
        genres = if (genres.isEmpty()) emptyList() else genres.split(","),
        publishers = if (publishers.isEmpty()) emptyList() else publishers.split(","),
        developers = if (developers.isEmpty()) emptyList() else developers.split(","),
        isWishlisted = isWishlisted,
        notes = notes,
        priority = priority.toPriority(),
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
        metacritic = metaCritic,
        platforms = platforms.joinToString(","),
        genres = genres.joinToString(","),
        publishers = publishers.joinToString(","),
        developers = developers.joinToString(","),
        isWishlisted = isWishlisted,
        notes = notes,
        priority = priority.toInt(),
        status = status
    )
}

fun Priority.toInt(): Int {
    return when (this) {
        Priority.LOW -> 0
        Priority.MEDIUM -> 1
        Priority.HIGH -> 2
    }
}

fun Int.toPriority(): Priority {
    return when (this) {
        0 -> Priority.LOW
        1 -> Priority.MEDIUM
        2 -> Priority.HIGH
        else -> Priority.LOW
    }
}