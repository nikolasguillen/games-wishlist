# Implementation Plan: Light Appearance & Theme Preference

**Branch**: `004-light-dark-theme` | **Date**: 2026-09-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-light-dark-theme/spec.md`

## Summary

A new "Appearance" control in Settings (Light / Dark / Follow System, default Follow System) lets the user
pick how the app renders. The choice persists in a new DataStore Preferences value read through a new
`AppearancePreferenceStore` domain port — not a second `GameRepository` — and is resolved to a plain
`darkTheme: Boolean` at the single production call site of `QuestLogTheme` (`MainActivity`), combining the
stored preference with Compose's live `isSystemInDarkTheme()` signal. `QuestLogTheme` gains a light
`ColorScheme`/`AppColors` variant built from new `*Light` tokens that reuse the existing Gold brand hue, and
now owns the system status/navigation bar icon color reactively, so both an explicit selection and a live
system change apply instantly with no restart. The selector itself reuses the existing, currently-unused
`CustomSegmentedButton` inline in a new Settings group — no new route, no new sub-screen.

## Technical Context

**Language/Version**: Kotlin 2.4.10, Java 11 target, JVM toolchain 21

**Primary Dependencies**: Compose BOM 2026.09.00, Hilt 2.60.1, `androidx.datastore:datastore-preferences`
1.2.1 (already in the version catalog, moving from an unused `:app` dependency to an actual `:core:data`
one), `androidx.core:core-ktx` (new dependency of `:core:designsystem`, for
`WindowCompat`/`WindowInsetsControllerCompat`)

**Storage**: DataStore Preferences — one new key, no Room change, no schema/version impact

**Testing**: JUnit4 + MockK + `kotlinx-coroutines-test` in the existing `src/test` source sets of
`:core:data`, `:core:domain` (where warranted — see below), `:feature:settings`, and a first real test in
`:app` (currently only a placeholder `ExampleUnitTest`)

**Target Platform**: Android, minSdk 29 / compileSdk & targetSdk 37

**Project Type**: Modular Android app (17 modules)

**Performance Goals**: appearance change visible within 1 second of selection (SC-001); no added work on
any screen's existing render path beyond reading one more `CompositionLocal`-backed color set that was
already being read

**Constraints**: `QuestLogTheme`'s existing `@Preview` call sites (dozens, across every module) must keep
compiling and rendering unchanged without being individually edited; no new module dependency edge in the
`feature/*` → `:core:data`/`:core:network`/`:core:database`/`:core:ai` direction

**Scale/Scope**: one new preference value, one new domain port + two use cases, a light color token set,
one new Settings UI group, no new screens/routes

## Constitution Check

*GATE: passed before Phase 0 research. Re-checked after Phase 1 design — still passing, with one deliberate
addition called out below for explicit review.*

| Principle | Verdict | How this plan satisfies it |
|---|---|---|
| I. Module boundaries are load-bearing | **Pass** | No new module edge. `feature/settings` still touches only `:core:{common,model,domain,ui,navigation,designsystem}` — it reaches the preference through `:core:domain` use cases, never through `:core:data`/DataStore directly. `AppThemeViewModel` and the resolved-theme wiring live in `:app`, which already depends on everything. No new `NavKey`, no new `entryProvider` branch — no route is added. |
| II. Typed errors cross layers | **Pass** | `AppearancePreferenceStore` is DB-only in spirit (DataStore, not network), so both its methods and both new use cases return a bare `Flow<AppearanceMode>` / `Unit` — no `AppResult`, matching the rule that only network-touching methods wrap. Nothing new throws across a layer. |
| III. The UI layer renders, it does not decide | **Pass** | Resolving `AppearanceMode` → `darkTheme: Boolean` combines a stored value with `isSystemInDarkTheme()`, which is unavoidably a UI-layer read (Compose has no other source for it) — but the *decision logic* (the three-way `when`) is a one-line, inline, side-effect-free expression at the composition root, not business logic hidden in a composable. `SettingsScreen` still only renders `state.appearanceMode` and emits `SettingsUiEvent.AppearanceModeChanged`; it does not decide anything. |
| IV. Reuse the shared layer before adding to it | **Pass** | Reuses `CustomSegmentedButton` (previously declared but unused `AppColors` tokens `segmentedButtonSelectedColor`/`segmentedButtonSelectedContentColor` now get their first real consumer), `SettingsGroup`/`SettingsRow`'s sibling pattern, and the existing `MaterialTheme.spacing`/`appColors` plumbing. **Deliberate, flagged addition**: a new domain port (`AppearancePreferenceStore`) rather than folding into the one existing `GameRepository` — see Complexity Tracking below, this is the rule IV explicitly asks to be discussed rather than silently done. |
| V. Verification is local, not automated | **Pass** | Verified with `./gradlew :app:assembleDebug` (spans modules, touches DI) plus `./gradlew test`. New store, use-case (where non-trivial), ViewModel and mapper logic gets a test in its own module's `src/test`. No lint gate or pipeline assumed. |

**Deliberate additions that are not violations, recorded so review does not mistake them for drift:**

- `:core:designsystem` gains one new dependency, `libs.androidx.core.ktx`, for
  `WindowCompat`/`WindowInsetsControllerCompat` inside `QuestLogTheme`'s system-bar `SideEffect`. A library
  dependency, not a module edge — same category as `:core:ui` gaining `activity-compose`/`core-ktx` in
  Phase 3.
- `:app` gains its first real `ViewModel` (`AppThemeViewModel`) and, with it, its first real unit test file
  beyond the generated placeholder. This is new *pattern* for the module, not a new *dependency* — `:app`
  already has `androidx.lifecycle.viewmodel.compose` and Hilt wired.
- `androidx.datastore:datastore-preferences` moves from a declared-but-unused `:app` dependency to an
  actually-used `:core:data` one. Net module-graph change: a dead dependency is removed from `:app`.
- `core/designsystem/CLAUDE.md`'s "dark theme only... do not add `isSystemInDarkTheme()` branches" line and
  `docs/roadmap.md`'s "appearance has nothing to switch, because `:core:designsystem` is dark-only by
  design" line are both direct statements of the constraint this feature removes. Both get updated in the
  same change that ships this feature, per the root `CLAUDE.md`'s "delete the rule in the same commit"
  instruction — not left to rot into stale documentation.

## Project Structure

### Documentation (this feature)

```text
specs/004-light-dark-theme/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── domain-ports.md  # Phase 1 output — :core:domain port + use cases
│   └── ui-contracts.md  # Phase 1 output — designsystem, core:ui, feature:settings, :app additions
├── checklists/
│   └── requirements.md  # Spec quality checklist (all items passing)
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
core/model/src/main/java/.../core/model/
└── AppearanceMode.kt                              # NEW  SYSTEM/LIGHT/DARK enum, id-based, fromId()

core/domain/src/main/java/.../core/domain/
├── settings/AppearancePreferenceStore.kt          # NEW  port: observe/set
├── usecase/settings/GetAppearanceModeUseCase.kt   # NEW
└── usecase/settings/SetAppearanceModeUseCase.kt   # NEW

core/data/src/main/java/.../core/data/
├── settings/AppearancePreferenceStoreImpl.kt      # NEW  DataStore Preferences
└── di/DataModule.kt                               # EDIT bind the new port
core/data/build.gradle.kts                         # EDIT add datastore-preferences

core/designsystem/src/main/java/.../core/designsystem/theme/
├── Color.kt                                       # EDIT new *Light tokens
├── AppColors.kt                                   # unchanged (same field set, two instances now)
└── QuestLogTheme.kt                               # EDIT darkTheme param, lightColorScheme + light AppColors,
                                                     #      reactive system-bar SideEffect
core/designsystem/build.gradle.kts                 # EDIT add core-ktx
core/designsystem/CLAUDE.md                        # EDIT remove the now-false "dark theme only" line

core/ui/src/main/java/.../core/ui/mapper/
└── AppearanceModeMapper.kt                        # NEW  AppearanceMode.toLabelUiText()
core/ui/src/main/res/values/strings.xml            # EDIT 3 new strings

feature/settings/src/main/java/.../feature/settings/
├── SettingsScreen.kt                              # EDIT new Appearance SettingsGroup
├── SettingsViewModel.kt                            # EDIT inject + fold in the two use cases
├── model/SettingsUiState.kt                       # EDIT appearanceMode field
└── model/SettingsUiEvent.kt                        # EDIT AppearanceModeChanged
feature/settings/src/main/res/values/strings.xml   # EDIT settings_group_appearance

app/src/main/java/.../
├── AppThemeViewModel.kt                            # NEW  resolves AppearanceMode -> darkTheme
└── MainActivity.kt                                 # EDIT setContent reads AppThemeViewModel + isSystemInDarkTheme()
app/build.gradle.kts                               # EDIT remove now-unused datastore-preferences

docs/roadmap.md                                    # EDIT delete the "appearance has nothing to switch" line
```

Per-file audit of the existing hardcoded `Color(0x...)`/`Color.Black`/`Color.White` usages flagged in
research.md §10 is enumerated as individual `tasks.md` items, not listed wholesale here — most of those
files will end up unedited once judged theme-independent by design.

**Structure Decision**: No new module, no new route. The feature follows the same layered shape Phase 3
(release notifications) established for a platform capability that isn't game CRUD — a narrow port in
`:core:domain`, an Android-specific implementation in `:core:data`, thin use cases, UI wiring in the
feature module that presents the control (`feature/settings`) plus the one place a resolved value has to
reach a `View`/`Window` (`:app`). The one structural difference from that precedent: the *rendering*
half of this feature (the actual color tokens) belongs to `:core:designsystem`, which no other recent
feature has had reason to touch.

## Phase 1 Design Artifacts

- [data-model.md](./data-model.md) — the `AppearanceMode` entity, its persistence encoding, and the derived
  (not persisted) `darkTheme` resolution
- [contracts/domain-ports.md](./contracts/domain-ports.md) — the port and the two use cases
- [contracts/ui-contracts.md](./contracts/ui-contracts.md) — `QuestLogTheme`'s new signature and system-bar
  behavior, the new `core:ui` mapper, the `feature/settings` state/event additions, `AppThemeViewModel`
- [quickstart.md](./quickstart.md) — how to verify all three user stories end to end, plus what's covered by
  JVM tests alone

## Complexity Tracking

> Fill ONLY if Constitution Check has violations that must be justified

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| New `AppearancePreferenceStore` port in `:core:domain`, a second non-`GameRepository` interface | `core/data/CLAUDE.md` reserves `GameRepository` for search/history/detail/wishlist/lists/Discover/platforms/Radar dates — an appearance preference is none of those, and stretching that interface's mandate to cover an unrelated device-local UI setting would blur what it's for. This is not a second *repository* in the pattern that rule is aimed at (arbitrary game-data access): it follows the port shape (`ReleaseNotificationScheduler`, `ReleaseNotifier`) Phase 3 already used for the same situation — a platform capability, not game data. | Extending `GameRepository` was considered and rejected in research.md §2: it would make the one repository interface responsible for a concern its own documented scope explicitly excludes. Flagged here rather than silently done, per rule IV, so the call gets a second look. |
