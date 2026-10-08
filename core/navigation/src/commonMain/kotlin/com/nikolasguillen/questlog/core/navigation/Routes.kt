package com.nikolasguillen.questlog.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every destination in the app. Adding a route takes three edits outside its feature: a subclass here, its
 * `subclass(...)` line in [GameNavSavedStateConfiguration] (the non-Android targets cannot restore a back
 * stack without it) and a branch in the single `entryProvider`.
 */
@Serializable
sealed interface GameNavKey : NavKey

@Serializable
data object SearchRoute : GameNavKey

@Serializable
data object ListsRoute : GameNavKey

@Serializable
data object RadarRoute : GameNavKey

@Serializable
data object SettingsRoute : GameNavKey

@Serializable
data object OwnedPlatformsRoute : GameNavKey

@Serializable
data object ReleaseNotificationsRoute : GameNavKey

@Serializable
data object OnboardingRoute : GameNavKey

@Serializable
data class WishlistRoute(val listId: Long) : GameNavKey

@Serializable
data class GameDetailRoute(val gameId: Int) : GameNavKey
