package com.nikolasguillen.questlog.shared

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

/**
 * Search on the left and the game opened from it on the right, in two equal panes.
 *
 * The Search entry is the same one the single-pane scene shows, so moving between the two scenes moves its
 * composition rather than recreating it: the query, the filters and the scroll position stay where they were.
 * The [key] is the Search entry's, and does not change while the right pane switches games, so a swap is not a
 * scene change and nothing animates around it.
 *
 * While the detail is showing, system back closes it. Search has its own back handler (it clears the query), and it
 * would otherwise win, so the scene adds one that outranks it ([SearchDetailBackHandler]) and hands the event to
 * [onBack], the same callback the display uses.
 *
 * @param previousEntries The entries left once back closes the detail: the stack up to and including Search.
 * @param onBack Closes the detail through the display's own back handling.
 */
internal class SearchDetailScene<T : Any>(
    private val searchEntry: NavEntry<T>,
    private val detailEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit
) : Scene<T> {

    override val key: Any = searchEntry.contentKey

    override val entries: List<NavEntry<T>> = listOf(searchEntry, detailEntry)

    // The panes appear and disappear at once. The app's crossfade would fade Search in from nothing every time a game
    // opens or closes, and Search is already there. The predictive back preview keeps the app's default slide.
    override val metadata: Map<String, Any> =
        NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
                NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

    override val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxSize()) {
            // A display cutout is reported against the window's edges. Each pane pads for the side it shares with the
            // window and leaves the one against the divider alone, so Search's scaffold does not inset from it twice.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.End))
            ) {
                searchEntry.Content()
            }
            VerticalDivider()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
            ) {
                // A different game is a different screen: it starts from the top with its own state.
                key(detailEntry.contentKey) {
                    detailEntry.Content()
                }
            }
            SearchDetailBackHandler(onBack = onBack)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchDetailScene<*>) return false
        return searchEntry == other.searchEntry &&
                detailEntry == other.detailEntry &&
                previousEntries == other.previousEntries
    }

    override fun hashCode(): Int =
        (searchEntry.hashCode() * 31 + detailEntry.hashCode()) * 31 + previousEntries.hashCode()

    override fun toString(): String =
        "SearchDetailScene(search=$searchEntry, detail=$detailEntry, previousEntries=$previousEntries)"
}
