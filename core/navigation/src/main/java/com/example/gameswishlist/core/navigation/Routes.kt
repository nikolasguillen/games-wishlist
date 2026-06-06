package com.example.gameswishlist.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameNavKey : NavKey

@Serializable
data object SearchRoute : GameNavKey

@Serializable
data object ListsRoute : GameNavKey

@Serializable
data class WishlistRoute(val listId: Long, val listName: String) : GameNavKey

@Serializable
data class GameDetailRoute(val gameId: Int) : GameNavKey
