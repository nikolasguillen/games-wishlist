# Implementation Plan: iOS Background Refresh of Release Dates

**Branch**: `012-ios-background-refresh` | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/012-ios-background-refresh/spec.md`

## Summary

On iOS, refresh saved games' release dates in the background with a `BGAppRefreshTask`, about once per
24 h when the system allows. The in-app refresh at launch and on wishlist change stays as the fallback.

All scheduling rules live in common Kotlin in `:core:data`: the due check, one run at a time, the timestamp
written only on success, and the next request's earliest date. They are JVM-tested there. Swift in `iosApp/`
only registers the task, submits requests, and reports completion and expiration, through two small
Kotlin-owned interfaces. This is the same pattern as `AppleLanguageModelBridge`.

Kotlin start-up moves from `MainViewController` to a new `startQuestLog(...)` called from an app delegate.
A background launch builds no UI, so Koin has to exist without it.

## Technical Context

**Language/Version**: Kotlin 2.4.20 (Multiplatform), Swift 5 in `iosApp/`

**Primary Dependencies**: kotlinx-coroutines, Koin 4.2.2, AndroidX DataStore Preferences (common), Apple
BackgroundTasks framework (`BGTaskScheduler`, `BGAppRefreshTask`). There is no new library.

**Storage**: the existing settings DataStore key `release_dates_last_refresh_epoch_ms`. No Room change

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test in `core/data/src/androidHostTest`. Manual simulator
checks on a Mac (quickstart §3)

**Target Platform**: iOS 16+ (the feature) and Android (unchanged, must stay green)

**Project Type**: mobile app, Kotlin Multiplatform with an Xcode host project

**Performance Goals**: a background run finishes within the system's app-refresh window (about 30 s) for a
typical wishlist. This is the existing chunked, concurrency-limited refresh, unchanged

**Constraints**: no exact cadence. The guarantee is "at most about once per 24 h, when the system allows".
The Simulator cannot run background tasks (research R8). There are no `suspend` members across the
Swift boundary. `commonMain` has no `android.*`/`java.*` imports

**Scale/Scope**: about 4 new Kotlin files, 1 removed class (and its test, whose cases are carried over),
2 new Swift files, `Info.plist`, 4 docs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass.*

| Principle / constraint | Status | Notes |
|---|---|---|
| I. Module boundaries | ✅ | No new module, and no new dependency edge. `:shared` → `:core:data` already exists; Swift reaches Kotlin through the `QuestLogShared` framework as it does today. Feature modules are untouched |
| II. Typed errors | ✅ | The runner consumes `AppResult`. `CancellationException` is rethrown by `toRepositoryError()` and is never mapped or recorded |
| III. UI renders | ✅ (n/a) | No UI change |
| IV. Reuse | ✅ | Same `RefreshReleaseDatesUseCase`, the same interval constant and the same DataStore key; no refresh logic duplicated (FR-013) |
| V. Local verification | ✅ | Real commands only (quickstart). New logic gets JVM tests in `:core:data`'s `androidHostTest`. The iOS-only parts are said to be Mac-only, and the OS-dispatch path to be device-only |
| KMP: capability through a shared contract | ✅ | `ReleaseRefreshScheduler` is unchanged. The new Kotlin↔Swift seam is added to `platform-contracts.md` first (task 1) |
| KMP: platform code placement | ✅ (documented exception) | The scheduler and runner live in `commonMain` and are bound only on iOS. This is the existing, recorded exception for `InProcessReleaseRefreshScheduler` and `AppleLanguageModelTranslator`, made so they are JVM-testable |
| KMP: Android green at every commit | ✅ | Android binds WorkManager and is untouched. Each step compiles and passes `./gradlew test` |
| No test deleted or weakened | ✅ | `InProcessReleaseRefreshSchedulerTest` is removed with its class, but every case is carried into the two new test classes **in the same commit** (research R9) |
| Injection | ✅ | Bound in the iOS `dataPlatformModule`, with the bridges bound by `startQuestLog`. `KoinGraphTest` checks the Android graph and is unaffected; the iOS graph not being covered by it is an existing gap |
| Persistence | ✅ | No entity or version change |
| Default to `internal` | ✅ | Only the three seam interfaces are public, because Swift/`:shared` must see them, as with `AppleLanguageModelBridge` |
| Dates | ✅ | `kotlin.time.Clock` / `Instant`, as the existing scheduler uses |
| Hard-to-undo decisions | none | Moving Koin start-up to the app delegate and adding a background mode are both easy to revert |

## Project Structure

### Documentation (this feature)

```text
specs/012-ios-background-refresh/
├── plan.md                              # this file
├── research.md                          # R1–R9 decisions
├── data-model.md                        # timestamp, run states, pending request
├── quickstart.md                        # how to verify
├── contracts/ios-background-refresh.md  # Kotlin↔Swift seams, entry points, Info.plist
├── checklists/requirements.md
└── tasks.md                             # /speckit-tasks
```

### Source Code (repository root)

```text
core/data/src/commonMain/kotlin/com/nikolasguillen/questlog/core/data/
├── scheduler/
│   ├── ReleaseRefreshInterval.kt                    # unchanged (the shared 24 h)
│   ├── ReleaseRefreshRunner.kt                      # NEW internal: due check, join-or-start, timestamp, next begin date
│   ├── ReleaseRefreshOutcome.kt                     # NEW internal enum: REFRESHED, NOT_DUE, FAILED
│   ├── BackgroundTaskReleaseRefreshScheduler.kt     # NEW internal: ReleaseRefreshScheduler + AppRefreshTaskHandler
│   ├── AppRefreshTaskBridge.kt                      # NEW public: Kotlin → Swift (submit)
│   ├── AppRefreshTaskHandler.kt                     # NEW public: Swift → Kotlin (run)
│   ├── AppRefreshTaskRun.kt                         # NEW public: cancel handle
│   └── InProcessReleaseRefreshScheduler.kt          # REMOVED (rules moved to the runner)
core/data/src/iosMain/.../di/DataPlatformModule.ios.kt  # bind the new scheduler as both interfaces
core/data/src/androidHostTest/.../scheduler/
├── ReleaseRefreshRunnerTest.kt                      # NEW (takes the old due/timestamp/guard/cancel cases)
├── BackgroundTaskReleaseRefreshSchedulerTest.kt     # NEW (in-app + background paths, submit rule, onFinished)
└── InProcessReleaseRefreshSchedulerTest.kt          # REMOVED in the same commit; cases carried over

shared/src/iosMain/kotlin/com/nikolasguillen/questlog/shared/
├── QuestLogIos.kt                                   # NEW: startQuestLog(...), runAppRefreshTask(...)
└── MainViewController.kt                            # loses its parameter and its Koin start

iosApp/iosApp/
├── iOSApp.swift                                     # @UIApplicationDelegateAdaptor(AppDelegate.self)
├── AppDelegate.swift                                # NEW: register the task handler, then startQuestLog
├── ReleaseRefreshTask.swift                         # NEW: AppRefreshTaskBridge impl, handler body, logger, #if DEBUG hooks
├── ContentView.swift                                # MainViewController() with no argument
└── Info.plist                                       # BGTaskSchedulerPermittedIdentifiers, UIBackgroundModes=fetch

docs and instructions (same commits as the code they describe):
├── specs/010-kmp-migration/contracts/platform-contracts.md  # iOS column, new seam, AppleLanguageModelBridge wiring
├── core/data/CLAUDE.md                              # iOS scheduler paragraph
├── CLAUDE.md                                        # "Where things live": iOS entry point row
├── docs/roadmap.md                                  # iOS follow-ups: background refresh done; scheduling note
└── docs/tech-debt.md                                # NEW entry: OS-dispatched run never exercised (device only)
```

**Structure Decision**: no new module. The Kotlin work sits in `:core:data` next to the scheduler it
replaces. The iOS entry points are in `:shared`'s `iosMain`, which already owns `MainViewController`. The
Swift work is in `iosApp/`.

## Implementation Order

Each step leaves Android building and `./gradlew test` green.

1. **Contracts first**: update `platform-contracts.md` (contract §"Changes to …").
2. **Runner + tests**: add `ReleaseRefreshRunner`/`ReleaseRefreshOutcome`, and move the old test cases into
   `ReleaseRefreshRunnerTest`. Add the join, next-begin and cancel cases. `InProcessReleaseRefreshScheduler`
   can delegate to the runner temporarily so iOS still binds something that works.
3. **Seam interfaces + `BackgroundTaskReleaseRefreshScheduler` + tests**. Switch the iOS binding. Remove
   `InProcessReleaseRefreshScheduler` and its test once every case is accounted for.
4. **`:shared` iOS entry points**: `startQuestLog`, `runAppRefreshTask`, and a parameterless
   `MainViewController`. Mac: link the framework.
5. **`iosApp/`**: the app delegate, `ReleaseRefreshTask.swift`, `Info.plist`, `ContentView`. Mac: xcodebuild,
   then quickstart §3.
6. **Docs**: `core/data/CLAUDE.md`, the root `CLAUDE.md` table, `docs/roadmap.md`, and the
   `docs/tech-debt.md` entry for the device-only gap.

## Spec Amendments Made With This Plan

- The Verification section and SC-001–SC-005 now say the Simulator cannot run background tasks. They are
  verified on the simulator through a debug hook, and the OS-dispatched path is deferred to a device (owner
  decision, research R8).

## Complexity Tracking

No constitution violations to justify.
