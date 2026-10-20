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

/** Sign in, back up and (later) restore the user's data in the cloud. Reached from the Settings hub. */
@Serializable
data object AccountBackupRoute : GameNavKey

@Serializable
data object OnboardingRoute : GameNavKey

@Serializable
data class WishlistRoute(val listId: Long) : GameNavKey

/**
 * A game's detail.
 *
 * @param fromSearch Whether it was opened from the Search flow (the results, the Discover feed, a "see all" shelf,
 * a genre browse, or a similar game inside another detail that was). Only these details take part in the two-pane
 * layout on a wide window; one opened from Radar, a wishlist or a release reminder is always full-screen. A route
 * saved before this field existed restores as `false`.
 */
@Serializable
data class GameDetailRoute(val gameId: Int, val fromSearch: Boolean = false) : GameNavKey

/**
 * A game character's detail page: its portrait, description and the games it appears in. Reached from a
 * Characters search; always full-screen, never part of the two-pane Search layout.
 */
@Serializable
data class CharacterDetailRoute(val characterId: Int) : GameNavKey

/**
 * A developer's detail page: its logo, description and every game it developed. Reached from a Developers
 * search; always full-screen, never part of the two-pane Search layout.
 */
@Serializable
data class DeveloperDetailRoute(val developerId: Int) : GameNavKey
