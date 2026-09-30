# Quickstart: verifying Light Appearance & Theme Preference

**Feature**: `004-light-dark-theme` | **Date**: 2026-09-30

How to prove the feature works end to end. There is no CI and no lint gate in this project —
verification is local. Most of this feature is a Compose/`Activity` concern, so a device or emulator is
needed for the system-integration scenarios (2 and 3); the persistence and mapping logic can be checked
with JVM tests alone.

## Prerequisites

- `local.properties` carries `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` (standard project setup, unrelated
  to this feature but needed to run the app at all).
- A device or emulator with a system-wide dark mode toggle reachable from Quick Settings (any API 29+
  device qualifies).
- No database change in this feature — no destructive migration to account for.

## Build and unit tests

On macOS/Linux:

```bash
./gradlew :app:assembleDebug        # spans modules and touches DI wiring, so the full build
./gradlew test                      # every JVM suite must stay green
```

On Windows (PowerShell), use `.\gradlew.bat` with the same arguments.

Single-module loops while working:

```bash
./gradlew :core:designsystem:compileDebugKotlin --console=plain -q
./gradlew :core:data:testDebugUnitTest --console=plain -q
./gradlew :feature:settings:testDebugUnitTest --console=plain -q
```

## Scenario 1 — pick Light or Dark manually (User Story 1)

1. Open Settings. A new "Appearance" group shows a three-way segmented control: Light / Dark / Follow
   System.
2. Tap "Light". Every currently-visible screen (Settings itself, then navigate to Search, Wishlist, Lists,
   Radar, a game detail page) switches to light surfaces immediately — no restart.
3. While in Light appearance, pull down the notification shade or glance at the status bar: the clock/
   battery icons and the gesture nav pill are dark, legible against the new light app content behind them
   (FR-012).
4. Confirm the FAB, any filter chips, and the bottom nav's selected-item indicator are still the same gold
   hue as they were in Dark appearance (FR-004) — only the surfaces around them changed.
5. Force-close the app (not just background it) and reopen it: it launches directly in Light, and Settings
   still shows "Light" selected (User Story 3).
6. Tap "Dark": the app returns to exactly its pre-feature look, system bar icons switch back to light-on-dark.

**Expected**: no screen is left with unreadable text/icons in Light appearance (FR-010) — pay particular
attention to any full-bleed image screens (game detail's gallery, the image viewer) and any card that used
a hardcoded black/white color, since research.md §10 flags those as needing a per-file check.

## Scenario 2 — Follow System tracks the device (User Story 2)

1. In Settings, select "Follow System".
2. Set the device's system-wide dark mode **off** (light). Reopen the app (or bring it to the foreground if
   backgrounded): it renders in Light.
3. Without touching the in-app setting, flip the device's system-wide dark mode **on**. Without closing the
   app, confirm it switches to Dark on its own within roughly a second — this exercises both the activity
   recreation Android performs on a `uiMode` configuration change and the reactive `darkTheme` recomputation
   in `MainActivity`'s `setContent` (research.md §3).
4. Repeat step 3 in the opposite direction (dark → light) to confirm both edges.

**Expected**: the displayed appearance matches the device's system setting every time (SC-004), with no
manual reopen required to see the update once it's already in the foreground.

## Scenario 3 — fresh install default (User Story 3 / Clarifications)

1. Clear app data (or install fresh).
2. Open Settings without changing anything: the Appearance control shows "Follow System" already selected.

**Expected**: matches the spec's Clarifications — every install, not just some, starts on Follow System,
since there is no existing-user population this app needs to special-case yet.

## What a JVM test can already cover, no device needed

Per [contracts/domain-ports.md](./contracts/domain-ports.md) and
[contracts/ui-contracts.md](./contracts/ui-contracts.md):

- `AppearancePreferenceStoreImpl` (in `:core:data`) round-trips a value through a real (temp-file-backed)
  DataStore instance, and falls back to `AppearanceMode.SYSTEM` when nothing has been written yet.
- `AppearanceMode.fromId` falls back to `SYSTEM` for an unrecognized id.
- `SettingsViewModel` reflects a `GetAppearanceModeUseCase` emission in `uiState.appearanceMode`, and
  dispatching `AppearanceModeChanged` calls `SetAppearanceModeUseCase` with the right value.

`:app` gets no new unit test — `MainActivity`'s `darkTheme` resolution is a three-line inline `when`
directly in `setContent`, with no ViewModel or other unit-testable seam introduced for it (research.md §3).
