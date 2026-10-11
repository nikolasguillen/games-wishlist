# Contract: iOS Background Refresh Seams

The signatures are the agreed shape, not finished code. KDoc and parameter names follow the house style
when they are written. The `specs/010-kmp-migration/contracts/platform-contracts.md` changes listed at the
end are made in the **first** implementation task, before any code, per the root `CLAUDE.md`.

## Domain contract — unchanged

`ReleaseRefreshScheduler` (`:core:domain`) keeps its signature. On iOS it is bound to
`BackgroundTaskReleaseRefreshScheduler` in place of `InProcessReleaseRefreshScheduler`.

| | iOS behavior |
|---|---|
| `schedulePeriodicRefresh()` | Submits the next background request, runs the refresh in the app if due, then submits again with the new date (`earliestBegin` per data-model) |
| `scheduleImmediateRefresh()` | Submits the next background request, runs the refresh in the app now, then submits again with the new date |
| *(system-launched task)* | Submits the next request, runs the refresh if due (joining one already in flight), submits again, then reports the outcome |

## Kotlin → Swift: `AppRefreshTaskBridge` — `:core:data` `commonMain`, implemented in Swift

```kotlin
interface AppRefreshTaskBridge {
    /** Replaces any pending request with one that may start no earlier than [earliestBeginEpochMillis]. */
    fun submitRequest(earliestBeginEpochMillis: Long)
}
```

- Swift (`iosApp/iosApp/ReleaseRefreshTask.swift`) builds a `BGAppRefreshTaskRequest` for the task identifier
  and calls `BGTaskScheduler.shared.submit`. A thrown error is logged through `os.Logger` (category
  `background-refresh`) and swallowed. Kotlin never learns of it and needs nothing from it.
- May be called from any thread.
- Bound in Koin by `startQuestLog(...)`, the way `AppleLanguageModelBridge` is bound today.

## Swift → Kotlin: `AppRefreshTaskHandler` and `AppRefreshTaskRun` — `:core:data` `commonMain`, implemented in Kotlin

```kotlin
interface AppRefreshTaskHandler {
    /**
     * Runs one background refresh. [onFinished] is called exactly once, from any thread, after the next
     * request has been submitted: `true` if the refresh succeeded or was not due, `false` if it failed or was
     * cancelled.
     */
    fun run(onFinished: (Boolean) -> Unit): AppRefreshTaskRun
}

interface AppRefreshTaskRun {
    /** Ends the run early. [AppRefreshTaskHandler.run]'s callback still fires, with `false`. */
    fun cancel()
}
```

- Implemented by `BackgroundTaskReleaseRefreshScheduler`, which is bound in the iOS `dataPlatformModule` as both
  `ReleaseRefreshScheduler` and `AppRefreshTaskHandler`.
- Nothing is `suspend`, for the reasons recorded for `AppleLanguageModelBridge`.
- All three interfaces are public only because Swift or `:shared` must see them. Everything else in the
  feature is `internal`.

## iOS entry points — `shared/src/iosMain`

```kotlin
/** Starts Kotlin once per process. Called from the app delegate's didFinishLaunching, before it returns. */
fun startQuestLog(
    languageModelBridge: AppleLanguageModelBridge,
    appRefreshTaskBridge: AppRefreshTaskBridge
)

/** The background task's body: resolves AppRefreshTaskHandler from Koin and runs it. */
fun runAppRefreshTask(onFinished: (Boolean) -> Unit): AppRefreshTaskRun

/** Builds the UI. Expects startQuestLog to have run. */
fun MainViewController(): UIViewController
```

`startQuestLog` starts Koin with both bridges bound and calls `schedulePeriodicRefresh()` once.
`MainViewController` loses its parameter and its Koin start.

## Swift side — `iosApp/`

```
AppDelegate.didFinishLaunching:
    BGTaskScheduler.shared.register(forTaskWithIdentifier: id, using: nil) { task in
        let run = QuestLogIosKt.runAppRefreshTask { ok in task.setTaskCompleted(success: ok.boolValue) }
        task.expirationHandler = { run.cancel() }
    }
    QuestLogIosKt.startQuestLog(languageModelBridge: FoundationModelsBridge(),
                                appRefreshTaskBridge: ReleaseRefreshTask())
```

Registration comes first and Kotlin start second, both before `didFinishLaunching` returns. The system
invokes launch handlers only after launch finishes, so Koin is always up when the handler runs.

`#if DEBUG` only: a static hook on `ReleaseRefreshTask` that runs the same body without a `BGTask` and keeps the
handle so a second hook can cancel it. It is called from LLDB on the simulator (research R8). It references
no private API.

## `Info.plist`

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array><string>com.nikolasguillen.questlog.release-dates-refresh</string></array>
<key>UIBackgroundModes</key>
<array><string>fetch</string></array>
```

There is no entitlement and no signing change. The "Background Modes → Background fetch" capability in Xcode
is this same `Info.plist` key.

## Changes to `specs/010-kmp-migration/contracts/platform-contracts.md`

- Rewrite the iOS column of the `ReleaseRefreshScheduler` table to the table above.
- Add an `AppRefreshTaskBridge` / `AppRefreshTaskHandler` section next to `AppleLanguageModelBridge`.
- In the `AppleLanguageModelBridge` section, replace "`ContentView` passes the implementation to
  `MainViewController(languageModelBridge:)`" with the app delegate and `startQuestLog`.
