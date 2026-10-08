package com.nikolasguillen.questlog.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The back stack is saved through [GameNavSavedStateConfiguration]. Android can fall back to reflection when a
 * route is missing from it; the other targets cannot, so a forgotten route would only fail on iOS, at runtime.
 */
class RoutesSerializationTest {

    private val json = Json { serializersModule = GameNavSavedStateConfiguration.serializersModule }
    private val navKey = PolymorphicSerializer(NavKey::class)

    private val everyRoute: List<GameNavKey> = listOf(
        SearchRoute,
        ListsRoute,
        RadarRoute,
        SettingsRoute,
        OwnedPlatformsRoute,
        ReleaseNotificationsRoute,
        OnboardingRoute,
        WishlistRoute(listId = 42L),
        GameDetailRoute(gameId = 1942)
    )

    @Test
    fun `every route survives a save and restore through the configuration`() {
        everyRoute.forEach { route ->
            val restored = json.decodeFromString(navKey, json.encodeToString(navKey, route))

            assertEquals(route, restored)
        }
    }

    @Test
    fun `route arguments are kept`() {
        val restored = json.decodeFromString(navKey, json.encodeToString(navKey, WishlistRoute(listId = 7L)))

        assertEquals(7L, (restored as WishlistRoute).listId)
    }

    @Test
    fun `every GameNavKey subclass is registered in the configuration`() {
        val module = GameNavSavedStateConfiguration.serializersModule

        GameNavKey::class.sealedSubclasses.forEach { subclass ->
            val serialName = serializer(subclass.java).descriptor.serialName

            assertNotNull(
                "${subclass.simpleName} is a GameNavKey but is missing from GameNavSavedStateConfiguration",
                module.getPolymorphic(NavKey::class, serialName)
            )
        }
    }

    @Test
    fun `the round-trip list covers every GameNavKey subclass`() {
        assertEquals(GameNavKey::class.sealedSubclasses.toSet(), everyRoute.map { it::class }.toSet())
    }
}
