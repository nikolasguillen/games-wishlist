# Baseline: Multiplatform Migration

Recorded before any migration change. Later phases compare against this file (SC-001, SC-002, the Android
update path). Do not edit the numbers; append results below.

## T001 — Build and test baseline

| | |
|---|---|
| Date | 2026-10-08 |
| Commit | `869e84ed` (branch `010-kmp-migration`, no code change yet) |
| Command | `./gradlew :app:assembleDebug test --console=plain --continue` |
| Result | `BUILD SUCCESSFUL` |
| Test classes | **48** |
| Tests | **456** (0 failures, 0 skipped) |

Per module (classes / tests): `app` 1 / 1, `core:data` 19 / 106, `core:domain` 14 / 127, `core:network` 1 / 9,
`core:ui` 1 / 12, `feature:game-detail` 2 / 35, `feature:lists` 2 / 11, `feature:onboarding` 1 / 27,
`feature:radar` 1 / 7, `feature:search` 1 / 35, `feature:settings` 3 / 31, `feature:wishlist` 2 / 55.

Counted from `*/build/test-results/testDebugUnitTest/*.xml`. SC-001's floor is **456 tests**.

## T002 — Parity screenshots (partial)

Captured with `adb` on the `Pixel_10_Pro` emulator (API 37, 1280x2856), app appearance "Follow System", the
system night mode toggled with `adb shell cmd uimode night yes|no`. Stored in `captures/baseline/`, which is
git-ignored.

| Screen | Light | Dark |
|---|---|---|
| Search (Discover) | `01-search-light.png` | `01-search-dark.png` |
| Radar | `02-radar-light.png` | `02-radar-dark.png` |
| Lists | `03-lists-light.png` | `03-lists-dark.png` |
| Settings | `04-settings-light.png` | `04-settings-dark.png` |

**Still to capture by hand** (needs interaction `adb` can't script reliably): game detail, a wishlist's detail
page, the edit-list sheet, onboarding, the owned-platforms picker, the reminders screen, search results with a
filter and sort, and the share sheet.

## T003 — Update-path device

The emulator `emulator-5554` already runs the pre-migration debug build with real data, so it serves as the
update-path device. Its state was snapshotted before any change, to `/tmp/questlog-baseline/data/`
(outside the repo, volatile), together with the baseline APK `/tmp/questlog-baseline/app-debug-baseline.apk`.

| Store | Baseline |
|---|---|
| `databases/quest_log_database` (+ `-wal`, `-shm`) | games: **9**, wishlists: **2**, `game_list_cross_ref`: **8** |
| `files/datastore/settings.preferences_pb` | 28 bytes, appearance "Follow System", onboarding completed |
| `files/wishlist_covers/` | empty (no custom cover) |
| Reminders | none scheduled; notifications are off on this emulator |

**Gaps against T003's wording**: no custom list cover, appearance is "Follow System" (not dark), and no release
reminder is enabled. The automated update-path comparison (row counts, settings file, onboarding not shown)
still works; the cover and reminder checks need the owner to set them up on the device before the first
install of a migrated build.

To recompute the counts after an update:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
mkdir -p /tmp/ql-after && cd /tmp/ql-after
for f in quest_log_database quest_log_database-wal quest_log_database-shm; do
  $ADB exec-out run-as com.nikolasguillen.questlog cat databases/$f > $f; done
sqlite3 quest_log_database "select 'games',count(*) from games union all select 'wishlists',count(*) from wishlists union all select 'cross_ref',count(*) from game_list_cross_ref;"
```

## iOS walkthrough (T104–T107)

Run on the **iPhone 17 simulator, iOS 26.5**, debug build, driven by an XCUITest scratch driver kept outside the
repo. The simulator runtimes installed here are iOS 26.5 and 27.0 only, so the deployment target (iOS 16.0) is
**not exercised**: it compiles and links against it, nothing more. Device install, signing and TestFlight stay in
`docs/roadmap.md`.

| # | Row | Result |
|---|---|---|
| 1 | Fresh install → launch | Pass. Launch screen (wordmark on `#FAFAFA`), then a five-page welcome flow with no reminders page. |
| 2 | Onboarding → search → save | Pass. Search returns live IGDB results (credentials reach the iOS build) and covers load. SC-003 (first game saved within 3 minutes) was not timed: the flow took a handful of taps, but the run was driven step by step. |
| 3 | Kill → relaunch | Pass. No welcome flow; both saved games and the custom list are still there (FR-010). |
| 4 | Radar dates at UTC−8 / UTC+9 | Pass, same as Android by construction: both format `DateUtils.timestampToLocalDate`, which uses the device zone. IGDB's midnight-UTC "Nov 19" shows as **Nov 18 at UTC−8** and Nov 19 at UTC+9 and UTC+1. This is existing Android behaviour, not an iOS regression. |
| 5 | Game detail | Pass. Share opens the iOS share sheet with the share text. No translate action, no reminder control or banner. |
| 6 | Settings | Pass. Appearance, owned platforms, welcome tour, About (`1.0`, from `CFBundleShortVersionString`). No reminders, permission or translation rows. |
| 7 | Appearance | Pass. Follow system, force light, force dark all switch. Status-bar text stays readable when the app theme and the system theme disagree, so T106 needed no change. |
| 8 | Lists | Pass. Create with a cover from the photo picker, rename, delete (the cover file is removed). The cover survived a relaunch. |
| 9 | Swipe back | Pass on Settings (back to Search) and detail. |
| 10 | Offline search | **Not exercised**: a simulator shares the Mac's network and cannot be taken offline from here. The Darwin error mapping is covered by `PlatformTransportFailureTest` (5 tests, `:core:network:iosSimulatorArm64Test`). |
| 11 | 24 h launch refresh | Partly. The timestamp key `release_dates_last_refresh_epoch_ms` is written to the DataStore after a launch; the 24 h policy itself is covered by `InProcessReleaseRefreshSchedulerTest` (10 tests). The stored timestamp was not edited to force a second refresh. |

**T107, performance.** After `simctl launch`, the launch screen is still up at 0.9 s and the Discover screen is
drawn by 1.4 s, inside the 2 s goal. Scroll smoothness was not measured: a simulator is not representative, so
that check waits for a device.

**Found and fixed during the walkthrough**

- Covers did not load: `coil-network-ktor3` was only a dependency of `:app`. It is now in `:shared` with the
  engine per platform (OkHttp on Android, Darwin on iOS).
- The welcome flow's Radar page told iOS users to "turn on a reminder", which they cannot. Platforms without
  reminders now get their own page copy.

## T109 — Boundary audit

Run on the branch after Phase 5.

- **Feature modules** list no `project(...)` in their own build files. `questlog.kmp.feature` gives each of them exactly
  `:core:common`, `:core:model`, `:core:domain`, `:core:ui`, `:core:navigation` and `:core:designsystem`.
- **`:shared`** depends on every module except `:core:ai`.
- **`:core:ai`** is referenced by `settings.gradle.kts`, its own build file, a comment in `:shared`, and
  `core/data/build.gradle.kts` line 27, inside `androidMain.dependencies`.
- **`:core:model`**: no `android`, `androidx.compose` or `androidx.activity` import under `core/model/src`.
- **Navigation 3**: no build file outside `core/navigation` and `shared` mentions it.

## T116 — History sampling (SC-005)

Five commits spread across Phases 2–5, each checked out in its own worktree with `local.properties` copied in,
then `./gradlew test :app:assembleDebug`. All five built and passed.

| Commit | Step | Test classes / tests | Failures |
|---|---|---|---|
| `459a02a4` | `refactor: replace hilt with koin` | 52 / 488 | 0 |
| `037989b5` | `refactor(database): move room to the bundled sqlite driver` | 54 / 513 | 0 |
| `c90dd39f` | `build: settle the unit test command for multiplatform modules` | 55 / 517 | 0 |
| `c795d30b` | `refactor: convert the ui layer to compose multiplatform` | 55 / 517 | 0 |
| `dc73f412` | `feat(ios): add the ios app shell` | 58 / 535 | 0 |

The floor from T001 is 456 tests; every sampled commit is above it.

## Phase 7 — Final verification

**T117, SC-001.** `./gradlew test` on the final commit ran **58 test classes and 535 Android-host tests with no
failures** (floor from T001: 456), plus the 5 tests of `:core:network:iosSimulatorArm64Test`. Beyond renames
(`src/test/java` → `src/androidHostTest/kotlin`) the existing tests changed in four ways only: `RequestBody` →
`String` in the `GameRepositoryImpl*` tests, the extra `ReleaseRemindersAvailability` constructor argument in the
ViewModel tests, Compose `Res` handles replacing `R.string`/`R.plurals` in expected `UiText`s, and one new
expectation in `OnboardingViewModelTest` for the iOS page list. No test was deleted or disabled.

**T118, SC-002.** Final Android debug build on `emulator-5554`, compared with `captures/baseline/` outside the top
140 px (status bar clock). Radar, Lists and Settings are **pixel-identical** in light and dark (0.0 % of pixels differ by
more than 24/255). Search has the same layout, and covers load, but its shelves hold different games than the baseline run.
They come from IGDB and the saved-games taste profile, so two runs on different days differ.

**T119, SC-004.** `commonMain` holds 26 753 lines and the Android/iOS source sets 1 648 (`:core:ai` excluded):
**94.2 %** shared. Every file under `androidMain` and `iosMain` is a contract implementation, a platform module, or one
of the small seams listed in `contracts/platform-contracts.md`.

**T120, SC-007.** One edit to `search_placeholder` in `feature/search/.../strings.xml`, then
`:app:assembleDebug` and `xcodebuild`: the new text shows on the Android emulator and on the iOS simulator. The edit
was reverted.

**T121, quickstart end to end.** Android: parity rows re-checked on the final build (T118), update path (the debug build was
installed over the existing data several times and the lists, the settings and the "onboarding done" flag survived),
deep link `questlog://game/{id}` on the **R8 release build** from a cold start (opens the game, covers load), `help
--configuration-cache` twice reuses the cache, `releaseRuntimeClasspath` resolves `material3` to `1.5.0-beta01`, and
`:shared:linkDebugFrameworkIosSimulatorArm64` plus `compileKotlinIosArm64` pass. iOS: the walkthrough above, run on the
code as it stands apart from the last build setting. Windows checks (T065, T090) still need the owner's Windows machine.
No scratch file remains in `specs/010-kmp-migration/`.

## Phase 8 — Convergence

**T123, Android reminders on the final build** (`emulator-5554`, debug build, notifications allowed). Turning the reminder on
from GTA VI's detail page shows the snackbar and queues a `ReleaseNotificationWorker` job next to the periodic
`ReleaseDatesRefreshWorker` in `dumpsys jobscheduler`. The emulator's clock cannot be moved from `adb shell`, so the game's
release date in the app's own database was set to yesterday and the queued job was forced with
`cmd jobscheduler run -f -n androidx.work.systemjobscheduler com.nikolasguillen.questlog <id>`. Run while the date was still
in the future, the worker correctly posted nothing; run after the edit, it posted "Grand Theft Auto VI is out today!", and
tapping the notification opened that game's detail page. Settings shows "Release reminders · 1 game", and with the
notification permission revoked it adds "Notifications are off — reminders won't arrive"; switching a reminder on then raises
the system permission request. The welcome tour has five pages and the "Never miss a release" page still offers reminders.
Everything was put back afterwards (date, opt-ins, permission). Radar's periodic refresh restores any real date on its own.

**T126, iOS row 11, the 24 h refresh.** One saved game with a stale release date was seeded into the simulator's database. With
`release_dates_last_refresh_epoch_ms` removed from `settings.preferences_pb`, the next launch replaced the date with IGDB's and
wrote a new timestamp. A second launch inside the 24 h window, with the date made stale again, left it alone. Row 10 (search
with the network off) is still open: it needs the Mac's network switched off, which was not done from here.

**T127, iOS scrolling.** Instruments' Animation Hitches template does not record on a simulator ("Hitches is not supported on this
platform"), so frame pacing could not be measured. The Discover shelves and the search grid were driven with swipes and kept
every cover loaded; the risk is recorded in `docs/tech-debt.md`.

**T125, older iOS runtime.** The app built for the simulator SDK and ran on an **iPhone 15 Pro, iOS 17.0** (an iOS 16 runtime
is not installed). Row 1: fresh install, launch screen, then the five-page welcome flow without a reminders page. Row 3: after
"Skip", a search for "hades" returned live results with covers, the favourite on Hades II's detail page saved it to the
default list, and after killing and relaunching the app there was no welcome flow and the wishlist still held the game.
Row 9: swiping back from the detail page returned to the previous screen. No difference from iOS 26.5 was seen. The deployment
target (16.0) is still not run: see the iOS 16 entry in `docs/tech-debt.md`.
