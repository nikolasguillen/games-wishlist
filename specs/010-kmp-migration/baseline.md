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
