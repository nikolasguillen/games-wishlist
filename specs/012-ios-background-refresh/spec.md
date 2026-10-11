# Feature Specification: iOS Background Refresh of Release Dates

**Feature Branch**: `012-ios-background-refresh`

**Created**: 2026-10-11

**Status**: Draft

**Input**: User description: "iOS background refresh of release dates — on iOS, refresh the release dates of saved games periodically in the background, without the user opening the app, matching Android's roughly daily refresh. Keep the in-app refresh as a fallback; no double refresh; same use case as Android."

## Background

Today the iOS app refreshes release dates only while it is open: once at launch (if the last successful
refresh is at least 24 hours old) and whenever the wishlist changes. While the app is closed nothing
refreshes, and the next launch has to catch up. Android already refreshes about once a day in the
background; iOS should match that.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Release dates are already current when I open Radar (Priority: P1)

A user who saved games and has not opened the app for a few days opens Radar and sees current release
dates straight away, with no waiting and no visible catch-up refresh. The dates were refreshed in the
background while the app was closed.

**Why this priority**: This is the whole feature. Without it the iOS app only catches up after the user
has already opened it, which is the gap with Android.

**Independent Test**: Save a few games, close the app, trigger one background run (see Verification), then
open Radar. The stored dates changed before the app was opened, and Radar shows them on its first frame
without a refresh indicator.

**Acceptance Scenarios**:

1. **Given** a wishlist with saved games and a last successful refresh at least one interval old, **When**
   the system grants a background run while the app is closed, **Then** the saved games' release dates are
   refreshed and the time of that refresh is recorded.
2. **Given** a background run has just completed successfully, **When** the user opens Radar, **Then** the
   current dates are shown immediately and the launch does not start another refresh.
3. **Given** the user never opens a setting, prompt or permission dialog for this, **When** the app is
   installed and used normally, **Then** background refresh works without any of them, and without
   notification permission.

---

### User Story 2 - A failed or cut-short run changes nothing I can see (Priority: P2)

When the background refresh cannot complete (no network, a server error, or the system ending the task
early), the user notices nothing: no error, no changed dates, no stale marker. The refresh is tried again
on a later opportunity.

**Why this priority**: Background work fails often on a phone. If failures were visible or corrupted state,
the feature would do more harm than the missing refresh.

**Independent Test**: Trigger a background run with the network off, and another that is ended early while
in progress. Afterwards the stored dates are unchanged, the last-refresh time is unchanged, and the next
run is still requested.

**Acceptance Scenarios**:

1. **Given** no network or a server error, **When** a background run executes, **Then** nothing visible
   changes, the last-successful-refresh time is not updated, and another run is requested.
2. **Given** a background run in progress, **When** the system ends it early, **Then** the work is
   cancelled cleanly, the last-successful-refresh time is not updated, and another run is requested.
3. **Given** a previous run failed, **When** a later run succeeds, **Then** the last-successful-refresh
   time is recorded for that later run only.

---

### User Story 3 - Background and in-app refreshes never overlap (Priority: P2)

The in-app refresh (at launch, and when the wishlist changes) stays as a fallback for when the system does
not grant background time. A background run and an in-app run are never active at the same time and never
refresh the same data twice.

**Why this priority**: The fallback keeps the app correct when iOS is stingy with background time, but two
overlapping refreshes would double the network traffic and could race on the recorded time.

**Independent Test**: Start an in-app refresh and request a background run while it is in flight, and the
reverse. In both cases exactly one refresh executes.

**Acceptance Scenarios**:

1. **Given** an in-app refresh is running, **When** a background run starts, **Then** it does not start a
   second refresh.
2. **Given** a background run is running, **When** the user opens the app or saves a game, **Then** no
   second refresh starts.
3. **Given** the system never grants background time, **When** the user opens the app, **Then** the
   existing launch refresh (if the last success is at least one interval old) and wishlist-change refresh
   behave exactly as they do today.

---

### User Story 4 - Nothing is requested for an empty wishlist (Priority: P3)

A user with no saved games causes no network traffic from the background refresh.

**Why this priority**: Avoids pointless requests and battery use; low effort, since the refresh already
treats an empty wishlist as an instant success.

**Independent Test**: With an empty wishlist, trigger a background run. No request leaves the device, the
run counts as a success, and the next run is requested.

**Acceptance Scenarios**:

1. **Given** an empty wishlist, **When** a background run executes, **Then** no network request is made
   and the run is recorded as a success.

---

### Edge Cases

- **The system never runs the task.** The user disabled background refresh for the app, force-quit it,
  or iOS keeps deferring it (Low Power Mode, rarely-used app, no network). The app still works: the
  in-app fallback refreshes at the next launch. No error is raised.
- **The app is launched by the system in the background, with no screen shown.** The refresh must still
  run to completion; it cannot rely on the UI having been created.
- **The run starts when the last success is newer than the interval** (for example the user refreshed in
  the app minutes ago). The run makes no network request and only requests the next run.
- **The run is ended by the system midway.** Stored dates must never end up half-updated in a way the user
  can see; the last-refresh time is untouched.
- **The user opens the app while a background run is in progress.** The app opens normally; the one refresh
  in progress finishes and is not duplicated.
- **The background request is lost** (reinstall, OS cleanup, a missed re-request). The next app launch
  requests it again, so the chain recovers without user action.
- **Wishlist changes while the app is closed.** Not possible; wishlist-change refreshes remain an in-app
  behavior.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On iOS, the app MUST refresh the release dates of saved games in the background, without the
  user opening the app.
- **FR-002**: The background refresh MUST require no setting, prompt or permission from the user, and MUST
  NOT depend on notification permission.
- **FR-003**: The refresh interval MUST be the same single value Android uses (about 24 hours); the two
  platforms MUST NOT hold separate copies of it.
- **FR-004**: The guarantee MUST be stated and documented as "at most about once per interval, when the
  system allows". The feature MUST NOT promise, and no user-facing or internal text may claim, an exact
  24-hour cadence.
- **FR-005**: A background run that starts when the last successful refresh is newer than one interval MUST
  skip the refresh and make no network request.
- **FR-006**: The next background run MUST be requested at every app launch and again every time a run
  finishes, whether it succeeded, failed or was ended early, so the chain of runs never breaks.
- **FR-007**: When the system ends a run early, work in progress MUST be cancelled cleanly, with no
  user-visible effect.
- **FR-008**: The last-successful-refresh time MUST be written only after a refresh fully succeeds, never
  after a failed, cancelled or partial run. Background and in-app runs MUST share that one time.
- **FR-009**: A failed refresh (no network, server error, early termination) MUST change nothing the user
  can see and MUST be retried on a later opportunity.
- **FR-010**: With an empty wishlist the refresh MUST make no network request and MUST count as a success.
- **FR-011**: At most one refresh MUST run at a time across the background and in-app paths. A refresh
  requested while another is running MUST NOT start a second one.
- **FR-012**: The in-app refresh (at launch when due, and on wishlist change) MUST remain as the fallback
  and keep its current behavior.
- **FR-013**: The background refresh MUST run the same refresh use case Android runs. No refresh logic
  MUST be duplicated for iOS.
- **FR-014**: The background refresh MUST work when the system launches the app in the background without
  presenting any screen.
- **FR-015**: Android behavior MUST be unchanged, and Android MUST still build and pass its tests.
- **FR-016**: The due check, the single-run guard and the timestamp rule MUST be covered by unit tests in
  common code that run without iOS.

### Key Entities

- **Last successful refresh time**: when the saved games' release dates were last refreshed in full. Shared
  by the background and in-app paths; written only on full success; decides whether a run is due.
- **Refresh run**: one attempt to refresh all saved games' release dates, started either by the system in
  the background or by the app in the foreground. Succeeds, fails, or is cancelled. At most one is active.
- **Pending background request**: the app's standing ask to the system for a future background run. Exactly
  one should be pending after every launch and after every run.

## Platform Constraints and Decisions

The owner asked this spec to settle the following; they are stated here rather than left to the plan. They
are the only place implementation vocabulary appears.

- **Contract**: `ReleaseRefreshScheduler` in `:core:domain` is reused unchanged. The iOS implementation
  replaces the `InProcessReleaseRefreshScheduler` binding in the iOS `DataPlatformModule`. No new contract
  is added; the iOS column of its table in
  `specs/010-kmp-migration/contracts/platform-contracts.md` is updated to match.
- **Bridge**: the operating system's background scheduling API (`BGTaskScheduler` / `BGAppRefreshTask`) is
  Swift/UIKit-only. The bridge is a Swift class in `iosApp/` that implements a small Kotlin-owned interface
  and is handed to Kotlin from Swift, the same shape as `FoundationModelsBridge` /
  `AppleLanguageModelBridge`. The scheduling decisions (due check, single-run guard, timestamp rule) stay
  in common Kotlin; Swift only registers the task, submits the request and reports completion.
- **Background launch**: task registration has to happen during app launch, before any view exists, and a
  background launch may never build `MainViewController` (where Koin starts today). The plan MUST resolve
  how the Kotlin side is available to a background run without a view controller (FR-014), and say so
  explicitly if that changes where Koin is initialized.
- **Modules and dependency edges**: none new. The scheduler stays in `:core:data` (its logic in
  `commonMain`, like `InProcessReleaseRefreshScheduler`); `:shared` passes the bridge through; the Swift
  side lives in `iosApp/`. Feature modules do not depend on `:core:data`; `:core:model` stays free of
  Android and Compose dependencies.
- **`iosApp/` configuration**: `Info.plist` gains the permitted background task identifier and the
  background-modes entry for app refresh; any matching capability change in the Xcode project is made
  there too.
- **Repo documentation**: the iOS notes in `core/data/CLAUDE.md`, `docs/roadmap.md` and the platform
  contracts stop saying "refresh only at launch" once this lands.

## Verification

The system decides when a background task runs and cannot be asked to run one on demand. The iOS
Simulator does not support background tasks at all: a request to schedule one is rejected there, and
Xcode's `_simulateLaunchForTaskWithIdentifier` / `_simulateExpirationForTaskWithIdentifier` debugger
commands work only on a physical device. Verification therefore has three parts (owner decision,
2026-10-11):

- **Common code (any machine)**: unit tests on the JVM for the due check, the single-run guard and the
  timestamp rule, using a fake bridge and a fake clock, in the same style as
  `InProcessReleaseRefreshSchedulerTest`.
- **On a Mac, on the simulator**: the app launches with the task registered and does not crash. A rejected
  scheduling request is handled quietly. The background run itself is exercised end to end through a
  debug-only hook that runs the same work without the system: success, not due, offline, cut short, empty
  wishlist, and overlap with an in-app refresh. None of this can be checked on Windows.
- **On a device, later**: the system-launched run and early termination, with the debugger commands above.
  Physical-device distribution is out of scope, so this path is recorded in `docs/tech-debt.md` as
  unverified until it lands. Nothing may be reported as verified on a device unless it was run there.

SC-001 to SC-005 are met on the simulator through the debug hook; the system's own dispatch of the run is
the one part left for a device.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After one granted background run on an app with saved games and a due refresh, Radar shows
  the refreshed dates on its first frame after opening, with no refresh indicator and no wait.
- **SC-002**: The background path performs at most one full refresh per interval; a run that finds the
  last success newer than one interval makes zero network requests.
- **SC-003**: With an empty wishlist, a background run makes zero network requests.
- **SC-004**: In 100% of failed or early-ended runs exercised in verification (offline, server error,
  forced expiration), the stored dates and the last-successful-refresh time are unchanged and a next run
  is still pending afterwards.
- **SC-005**: No scenario exercised in verification produces two refreshes at once, whichever path
  (background or in-app) started first.
- **SC-006**: The user needs no setting, prompt or permission to benefit: a fresh install reaches a
  working background refresh with zero additional taps.
- **SC-007**: The Android build and the full JVM test suite pass unchanged, and the new common-code tests
  cover the due check, the single-run guard and the timestamp rule.

## Assumptions

- The iOS system may defer, throttle or skip background runs (Low Power Mode, user turned background
  refresh off, app force-quit, rarely used app). This is accepted; the in-app fallback covers it, and the
  spec promises only "at most about once per interval, when the system allows".
- A short, system-limited run window is enough for the existing refresh on a typical wishlist; if the
  system ends it early, the next opportunity retries the whole refresh.
- After a failed or cut-short run, the next request asks the system to run again no earlier than the
  failure itself; the system's own throttling spaces retries, and the app adds no custom backoff.
- A background run that finds a refresh already in progress does not wait for it or start another; it ends
  as handled and the next run is requested. The in-progress refresh decides the recorded time.
- The refresh is an idempotent update of stored dates, so a cancelled run leaves them as before or fully
  updated. The plan verifies this against the repository code.
- The existing in-app behaviors (refresh at launch when due, refresh on wishlist change) are correct as
  they are and are not redesigned here.
- Out of scope, tracked as separate follow-ups in `docs/roadmap.md`: local release notifications, the
  notification permission prompt, the Settings rows for reminders, removing the `RadarWithoutReminders`
  onboarding page, and distribution to physical devices.
