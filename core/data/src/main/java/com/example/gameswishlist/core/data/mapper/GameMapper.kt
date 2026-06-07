package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.network.model.IgdbGame
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun IgdbGame.toGame(): Game {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val releasedDate = firstReleaseDate?.let { dateFormat.format(Date(it * 1000)) }

    // IGDB cover URLs start with //, so we add https:
    val imageUrl = cover?.url?.let {
        if (it.startsWith("//")) "https:$it" else it
    }?.replace("t_thumb", "t_720p") // Better quality

    return Game(
        id = id,
        name = name,
        description = summary ?: "",
        released = releasedDate,
        backgroundImage = imageUrl,
        rating = totalRating ?: 0.0,
        platforms = platforms?.map { it.name } ?: emptyList(),
        genres = genres?.map { it.name } ?: emptyList(),
        publishers = involvedCompanies?.filter { it.publisher }?.map { it.company.name }
            ?: emptyList(),
        developers = involvedCompanies?.filter { it.developer }?.map { it.company.name }
            ?: emptyList()
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