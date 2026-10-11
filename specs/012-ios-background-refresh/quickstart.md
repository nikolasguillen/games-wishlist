# Quickstart: Verifying iOS Background Refresh

The rules are in [data-model.md](data-model.md) and the seams in
[contracts/ios-background-refresh.md](contracts/ios-background-refresh.md). This file only says how to prove
they hold.

## 1. Any machine: build and JVM tests

```bash
./gradlew :core:data:compileAndroidMain --console=plain -q
./gradlew :core:data:testAndroidHostTest --console=plain -q   # ReleaseRefreshRunnerTest, BackgroundTaskReleaseRefreshSchedulerTest
./gradlew :androidApp:assembleDebug
./gradlew test
```

On Windows, use `.\gradlew.bat`. Expected: all green. The new tests cover, at least:

| Rule | Case |
|---|---|
| Due check | never refreshed → runs; 24 h old → runs; 23 h 59 min old → no use-case call |
| Timestamp | success writes `now`; failure and cancellation leave it as it was |
| Single run | a second request during a run (in-app or background) joins it: one use-case call, same outcome |
| Background outcome | refreshed / not due → `onFinished(true)`; failed / cancelled → `onFinished(false)`, exactly once |
| Next request | submitted after every run, any origin or outcome; `earliestBegin` = now when never refreshed or overdue, `lastSuccess + 24h` otherwise |
| Cancel | `cancel()` during a run stops the use case, writes nothing, still submits and reports `false` |

Every case from the old `InProcessReleaseRefreshSchedulerTest` appears in one of the two new classes.

## 2. Mac only: iOS compile

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

## 3. Mac only: simulator checks

The Simulator cannot run or simulate background tasks (research R8), so these checks exercise everything
except the system's own dispatch.

1. **Launch**: run from Xcode. The app opens normally; Radar works; no crash at registration. The console
   shows the `background-refresh` log line saying the submit was rejected as `unavailable`. That is
   expected on the Simulator.
2. **Background body, success**: save two games, pause the debugger, run
   `e -l swift -- ReleaseRefreshTask.debugRun()` (exact name per implementation), and resume. The log
   shows `success: true`. Relaunch: Radar shows the refreshed dates immediately, and no launch refresh
   runs, because the timestamp is fresh.
3. **Not due**: run the hook again straight away. It reports `true` with no network traffic. Check the
   Ktor log in the debug build.
4. **Offline**: turn the Mac's network off, or use Network Link Conditioner at 100% loss, and run the
   hook. It reports `false`, Radar is unchanged, and the timestamp is unchanged.
5. **Cut short**: start the hook and immediately run the debug cancel hook. It reports `false` exactly once,
   and nothing is written.
6. **Empty wishlist**: remove all saved games and run the hook. No request is logged, and it reports `true`.
7. **No overlap**: save a game (an in-app refresh starts) and run the hook at once. The Ktor log shows one
   refresh.

## 4. Device, later (not part of this feature)

When device distribution lands (`docs/roadmap.md`), on a physical iPhone: background the app, pause, then
run
`e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.nikolasguillen.questlog.release-dates-refresh"]`,
resume, and confirm the dates refresh with the app never opened. Repeat with
`_simulateExpirationForTaskWithIdentifier:` during a run. Until then, this gap stays in `docs/tech-debt.md`.
