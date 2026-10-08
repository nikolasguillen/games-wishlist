# Quickstart: Validating the Multiplatform Migration

How to prove each phase of `plan.md` is done, and how to prove the whole feature against the spec's
success criteria. There is no CI, so every check here is run locally (constitution, Principle V).

## Prerequisites

| | Android checks | iOS checks |
|---|---|---|
| OS | macOS or Windows | macOS only |
| Tools | JDK 21 toolchain (Gradle downloads it), Android SDK 37 | Xcode (current stable) with an iOS 16+ simulator; optionally a device with a free provisioning profile |
| Secrets | `IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` in `local.properties` (as today) | same file, which the iOS framework build reads too |

On Windows, use `.\gradlew.bat` in place of `./gradlew`. The iOS tasks are skipped there
(`kotlin.native.ignoreDisabledTargets=true`).

## Phase 0 — record the parity baseline (before any code change)

1. On `develop`, install the debug build, then run the full suite and note the count:

   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew test
   ```

   Record the number of tests that ran (the sum of the `tests completed` lines in the console or the
   test reports). This number is SC-001's floor.
2. On a fresh install, walk through the **parity checklist** below and capture a screenshot of each
   screen in light and in dark appearance. Store them outside the repo; they are the reference for
   SC-002.
3. Save a few games into two lists, set statuses, and set a custom cover on one list. **Do not uninstall
   the app**: that device is used in step 2 of "Android update path".

### Parity checklist (SC-002)

| # | Flow | Expected |
|---|---|---|
| 1 | Fresh install → welcome flow → platforms step → reminders page → start screen | Same pages, same order; relaunch skips the flow |
| 2 | Discover feed loads; scroll the shelves; open a game from a shelf | Same shelves, covers load |
| 3 | Search a title; apply a filter and a sort; open a result | Same results, same order |
| 4 | Game detail: save to a list, change status, share, translate (on a supported device), toggle a reminder | Same behaviour; the share sheet opens; the reminder is scheduled |
| 5 | Lists: create, edit (name, icon, custom cover from the photo picker), delete | Same |
| 6 | Wishlist: grid/list toggle, swipe to remove, back-to-top | Same |
| 7 | Radar: timeline buckets and date labels for saved games | Same labels, same days |
| 8 | Settings: appearance (system/light/dark), owned platforms, reminders, translation model row, version label | Same |
| 9 | Release reminder notification → tap | Opens the game's detail (`questlog://game/{id}`) |
| 10 | System back from every screen; back on the search bar overlay and in onboarding | Same behaviour as today |
| 11 | Airplane mode → search | Same offline error page and retry |

## Checks after every commit (FR-011, SC-005)

```bash
./gradlew :app:assembleDebug
./gradlew test            # or the aggregate the Phase C task settles on (research R11)
```

Both must pass, and the reported test count must be ≥ the Phase 0 count. From Phase C onwards, also
run this on macOS:

```bash
./gradlew :core:model:compileKotlinIosSimulatorArm64      # replace with the module just converted
```

From Phase F onwards, the iOS check is the whole framework:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## Phase-specific checks

| Phase (`plan.md`) | Extra check | Pass when |
|---|---|---|
| A — build foundation | `./gradlew help --configuration-cache` twice | The second run reuses the configuration cache; every module still assembles |
| A | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` after CMP is added | `androidx.compose.material3:material3` resolves to `1.5.0-beta01`, not an alpha (research R1) |
| B — platform-neutral code | `./gradlew test` | The new date and number-format host tests pass with the exact strings today's code produces |
| B — Koin | `./gradlew :app:testDebugUnitTest` (Koin `verify()` lives here until `:shared` exists) | `verify()` passes; the app launches and every screen opens; WorkManager workers still run (`adb shell dumpsys jobscheduler \| grep questlog` shows the periodic job) |
| B — Ktor | Parity rows 2, 3, 4, 11 | Same results; an invalid token is still refreshed once; airplane mode shows the network error, not an unknown error |
| B — Room driver / DataStore paths | "Android update path" below | Data and settings survive |
| C — non-UI modules to KMP | iOS compile for each converted module | Compiles; host tests moved, not edited (`git diff -M --stat` shows renames) |
| D — UI to Compose Multiplatform | Parity rows 1–10 in light and dark, compared with the Phase 0 screenshots | No visual or behavioural regression; the `GameDetailActionPill` glow is compared side by side |
| E — `:shared` root | Parity row 9 (deep link), row 10 (back) | Same |
| F — iOS app | "iOS walkthrough" below | All rows pass |
| G — docs | Read the root and directory `CLAUDE.md` files, the constitution, `docs/tech-debt.md` and `docs/roadmap.md` | No rule refers to Hilt, Retrofit, `src/main/java`, `R` aliases, `BuildConfig` credentials or "no build-logic" unless it is still true |

## Android update path (FR-015, FR-010)

1. Use the device prepared in Phase 0, still running the pre-migration build.
2. Install the migrated debug build **over** it, without uninstalling:

   ```bash
   ./gradlew :app:installDebug
   ```

3. Open the app. Expected:
   - **Onboarding** does not reappear (the DataStore path is unchanged).
   - **Appearance**: the mode chosen in Phase 0 is still applied.
   - **Lists**: both lists, their games, statuses and the custom cover are all present (database and cover files unchanged).
   - **Reminders**: still scheduled (WorkManager state survives an update).

If any of these is lost, the step that changed the corresponding path is wrong; fix it, don't accept the
reset. The destructive fallback is for schema changes, and this feature makes none.

## iOS walkthrough (User Story 2, SC-003, SC-006)

Build and run on macOS:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj      # then Run on an iOS 16+ simulator
```

| # | Step | Pass when |
|---|---|---|
| 1 | Fresh install → launch | The launch screen is followed by the welcome flow, **without** a reminders page |
| 2 | Finish onboarding, then search a game and save it to the default list | Saved within the 3-minute budget of SC-003 (time it from the first launch) |
| 3 | Kill the app (swipe away) → relaunch | No welcome flow; the game is still in the list (FR-010) |
| 4 | Open Radar | The saved game appears in the right bucket with the same date label as on Android for the same game (FR-009). Check with the device time zone set to UTC−8 and then UTC+9 |
| 5 | Game detail | Share opens the iOS share sheet. There is **no** translate action and **no** reminder control or banner (SC-006) |
| 6 | Settings | No reminders row, no notification-permission row, no translation-model row. The version label shows the `CFBundleShortVersionString` |
| 7 | Appearance: follow system, then force light and dark | The theme switches. Note the status-bar contrast; a failure goes in `iosApp/` (contracts) |
| 8 | Lists: create a list with a cover from the photo picker; edit it; delete it | Works; the cover survives a relaunch |
| 9 | Swipe back from the screen's left edge on detail, wishlist and settings | Navigates back one entry, like Android's back |
| 10 | Airplane mode → search | The same offline error page as Android; retry works after reconnecting |
| 11 | Leave the app for more than 24h (or change the stored timestamp in a debug build) → relaunch | Radar's dates refresh once at launch |

## Shared-code ratio (SC-004)

After Phase F, from the repo root:

```bash
find core feature shared -path '*/build' -prune -o -path '*/commonMain/*' -name '*.kt' -print | xargs cat | wc -l
find core feature shared app -path '*/build' -prune -o \( -path '*/androidMain/*' -o -path '*/iosMain/*' -o -path 'app/src/main/*' \) -name '*.kt' -print | xargs cat | wc -l
```

Pass when the first number is at least 90% of the sum of the two, excluding `:core:ai` (Android-only by
decision), and when every file in `androidMain`/`iosMain` implements a capability listed in
`contracts/platform-contracts.md`.
