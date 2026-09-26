# Contract: UI surfaces, permission, deep link

**Feature**: `003-release-notifications` | **Date**: 2026-09-27

Three surfaces present the opt-in, one screen manages it, and one deep link brings the user back in. Each
follows the state/event/effect shape in `feature/CLAUDE.md`.

## Shared: notification permission

`core/ui/src/main/java/.../core/ui/util/NotificationPermission.kt`

```kotlin
@Immutable
data class NotificationPermissionState(
    /** True when a notification posted right now would actually reach the user. */
    val canDeliver: Boolean,
    /** True when the system will no longer show a request dialog, so only app settings can fix it. */
    val isPermanentlyDenied: Boolean,
    /** Requests the permission on API 33+, or opens app notification settings when that is the only route. */
    val request: () -> Unit
)

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState
```

Contract details that matter:

- `canDeliver` is `NotificationManagerCompat.areNotificationsEnabled()`, **not**
  `checkSelfPermission(POST_NOTIFICATIONS)`. minSdk is 29, so below API 33 there is no runtime permission
  at all — but the user can still have switched notifications off in system settings. `areNotificationsEnabled()`
  is the one question that is correct on every supported level.
- The runtime request is gated to API 33+. Below that, `request()` goes straight to app settings.
- The state re-reads on `ON_RESUME`, so returning from system settings updates the UI without a restart.

`core/ui/component/NotificationPermissionDeniedDialog.kt` — a `CustomAlertDialog` (never Material's
`AlertDialog`) explaining that reminders will not arrive until notifications are enabled, with an action
routing to settings. Shared because Radar and game-detail both need it; strings live in `:core:ui`'s
`strings.xml`.

## Radar

`RadarViewModel` currently exposes only `uiState` — this feature gives it its first events and effects.

```kotlin
internal sealed interface RadarUiEvent {
    /** The bell on a row was tapped. [gameId], not the row: a multi-platform game's rows share one opt-in. */
    data class ToggleReleaseNotification(val gameId: Int) : RadarUiEvent
}

internal sealed interface RadarUiEffect {
    /** An opt-in was just turned on; ask for permission if it is not already granted. */
    data object RequestNotificationPermission : RadarUiEffect
}
```

- `RadarEntryUiModel` gains `val isNotificationEnabled: Boolean`. The opt-in `Flow<Set<Int>>` folds into the
  existing `combine` in the state pipeline, and the mapper sets the flag per entry.
- `RadarGameRow` gains a bell `IconButton` in its trailing slot, after the `PlatformTile`: filled when on,
  outlined when off, with a 48dp touch target and a `contentDescription` that states the action.
- The effect is emitted on every opt-in **on**; the shared helper no-ops when permission is already
  granted, which keeps the platform check in one place instead of in the ViewModel. On denial the screen
  shows `NotificationPermissionDeniedDialog`.
- Effects are consumed with `Channel(Channel.BUFFERED)` + `receiveAsFlow()` and
  `LaunchedEffect { lifecycle.repeatOnLifecycle(STARTED) { ... } }`, as in `WishlistScreen`.

**Rows the bell does not appear on**: a game already released (the `RECENTLY_RELEASED` bucket) has nothing
left to remind about, so the bell is omitted there rather than shown disabled.

## Game detail

```kotlin
// added to the existing GameDetailUiEvent
data object ToggleReleaseNotification : GameDetailUiEvent
```

- `AvailabilityUiModel` gains `isNotificationEnabled: Boolean` and `isNotificationAvailable: Boolean` — the
  second is false for a game that is not saved or is already released, which is what hides the bell.
- The bell sits in `GameReleaseInfoCard`'s trailing `Row`, before the expand chevron. The card is itself
  clickable when expandable; an `IconButton` consumes its own taps, so the two do not fight.
- Same permission effect and dialog as Radar. `GameDetailUiEffect` gains
  `RequestNotificationPermission`.

**Not the action pill**: `GameDetailActionPill` is a fixed 220dp, three-slot, glow-treated component; a
fourth action means redesigning it. The release card is also where the date the toggle acts on already is.

## Settings

The hub gains a third group, per the roadmap's rule that a group only exists once something backs it:

```kotlin
SettingsGroup(title = stringResource(R.string.settings_group_notifications)) {
    // row 1 → ReleaseNotificationsRoute, subtitle = "N games" / "None yet"
    // row 2 → permission status, shown only when notifications cannot be delivered
}
```

- `SettingsUiState` gains `releaseNotificationCount: Int` and
  `notificationPermission: NotificationPermissionRowState` (`Granted` / `Blocked`), following the existing
  `TranslationModelRowState` pattern so the row's text still comes from the mapper.
- The permission row is **absent when notifications work** — a settings row that only ever says "fine" is
  noise. When blocked it states the consequence and routes to system settings.
- `SettingsUiEvent` gains `NotificationPermissionChanged(canDeliver: Boolean, isPermanentlyDenied: Boolean)`,
  which the screen dispatches from the shared helper. This is how a composition-only platform read gets
  into UiState without a composable deciding anything.

### Management sub-screen

`ReleaseNotificationsScreen` + `ReleaseNotificationsViewModel`, mirroring
`OwnedPlatformsScreen`/`OwnedPlatformsViewModel`:

- Route: `@Serializable data object ReleaseNotificationsRoute : GameNavKey` in `core/navigation/Routes.kt`,
  plus one `entryProvider` branch in `QuestLogNavDisplay.kt` using `hiltViewModel<ReleaseNotificationsViewModel>()`
  (no arguments, so standard Hilt injection, not assisted).
- Content: `ReleaseNotificationsContentState` with `Loading` / `Empty` / `Success(List<ReleaseNotificationUiModel>)`.
  Each row shows the game's cover, name, and the date the reminder is set for — or a "no date yet" label for
  an opt-in whose date is still imprecise, which is how FR-013 becomes visible to the user.
- Turning a row off removes it from the list, since the list *is* the set of opt-ins.
- Reuses `GameListRow` for the rows and `EmptyPage` for the empty state.

## Deep link

`questlog://game/<gameId>`

- `:app`'s `AndroidManifest.xml`: `MainActivity` gains `android:launchMode="singleTop"` and an
  `intent-filter` with `action.VIEW`, `category.DEFAULT`, `category.BROWSABLE` and
  `<data android:scheme="questlog" android:host="game" />`.
- `ReleaseNotifierImpl` builds the `PendingIntent` from that URI — this is the only way `:core:data` can
  point at a screen owned by `:app` without inverting the dependency.
- `MainActivity` reads `intent.data`, and on a match pushes `GameDetailRoute(gameId)` onto the back stack
  above the `SearchRoute` root, guarded by the project's usual `if (backStack.lastOrNull() != nextRoute)`.
  `onNewIntent` covers the already-running case.
- Also declared in `:app`'s manifest: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`,
  alongside the existing `INTERNET` and `ACCESS_NETWORK_STATE`.

## Strings

| Module | Adds |
|---|---|
| `:core:data` | notification channel name and description, reminder title and body (new `res/values/strings.xml`; `:core:database` sets the precedent for a non-UI module owning one) |
| `:core:ui` | permission-denied dialog text, bell content descriptions |
| `:feature:settings` | group title, management row title and subtitles, sub-screen title and empty state |
| `:feature:radar`, `:feature:game-detail` | nothing new if the bell's content description lives in `:core:ui` |

No display text is hardcoded in a model, mapper or composable. Cross-module `R` imports are aliased
`CoreUiR`; a module's own `R` is bare.
