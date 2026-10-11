# Quickstart: Verifying iOS Release Notifications

Android verification works on macOS and Windows. Everything under "iOS" needs a Mac with Xcode and a simulator.
Say which steps were run; do not assume them.

## 1. Android: nothing regressed (every commit)

```bash
./gradlew :androidApp:assembleDebug
./gradlew test
```

Expected: both succeed. `KoinGraphTest`, `SyncReleaseNotificationsUseCaseTest`,
`DeliverReleaseNotificationUseCaseTest` and the four ViewModel suites are green. Then, by hand on a device:
turn "Notify me" on for a game releasing today. The notification arrives with the same title and body as before,
and a tap opens the game.

## 2. Shared logic: host tests

```bash
./gradlew :core:data:testAndroidHostTest --console=plain -q
./gradlew :core:domain:testAndroidHostTest --console=plain -q
```

These must cover the following (see [contracts](contracts/ios-release-reminders.md) §3–4 and
[data-model](data-model.md)):

| Scenario | Expected |
|---|---|
| `schedule` for a game whose ledger entry has the same date and is not pending | Nothing is added |
| `schedule` refused by the centre | No ledger entry. A later `schedule` after permission is granted arms it |
| 70 reminders scheduled in random order | The 64 soonest are pending, and the other 6 have no ledger entry |
| `takeFiredReminders` | Returns ledger entries not pending, and only once |
| Sync after a fire | Marks it delivered and does not re-arm the same date. It does re-arm when the date moved |
| Two overlapping syncs right after a reminder fired | The second waits for the first, and neither re-arms the fired date |
| Coordinator: a permission-granted trigger event | A full sync runs, which arms a reminder that was refused while permission was undecided |
| Coordinator: a saved game leaves the set | `sync(gameId)` runs for that id. The first emission triggers nothing |
| Coordinator: trigger events during a sync | Exactly one follow-up full sync |
| Runner: successful refresh | A full sync runs before `run()` returns. A failed refresh runs none |

## 3. iOS: build

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

## 4. iOS: on the simulator (Mac only)

Reset the permission between runs with `xcrun simctl privacy booted reset all com.nikolasguillen.questlog`, or by
deleting the app.

1. **Welcome flow (US5)**: Fresh install. The flow shows the reminders page and no "unavailable" page. "Allow
   notifications" brings up the system prompt.
2. **Opt in (US1)**: Save a game whose exact release day is **today**. After 09:00 local, the reminder resolves to
   "now". Turn "Notify me" on in Radar and allow the prompt, with permission not yet decided and **without
   relaunching**. Within a few seconds a notification titled
   "<Game> is out today!" appears. With the app in the foreground it still shows as a banner (FR-014).
3. **Future date**: Opt in to a game with a future exact date. Check the pending request in the debugger with
   `UNUserNotificationCenter.current().getPendingNotificationRequests`. Its trigger is 09:00 on the release day
   and has no time zone set.
4. **No duplicate**: After step 2, background the app and bring it back twice. No second notification appears.
5. **Unsave (US2)**: Opt in to a future game, then remove it from every list and clear its status. Its pending
   request is gone at once.
6. **Tap (US3)**: With the app killed, tap the notification from step 2. The app opens on that game's detail
   screen. Repeat with the app backgrounded. For a repeatable tap without waiting for a date, run
   `xcrun simctl push booted com.nikolasguillen.questlog payload.apns` with
   `{"aps":{"alert":{"title":"t","body":"b"}},"gameId":<id>}`.
7. **Permission denied (US4)**: Deny in iOS Settings → Quest Log → Notifications. Settings → Release reminders shows
   the blocked state. Its action opens the app's notification settings. Allow again and return to the app: a game
   opted in while blocked now has a pending request (FR-012).
8. **Date moved (US2)**: Opt in, then let a refresh change the date (or, in a debug build, edit the stored date
   and run `ReleaseRefreshTask.debugRun()` from LLDB, as in spec 012). The pending trigger moves to the new day.

## 5. Docs touched

`docs/roadmap.md` loses "Release reminders on iOS". `CLAUDE.md`, the constitution's KMP paragraph and
`specs/010-kmp-migration/contracts/platform-contracts.md` no longer say that iOS ships without reminders.
