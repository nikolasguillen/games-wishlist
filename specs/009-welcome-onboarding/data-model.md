# Data Model: Welcome Onboarding

There are no Room entity or schema changes, only new DAO queries (research R2), and no new
`:core:model` types. The only persisted state is one boolean.
Everything else is either UI state in `:feature:onboarding`, or state this feature reuses and does not
own.

## Persisted

### Onboarding completion

| Field | Type | Storage | Default |
|-------|------|---------|---------|
| `onboarding_completed` | `Boolean` | `settings` `DataStore<Preferences>` (shared with appearance and wishlist view mode) | `false` when the key is absent |

- **Written** once per finish or skip, through `CompleteOnboardingUseCase`. Writing it again during a
  replay is a harmless no-op, since the value is already `true`.
- **Read** once at cold start by `MainActivity` to pick the initial back-stack entry (research R5).
- **Lifecycle**: absent → `true`. There is no path back to `false` other than clearing the app's data.
  Room's destructive migration does not touch it. Auto Backup may restore it on reinstall (research R4).

### Reused, not owned

- **Owned platforms**: the existing `owned_platforms` table. The flow and Settings both write it through
  the new `ToggleOwnedPlatformUseCase`, which runs one transaction per tap (research R2). It replaces the
  whole-set `SetOwnedPlatformsUseCase`. The table and its entity do not change.
- **Notification permission**: a system state, observed through `rememberNotificationPermissionState()`.
  Nothing is persisted by the app.

## UI state (`feature/onboarding/model/`)

One type per file. The exception is sealed hierarchies, which keep their implementations together.

### `OnboardingPage` (sealed interface)

The closed set of steps.

| Case | Kind | Content |
|------|------|---------|
| `Welcome` | informational | What QuestLog is for |
| `Discover` | informational | Search and the Discover feed |
| `Lists` | informational | Lists and play status |
| `Radar` | informational | Release timeline and optional reminders |
| `Platforms` | setup | The shared `PlatformSearchField` + `PlatformPickerList` from `:core:ui`, driven by `OnboardingViewModel` |
| `Reminders` | setup | The notification-permission step; driven by `ReminderStepState` |

The four informational pages map to an `@Immutable OnboardingInfoPageUiModel` holding `headline: UiText`,
`body: UiText` and an `ImageVector`. The mapping is a mapper in `feature/onboarding/mapper/`. Text always
comes from `strings.xml` (FR-008).

### `ReminderStepState` (sealed interface)

| State | Shown | Actions |
|-------|-------|---------|
| `Undecided` | Why reminders are useful (a heads-up on a saved game's release day) | "Allow notifications" (primary), "Not now" (secondary) |
| `Granted` | Confirms reminders are available, noting they are switched on per game | "Get started" |
| `Declined` | Reminders are off; this can be changed later from Settings | "Get started" |

Transitions:

```
Undecided --AllowClicked--> (effect: RequestNotificationPermission) --result(true)--> Granted
Undecided --AllowClicked--> (effect: RequestNotificationPermission) --result(false)--> Declined
Undecided --NotNowClicked--> Declined
Declined  --PermissionStateChanged(canDeliver = true)--> Granted   // granted from system settings mid-flow
```

There is no transition out of `Granted`. The state is not persisted: the flow starts over on a new
process, so the step starts `Undecided` again.

### `OnboardingUiState` (`@Immutable`)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `contentState` | `OnboardingContentState` | `Loading` | |
| `reminderStep` | `ReminderStepState` | `Undecided` | |
| `platformPicker` | `PlatformPickerContentState` (`:core:ui`) | `Loading` | Built by `toPlatformPickerContentState(selected, query, pinned)` |
| `selectedPlatformCount` | `Int` | `0` | Drives the platforms page caption and its "Skip" / "Continue" label |

The search query lives in a `TextFieldState` owned by the ViewModel, the same as in
`OwnedPlatformsViewModel`. Its text reaches the pipeline through `snapshotFlow`.

There are two kinds of state, built differently, the way `feature/CLAUDE.md` allows:

- The platforms part is derived continuously from use-case flows. It is folded into the state with a
  `viewModelScope.launch { combine(...).collect { _uiState.update { ... } } }` started in `init`.
- Everything else is changed locally by event handlers on a `MutableStateFlow`.

The pinned set is the selection read once in `init`. On a first launch it is empty. On a replay it is the
current picks, so they are listed first, as the spec's edge case requires.

### `OnboardingContentState` (sealed interface, `@Immutable`)

- `Loading`: until the platform facts arrive (one frame, research R7).
- `Ready(pages: List<OnboardingPage>)`: the page list built once from those facts. It is not rebuilt if
  the permission changes later, so the page count never shifts under the user.

### `OnboardingUiEvent` (sealed interface)

| Event | Effect in the ViewModel |
|-------|-------------------------|
| `NotificationFactsResolved(requiresRuntimePermission, canDeliver)` | Builds `Ready(pages)`. `Reminders` is included only when `requiresRuntimePermission && !canDeliver`. Ignored after the first time. |
| `AllowNotificationsClicked` | Sends `RequestNotificationPermission`. |
| `NotNowClicked` | `reminderStep = Declined` |
| `NotificationPermissionResult(granted)` | `Granted` or `Declined` |
| `PermissionStateChanged(canDeliver)` | `Declined → Granted` when `canDeliver` becomes true. Otherwise no-op. |
| `PlatformToggled(platformId)` | `ToggleOwnedPlatformUseCase(platformId)`. Saved immediately (clarification 1). |
| `ClearPlatformQuery` | Clears the `TextFieldState`. |
| `RetryPlatformSync` | Runs `SyncPlatformCatalogUseCase` again. Only reachable from the picker's `Empty` state. |
| `SkipClicked` / `FinishClicked` | Calls `CompleteOnboardingUseCase`, then sends `Finished`. Repeat taps are guarded, so only one `Finished` is sent. |

### `OnboardingUiEffect` (sealed interface)

- `RequestNotificationPermission`: the screen calls `permissionState.request()`. This is skipped when
  `isPermanentlyDenied`; the ViewModel is then told `NotificationPermissionResult(false)` directly
  (research R8).
- `Finished`: the screen calls `onFinish()`, and `:app` resolves it as first launch or replay
  (research R6).

The `Channel(Channel.BUFFERED)` + `receiveAsFlow()` pattern is used for these, as in every other feature.

## Validation rules (from the spec)

- Every page can be left with Skip (FR-004). Skip and Finish both persist completion (FR-002).
- No step blocks: `Platforms` with zero picks and `Reminders` in `Declined` both still reach `Finished`
  (FR-010).
- The `Reminders` page never appears when `!requiresRuntimePermission || canDeliver` (FR-018).
- The system request is made only from `AllowNotificationsClicked` (FR-016).
