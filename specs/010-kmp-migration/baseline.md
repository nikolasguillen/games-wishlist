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
