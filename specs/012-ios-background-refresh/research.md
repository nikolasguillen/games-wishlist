# Research: iOS Background Refresh of Release Dates

Every unknown from the plan's Technical Context is resolved here. Each entry gives the decision, why it was
chosen, and what else was weighed.

## R1. Which iOS background API runs the refresh

**Decision**: `BGAppRefreshTask` from the BackgroundTasks framework, registered with
`BGTaskScheduler.shared.register(forTaskWithIdentifier:using:launchHandler:)`. Its requests are
`BGAppRefreshTaskRequest`s with an `earliestBeginDate`.

**Rationale**: App refresh is the task type meant for "fetch a little fresh content so the app is current
when opened", which is exactly this feature. It needs no entitlement, only the `fetch` background mode and a
permitted identifier in `Info.plist`, so it works with automatic signing and no development team. It is
available from iOS 13, below the iOS 16 deployment target.

**Alternatives considered**:
- `BGProcessingTask`: for long, deferrable maintenance, often only while charging. Wrong shape for a
  daily refresh of a few HTTP calls, and it would run less often.
- The legacy `application(_:performFetchWithCompletionHandler:)` background fetch: deprecated since iOS 13.
- Silent push notifications: needs a server and push entitlement. Out of proportion.

## R2. Where the task handler is registered, and where Kotlin starts

**Decision**: add a `UIApplicationDelegate` to `iosApp` through `@UIApplicationDelegateAdaptor`.
`application(_:didFinishLaunchingWithOptions:)` registers the task handler and then starts the Kotlin side
(Koin and the launch-time `schedulePeriodicRefresh()`), through a new `startQuestLog(...)` entry point in
`shared/src/iosMain`. `MainViewController()` stops starting Koin and only builds the UI.

**Rationale**: Apple requires every launch handler to be registered before the app finishes launching, and
documents `didFinishLaunchingWithOptions` as the place. More importantly, when the system launches the app
**in the background** to run the task, no scene is connected, so `ContentView` and `MainViewController` may
never be built. Koin starts inside `MainViewController` today, so a background launch would find no Kotlin
graph (spec FR-014). Starting Kotlin from the delegate makes the graph exist on every launch, with or
without UI. The guard against SwiftUI building the hosting view twice becomes unnecessary for Koin, because
`didFinishLaunching` runs once per process.

**Alternatives considered**:
- Register in the SwiftUI `App.init()`: works in practice, but it is not the documented deadline, and it
  would still need Kotlin started outside the view controller. The delegate is no more code.
- The SwiftUI `.backgroundTask(.appRefresh(...))` scene modifier (iOS 16+): it registers for you, but the
  handler is a Swift `async` closure whose cancellation does not reach Kotlin coroutines. A Kotlin `suspend`
  call from Swift has no cancellation. It hides `setTaskCompleted` and the expiration handler, which are
  exactly what FR-007 needs to control. It would still need Kotlin started outside the view controller.
- Keep Koin in `MainViewController` and lazily start it from the task handler too: two start paths, each
  with its own set of bridges. This is error-prone and duplicates the start-up.

## R3. The Kotlin↔Swift seam

**Decision**: two directions, each a small interface owned by Kotlin in `:core:data`
(`commonMain/.../scheduler/`), public only because Swift has to see them. This is the same shape as
`AppleLanguageModelBridge`.

- **Kotlin → Swift**: `AppRefreshTaskBridge`, implemented in Swift. It has one method: submit the next
  request no earlier than a given instant. Swift owns the task identifier and logs submit failures; the
  identifier never crosses into Kotlin.
- **Swift → Kotlin**: `AppRefreshTaskHandler`, implemented in Kotlin. The Swift launch handler calls it
  through `runAppRefreshTask(...)` in `shared/src/iosMain`. It takes an `onFinished(success)` callback and
  returns an `AppRefreshTaskRun` handle whose `cancel()` the Swift expiration handler calls.

Nothing on either interface is `suspend`. The reasons are the ones recorded for `AppleLanguageModelBridge`:
there is no cancellation across the boundary, and a thrown Swift error would crash.

**Rationale**: all scheduling decisions (due check, single-run guard, timestamp rule, next begin date) stay
in common Kotlin, where the JVM tests reach them (FR-016). Swift does only what Kotlin cannot: register,
submit, complete, expire.

**Alternatives considered**:
- A Swift class that decides when to submit: puts logic where no test reaches it.
- Kotlin/Native calling `BGTaskScheduler` directly from `iosMain` via the `platform.BackgroundTasks` cinterop:
  technically reachable from Objective-C headers. But registration must still happen in
  `didFinishLaunching`, and the handler's `BGTask` would be handled in `iosMain`, which has no tests. The
  user's brief and the existing bridge pattern both point to Swift. Rejected to keep one way of reaching
  platform services from `iosApp/`.
- A new `:core:domain` contract: not needed. `ReleaseRefreshScheduler` stays the only domain contract, and
  the two interfaces above are a platform seam inside `:core:data`, like `AppleLanguageModelBridge`.

## R4. One refresh at a time across both paths, and what a background run does when one is in flight

**Decision**: extract the scheduling rules into `ReleaseRefreshRunner` (internal, `commonMain`). It holds
the one in-flight run as a shared `Deferred` in the app scope. A request that arrives during a run **joins**
it instead of starting another, and receives its outcome. Check-and-start is guarded by a `Mutex`.

**Rationale**: a background launch also runs `didFinishLaunching`, and `startQuestLog` calls
`schedulePeriodicRefresh()`, which starts an in-app refresh when it is due. Moments later the system invokes
the task handler. With today's guard (`tryLock` and drop), the background run would see the lock taken,
report "done" to the system right away, and iOS would suspend the app while the in-app refresh is still
mid-flight. That is a refresh cut short every time. Joining keeps the task open until the one real refresh
finishes, so there is no double refresh (FR-011) and no premature completion. For in-app callers, joining is
indistinguishable from today's "dropped", because they are fire-and-forget.

**Alternatives considered**:
- Keep `tryLock`-and-drop and skip the launch-time refresh when the app is in the background: needs
  `UIApplication.applicationState` in Kotlin and still drops a background run that races a wishlist-change
  refresh.
- Let the background run wait for the lock and then run its own refresh: refreshes twice.

## R5. When the next run is requested

**Decision**: the scheduler submits a request **before** every runner call starts and again **after** it
completes, whatever the origin (launch, wishlist change, background task) and whatever the outcome
(refreshed, not due, failed, cancelled). Each submit uses:

```
earliestBegin = lastSuccess == null ? now : max(now, lastSuccess + RELEASE_DATES_REFRESH_INTERVAL)
```

**Rationale**: one rule covers FR-005, FR-006 and FR-009. The launch always produces a runner call, even
when it is not due, so "submit at launch" falls out of it. The submit **before** the work is what keeps the
chain unbroken (FR-006): a launch refresh does network work, and if the system suspends or kills the app
during it, the post-run submit would never happen. The first submit uses the date from the old timestamp, the
second the date from the new one, and the second replaces the first. A failure leaves `lastSuccess` old, so the next
request is "now" and the system's own throttling spaces the retries (spec assumption: no custom backoff).
Because the date derives from the last *success*, re-submitting at every launch does not push the window
out, which is the iOS equivalent of the WorkManager `KEEP` policy. A submit for an identifier that already
has a pending request replaces it, so there is never more than one.

**Alternatives considered**: `now + interval` after each run, which drifts later with every launch and
never runs for a daily user. A fixed short backoff after failure, which the spec's assumptions rule out.

## R6. What "the system ended the task early" does

**Decision**: the Swift expiration handler calls `AppRefreshTaskRun.cancel()`. That cancels the run
coroutine **and** the shared in-flight refresh, even when an in-app call started it, because the app is
about to be suspended either way. The run's completion path, in `NonCancellable`, submits the next request
and then calls `onFinished(false)` exactly once. Swift passes that to `setTaskCompleted(success:)`.

**Rationale**: Apple requires `setTaskCompleted` even after expiration, and recommends submitting the next
request before completing. The timestamp is written only when the use case returns `Success`. A
cancellation propagates as `CancellationException`, which `toRepositoryError()` rethrows, so it is never
recorded (FR-008).

**Data safety on cancellation (spec assumption verified)**: `refreshSavedGameReleaseDates()` performs every
network call before writing anything, then writes platforms (`IGNORE`) and cross-refs (one `REPLACE` batch).
A cancellation between the two writes leaves at most extra platform rows that nothing displays. Radar never
shows a half-updated state.

## R7. Outcome reported to the system

**Decision**: `REFRESHED` → `true`; `NOT_DUE` → `true` (nothing to do is a successful run); `FAILED` → `false`;
cancelled → `false`.

**Rationale**: `success: false` tells iOS the work did not happen. That is honest for failures, and harmless
because the next request is already submitted.

## R8. Verification on the simulator (owner decision, 2026-10-11)

**Finding**: Apple's documentation says the Simulator does not support background processing.
`BGTaskScheduler.submit` fails there with `BGTaskScheduler.Error.Code.unavailable`, and the
`_simulateLaunchForTaskWithIdentifier:` / `_simulateExpirationForTaskWithIdentifier:` debugger commands
"work only on devices". The spec's Verification section assumed the simulator, so it is corrected.

**Decision** (owner chose "simulator now, device later"):
- On the simulator: the app launches with the task registered and does not crash; a rejected `submit` is
  logged and swallowed; the Kotlin run path is exercised end to end through a `#if DEBUG` Swift hook,
  called from LLDB, that runs the same handler body without a `BGTask` (run, offline run, cancel-midway).
- On a device, later: the OS-triggered launch and expiration with the private debugger commands. Recorded
  in `docs/tech-debt.md` as an unverified path, to be closed when device distribution lands
  (`docs/roadmap.md`).

**Alternatives considered**: pulling a device run into this feature (adds signing setup that the spec ruled
out of scope), or simulator-only with no recorded follow-up (hides a gap).

## R9. What happens to `InProcessReleaseRefreshScheduler`

**Decision**: remove it. Its rules move into `ReleaseRefreshRunner`, and its in-app behavior moves into the
new `BackgroundTaskReleaseRefreshScheduler`. Every test case in `InProcessReleaseRefreshSchedulerTest` is
carried over, unchanged in intent, into `ReleaseRefreshRunnerTest` or `BackgroundTaskReleaseRefreshSchedulerTest`
in the same commit. None are dropped or weakened.

**Rationale**: after the iOS binding moves, nothing binds it. Android uses WorkManager. Keeping a dead
class next to its replacement is the kind of drift `docs/tech-debt.md` tracks.

**Alternatives considered**: keep `InProcessReleaseRefreshScheduler` as the in-app half and wrap it. That
means three scheduler-shaped classes for one platform, and its name would no longer be true.
