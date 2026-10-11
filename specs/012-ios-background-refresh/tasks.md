---

description: "Task list for iOS background refresh of release dates"
---

# Tasks: iOS Background Refresh of Release Dates

**Input**: Design documents from `/specs/012-ios-background-refresh/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ios-background-refresh.md, quickstart.md

**Tests**: Included. Spec FR-016 requires unit tests in common code for the due check, the single-run guard and
the timestamp rule. They are JUnit4 + MockK + `kotlinx-coroutines-test` in `:core:data`'s `androidHostTest`, as the
constitution prescribes. Tests come before the code they cover.

**Organization**: Grouped by user story. Swift and iOS-link tasks are marked **(Mac only)**: they cannot be
compiled or run on Windows, and must not be reported as verified unless they were run on a Mac.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US4, mapping to the user stories in spec.md

## Path Conventions

Short names used below:

- `DATA-MAIN` = `core/data/src/commonMain/kotlin/com/nikolasguillen/questlog/core/data/scheduler`
- `DATA-TEST` = `core/data/src/androidHostTest/kotlin/com/nikolasguillen/questlog/core/data/scheduler`
- `SHARED-IOS` = `shared/src/iosMain/kotlin/com/nikolasguillen/questlog/shared`
- `IOSAPP` = `iosApp/iosApp`

Rules that apply to every task: one top-level `class`/`interface`/`enum` per file; new date code uses
`kotlin.time` (`Clock`, `Instant`); `commonMain` imports no `android.*`/`java.*`; implementations are `internal`,
and only the three seam interfaces are public (Swift and `:shared` must see them); KDoc and comments are in
English. Android must build and `./gradlew test` must pass after every task that touches Kotlin.

---

## Phase 1: Setup

**Purpose**: The contract change comes before the code, as the root `CLAUDE.md` requires for platform capabilities.

- [ ] T001 Update `specs/010-kmp-migration/contracts/platform-contracts.md` per `contracts/ios-background-refresh.md` §"Changes to platform-contracts.md": rewrite the iOS column of the `ReleaseRefreshScheduler` table (periodic: run in app if due, then submit the next background request; immediate: run in app now, then submit; system-launched task: run if due, joining a refresh in flight, submit, report); add an `AppRefreshTaskBridge` / `AppRefreshTaskHandler` section next to `AppleLanguageModelBridge`; in the `AppleLanguageModelBridge` section replace "`ContentView` passes the implementation to `MainViewController(languageModelBridge:)`" with the app delegate and `startQuestLog`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The three public seam types and the outcome enum, which every story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 [P] Create `DATA-MAIN/ReleaseRefreshOutcome.kt`: `internal enum class ReleaseRefreshOutcome { REFRESHED, NOT_DUE, FAILED }`, with KDoc saying `NOT_DUE` means the last success was newer than `RELEASE_DATES_REFRESH_INTERVAL` and no network request was made.
- [ ] T003 [P] Create `DATA-MAIN/AppRefreshTaskBridge.kt`: public `interface AppRefreshTaskBridge { fun submitRequest(earliestBeginEpochMillis: Long) }`. KDoc: implemented in Swift (`iosApp`), replaces any pending request, may be called from any thread, failures are logged and swallowed on the Swift side, nothing is `suspend`. Public only because Swift must see it (mirror the wording of `AppleLanguageModelBridge`).
- [ ] T004 [P] Create `DATA-MAIN/AppRefreshTaskRun.kt`: public `interface AppRefreshTaskRun { fun cancel() }`. KDoc: ends the run early; the run's `onFinished` callback still fires, with `false`.
- [ ] T005 [P] Create `DATA-MAIN/AppRefreshTaskHandler.kt`: public `interface AppRefreshTaskHandler { fun run(onFinished: (Boolean) -> Unit): AppRefreshTaskRun }`. KDoc: `onFinished` is called exactly once, from any thread, after the next request has been submitted; `true` if the refresh succeeded or was not due, `false` if it failed or was cancelled.
- [ ] T006 Run `./gradlew :core:data:compileAndroidMain --console=plain -q` and confirm it compiles.

**Checkpoint**: seams exist. User stories can begin.

---

## Phase 3: User Story 1 - Release dates are already current when I open Radar (Priority: P1) 🎯 MVP

**Goal**: A background run, launched by the system with no UI, refreshes the saved games' dates when due and
requests the next run. The in-app launch and wishlist-change refreshes keep working and also request the next run.

**Independent Test**: With saved games and a due refresh, call the background body (JVM: a fake bridge; simulator:
the debug hook of T017): the use case runs once, the timestamp is written, and one next request is submitted with
`earliestBegin` = `lastSuccess + 24 h`. Opening Radar afterwards shows the new dates with no launch refresh.

### Tests for User Story 1 ⚠️ write first, confirm they fail

- [ ] T007 [P] [US1] Create `DATA-TEST/ReleaseRefreshRunnerTest.kt` with the due-check and timestamp cases carried over from `InProcessReleaseRefreshSchedulerTest` (same fakes: `FakeClock`, `InMemoryPreferencesDataStore`, `TestScope`): first launch refreshes because nothing was ever refreshed; a refresh exactly 24 h old is due; a refresh from 23 h 59 min ago is not due and returns `NOT_DUE` with zero use-case calls; a successful refresh records `clock.now()`; `onlyIfDue = false` ignores how recent the last refresh was. Add: `nextEarliestBegin()` is `now` when never refreshed, `now` when overdue, and `lastSuccess + 24 h` otherwise. Key is `release_dates_last_refresh_epoch_ms`.
- [ ] T008 [P] [US1] Create `DATA-TEST/BackgroundTaskReleaseRefreshSchedulerTest.kt` with a fake `AppRefreshTaskBridge` that records submitted instants: `schedulePeriodicRefresh()` on a due state submits first, runs the use case once, then submits again at `lastSuccess + 24 h` (the new timestamp); `schedulePeriodicRefresh()` when not due makes no use-case call and still submits at `lastSuccess + 24 h`; `scheduleImmediateRefresh()` runs regardless of recency and submits; `run(onFinished)` on a due state runs once and calls `onFinished(true)` exactly once, after the final submit; a request is already submitted (at the date from the old timestamp) while the use case is still running, gated with a `CompletableDeferred`, and a second one replaces it after the run, at the new date; `run` when not due calls `onFinished(true)` with zero use-case calls.

### Implementation for User Story 1

- [ ] T009 [US1] Create `DATA-MAIN/ReleaseRefreshRunner.kt` (`internal class ReleaseRefreshRunner(refreshReleaseDates: RefreshReleaseDatesUseCase, settingsDataStore: DataStore<Preferences>, scope: CoroutineScope, clock: Clock)`): `suspend fun run(onlyIfDue: Boolean): ReleaseRefreshOutcome` (due check as in `InProcessReleaseRefreshScheduler.isDue()`: missing or `elapsed >= RELEASE_DATES_REFRESH_INTERVAL` is due; on `AppResult.Success` write `clock.now()` under the key `release_dates_last_refresh_epoch_ms`; `Failure` → `FAILED`, nothing written) and `suspend fun nextEarliestBegin(): Instant` (`lastSuccess == null ? now : max(now, lastSuccess + RELEASE_DATES_REFRESH_INTERVAL)`). Keep the private key declaration here; the old class keeps its own until T035. Run T007.
- [ ] T010 [US1] Create `DATA-MAIN/BackgroundTaskReleaseRefreshScheduler.kt` (`internal class`, implements `ReleaseRefreshScheduler` and `AppRefreshTaskHandler`; constructor takes the runner, an `AppRefreshTaskBridge`, the app `CoroutineScope` and a `Clock`): `schedulePeriodicRefresh()` launches `runner.run(onlyIfDue = true)`; `scheduleImmediateRefresh()` launches `runner.run(onlyIfDue = false)`; call `bridge.submitRequest(runner.nextEarliestBegin().toEpochMilliseconds())` **before** each `runner.run(...)` and again **after** it, the second in `withContext(NonCancellable)` (research R5: the first keeps the chain unbroken if the app is suspended or killed mid-refresh; the second replaces it with the date from the new timestamp). `run(onFinished)` submits, launches `runner.run(onlyIfDue = true)`, submits again, then calls `onFinished(outcome != FAILED)` exactly once, and returns an `AppRefreshTaskRun` whose `cancel()` cancels the launched job (full cancel handling is US2). Run T008.
- [ ] T011 [US1] In `core/data/src/iosMain/kotlin/com/nikolasguillen/questlog/core/data/di/DataPlatformModule.ios.kt` replace the `InProcessReleaseRefreshScheduler` binding with a `ReleaseRefreshRunner` single and a `BackgroundTaskReleaseRefreshScheduler` single bound as both `ReleaseRefreshScheduler` and `AppRefreshTaskHandler` (`bind<…>()`), taking `AppRefreshTaskBridge` from Koin. Update the comment above it: the refresh now also runs in the background. Do not remove `InProcessReleaseRefreshScheduler` yet. **(Mac only to compile)**
- [ ] T012 [US1] Create `SHARED-IOS/QuestLogIos.kt` per `contracts/ios-background-refresh.md` §"iOS entry points": `fun startQuestLog(languageModelBridge: AppleLanguageModelBridge, appRefreshTaskBridge: AppRefreshTaskBridge)` (no-op if Koin already started; otherwise `initKoin(isDebugBuild = Platform.isDebugBinary)` with a module binding both bridges, then `schedulePeriodicRefresh()` once; keep the `@OptIn(ExperimentalNativeApi::class)` and the KDoc about why Koin starts here: a background launch builds no UI) and `fun runAppRefreshTask(onFinished: (Boolean) -> Unit): AppRefreshTaskRun` (resolves `AppRefreshTaskHandler` from `KoinPlatform.getKoin()` and calls `run`). **(Mac only to compile)**
- [ ] T013 [US1] Edit `SHARED-IOS/MainViewController.kt`: remove the `languageModelBridge` parameter, the Koin start and the `schedulePeriodicRefresh()` call, and the now-unused imports; keep the `ComposeUIViewController` body. Update the KDoc: expects `startQuestLog` to have run. **(Mac only to compile)**
- [ ] T014 [US1] Run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and fix any compile error in T011–T013. **(Mac only)**
- [ ] T015 [P] [US1] Create `IOSAPP/ReleaseRefreshTask.swift`: `final class ReleaseRefreshTask: NSObject, DataAppRefreshTaskBridge` (use the framework-exported name; check the generated header if it differs, as `FoundationModelsBridge` uses `DataAppleLanguageModelBridge`) with a static `identifier = "com.nikolasguillen.questlog.release-dates-refresh"`; `submitRequest(earliestBeginEpochMillis:)` builds a `BGAppRefreshTaskRequest` with `earliestBeginDate = Date(timeIntervalSince1970: ms / 1000)` and calls `BGTaskScheduler.shared.submit`, logging any thrown error through `os.Logger(subsystem: "com.nikolasguillen.questlog", category: "background-refresh")` and swallowing it. Add `static func handle(_ task: BGAppRefreshTask)`: `let run = QuestLogIosKt.runAppRefreshTask { ok in task.setTaskCompleted(success: ok.boolValue) }`. Add `#if DEBUG static func debugRun()` that calls `runAppRefreshTask` with a logger-only callback and keeps the handle in a static. Import `BackgroundTasks`, `os` and `QuestLogShared`. **(Mac only)**
- [ ] T016 [US1] Create `IOSAPP/AppDelegate.swift`: `final class AppDelegate: NSObject, UIApplicationDelegate` whose `application(_:didFinishLaunchingWithOptions:)` first calls `BGTaskScheduler.shared.register(forTaskWithIdentifier: ReleaseRefreshTask.identifier, using: nil) { task in ReleaseRefreshTask.handle(task as! BGAppRefreshTask) }`, then `QuestLogIosKt.startQuestLog(languageModelBridge: FoundationModelsBridge(), appRefreshTaskBridge: ReleaseRefreshTask())`, and returns `true`. Registration must come first and both must happen before it returns. **(Mac only)**
- [ ] T017 [US1] Edit `IOSAPP/iOSApp.swift` to add `@UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate`, and `IOSAPP/ContentView.swift` to call `MainViewControllerKt.MainViewController()` with no argument. **(Mac only)**
- [ ] T018 [US1] Edit `IOSAPP/Info.plist`: add `BGTaskSchedulerPermittedIdentifiers` = `[com.nikolasguillen.questlog.release-dates-refresh]` and `UIBackgroundModes` = `[fetch]`. Add `AppDelegate.swift` and `ReleaseRefreshTask.swift` to `iosApp/iosApp.xcodeproj/project.pbxproj` by hand (PBXFileReference, PBXBuildFile, the group's children and the Sources build phase), following how `FoundationModelsBridge.swift` is listed; the project does not use synchronized folders. **(Mac only to verify)**
- [ ] T019 [US1] Run `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build`; fix any error. Then run `quickstart.md` §3 steps 1–2 (launch without a crash, the log shows the submit was rejected as unavailable; `ReleaseRefreshTask.debugRun()` from LLDB logs `success: true`, and a relaunch shows the refreshed dates with no launch refresh). **(Mac only)**
- [ ] T020 [US1] Run `./gradlew :androidApp:assembleDebug` and `./gradlew test`; confirm both pass.

**Checkpoint**: US1 works on the simulator (through the debug hook) and Android is unchanged. This is the MVP.

---

## Phase 4: User Story 2 - A failed or cut-short run changes nothing I can see (Priority: P2)

**Goal**: Failure and early termination leave the stored dates and the timestamp untouched, the system is told
`false`, and the next request is still submitted.

**Independent Test**: With a use case that fails, and with a run cancelled mid-flight: the timestamp is unchanged,
`onFinished(false)` fires exactly once, and a next request is submitted. Offline on the simulator via the debug
hook, Radar and the timestamp are unchanged.

### Tests for User Story 2 ⚠️ write first, confirm they fail

- [ ] T021 [P] [US2] Add to `DATA-TEST/ReleaseRefreshRunnerTest.kt` the failure and cancellation cases carried over from `InProcessReleaseRefreshSchedulerTest`: a failed refresh (`RepositoryError.NoNetwork`) returns `FAILED` and does not write the timestamp; a failed refresh (`RequestTimeout`) leaves an earlier timestamp as it was; a cancelled refresh (use case throws `CancellationException`) records nothing and does not block the next run. Add: `cancelRunning()` during a run stops the use case and writes nothing.
- [ ] T022 [P] [US2] Add to `DATA-TEST/BackgroundTaskReleaseRefreshSchedulerTest.kt`: a failed `run` calls `onFinished(false)` once and still submits after the run; `AppRefreshTaskRun.cancel()` during a run ends it, submits after the run, calls `onFinished(false)` exactly once, and leaves the timestamp as it was; a second `cancel()` and a `cancel()` after the run finished do nothing and do not call `onFinished` again; a failed in-app refresh still submits the next request.

### Implementation for User Story 2

- [ ] T023 [US2] In `DATA-MAIN/ReleaseRefreshRunner.kt` add `fun cancelRunning()` that cancels the in-flight refresh (if any) and make a `CancellationException` from the use case propagate without writing anything (never fold it into `FAILED`: constitution principle II). Run T021.
- [ ] T024 [US2] In `DATA-MAIN/BackgroundTaskReleaseRefreshScheduler.kt` make the `AppRefreshTaskRun` returned by `run` cancel both its own job and `runner.cancelRunning()`; run the completion block (submit next, then `onFinished(false)`) in `withContext(NonCancellable)` so it executes after a cancellation, once. Run T022.
- [ ] T025 [US2] Edit `IOSAPP/ReleaseRefreshTask.swift` `handle(_:)` to set `task.expirationHandler = { run.cancel() }` after starting the run, and add `#if DEBUG static func debugCancel()` that cancels the handle `debugRun()` kept. **(Mac only)**
- [ ] T026 [US2] Run `quickstart.md` §3 steps 4–5 on the simulator: offline `debugRun()` reports `false` with Radar and the timestamp unchanged; `debugRun()` immediately followed by `debugCancel()` reports `false` exactly once and writes nothing. **(Mac only)**
- [ ] T027 [US2] Run `./gradlew :core:data:testAndroidHostTest --console=plain -q` and `./gradlew test`; confirm both pass.

**Checkpoint**: US1 and US2 both hold.

---

## Phase 5: User Story 3 - Background and in-app refreshes never overlap (Priority: P2)

**Goal**: At most one refresh runs at a time across both paths; a second request joins the in-flight run and gets
its outcome, so a background launch whose launch-time refresh is already running neither double-refreshes nor
reports "done" early.

**Independent Test**: Start an in-app refresh, then call `run(onFinished)`: the use case is called once, and
`onFinished` fires only after that one refresh completes. The reverse order also calls it once.

### Tests for User Story 3 ⚠️ write first, confirm they fail

- [ ] T028 [P] [US3] Add to `DATA-TEST/ReleaseRefreshRunnerTest.kt` the single-run cases: a request while a refresh is running does not start a second one (gate with `CompletableDeferred`, as the old test did) and both callers receive the same outcome; once a refresh has finished, the next request runs (use case called twice); a not-due caller that arrives during a run joins it instead of returning `NOT_DUE`.
- [ ] T029 [P] [US3] Add to `DATA-TEST/BackgroundTaskReleaseRefreshSchedulerTest.kt`: `scheduleImmediateRefresh()` then `run(onFinished)` while it is in flight → one use-case call, and `onFinished` has not fired before the gate opens; `run(onFinished)` then `scheduleImmediateRefresh()` and `schedulePeriodicRefresh()` → one use-case call; cancelling the background `AppRefreshTaskRun` while it joined an in-app refresh cancels that refresh and reports `false` once.

### Implementation for User Story 3

- [ ] T030 [US3] In `DATA-MAIN/ReleaseRefreshRunner.kt` make `run` join: hold the in-flight refresh as a shared `Deferred<ReleaseRefreshOutcome>` started in the app `scope`, check-and-start guarded by a `Mutex` so two requests in a row cannot both start; a caller that finds one in flight awaits it (ignoring its own `onlyIfDue`); clear the field when it completes; `cancelRunning()` cancels that deferred. Run T028 and T029.
- [ ] T031 [US3] Run `quickstart.md` §3 step 3 (not due: run the hook twice in a row, the second makes no network request in the Ktor debug log and reports `true`) and step 7 (save a game so an in-app refresh starts, then call `debugRun()` at once: one refresh in the log, and the callback fires only after it finishes) on the simulator. **(Mac only)**

**Checkpoint**: US3 holds; US1 and US2 still pass.

---

## Phase 6: User Story 4 - Nothing is requested for an empty wishlist (Priority: P3)

**Goal**: An empty wishlist causes no network request, counts as a success, and the next request is still made.

**Independent Test**: With an empty wishlist, a background run makes no request, `onFinished(true)` fires and the
timestamp is written.

The behavior already exists (`GameRepositoryImpl.refreshSavedGameReleaseDates()` returns success on an empty list
before any network call), so this phase proves it rather than adding code.

- [ ] T032 [US4] Add to `DATA-TEST/BackgroundTaskReleaseRefreshSchedulerTest.kt` a case where the use case returns `AppResult.success(Unit)` (what an empty wishlist yields): `run` reports `true`, writes the timestamp and submits after the run. In the same file's header KDoc, state that the no-network part of an empty wishlist is owned by `GameRepositoryImpl`.
- [ ] T033 [US4] Check `core/data/src/androidHostTest` for an existing test of `refreshSavedGameReleaseDates()` with an empty saved set that asserts no `IgdbApiService` call. If there is none, add one in the repository's existing test class using its fakes. **Do not change `GameRepositoryImpl`.**
- [ ] T034 [US4] Run `quickstart.md` §3 step 6 on the simulator (empty wishlist, `debugRun()`: no request in the Ktor debug log, `true` reported). **(Mac only)**

**Checkpoint**: all four stories hold.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T035 Remove `DATA-MAIN/InProcessReleaseRefreshScheduler.kt` and `DATA-TEST/InProcessReleaseRefreshSchedulerTest.kt` **in one commit**, after confirming each of the 10 old test cases lives in a new test: first launch refreshes, 24 h old is due, 23 h 59 min is not due, success records the time, immediate ignores recency → T007; failed refresh does not advance, failed leaves an earlier timestamp, cancelled records nothing and does not block → T021; request during a run does not start a second, next request after finish runs → T028. If any is missing, add it first. Then run `./gradlew test`.
- [ ] T036 [P] Update `core/data/CLAUDE.md`: replace the iOS paragraph ("iOS has no background job runner…" and the `InProcessReleaseRefreshScheduler` mention) with the new shape (`BackgroundTaskReleaseRefreshScheduler` + `ReleaseRefreshRunner`, the two bridges, the guarantee "at most about once per interval, when the system allows", Koin started by `startQuestLog` so a background launch has it); keep it an instruction, not a changelog. Also rewrite the KDoc of `RELEASE_DATES_REFRESH_INTERVAL` in `DATA-MAIN/ReleaseRefreshInterval.kt` (it says "the in-process check on iOS"): one value for both platforms, WorkManager's periodic job on Android and the iOS background task, which runs at most about once per interval when the system allows. Check the `ReleaseRefreshScheduler` KDoc in `core/domain/.../radar/ReleaseRefreshScheduler.kt` for the same stale claim; no text may promise an exact cadence (FR-004).
- [ ] T037 [P] Update the root `CLAUDE.md`, "Where things live": the iOS entry point row now names `startQuestLog` / `runAppRefreshTask` / `MainViewController`, and the Swift-services row says the bridges are passed in through `startQuestLog` from the app delegate (it now says `MainViewController`). Check the other mentions of "refresh at launch" in the file.
- [ ] T038 [P] Update `docs/roadmap.md`: in "iOS follow-ups" drop the background refresh from the release-reminders bullet (keep notifications, permission prompt and Settings rows); in "Decisions that would be expensive to reverse" replace the `InProcessReleaseRefreshScheduler` (once per 24 h, at launch) sentence with the new scheduler. Delete rather than annotate what is no longer true.
- [ ] T039 [P] Add an entry to `docs/tech-debt.md`: the system-launched run, early termination, and a background launch with no UI (spec FR-014) of the iOS background refresh were never exercised, because the Simulator cannot run background tasks and the debugger commands work only on a device; the simulator debug hook covers the Kotlin path only. Resolve it by running `_simulateLaunchForTaskWithIdentifier:` and `_simulateExpirationForTaskWithIdentifier:` on a physical iPhone when device distribution lands.
- [ ] T040 Find remaining mentions of `InProcessReleaseRefreshScheduler` as the live iOS binding with `grep -rn InProcessReleaseRefreshScheduler --include='*.md' .` and update the ones in living documents (`specs/010-kmp-migration/contracts/*.md`, `docs/`, `CLAUDE.md` files). Run it after T036–T038, which edit some of the same files. Leave `specs/010-kmp-migration/tasks.md`, `baseline.md`, `specs/003-*` and `specs/011-*` alone: they are historical records.
- [ ] T041 Final verification: `./gradlew :androidApp:assembleDebug`, `./gradlew test`; on a Mac also the `xcodebuild` command from the root `CLAUDE.md` and `quickstart.md` §3 steps 1–7 (step 7: save a game, and `debugRun()` at once, to show one refresh in the Ktor log). Report iOS items as simulator-verified only, and the system-dispatched run as unverified.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after T001. Blocks every story.
- **US1 (Phase 3)**: after Phase 2. MVP.
- **US2 (Phase 4)**: after US1 (edits the same two Kotlin files and `ReleaseRefreshTask.swift`).
- **US3 (Phase 5)**: after US1; edits the runner, so after US2 to avoid conflicting edits in `ReleaseRefreshRunner.kt`.
- **US4 (Phase 6)**: after US1. T032 edits the same test file as T022 and T029, so run it after US3; T033 (repository test) and the simulator check can be done any time after US1.
- **Polish (Phase 7)**: after all stories. T035 needs T007, T021, T028 done.

### Within Each Story

Tests first (they fail), then the runner, then the scheduler, then DI/shared, then Swift. Mac-only tasks last.

### Parallel Opportunities

- Phase 2: T002–T005 are different files.
- US1: T007 and T008 together; T015 (Swift) can be written in parallel with T009–T013 once T003 exists, but cannot be built until T014.
- US2: T021 and T022 together. US3: T028 and T029 together.
- US4: T033 (repository test) can run alongside US2/US3 work; T032 shares a test file with T022/T029, so it follows them.
- Polish: T036–T039 are different files; T040 runs after them.

## Parallel Example: User Story 1

```text
Task: "Create DATA-TEST/ReleaseRefreshRunnerTest.kt (T007)"
Task: "Create DATA-TEST/BackgroundTaskReleaseRefreshSchedulerTest.kt (T008)"
Task: "Create IOSAPP/ReleaseRefreshTask.swift (T015)"   # after T003; build only after T014
```

## Implementation Strategy

### MVP First (US1 only)

1. T001, then Phase 2.
2. Phase 3 (US1). Stop at the checkpoint and validate on the simulator through the debug hook.
3. At this point background refresh works and the in-app fallback is intact. Failures are already harmless
   because nothing is written unless the use case succeeds; US2 adds the explicit guarantees and tests.

### Incremental Delivery

US1 → US2 (failure/expiration) → US3 (no overlap) → US4 (proof for the empty wishlist) → Polish. Android builds
and `./gradlew test` passes after each Kotlin task. Commit after each story.

## Notes

- Mac-only tasks are marked. A Windows session can do T001–T013, T020–T024, T027–T030, T032–T033 and T035–T040, and must
  say the rest is unverified.
- Constants and names that must match exactly: the task identifier `com.nikolasguillen.questlog.release-dates-refresh`
  in `Info.plist` and `ReleaseRefreshTask.identifier`; the DataStore key `release_dates_last_refresh_epoch_ms`.
- Never edit `**/build/generated/**`. Swift sees Kotlin names with the module prefix (`DataAppRefreshTaskBridge`);
  confirm against the generated header rather than assuming.
