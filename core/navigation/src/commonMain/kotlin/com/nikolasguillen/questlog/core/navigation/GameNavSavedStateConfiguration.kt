package com.nikolasguillen.questlog.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * How the back stack is saved and restored. Android finds a [NavKey]'s serializer on its own; the other
 * targets cannot, so every route has to be registered here. Adding a route to `Routes.kt` means adding it
 * to this list too — `RoutesSerializationTest` fails when one is missing.
 */
val GameNavSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SearchRoute::class)
            subclass(ListsRoute::class)
            subclass(RadarRoute::class)
            subclass(SettingsRoute::class)
            subclass(OwnedPlatformsRoute::class)
            subclass(ReleaseNotificationsRoute::class)
            subclass(OnboardingRoute::class)
            subclass(WishlistRoute::class)
            subclass(GameDetailRoute::class)
        }
    }
}
