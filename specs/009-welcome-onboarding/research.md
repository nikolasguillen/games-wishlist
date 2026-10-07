# Research: Welcome Onboarding

All decisions below were taken against the code on `develop` at `4087fc8b`. There were no open
`NEEDS CLARIFICATION` items in the Technical Context; this file records the design choices and the
alternatives weighed.

## R1 — Where the onboarding flow lives

**Decision**: A new feature module, `:feature:onboarding`, copied from `feature/search/build.gradle.kts`.

**Rationale**: Every other user-facing area has its own feature module, and the flow is not a setting —
it only *reuses* two settings concerns. A dedicated module keeps its strings, ViewModel and tests in one
place and leaves `:feature:settings` describing only Settings.

**Alternatives considered**:

- *Put it inside `:feature:settings`*. Fewest files, and the platform picker's ViewModel could be shared
  directly since it is `internal` there. Rejected because the module would stop meaning "Settings", and
  the owner chose for each screen to own its own ViewModel (R2).
- *Put it inside `:app`*. `:app` would then own a screen, a ViewModel and strings, which no other screen
  does; screens live in feature modules.

## R2 — Reusing the owned-platforms picker in the flow

**Decision** (chosen by the owner): each screen owns its own ViewModel. The picker's rules and UI move to
shared places, and the one piece of write logic becomes a use case.

1. **Picker rules and UI move from `:feature:settings` into `:core:ui`.**

   | Today (`feature/settings/`) | Moves to (`core/ui/`) |
   |-----------------------------|-----------------------|
   | `mapper/OwnedPlatformsUiMapper.kt` (`toContentState`, `PLATFORM_ORDER`, `matches`) | `mapper/PlatformPickerMapper.kt` (`List<Platform>.toPlatformPickerContentState(selectedIds, query, pinnedIds)`) |
   | `model/PlatformUiModel.kt` | `model/PlatformPickerItemUiModel.kt`, renamed so it is not confused with `PlatformTileUiModel` |
   | `model/OwnedPlatformsContentState.kt` | `model/PlatformPickerContentState.kt` (`Loading`, `Empty`, `NoSearchResults`, `Success`) |
   | `components/PlatformRow.kt`, `components/PlatformSearchField.kt` | `component/PlatformRow.kt`, `component/PlatformSearchField.kt` |
   | the `when` over the content state inside `OwnedPlatformsContent` | `component/PlatformPickerList.kt`: stateless, renders the four states, emits `onToggle(id)`, `onClearQuery()` and `onRetry()` |

   The Settings-specific strings these pieces use (`owned_platforms_empty`, `owned_platforms_no_results`,
   `owned_platforms_clear_search_action`, `owned_platforms_search_hint`,
   `owned_platforms_search_clear_content_description`) move to `:core:ui`'s `strings.xml` with them.
   `:core:ui` is the only module whose resources are read from outside it.

2. **A new `ToggleOwnedPlatformUseCase` replaces `SetOwnedPlatformsUseCase`.** It calls
   `GameRepository.toggleOwnedPlatform(platformId)`, which:
   - clears the Discover lane cache first, the same ordering `setOwnedPlatforms` uses today;
   - then calls a new `PlatformDao.toggleOwnedPlatform(platformId)`. That is a default
     `@Transaction suspend fun` in the interface (the `core/database/CLAUDE.md` pattern), built on two new
     queries, `isOwned` (`SELECT EXISTS`) and `deleteOwnedPlatform`, plus the existing
     `insertOwnedPlatform`.

   Room serialises write transactions, so two quick taps each see the other's committed result. That is
   the guarantee the ViewModel's mutex gives today, moved to the data layer, where it holds for every
   caller.

   Nothing else calls `setOwnedPlatforms` once Settings switches to the toggle. So these are deleted or
   replaced together, and no dead path is left:
   - `SetOwnedPlatformsUseCase`;
   - `GameRepository.setOwnedPlatforms` and its implementation;
   - `PlatformDao.setOwnedPlatforms` and `clearOwnedPlatforms`;
   - `GameRepositoryImplSetOwnedPlatformsTest`, which becomes `GameRepositoryImplToggleOwnedPlatformTest`
     with the same cache-cleared-first assertion.

   No entity changes, so the database schema does not change.

3. **Each ViewModel keeps only the wiring**, about 30 lines. `OwnedPlatformsViewModel` is slimmed to this;
   `OnboardingViewModel` gets the same wiring:
   - `combine` over the catalogue (`GetKnownPlatformsUseCase`), the selection
     (`GetSelectedPlatformIdsUseCase`), the search query (a `TextFieldState` via `snapshotFlow`) and the
     pinned set;
   - reading the pinned set once in `init`;
   - starting `SyncPlatformCatalogUseCase` in `init`;
   - a one-line toggle through the use case.

**Rationale**:

- FR-011 and the clarifications ("stored exactly as if made from Settings", "the same full, searchable
  list") hold, because both screens share the same mapper, the same components and the same write path.
- The only per-screen code is plumbing. The ordering, search and pinning rules and the race-free write
  each exist exactly once.
- No slot and no feature→feature edge. `:app` goes back to doing only navigation.
- `:core:ui` is the right home for the ordering: it ranks by `PlatformVisuals.curatedPlatformIds`, whose
  KDoc says it is deliberately the same list as the platform tiles. Domain cannot see it, and copying the
  list into domain would undo that. A mapper is a permitted home for sorting (Principle III).
- The onboarding platforms page can now say what the user picked. For example, the bottom action can
  read "Skip" or "Continue" depending on the selection, without touching Settings.

**Alternatives considered**:

- *Share `OwnedPlatformsViewModel` through a composable slot filled by `:app`* (the first draft of this
  plan). It has no duplication, but the onboarding page would not own its state, its behaviour would be
  set by another module's screen, and it introduced a new slot pattern. The owner rejected it.
- *A shared building-blocks approach that keeps the mutex in each ViewModel*. It is simpler at the data
  layer, but the read-modify-write would then be copied in two ViewModels, and that is the kind of code
  that gets "simplified" in one copy and breaks there.
- *A "Choose platforms" button on the page that pushes the existing Settings screen*. It needs almost no
  code, but adds a tap and leaves the flow's full-screen pages.
- *Make `:feature:onboarding` depend on `:feature:settings`*. That breaks Principle I.

## R3 — Persisting "onboarding completed"

**Decision**: A boolean key `onboarding_completed` in the existing `settings` `DataStore<Preferences>`.
It is accessed through a new `OnboardingPreferenceStore` contract in `core/domain/settings/` and an
`OnboardingPreferenceStoreImpl` in `core/data/settings/`, bound in `DataModule`. Two use cases sit in
`core/domain/usecase/settings/`: `GetOnboardingCompletedUseCase` returns `Flow<Boolean>` and
`CompleteOnboardingUseCase` is a `suspend` function returning `Unit`.

**Rationale**: This is exactly the shape of `AppearancePreferenceStore` and
`WishlistViewModePreferenceStore`. DataStore is outside Room, so the destructive migration never resets it,
which is what FR-002 needs. The store is DataStore-only, so it returns a bare `Flow`/`Unit` (Principle II).
Preferences DataStore is already multiplatform-capable, so there is no KMP cost.

**Alternatives considered**:

- *A Room table*. That is overkill for one flag, and the flag would be wiped by every destructive
  migration, re-showing the tour to the owner after each schema change.
- *A separate DataStore file, so it can be excluded from Auto Backup*. Rejected: see R4.

## R4 — Auto Backup

**Decision**: Leave `allowBackup="true"` and the default rules unchanged. A reinstall that restores a
backup brings the flag back, along with the appearance choice and the Room database, so the user is
treated as returning and the flow is not shown. The spec's edge case was amended to say so.

**Rationale**: A user whose lists and settings were restored does not need the tour. Excluding only the
flag would show "first launch" onboarding to someone whose data is all still there.

## R5 — Gating the first screen on the flag

**Decision**:

- `MainActivity` reads `GetOnboardingCompletedUseCase().first()` once in `onCreate` (in `lifecycleScope`)
  into a nullable state field.
- It holds the first frame with a `ViewTreeObserver.OnPreDrawListener` on `android.R.id.content` until that
  field is non-null. That is the documented way to keep the splash screen up without the
  `core-splashscreen` library.
- `setContent` then renders `MainContent(startWithOnboarding = !completed)`, and the back stack is
  created with `rememberNavBackStack(if (startWithOnboarding) OnboardingRoute else SearchRoute)`.

**Rationale**: `rememberNavBackStack` takes its initial key once, so the flag must be known before
`MainContent` composes. Otherwise Search would flash before the flow appears. Holding the first draw for
one DataStore read, which takes a few milliseconds, keeps the system splash on API 31+ and the window
background below that, with no new dependency. Later changes to the flag are irrelevant because, after
the first composition, navigation is driven by the back stack.

**Alternatives considered**:

- *`androidx.core:core-splashscreen` with `setKeepOnScreenCondition`*. It does the same thing but adds a
  dependency and a theme change.
- *Render nothing until loaded*. That draws one blank frame, which ends the system splash early and
  flickers.
- *`runBlocking` in `onCreate`*. That blocks the main thread on disk I/O.

## R6 — One route for both first launch and replay

**Decision**: `data object OnboardingRoute : GameNavKey` in `core/navigation/Routes.kt`, and one
`entryProvider` branch. `OnboardingScreen` reports `onFinish`, and `:app` decides what that means:

- **First launch** (the route is the root, `backStack.size == 1`): add `SearchRoute`, then remove the
  onboarding entry, so the stack is never empty and back cannot return to the flow (FR-006).
- **Replay** (pushed from Settings): `removeLastOrNull()`, which returns to Settings (FR-019).

**Rationale**:

- The bottom bar is already shown only for Search, Radar and Lists, so it hides itself during the flow.
- The system back on page 1 of a first launch hits a single-entry back stack, so the activity finishes,
  which matches the spec's edge case.
- On replay, back on page 1 pops to Settings.
- The ViewModel does not need to know which case it is in, because marking the flow complete is
  idempotent.

**Alternatives considered**:

- *A gate outside navigation (`if (!completed) OnboardingScreen() else MainContent()`)*. Simpler for the
  first launch, but replay would need the route anyway, so there would be two entry paths for one screen.
- *`OnboardingRoute(isReplay: Boolean)`*. Redundant, because the root check already tells the two cases
  apart.

The release-notification deep link in `MainContent` is ignored while the root is `OnboardingRoute`, which
is the spec's edge case. It is consumed rather than queued, because the reminder it belongs to no longer
exists.

## R7 — Page order and which pages appear

**Decision**: The order is Welcome → Search & Discover → Lists → Radar → Your platforms → Release
reminders. The reminders page is included only when the device needs a runtime permission (API 33+) and
notifications cannot currently be delivered (FR-018). On API 29–32 it is never shown.

**Rationale**:

- The informational pages come first, so the setup steps follow the tour that explains what they affect.
- The permission prompt comes last, once the user has seen everything. Its outcome (the decline
  explanation or the confirmation) is then the last thing read before "Get started", which helps meet
  SC-007.
- When the reminders page is absent, the platforms page is last and carries "Get started".

The composable reports the platform facts once, as an event:

- whether a runtime permission is required (`Build.VERSION.SDK_INT >= TIRAMISU`);
- the current `canDeliver` value.

The ViewModel then builds the page list (Principle III). Until that event arrives, which takes one
frame, the content state is `Loading`.

**Alternatives considered**: *Reminders straight after the Radar page*, which reads naturally. Rejected
because it would put a system dialog in the middle of the tour, and the decline explanation would be
followed by more tour pages instead of the end of the flow.

## R8 — Detecting the result of the permission request

**Decision**: Add an optional `onResult: (granted: Boolean) -> Unit = {}` parameter to
`rememberNotificationPermissionState()` in `core/ui/util/NotificationPermission.kt`. It is invoked from
the launcher callback after `canDeliver` and `isPermanentlyDenied` are updated. The existing callers do
not change.

**Rationale**: On a first denial neither `canDeliver` nor `isPermanentlyDenied` changes. Both stay
`false`, so observing the state cannot tell "denied" apart from "not asked yet". The callback is the only
reliable signal.

How the edge case plays out:

- The `isPermanentlyDenied` state is recreated with each composition, so it starts `false` on every visit
  to the flow.
- If the permission is already permanently denied, the system therefore returns "denied" at once, with no
  dialog, and `onResult(false)` moves the page to its declined state. This is the "behaves as a decline"
  edge case: the user is never left with an "Allow" button that does nothing.
- The flow never calls `request()` once `isPermanentlyDenied` is true. That would open the system
  settings screen, and the flow's job at that point is to explain, not redirect.

**Alternatives considered**: *A second, onboarding-only permission helper*. That would duplicate the
launcher and lifecycle plumbing (Principle IV).

## R9 — Pager mechanics

**Decision**: A `HorizontalPager` driven by `rememberPagerState`, with the page count taken from the
ViewModel's page list:

- Swipe is enabled.
- The Back and Next buttons call `animateScrollToPage` from the composable.
- `BackHandler(enabled = currentPage > 0)` turns system back into "previous page".
- The position indicator reuses `CustomPagerIndicator` from `:core:ui`.

**Rationale**:

- `rememberPagerState` is saveable through the entry's `rememberSaveableStateHolderNavEntryDecorator`, so
  the current page survives rotation and process death without `SavedStateHandle`, which the constitution
  bans.
- Moving between pages is presentation mechanics, the same way `ScrollToTopFab` callers scroll their own
  list. What the ViewModel decides is which pages exist, the state of the reminders step, and completion.

## R10 — Illustrations and sizes

**Decision**: Each informational page shows an icon from Material Icons Extended in a container styled
with the existing modifiers (`metallicBorder`, `rememberCoverBrush`). The Welcome page reuses
`ControllerLoadingAnimation`. The illustration is sized by `fillMaxWidth(fraction)` plus `aspectRatio(1f)`
rather than a `dp` literal.

**Rationale**:

- There is no artwork budget, which the spec's assumption already states.
- Relative sizing avoids adding another entry to the open "component sizes have no spacing token" item in
  `docs/tech-debt.md`.
- Relative sizing also keeps the text readable at large font scales, because the illustration shrinks
  instead of pushing the text off the page.
