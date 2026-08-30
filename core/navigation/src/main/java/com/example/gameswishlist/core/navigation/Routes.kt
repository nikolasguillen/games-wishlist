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
data object SettingsRoute : GameNavKey

@Serializable
data object OwnedPlatformsRoute : GameNavKey

@Serializable
data class WishlistRoute(val listId: Long) : GameNavKey

@Serializable
data class GameDetailRoute(val gameId: Int) : GameNavKey
