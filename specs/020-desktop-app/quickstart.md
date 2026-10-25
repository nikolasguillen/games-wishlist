# Quickstart: Desktop App (Windows and macOS)

How to run the desktop app, build its installers and prove the feature end to end. Commands are given for both
wrappers: `./gradlew` on macOS and `.\gradlew.bat` on Windows (PowerShell).

## 1. One-time setup

| What | Where | Needed for |
|---|---|---|
| JDK 21 (has `jpackage`) | Gradle toolchain (foojay downloads it) | Everything |
| `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET` | `local.properties` (existing) | Every screen that loads games |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_PROJECT_ID` | `local.properties`. Values: `api_key` / `project_id` in `google-services.json` | Cloud account and backup |
| A **Desktop app** OAuth client in the Firebase project's Google Cloud console → `GOOGLE_DESKTOP_CLIENT_ID`, `GOOGLE_DESKTOP_CLIENT_SECRET` | `local.properties` | Google sign-in |
| Xcode command-line tools (`swiftc`) | macOS only; already present for iOS | macOS notifications library |
| WiX | Downloaded by `packageMsi` itself | Windows installer |

Without the cloud keys the app builds and runs, and sign-in fails with the generic error (research R6).

## 2. Run from the repository

```bash
./gradlew :desktopApp:run                  # macOS (notifications show as blocked: no app bundle, research R9)
./gradlew :desktopApp:runDistributable     # macOS, as an app bundle: notifications work
```

```powershell
.\gradlew.bat :desktopApp:run              # Windows: everything works except closed-app click-through
```

## 3. Build the installers

```powershell
.\gradlew.bat :desktopApp:packageMsi       # Windows → desktopApp\build\compose\binaries\main\msi\QuestLog-1.0.0.msi
```

```bash
./gradlew :desktopApp:packageDmg           # macOS → desktopApp/build/compose/binaries/main/dmg/QuestLog-1.0.0.dmg
```

## 4. Automated checks (every commit)

```bash
./gradlew :androidApp:assembleDebug        # Android still builds
./gradlew test                             # every androidHostTest and desktopTest
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q   # macOS: iOS still links
./gradlew :desktopApp:compileKotlin        # desktop compiles
```

## 5. Walkthrough

Each row names the requirement it proves. **W** = Windows (required). **M** = macOS: required unless marked
*(if possible)*, where macOS needs signing to be accepted (spec clarification 1). Record the result of every row
marked **record** in the platform contracts' register.

| # | Steps | Expected | Proves |
|---|---|---|---|
| 1 | Fresh app data dir. Start the app | Welcome flow; then Search with the phone's destinations (rail on a wide window) | US1 #1, FR-001 |
| 2 | Search a game, a character and a developer; open each; save the game; add it to a list | Same results, wording and states as on a phone | US1 #2, SC-002 |
| 3 | Widen the window past 840 dp with Search open, open a game; narrow again | Two panes, then one, same game open | US1 #3, FR-004 |
| 4 | Quit (close the window); check Task Manager / Activity Monitor; reopen | No QuestLog process left. Data and "welcome done" kept | US1 #4, FR-006a, SC-003 |
| 5 | Disconnect the network; start the app | Saved data shown; network screens show their offline/error states | US1 #5 |
| 6 | Mouse and keyboard only: copy/paste in search, wheel-scroll lists, Escape and mouse back button | Every action reachable; Escape and mouse back go back | US1 #6, FR-005 |
| 7 | With the app open, start it again (Start menu / Finder / second `run`) | The existing window comes forward; one process | FR-006 |
| 8 | Look for translation, a badge, a translation download row, "background" wording | None anywhere | FR-025–027, SC-011 |
| 9 | Game detail → Share | "Copied to clipboard"; the text is on the clipboard | FR-027 |
| 10 | W: installed copy. Save a game with a date tomorrow; shift its stored date (or the clock) so 09:00 is a few minutes away; Notify me on; quit | One toast naming the game at 09:00, app not running | US2 #1, FR-011–012, SC-005 |
| 11 | Same as 10 with the app open | System toast still shown | US2 #2 |
| 12 | Opt in, then turn it off (another game: unsave it); quit; wait past the time | Nothing delivered | US2 #3, SC-008 |
| 13 | Opt in for D1; force a refresh that moves the date to D2 (app open) | Pending toast is for D2 (check via the next sync's pending list or wait) | US2 #4, SC-007 |
| 14 | TBA / month-only game: opt in | Opt-in kept; the control says no reminder until a day is known | US2 #6, FR-015 |
| 15 | Click a delivered toast with the app closed (installed copy) | App launches on that game's detail | US3 #1, SC-006 |
| 16 | Click a toast with the app open, unfocused | Same window comes forward on the game; one process | US3 #2 |
| 17 | Click a toast for a game unsaved meanwhile | App opens normally; detail or its error state, no crash | US3 #3 |
| 18 | Settings → Notifications with two opt-ins | Both listed; toggles don't remove rows | US4 #1 |
| 19 | W: turn QuestLog notifications off in Windows Settings; return to the app | Blocked state; the action opens Windows' notification settings; turn it back on; return: reminders re-armed with no re-opt-in | US4 #3–4, SC-009 |
| 20 | M *(bundle)*: first opt-in | macOS prompt; decline → opt-in not shown as active | US4 #2, FR-017 |
| 21 | Welcome flow on both | macOS shows the reminders page while undecided; Windows skips it when enabled; no "unavailable" page | US4 #5, FR-022 |
| 22 | Sign in with Google (browser opens, return to app) | Signed in as the phone's account | US5 #3 |
| 23 | Backup on Android; restore on desktop | Same games, lists and settings | US5 #1, SC-004 |
| 24 | Change data on desktop; back up; restore on Android | Same data on the phone | US5 #2, SC-004 |
| 25 | Sign out; sign in; delete account (reauthenticate first) | Same behaviour as mobile; session file gone | US5 #4 |
| 26 | Restore a backup that drops an opted-in game | No reminder for it remains pending | Edge: restore |
| 27 | Install the `.msi` / `.dmg` on a clean user account; launch from Start / Applications | Runs; row 10 holds from the installed copy | US6 #1–2, SC-010 |
| 28 | W: uninstall; wait past a pending reminder's time | **record** whether a stale toast appears (expected: possible, research R14) | FR-021 |
| 29 | Reinstall; launch | No reminder pending for a game not opted in | Edge: reinstall |
| 30 | W: sleep or shut down over a reminder's 09:00; wake / start later | **record** whether it is shown late | Edge: computer off |
| 31 | W: change the time zone after arming; relaunch | Re-armed to 09:00 in the new zone | FR-019 |
| 32 | M *(if possible)*: rows 10, 15 and 16 from `runDistributable` | **record** the result | SC-005/006 on macOS |
| 33 | Opt in a game whose release date was refreshed to a past day, and one that is already released | No notification is armed or delivered for either | FR-014 |
| 34 | Opt in 65 games with different future dates | Only the 64 soonest are pending; unsaving one arms the next | FR-020 |
| 35 | Toggle "Notify me" on Radar, game detail and Settings for one game | The state matches on all three surfaces | FR-009 |
| 36 | W: with notifications blocked, opt in from game detail | The denied dialog appears once and its button opens Windows Settings | FR-017 |
