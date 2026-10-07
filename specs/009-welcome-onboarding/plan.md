# Implementation Plan: Welcome Onboarding

**Branch**: `009-welcome-onboarding` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/009-welcome-onboarding/spec.md`

## Summary

A first-launch welcome flow made of full-screen pages:

- Four tour pages: Welcome, Search & Discover, Lists, Radar.
- An owned-platforms step.
- A release-reminders page that explains notifications, then asks for permission explicitly. The page is
  conditional, and it explains how to change the choice in Settings if the user declines.

Finishing or skipping stores one boolean in the existing `settings` DataStore. `MainActivity` reads it
before the first frame to decide whether the back stack starts at `OnboardingRoute` or `SearchRoute`. The
same route is pushed from a new "Show welcome tour" row in Settings for replay.

The flow lives in a new `:feature:onboarding` module with its own ViewModel. The platforms step is shared
with Settings without sharing a ViewModel (research R2):

- The picker's ordering, search and pinning rules and its UI move from `:feature:settings` into `:core:ui`.
- Writes go through a new `ToggleOwnedPlatformUseCase`, which runs one database transaction per tap. It
  replaces the whole-set `SetOwnedPlatformsUseCase` and the ViewModel mutex that guarded it.

## Technical Context

**Language/Version**: Kotlin 2.4.10, Java 11 target, JVM toolchain 21

**Primary Dependencies**:

- Jetpack Compose (BOM 2026.09.00): Material 3, plus `HorizontalPager` from foundation.
- Hilt 2.60.1, Navigation 3, DataStore Preferences, Room 2.8.5.
- `androidx.activity` for `rememberLauncherForActivityResult` and `BackHandler`.

There are no new libraries.

**Storage**:

- The `settings` `DataStore<Preferences>` already provided by `DataModule`, with one new boolean key.
- Room: new `PlatformDao` queries only. No entity change, so the schema and version stay as they are.

**Testing**: JUnit4, MockK and `kotlinx-coroutines-test`, in:

- `feature/onboarding/src/test` (new);
- `core/ui/src/test` (new; it is `:core:ui`'s first test source set);
- `core/data/src/test` and `feature/settings/src/test` (updated).

**Target Platform**: Android, minSdk 29 / targetSdk 37. The reminders page applies only on API 33+, where
`POST_NOTIFICATIONS` is a runtime permission.

**Project Type**: modular Android mobile app

**Performance Goals**:

- The first frame is held for a single DataStore read only, a few milliseconds and within the splash.
- A full read-through takes under 60 s (SC-001).

**Constraints**:

- The flow must complete fully offline (SC-006).
- No step may block (FR-010).
- No `SavedStateHandle`.
- No `dp` literals for component sizes.

**Scale/Scope**:

- One new module of about 15 files.
- Five platform-picker files move from `:feature:settings` to `:core:ui`.
- Edits in `:core:database`, `:core:domain`, `:core:data`, `:core:navigation`, `:feature:settings` and
  `:app`.
- About 30 new strings.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|-----------|--------|-----|
| **I. Module boundaries** | ✅ | `:feature:onboarding` depends only on the six allowed core modules. Code shared with Settings moves *down* into `:core:ui` and `:core:domain`, so there is no feature→feature edge. The route takes two edits outside the feature (`Routes.kt` and the `entryProvider`). `:core:model` is untouched. |
| **II. Typed errors** | ✅ | No new network calls. `toggleOwnedPlatform` and the onboarding store are DB/DataStore-only, so they return `Unit` or a bare `Flow`. The catalogue sync keeps its existing `AppResult`. |
| **III. UI renders, does not decide** | ✅ | Ordering, search and pinning stay in a mapper, now in `:core:ui`. The onboarding ViewModel builds the page list and owns the reminders state machine, the picker state and completion. The composable only reports platform facts (SDK level, `canDeliver`, the permission result) as events. The screen is split into a public part and an internal stateless part, with `@Immutable` state, a sealed event type and effects via `Channel(BUFFERED)`. Flows are collected with `collectAsStateWithLifecycle`. All text goes through `UiText`/`strings.xml`. |
| **IV. Reuse first** | ✅ | The picker's components and mapper are moved, not copied. Also reused: `rememberNotificationPermissionState` (extended, research R8), `CustomPagerIndicator`, `SettingsRow`, `EmptyPage`, `ControllerLoadingAnimation`, `metallicBorder`/`rememberCoverBrush` and the `MaterialTheme.spacing` tokens. Illustrations are sized relatively, so there are no new `dp` literals. No second repository. |
| **V. Local verification** | ✅ | `:app:assembleDebug` plus `./gradlew test`. New tests cover the ViewModel, the store, the mapper (moved) and the repository toggle. No CI or lint gate is assumed. |
| Persistence | ✅ | No entity change. The DAO follows the "multi-step write is a default `@Transaction` body in the interface" rule from `core/database/CLAUDE.md`. No database version bump. |
| Injection | ✅ | `@HiltViewModel` on a route without arguments. `rememberPagerState` (via `rememberSaveable`) persists the page instead of `SavedStateHandle`. |
| KMP | ✅ | Preferences DataStore is multiplatform-capable. Nothing here is hard to undo after a KMP move. |

**Post-design re-check**: still passing. The R2 revision removed the composable-slot pattern that the
first draft needed, so nothing remains in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/009-welcome-onboarding/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ui-and-domain-contracts.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
feature/onboarding/                                   # NEW module (build file copied from feature/search)
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── java/com/nikolasguillen/questlog/feature/onboarding/
    │   │   ├── OnboardingScreen.kt                   # public screen + internal OnboardingContent + previews
    │   │   ├── OnboardingViewModel.kt
    │   │   ├── components/
    │   │   │   ├── OnboardingInfoPage.kt             # illustration + headline + body
    │   │   │   ├── OnboardingPlatformsPage.kt        # header + PlatformSearchField + PlatformPickerList
    │   │   │   ├── OnboardingRemindersPage.kt        # Undecided / Granted / Declined
    │   │   │   └── OnboardingBottomBar.kt            # Back · indicator · Next / Get started
    │   │   ├── mapper/OnboardingPageUiMapper.kt
    │   │   └── model/
    │   │       ├── OnboardingPage.kt                 # sealed
    │   │       ├── OnboardingInfoPageUiModel.kt
    │   │       ├── ReminderStepState.kt              # sealed
    │   │       ├── OnboardingContentState.kt         # sealed
    │   │       ├── OnboardingUiState.kt
    │   │       ├── OnboardingUiEvent.kt              # sealed
    │   │       └── OnboardingUiEffect.kt             # sealed
    │   └── res/values/strings.xml
    └── test/java/com/nikolasguillen/questlog/feature/onboarding/OnboardingViewModelTest.kt

core/ui/                                              # platform picker moves here
├── build.gradle.kts                                  # + testImplementation(junit) for the first test set
└── src/
    ├── main/java/com/nikolasguillen/questlog/core/ui/
    │   ├── component/PlatformRow.kt                  # MOVED from feature/settings
    │   ├── component/PlatformSearchField.kt          # MOVED from feature/settings
    │   ├── component/PlatformPickerList.kt           # NEW — the four-state list body
    │   ├── mapper/PlatformPickerMapper.kt            # MOVED (was OwnedPlatformsUiMapper)
    │   ├── model/PlatformPickerItemUiModel.kt        # MOVED + renamed (was PlatformUiModel)
    │   ├── model/PlatformPickerContentState.kt       # MOVED + renamed (was OwnedPlatformsContentState)
    │   └── util/NotificationPermission.kt            # + onResult parameter
    ├── main/res/values/strings.xml                   # + the picker strings, moved from feature/settings
    └── test/java/com/nikolasguillen/questlog/core/ui/mapper/PlatformPickerMapperTest.kt   # NEW

core/database/.../dao/PlatformDao.kt                  # + isOwned, deleteOwnedPlatform, @Transaction toggleOwnedPlatform
                                                      # − setOwnedPlatforms, clearOwnedPlatforms
core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/
├── repository/GameRepository.kt                      # setOwnedPlatforms → toggleOwnedPlatform
├── settings/OnboardingPreferenceStore.kt             # NEW
└── usecase/
    ├── discover/ToggleOwnedPlatformUseCase.kt        # NEW, replaces SetOwnedPlatformsUseCase (deleted)
    └── settings/
        ├── GetOnboardingCompletedUseCase.kt          # NEW
        └── CompleteOnboardingUseCase.kt              # NEW

core/data/src/main/java/com/nikolasguillen/questlog/core/data/
├── repository/GameRepositoryImpl.kt                  # setOwnedPlatforms → toggleOwnedPlatform
├── settings/OnboardingPreferenceStoreImpl.kt         # NEW
└── di/DataModule.kt                                  # + @Binds
core/data/src/test/.../
├── repository/GameRepositoryImplToggleOwnedPlatformTest.kt   # replaces …SetOwnedPlatformsTest
└── settings/OnboardingPreferenceStoreImplTest.kt             # NEW

core/navigation/.../Routes.kt                         # + OnboardingRoute

feature/settings/
├── src/main/java/.../feature/settings/
│   ├── OwnedPlatformsViewModel.kt                    # toggle use case, no mutex, shared mapper
│   ├── OwnedPlatformsScreen.kt                       # renders core:ui PlatformSearchField + PlatformPickerList
│   ├── SettingsScreen.kt                             # + "Show welcome tour" row, onShowWelcomeTourClick
│   ├── model/OwnedPlatformsUiState.kt                # contentState: PlatformPickerContentState
│   └── (components/Platform*.kt, model/PlatformUiModel.kt, model/OwnedPlatformsContentState.kt,
│        mapper/OwnedPlatformsUiMapper.kt)            # DELETED — moved to core:ui
├── src/main/res/values/strings.xml                   # − moved picker strings, + settings_show_welcome_tour
└── src/test/.../OwnedPlatformsViewModelTest.kt       # ordering/search cases move to PlatformPickerMapperTest

app/
├── build.gradle.kts                                  # + :feature:onboarding
└── src/main/java/com/nikolasguillen/questlog/
    ├── MainActivity.kt                               # read flag, hold first draw, start route, deep-link guard
    └── QuestLogNavDisplay.kt                         # + OnboardingRoute branch, Settings lambda

settings.gradle.kts                                   # + include(":feature:onboarding")
```

**Documentation updated in the same commits.** The owner approved these on 2026-10-07, including the
constitution amendment.

- Root `CLAUDE.md`:
  - "17 modules" becomes 18.
  - The module graph gains `onboarding`.
- `feature/CLAUDE.md`: add `feature/onboarding` to "Applies to".
- `core/ui/CLAUDE.md`:
  - The component inventory gains `PlatformRow`, `PlatformSearchField` and `PlatformPickerList`.
  - Mention the new mapper.
- `docs/tech-debt.md`:
  - "all 17 module build files" becomes 18.
  - The test-coverage entry no longer says `:core:ui` mappers have no tests at all. It is reworded to
    what is still true, which is that the other `:core:ui` mappers are untested.
- `.specify/memory/constitution.md`, PATCH amendment:
  - Principle I's module count.
  - Principle V's test source sets gain `core/ui` and `feature/onboarding`.

**Structure Decision**: The flow is a standard feature module laid out like every other one (see
`feature/CLAUDE.md`). The platform picker becomes a shared `:core:ui` component used by two screens,
each with its own ViewModel. Everything else is a small, additive edit at an existing seam.

## Risks and notes

- **The move touches Settings' platform screen.** That screen has to look and behave exactly as before.
  The existing `OwnedPlatformsViewModelTest` cases are kept, either in that test (the wiring) or moved to
  `PlatformPickerMapperTest` (ordering, search, pinning). Run it before and after the move.
- **The toggle changes the write semantics** from replacing the whole set to flipping one row. The
  behaviour is the same for the user, and the Discover cache is still cleared first on every change.
  `GameRepositoryImplToggleOwnedPlatformTest` keeps the ordering assertion.
- **Flash of the wrong first screen.** This is mitigated by holding the first draw until the flag is read
  (research R5).
- **The platforms page inside a horizontal pager.** The picker is a vertical `LazyColumn` with a text field
  in it. Nested scrolling in opposite directions is supported. The risk is the keyboard staying open when
  the user swipes away, so the pager should clear focus when the page changes.
- **`rememberNotificationPermissionState` is shared by four screens.** The new parameter is optional and
  defaults to a no-op, so their behaviour is unchanged. Compiling `:app` covers all callers.
- **Owner's device.** Existing installs have no flag, so the owner sees the flow once after updating. That
  is expected; the app is unpublished.

## Complexity Tracking

No constitution violations and no new patterns.
