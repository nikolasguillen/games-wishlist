---

description: "Task list for the Desktop App (Windows and macOS)"
---

# Tasks: Desktop App (Windows and macOS)

**Input**: Design documents from `specs/020-desktop-app/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/desktop-platform-contracts.md](contracts/desktop-platform-contracts.md),
[contracts/firestore-rest.md](contracts/firestore-rest.md), [quickstart.md](quickstart.md)

**Tests**: The plan (R18) and constitution Principle V require JUnit4 tests for new logic, so test tasks are included
and sit beside the code they cover. They are written with the code, not as a failing-first phase. The native Windows
and macOS bindings are covered by the walkthrough in `quickstart.md §5`, not by unit tests.

**Organization**: Grouped by user story. Phases 1–2 are shared and block every story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1–US6, from `spec.md`
- Paths are from the repository root. `PKG` below means `com/nikolasguillen/questlog`.

## Conventions every task follows

- **Android must build and pass at every commit.** After any task that touches `commonMain`, a build file or a
  shared contract, run `./gradlew :androidApp:assembleDebug` and `./gradlew test` (Windows: `.\gradlew.bat`).
- One `data class` / `class` / `interface` per file. Default to `internal`. Comments and KDoc in English.
- Commit messages: `type(scope): subject` (hook in `.githooks/commit-msg`); scope `desktop` is **not** in the hook's
  `SCOPES` today, so T001 adds it.
- Desktop code never leaks into `commonMain`/`androidMain`/`iosMain`. No `android.*` or `java.*` in `commonMain`.
- Never edit `**/build/generated/**`. Never commit `local.properties` values.

---

## Phase 1: Setup (shared build infrastructure)

**Purpose**: Make the build able to have a desktop target at all.

- [X] T001 Add `desktop` to the `SCOPES` list in `.githooks/commit-msg` so `feat(desktop): …` commits pass the hook
- [X] T002 [P] Add `jna` and `jna-platform` (version `5.19.1`) and `kotlinx-coroutines-swing` (version `1.11.0`, group `org.jetbrains.kotlinx`) to `gradle/libs.versions.toml` (`[versions]` + `[libraries]`)
- [X] T003 [P] Add `include(":desktopApp")` after `include(":shared")` in `settings.gradle.kts`
- [X] T004 Add `jvm("desktop")` to `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` inside the `KotlinMultiplatformExtension` block, with `compilerOptions.jvmTarget.set(JvmTarget.JVM_17)` for the desktop target (R1); also add `desktopTest` dependencies `junit`, `mockk`, `kotlinx-coroutines-test` there so every module can host desktop tests. Update the class KDoc ("an Android target, the two iOS targets and a desktop target")
- [X] T005 Extend the root `test` task in `build.gradle.kts` so each multiplatform module also `dependsOn(module.tasks.named("desktopTest"))` next to `testAndroidHostTest`; update its comment ("every JVM unit test: Android host tests and desktop tests"). Also make `test` depend on the plain JVM shell's tests, because `:desktopApp` has no `kotlin.multiplatform` plugin and the loop skips it: `project(":desktopApp").pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { dependsOn(project(":desktopApp").tasks.named("test")) }`
- [X] T006 [P] Add to `.gitignore`: `desktopApp/resources/` (the built macOS dylib) and any `desktopApp/build` output not already covered

**Checkpoint**: `./gradlew :core:model:tasks` still configures; no module compiles for desktop yet.

---

## Phase 2: Foundational (the app compiles and runs locally on desktop)

**Purpose**: Every `expect` gets a desktop `actual`, the `:desktopApp` shell opens a window on the real root, and the
risky native/REST paths are proven by spikes. **No user story starts until this phase is done.**

Reminders and cloud are temporarily inert here (bound but doing nothing); US2–US5 replace them.

### Platform actuals per module

- [X] T007 [P] `core/common/build.gradle.kts`: add `desktopMain` dependencies `libs.jna`, `libs.jna.platform`, `libs.kotlinx.coroutines.core`, `libs.koin.core` (match the module's existing Android/iOS dependencies for everything else)
- [X] T008 [P] `core/common/src/desktopMain/kotlin/PKG/core/common/DateRendering.desktop.kt`: `actual fun renderLocalDate(date, pattern)`, `renderLocalDate(date, style: DateStyle)` and `renderLocalDateTime(dateTime, dateStyle, timeStyle)` using `java.time.format.DateTimeFormatter` with `Locale.getDefault()`, same output as `DateRendering.android.kt`
- [X] T009 [P] `core/common/src/desktopMain/kotlin/PKG/core/common/AppVersionProviderImpl.kt` (`versionName = System.getProperty("questlog.version").orEmpty()`) and `NetworkStatusProviderImpl.kt` (`isUnmeteredNetworkAvailable = true`)
- [X] T010 [P] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/AppDataDirectory.kt`: `fun appDataDirectory(): java.nio.file.Path` — `%LOCALAPPDATA%\QuestLog` on Windows (fallback `user.home\AppData\Local\QuestLog`), `~/Library/Application Support/QuestLog` on macOS; creates the directory on first use (data-model §1, R10)
- [X] T011 `core/common/src/desktopMain/kotlin/PKG/core/common/di/CommonPlatformModule.desktop.kt`: `actual val commonPlatformModule` binding `AppVersionProvider` and `NetworkStatusProvider` (after T009)
- [X] T012 `core/database/build.gradle.kts` + `core/database/src/desktopMain/kotlin/PKG/core/database/di/DatabasePlatformModule.desktop.kt`: add `kspDesktop(libs.androidx.room.compiler)` and `libs.androidx.sqlite.bundled` for desktop; bind `Room.databaseBuilder<QuestLogDatabase>(name = appDataDirectory().resolve("questlog.db").toString())` with `BundledSQLiteDriver` and the same callbacks/`fallbackToDestructiveMigration(true)` the other platforms use (after T010). Database `version` stays `1`; confirm no schema file under `core/database/schemas/` changes
- [X] T013 [P] `core/network/build.gradle.kts` + `core/network/src/desktopMain/kotlin/PKG/core/network/PlatformTransportFailure.desktop.kt` (`toPlatformTransportFailure() = null`) + `di/NetworkPlatformModule.desktop.kt` (OkHttp engine via `libs.ktor.client.okhttp`; `ElapsedRealtimeSource` over `System.currentTimeMillis()`: the JVM monotonic clock stops during sleep on macOS), mirroring the Android files
- [X] T014 [P] `core/designsystem/src/desktopMain/kotlin/PKG/core/designsystem/theme/SystemBarsAppearance.desktop.kt`: no-op `internal actual fun SystemBarsAppearance(darkTheme: Boolean)`
- [X] T015 `core/data/build.gradle.kts` + `core/data/src/desktopMain/kotlin/PKG/core/data/repository/RepositoryErrorMapper.desktop.kt`: `actual fun Throwable.isConnectivityFailure()` / `isTimeoutFailure()` with the same classes `RepositoryErrorMapper.android.kt` checks (`UnknownHostException`, `ConnectException`, `SocketException`; `SocketTimeoutException`; the network module's two exception types). `:core:ai` must NOT be added for desktop
- [X] T016 [P] `core/data/src/desktopMain/kotlin/PKG/core/data/translation/UnsupportedDescriptionTranslator.kt`: implements `GameDescriptionTranslator`; `modelStatus()` returns `TranslationModelStatus.UNSUPPORTED`; `translate` returns `null`; a download reports failure immediately (FR-025)
- [X] T017 [P] `core/data/src/desktopMain/kotlin/PKG/core/data/local/WishlistCoverImageStorageImpl.kt`: implements all five `WishlistCoverImageStorage` members with `javax.imageio` decode + downscale to the existing max dimension, JPEG output in `appDataDirectory()/covers`, file name `java.util.UUID`; `persist` must **never delete its source** (it is the user's own file); `read` returns `null` for paths outside the covers directory
- [X] T018 [P] `core/data/src/desktopMain/kotlin/PKG/core/data/scheduler/InAppReleaseRefreshScheduler.kt`: implements `ReleaseRefreshScheduler` over `ReleaseRefreshRunner`; `schedulePeriodicRefresh()` = `runner.run(onlyIfDue = true)` at launch, then re-check every 1 hour while the process lives; `scheduleImmediateRefresh()` = `runner.run(onlyIfDue = false)`; failures swallowed except `CancellationException`; nothing runs while the app is closed (R12, FR-026)
- [X] T019 [P] `core/data/src/desktopMain/kotlin/PKG/core/data/notification/InertReleaseNotificationScheduler.kt`: temporary `ReleaseNotificationScheduler` whose `schedule`/`cancel` do nothing and `takeFiredReminders()` returns `emptyMap()`. **Replaced and deleted in T062**
- [X] T020 `core/data/src/desktopMain/kotlin/PKG/core/data/di/DataPlatformModule.desktop.kt`: `actual val dataPlatformModule` binding `ReleaseRefreshRunner` (as `dataPlatformModule.ios.kt` does), `InAppReleaseRefreshScheduler` as `ReleaseRefreshScheduler`, `UnsupportedDescriptionTranslator` as `GameDescriptionTranslator`, `WishlistCoverImageStorageImpl`, `InertReleaseNotificationScheduler`, and `DataStore<Preferences>` via `PreferenceDataStoreFactory.createWithPath` at `appDataDirectory()/datastore/settings.preferences_pb`. No `ReleaseNotifier` (after T010, T016–T019)
- [X] T021 `core/cloud/build.gradle.kts` + `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/di/CloudPlatformModule.desktop.kt`: temporary `actual val cloudPlatformModule` binding a `PlatformProviderAuth` stub (`provider = GOOGLE`, `signIn` throws `CloudException.Unknown("Cloud is not available yet on desktop")`, other members no-ops). GitLive stays in `commonMain` for now. **Replaced in T096**
- [X] T022 [P] `core/ui/build.gradle.kts` + `core/ui/src/desktopMain/kotlin/PKG/core/ui/component/FullScreenDialogProperties.desktop.kt` (`DialogProperties(usePlatformDefaultWidth = false)`), `component/translation/TranslationEngineBadge.desktop.kt` (draws nothing), `util/ReadableBitmaps.desktop.kt` (returns the builder unchanged)
- [X] T023 [P] `core/ui/src/desktopMain/kotlin/PKG/core/ui/util/`: **stub** actuals so the module compiles — `NotificationPermission.desktop.kt` (`canDeliver = false`, `isPermanentlyDenied = false`, `requiresRuntimePermission = false`, `request = {}`), `CoverImagePicker.desktop.kt` (launcher calls `onPicked(null)`), `TextSharer.desktop.kt` (no-op), and `component/NotificationPermissionDeniedDialog.desktop.kt` (calls `onDismiss` immediately). **Replaced in T044, T045, T078, T079**
- [X] T024 [P] `feature/search/src/desktopMain/kotlin/PKG/feature/search/components/SearchBarScrollBehaviorCompat.desktop.kt`: copy the iOS `actual`s (`resetToTop`, `barModifier`) — desktop runs the same JetBrains Material 3 alpha
- [X] T025 Move `shared/src/iosMain/kotlin/PKG/shared/ReminderDeepLinks.kt` to `shared/src/commonMain/kotlin/PKG/shared/ReminderDeepLinks.kt` unchanged (same package, still `internal`); verify iOS files still resolve it
- [X] T026 [P] `shared/src/desktopMain/kotlin/PKG/shared/RunBlocking.desktop.kt`: `internal actual fun <T> runBlockingCompat(block) = kotlinx.coroutines.runBlocking(block = block)`
- [X] T027 `shared/build.gradle.kts` + `shared/src/desktopMain/kotlin/PKG/shared/QuestLogDesktop.kt`: add `desktopMain` dependencies (`libs.ktor.client.okhttp`, `libs.jetbrains.compose.ui`, `libs.kotlinx.coroutines.swing`) and `desktopTest` dependencies (`libs.koin.test`, `libs.koin.test.junit4`); add `fun startQuestLogDesktop(isDebugBuild: Boolean): Koin` that calls `initKoin(isDebugBuild)` once (idempotent via `KoinPlatform.getKoinOrNull()`), schedules `ReleaseRefreshScheduler.schedulePeriodicRefresh()` and returns the Koin instance. KDoc states what `main` does around it (after T020, T021)

### The desktop shell

- [X] T028 Create `desktopApp/build.gradle.kts` (plugins `org.jetbrains.kotlin.jvm`, `org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`; hand-written, like `:core:ai`; JVM toolchain 21; Kotlin `jvmTarget` 17) with dependencies `project(":shared")`, `project(":core:common")`, `compose.desktop.currentOs`, `libs.kotlinx.coroutines.swing`, `libs.jna`, `libs.jna.platform`; `compose.desktop.application { mainClass = "com.nikolasguillen.questlog.desktop.MainKt"; jvmArgs += "-Dquestlog.version=1.0.0" }`. Add `testImplementation(libs.junit)` and `testImplementation(libs.kotlinx.coroutines.test)` (run by the root `test` task, T005). It must NOT depend on `:core:cloud`, `:core:data`, `:core:network` or `:core:database` (constitution Principle I)
- [X] T029 `desktopApp/src/main/kotlin/PKG/desktop/Main.kt`: minimal `main()`: `startQuestLogDesktop(isDebugBuild = true)`; `application { Window(onCloseRequest = ::exitApplication, title = "QuestLog") { QuestLogRoot(pendingDeepLinkGameId = null, onDeepLinkConsumed = {}) } }`. The window title is the brand name only, so it needs no string resource (after T027, T028)
- [X] T030 [P] `shared/src/desktopTest/kotlin/PKG/shared/di/DesktopKoinGraphTest.kt`: verifies every definition of `allModules(isDebugBuild = false)` is resolvable on the desktop graph, mirroring `shared/src/androidHostTest/.../KoinGraphTest.kt` (declare as runtime-supplied only what that test declares and applies to desktop; the Swift/Android-only types are not needed)
- [X] T031 Compile gate: run `./gradlew compileKotlinDesktop --console=plain -q` (every module) and `./gradlew :desktopApp:compileKotlin`; fix per-module desktop dependency gaps (a library without a JVM variant, a missing `desktopMain` dependency) in the owning `build.gradle.kts`. Record any library found without a JVM variant in `research.md` R1
- [X] T032 Regression gate: `./gradlew :androidApp:assembleDebug`, `./gradlew test` (now includes `desktopTest`), and on macOS `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`. Everything green before continuing
- [ ] T033 Smoke: `./gradlew :desktopApp:run` (Windows: `.\gradlew.bat :desktopApp:run`) opens a window on the welcome flow and Search loads against IGDB. Fix desktop-only rendering crashes (Haze blur, adaptive navigation suite) here

### Gate spikes (throwaway; decide whether the plan holds — plan Build Order step 1)

- [ ] T034 **Spike 1a (Windows, blocks US2/US3 on Windows)**: in a scratch file under `desktopApp/src/main/kotlin/PKG/desktop/spike/` (deleted afterwards), with JNA: register `HKCU\Software\Classes\AppUserModelId\com.nikolasguillen.questlog`, schedule a `ScheduledToastNotification` 1 minute out, quit the app, confirm the toast shows. Then register an `INotificationActivationCallback` COM class object + `HKCU\…\CLSID\{…}\LocalServer32` pointing at the `runDistributable` launcher, click the toast with the app closed, and confirm a fresh process receives `Activate` with `gameId=<id>`. Write the outcome into `research.md` R8. **If the closed-app click fails, STOP and ask the owner about the C# helper fallback** (it adds the .NET SDK to Windows builds)
- [ ] T035 [P] **Spike 1b (macOS)**: a minimal `swiftc`-built dylib scheduling a `UNCalendarNotificationTrigger` request and installing a `UNUserNotificationCenter` delegate, loaded by JNA from a `runDistributable` bundle; confirm schedule + click callback. Record in `research.md` R9. Failure here does not block the feature (macOS reminders are not accepted without signing); record it and continue
- [ ] T036 [P] **Spike 1c (cloud)**: with the Desktop OAuth client keys in `local.properties`, run the PKCE + loopback Google flow in a scratch file and call `accounts:signInWithIdp` with the returned ID token; confirm a Firebase `idToken` and `localId`. Record in `research.md` R3. If Firebase refuses the ID token, try passing the access token (plan Risks) before escalating

**Checkpoint**: Foundation ready. Desktop runs locally; Android/iOS untouched; spikes answered. Stories can start.

---

## Phase 3: User Story 1 — Use QuestLog as a desktop app (Priority: P1) 🎯 MVP

**Goal**: Every screen and flow works on a Windows/macOS window with a mouse and keyboard; one instance; quit on close;
data persists; translation and background-refresh wording absent.

**Independent Test**: quickstart §5 rows 1–9.

### Tests for User Story 1

- [X] T037 [P] [US1] `core/data/src/desktopTest/kotlin/PKG/core/data/scheduler/InAppReleaseRefreshSchedulerTest.kt`: with a fake runner/clock — launch run is `onlyIfDue = true`; the hourly recheck repeats while the scope lives and stops when it is cancelled; `scheduleImmediateRefresh` runs with `onlyIfDue = false`; a failing run does not stop the loop
- [X] T038 [P] [US1] `core/data/src/desktopTest/kotlin/PKG/core/data/translation/UnsupportedDescriptionTranslatorTest.kt`: status is `UNSUPPORTED`, translate returns `null`
- [X] T039 [P] [US1] `desktopApp/src/test/kotlin/PKG/desktop/SingleInstanceMessageTest.kt`: `FOCUS` parses; `OPEN_GAME 123` parses to id `123`; `OPEN_GAME 0`, `OPEN_GAME -5`, `OPEN_GAME abc`, empty and unknown lines are ignored (the id MUST be "a positive integer")

### Implementation for User Story 1

- [X] T040 [P] [US1] `desktopApp/src/main/kotlin/PKG/desktop/SingleInstanceMessage.kt`: sealed `Focus` / `OpenGame(gameId: Int)` with a `parse(line: String): SingleInstanceMessage?` per data-model §6 (one UTF-8 line; anything else → `null`)
- [X] T041 [US1] `desktopApp/src/main/kotlin/PKG/desktop/SingleInstance.kt`: first process takes an exclusive `FileChannel.tryLock()` on `appDataDirectory()/instance.lock` and listens on a Unix-domain `ServerSocketChannel` bound to `instance.sock` (delete a stale socket file first); a second process fails the lock, connects, sends `FOCUS` or `OPEN_GAME <id>`, calls `AllowSetForegroundWindow(ASFW_ANY)` via JNA on Windows, and exits. Exposes `val messages: Flow<SingleInstanceMessage>` for the running instance (R11, FR-006)
- [X] T042 [P] [US1] `desktopApp/src/main/kotlin/PKG/desktop/DesktopBackKeys.kt`: a modifier/handler mapping **Escape** and **mouse button 4** to the back event the navigation-event dispatcher already handles (R16); Escape is not consumed while a text field has a selection or a menu/dialog is open
- [X] T043 [US1] Rewrite `desktopApp/src/main/kotlin/PKG/desktop/Main.kt`: order is (1) `desktopNotifications().setClickHandler(…)` (wired in T074; a no-op until then, and the Windows `AumidRegistration` of T067 also belongs to this step, before the handler); (2) the single-instance check — hand over and `exitProcess(0)` if another instance runs; (3) `startQuestLogDesktop`; (4) `application { Window(title = "QuestLog", state = rememberWindowState(size = DpSize(1100.dp, 760.dp)), onCloseRequest = ::exitApplication) { window.minimumSize = Dimension(400, 640); … } }`; collect `SingleInstance.messages` to `window.isMinimized = false; window.toFront(); window.requestFocus()` and, for `OpenGame`, `ReminderDeepLinks.emit(id)`; wrap content in `DesktopBackKeys`; feed `ReminderDeepLinks.pending` / `consume` into `QuestLogRoot` like `MainViewController.kt` does. Closing the window MUST quit completely (no tray, no Dock-only state — FR-006a). Window size/position are not persisted. `ReminderDeepLinks` is `internal` to `:shared`: add a public `fun emitReminderDeepLink(gameId: Int)` and `val pendingReminderDeepLink` accessors in `shared/src/desktopMain/kotlin/PKG/shared/QuestLogDesktop.kt` (after T040–T042)
- [X] T044 [P] [US1] `core/ui/src/desktopMain/kotlin/PKG/core/ui/util/CoverImagePicker.desktop.kt`: replace the stub with `java.awt.FileDialog(null as Frame?, "", FileDialog.LOAD)` filtered to JPEG/PNG/GIF/BMP via `filenameFilter`; returns the picked file's absolute path as `source`, `null` when cancelled; shown off the composition thread (R13)
- [X] T045 [US1] Change the `TextSharer` contract in `core/ui/src/commonMain/kotlin/PKG/core/ui/util/TextSharer.kt` to `class TextSharer(val copiesToClipboard: Boolean = false, private val onShare: (text: String) -> Unit)`; leave the Android/iOS `actual`s compiling unchanged (default `false`); replace the desktop stub in `TextSharer.desktop.kt` with a clipboard copy (`Toolkit.getDefaultToolkit().systemClipboard`, `StringSelection`) and `copiesToClipboard = true`
- [X] T046 [US1] Add string `share_copied_to_clipboard` ("Copied to clipboard") to `core/ui/src/commonMain/composeResources/values/strings.xml`; in `feature/game-detail/src/commonMain/kotlin/PKG/feature/gamedetail/GameDetailScreen.kt`, after `textSharer.share(effect.text.resolve())`, show the snackbar through the existing `SnackbarHostState` when `textSharer.copiesToClipboard` is true. Import the string aliased `CoreUiRes` per the project rule (after T045)
- [X] T047 [US1] Audit for FR-025–027/SC-011: search every `strings.xml` and composable for translation actions, badges, download rows and "background" wording reachable on desktop; confirm Settings hides the translation row and game detail hides the translate action when status is `UNSUPPORTED`. Fix only genuine gaps; list findings in the commit message. (R12 found no "background" string in the app)
- [ ] T048 [US1] Verify US1: `./gradlew :desktopApp:run`, then quickstart §5 rows 1–9 on Windows (and macOS when at hand: rows 1–9 only). Run T032's regression gate again

**Checkpoint**: User Story 1 works alone — a complete local desktop app.

---

## Phase 4: User Story 2 — Release reminders arrive, even when the app is closed (Priority: P1)

**Goal**: An opted-in saved game's reminder is delivered by the OS on its release day at 09:00 with the app closed.

**Independent Test**: quickstart §5 rows 10–14, 26, 29–31 (Windows required; macOS built, not accepted).

### Tests for User Story 2

- [ ] T049 [P] [US2] `core/common/src/desktopTest/kotlin/PKG/core/common/desktop/windows/ToastXmlTest.kt`: title and body are XML-escaped (`&`, `<`, `>`, `"`, `'`); output has `launch="gameId=123"` and `activationType="foreground"`; toast id is `"g" + gameId` and a 10-digit id stays ≤ 16 characters; the WinRT `DateTime` for an `Instant` is 100 ns ticks since 1601-01-01 UTC (check a known instant)
- [ ] T050 [P] [US2] `core/data/src/desktopTest/kotlin/PKG/core/data/notification/DesktopLocalNotificationCenterTest.kt`: against a fake `DesktopNotifications` — `add` returns `false` when status is not `AUTHORIZED`; `fireAt == null` maps to an immediate request; `pendingReminders()` maps only ids the app owns; `remove` forwards ids
- [ ] T051 [P] [US2] Update `core/data/src/androidHostTest/kotlin/PKG/core/data/notification/` coordinator tests (find with `grep -rl ReleaseReminderCoordinator core/data/src/androidHostTest`) and add cases: `beforeFirstSync` runs once, before the launch sync; a throwing `beforeFirstSync` is swallowed and the sync still runs; the default (no-op) keeps the iOS behaviour

### Implementation for User Story 2

- [ ] T052 [P] [US2] In `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/`, one type per file: `DesktopNotificationStatus.kt` (`NOT_DETERMINED, AUTHORIZED, DENIED, UNSUPPORTED`), `DesktopNotificationRequest.kt` (`gameId: Int, title: String, body: String, fireAt: LocalDateTime?`), `DesktopPendingNotification.kt` (`gameId: Int, fireAt: Instant?`), `DesktopNotifications.kt` (the interface in `contracts/desktop-platform-contracts.md`: `status`, `requestAuthorization`, `schedule`, `pending`, `remove`, `setClickHandler`; nothing throws except `CancellationException`; every call off the main thread)
- [ ] T053 [P] [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/OpenSystemNotificationSettings.kt`: `fun openSystemNotificationSettings()` — Windows `ms-settings:notifications` via `Desktop.browse`/`rundll32 url.dll,FileProtocolHandler`; macOS `open "x-apple.systempreferences:com.apple.Notifications-Settings.extension"`
- [ ] T054 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/WinRt.kt`: JNA mappings for `combase.dll` (`RoInitialize(RO_INIT_MULTITHREADED)`, `RoGetActivationFactory`, `RoActivateInstance`, `WindowsCreateString`, `WindowsDeleteString`, `WindowsGetStringRawBuffer`), `ole32`/`shell32` (`SetCurrentProcessExplicitAppUserModelID`) and a small `ComPtr` helper that calls vtable slots (QueryInterface/AddRef/Release + indexed calls) with `HRESULT` checking. All WinRT work runs on one dedicated MTA thread (single-thread dispatcher)
- [ ] T055 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/ToastXml.kt`: builds the toast XML from the contract (`<toast launch="gameId=…" activationType="foreground"><visual><binding template="ToastGeneric"><text>title</text><text>body</text></binding></visual></toast>`), escapes text, formats the scheduled-toast id `g<gameId>`, and converts `Instant` → WinRT `DateTime` ticks (after T054's types if shared)
- [ ] T056 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/AumidRegistration.kt`: idempotent at every launch — write `HKCU\Software\Classes\AppUserModelId\com.nikolasguillen.questlog` values `DisplayName` = `QuestLog` and `IconUri` (absolute path of `notification.png` from the app resources) with `Advapi32Util`/`WinReg`; call `SetCurrentProcessExplicitAppUserModelID("com.nikolasguillen.questlog")`. (The `CustomActivator` value is added in T071)
- [ ] T057 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/ToastNotifierBinding.kt`: typed wrappers over `Windows.UI.Notifications` — `ToastNotificationManager.CreateToastNotifierWithId(aumid)`, `XmlDocument.LoadXml`, `ScheduledToastNotification` (`put_Id`, `get_Id`, `get_DeliveryTime`), `AddToSchedule`, `RemoveFromSchedule`, `GetScheduledToastNotifications` (iterate the `IVectorView`), `ToastNotification` + `Show`, `get_Setting` (`NotificationSetting`)
- [ ] T058 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/WindowsToastNotifications.kt`: `DesktopNotifications` over T055–T057. `status()` maps `Enabled`→`AUTHORIZED`; `DisabledForApplication`, `DisabledForUser`, `DisabledByGroupPolicy`, `DisabledByManifest`→`DENIED`; a failed WinRT call→`UNSUPPORTED`. `requestAuthorization()` returns `status() == AUTHORIZED` (Windows has no prompt). `schedule` refuses (`false`) unless `AUTHORIZED`; converts the floating `LocalDateTime` with `TimeZone.currentSystemDefault()`; `fireAt == null` uses `Show` (not pending). `pending()` keeps scheduled toasts whose id parses as `g<int>`. `remove` calls `RemoveFromSchedule` for each match. Catches every exception except `CancellationException`
- [ ] T059 [P] [US2] `desktopApp/native/macos/QuestLogNotifications.swift` (+ `desktopApp/native/macos/README.md` stating how it is built): the `@_cdecl` ABI exactly as in `contracts/desktop-platform-contracts.md` — `ql_authorization_status` (`0` notDetermined, `1` authorized, `2` denied, `-1` no bundle identifier), `ql_request_authorization`, `ql_add_calendar` (floating `UNCalendarNotificationTrigger` for y/m/d/h/min, identifier `release-reminder-<id>`, `userInfo["gameId"]` as `NSNumber`), `ql_add_immediate` (1 s interval trigger), `ql_pending` (`NaN` seconds for immediate), `ql_remove`, `ql_set_click_handler` (installs the `UNUserNotificationCenter` delegate). Every callback fires exactly once; without a bundle identifier all calls are no-ops
- [ ] T060 [US2] In `desktopApp/build.gradle.kts` add the macOS-only `buildMacNotifications` `Exec` task (`swiftc -emit-library -o desktopApp/resources/macos-arm64/libQuestLogNotifications.dylib desktopApp/native/macos/QuestLogNotifications.swift -framework UserNotifications -framework Foundation`, enabled only when `os.name` is Mac) and wire it before `run`, `runDistributable`, `packageDmg`; declare `desktopApp/resources` as the compose `appResourcesRootDir`
- [ ] T061 [US2] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/macos/MacUserNotifications.kt` and `DesktopNotificationsFactory.kt`: `MacUserNotifications` loads the dylib with JNA (`Native.load`, from the app resources dir via `compose.application.resources.dir` system property), maps statuses per data-model §2 (`-1` or load failure → `UNSUPPORTED`), bridges each callback with `suspendCancellableCoroutine` (callbacks are called exactly once, on any thread), and builds the `release-reminder-<gameId>` scheme. `fun desktopNotifications(): DesktopNotifications` picks `WindowsToastNotifications`, `MacUserNotifications`, or an always-`UNSUPPORTED` fallback from `os.name`, once per process
- [ ] T062 [US2] `core/data/src/desktopMain/kotlin/PKG/core/data/notification/DesktopLocalNotificationCenter.kt`: `internal class DesktopLocalNotificationCenter(private val notifications: DesktopNotifications) : LocalNotificationCenter` mapping `LocalReminderRequest`/`PendingReminder` per data-model §3 (`add` false if not `AUTHORIZED` or the call fails). Then in `DataPlatformModule.desktop.kt` replace the inert scheduler: bind `DesktopNotifications` (`desktopNotifications()`), `LocalNotificationCenter`, `ArmedReminderLedger(get())`, `ReleaseNotificationScheduler = LocalNotificationReleaseScheduler(center, text, ledger, Clock.System)` and `ReleaseReminderCoordinator` (as `dataPlatformModule.ios.kt` does, plus `beforeFirstSync` from T064). **Delete `InertReleaseNotificationScheduler.kt`** (T019)
- [ ] T063 [US2] `core/data/src/commonMain/kotlin/PKG/core/data/notification/ReleaseReminderCoordinator.kt`: add constructor parameter `beforeFirstSync: suspend () -> Unit = {}`; in `start()`, run it once (exceptions except `CancellationException` swallowed) before the first full sync is consumed. Update the class KDoc. The iOS module keeps the default
- [ ] T064 [US2] Orphan sweep in `core/data/src/desktopMain/kotlin/PKG/core/data/notification/OrphanReminderSweep.kt`: `suspend fun sweepOrphanReminders(center: LocalNotificationCenter, ledger: ArmedReminderLedger)` removes every pending reminder whose game id is not in the ledger (R15, FR "no stale notification after reinstall"); pass it as `beforeFirstSync` in T062. Add `OrphanReminderSweepTest.kt` in `core/data/src/desktopTest/kotlin/PKG/core/data/notification/` (pending ids {1,2,3}, ledger {2} → removes {1,3})
- [ ] T065 [P] [US2] `core/ui/src/desktopMain/kotlin/PKG/core/ui/util/DesktopNotificationAuthorization.kt`: process-wide `object` with `val status: StateFlow<DesktopNotificationStatus>`, `suspend fun refresh()` (reads `desktopNotifications().status()`) and `suspend fun request(): Boolean`, the desktop counterpart of iOS's `NotificationAuthorization`. Public, because `:shared` observes it
- [ ] T066 [US2] `shared/src/desktopMain/kotlin/PKG/shared/DesktopReminderSyncTriggers.kt`: `ReminderSyncTriggers` whose `events` merge (a) the window gaining focus — refresh `DesktopNotificationAuthorization` first, then emit — and (b) the status moving to `AUTHORIZED` (copy the shape of `IosReminderSyncTriggers.kt`). The focus signal is a `SharedFlow<Unit>` object `DesktopWindowFocus` in the same module that `Main.kt` feeds from `LocalWindowInfo`/`WindowFocusListener`
- [ ] T067 [US2] In `QuestLogDesktop.kt` bind `ReminderSyncTriggers` (T066) in an extra Koin module passed to `initKoin`, then after `schedulePeriodicRefresh()` call `NotificationAuthorization`-equivalent `DesktopNotificationAuthorization.refresh()` and `ReleaseReminderCoordinator.start()`. In `desktopApp/.../Main.kt` call `AumidRegistration` (Windows) as part of step (1) of `main` in T043, via `desktopNotifications()` initialisation and before `setClickHandler`, and forward window focus to `DesktopWindowFocus`
- [ ] T068 [US2] Verify US2: run quickstart §5 rows 10–14, 26, 29–31 and 33–35 on Windows from `./gradlew :desktopApp:runDistributable`; on macOS try row 32 from `runDistributable` and record the result. Write the recorded outcomes (late delivery after sleep, time-zone re-arm, uninstall) into `research.md` R14. Run T032's regression gate

**Checkpoint**: US1 and US2 both work — reminders fire with the app closed on Windows.

---

## Phase 5: User Story 3 — Tapping a reminder opens the game (Priority: P2)

**Goal**: Clicking a delivered notification brings the single window forward on that game's detail screen, launching the
app first when it is closed.

**Independent Test**: quickstart §5 rows 15–17.

- [ ] T069 [P] [US3] `core/common/src/desktopTest/kotlin/PKG/core/common/desktop/windows/ActivationArgumentsTest.kt`: parse `gameId=123` → `123`; reject `gameId=`, `gameId=abc`, `gameId=-1`, empty and unrelated strings (returns `null`)
- [ ] T070 [US3] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/ActivationArguments.kt`: `fun parseGameId(arguments: String): Int?` for the toast `launch` payload `gameId=<id>` (positive integer only)
- [ ] T071 [US3] Extend `AumidRegistration.kt` (T056): write `CustomActivator` = the activator CLSID under the AUMID key, and `HKCU\Software\Classes\CLSID\{CLSID}\LocalServer32` = the installed launcher path from the `jpackage.app-path` system property followed by ` -ToastActivated` (skip this part, with a log line, when `jpackage.app-path` is unset: a plain `run` cannot be relaunched by Windows)
- [ ] T072 [US3] `core/common/src/desktopMain/kotlin/PKG/core/common/desktop/windows/NotificationActivator.kt`: a JNA-implemented COM class factory + `INotificationActivationCallback` (`Activate(appUserModelId, invokedArgs, data, count)`), registered with `CoRegisterClassObject(CLSID, factory, CLSCTX_LOCAL_SERVER, REGCLS_MULTIPLEUSE)`; `Activate` parses the args with T070 and invokes the click handler. Revoke with `CoRevokeClassObject` on exit. Built on the approach proven in T034
- [ ] T073 [US3] Wire the click handler in `WindowsToastNotifications.setClickHandler` (start `NotificationActivator`) and in `MacUserNotifications.setClickHandler` (`ql_set_click_handler`), each calling `onClick(gameId)` on any thread
- [ ] T074 [US3] In `desktopApp/.../Main.kt`: in step (1) of `main` from T043 (before the single-instance check, so a click that launched the process is not missed), call `desktopNotifications().setClickHandler { id -> ReminderDeepLinks emit via emitReminderDeepLink(id); bring the window forward }` for a click that launched the process (`-ToastActivated` on Windows, the delegate on macOS); a second process started by Windows with `-ToastActivated` while a first instance exists must still hand over `OPEN_GAME <id>` through `SingleInstance` (T041) and exit; the bring-forward code is shared with the `SingleInstance.messages` collector from T043 (`isMinimized = false`, `toFront()`, `requestFocus()`)
- [ ] T075 [US3] Confirm US3 #3 (game unsaved since): follow the `ReminderDeepLinks` → `QuestLogRoot` → `GameDetailRoute` path with a game id that is not in the database and not on IGDB; the app must open normally and show the detail screen's existing error/empty state, never crash or show a blank window. Fix only if it fails and note the fix in the commit message
- [ ] T076 [US3] Verify US3: quickstart §5 rows 15–17 from the **installed copy** or `runDistributable` on Windows (closed-app click needs `jpackage.app-path`); macOS rows recorded under row 32

**Checkpoint**: US1–US3 work; notification clicks open the game.

---

## Phase 6: User Story 4 — Manage reminders and fix permission from Settings (Priority: P2)

**Goal**: The Notifications group and the welcome flow show the OS permission state and take the user to the system
settings when blocked.

**Independent Test**: quickstart §5 rows 18–21.

- [ ] T077 [P] [US4] `core/ui/src/desktopTest/kotlin/PKG/core/ui/util/DesktopPermissionMappingTest.kt`: a pure function `DesktopNotificationStatus.toPermissionState(isWindows: Boolean)` returns exactly the table in data-model §2 — Windows: `canDeliver = AUTHORIZED`, `isPermanentlyDenied = !AUTHORIZED`, `requiresRuntimePermission = false`; macOS: `canDeliver = AUTHORIZED`, `isPermanentlyDenied = DENIED or UNSUPPORTED`, `requiresRuntimePermission = true`
- [ ] T078 [US4] Replace the stub `core/ui/src/desktopMain/kotlin/PKG/core/ui/util/NotificationPermission.desktop.kt`: `actual rememberNotificationPermissionState(onResult)` collects `DesktopNotificationAuthorization.status`, maps it with the T077 function, refreshes at composition start and when the window regains focus, and `request()`: on macOS it prompts while `NOT_DETERMINED` (then `onResult(granted)`) and otherwise calls `openSystemNotificationSettings()`. On Windows there is no prompt, and `rememberNotificationOptInRequest` (`core/ui/.../util/NotificationOptInPermission.kt`) only shows the denied dialog when `canDeliver`/`isPermanentlyDenied` change after a request, so `request()` MUST NOT open Settings directly: it calls `onResult(false)` and makes the Windows state report `isPermanentlyDenied = true` while blocked, so that `NotificationPermissionDeniedDialog` appears and its confirm button opens Windows Settings. Read that helper first and choose the smallest change; if it needs a one-line edit to the shared helper, say so in the commit message
- [ ] T079 [US4] Replace the stub `core/ui/src/desktopMain/kotlin/PKG/core/ui/component/NotificationPermissionDeniedDialog.desktop.kt`: a `CustomAlertDialog` (never Material's `AlertDialog`) reusing the strings the Android `actual` uses (read `NotificationPermissionDeniedDialog.android.kt` first and import the same resources); the confirm button calls `openSystemNotificationSettings()` then `onDismiss()`. No new string unless the Android one has no equivalent (then add it to `core/ui` `strings.xml` — Windows and macOS name the settings differently, so keep the wording generic)
- [ ] T080 [US4] Audit US4 acceptance against existing feature code without changing it unless broken: `feature/settings` Notifications group lists every saved game with a toggle that does not remove the row (US4 #1), shows the blocked state (US4 #3), and `feature/onboarding/.../mapper/OnboardingPageUiMapper.kt` includes the reminders page iff `requiresRuntimePermission && !canDeliver` (so macOS shows it while undecided, Windows skips it when enabled) with no "unavailable" page anywhere (US4 #5, FR-022). Confirm re-granting resumes reminders without re-opting in via the T066 trigger (US4 #4). Also walk the game-detail and Radar opt-in path (`rememberNotificationOptInRequest`) with notifications blocked on Windows and confirm the denied dialog appears exactly once per attempt
- [ ] T081 [US4] Verify US4: quickstart §5 rows 18–21 and 36 on Windows (macOS row 20 from `runDistributable`). Run T032's regression gate

**Checkpoint**: Reminders are manageable and recoverable from Settings.

---

## Phase 7: User Story 5 — Sign in and share data with the phone (Priority: P2)

**Goal**: Google sign-in, backup and restore on desktop, interchangeable with the phones' backups.

**Independent Test**: quickstart §5 rows 22–25.

### Step 1 — move GitLive off desktop (mobile must stay green)

- [X] T082 [US5] `core/cloud/build.gradle.kts`: define a `mobileMain` intermediate source set (`dependsOn(commonMain)`; `androidMain` and `iosMain` depend on it) and move the GitLive dependencies (`firebase-auth`, `firebase-firestore`) from `commonMain` to `mobileMain`; keep `koin`, coroutines, serialization in `commonMain`. Use `applyDefaultHierarchyTemplate` carefully so the default template's `iosMain` is kept
- [X] T083 [US5] Move `FirebaseCloudAuth.kt`, `FirestoreBackupStore.kt` and `error/FirebaseErrorClassifier.kt` from `core/cloud/src/commonMain/kotlin/PKG/core/cloud/` to `core/cloud/src/mobileMain/kotlin/PKG/core/cloud/` (same packages), and replace `cloudModule` in `di/CloudKoin.kt` by `internal val firebaseCloudModule` in `core/cloud/src/mobileMain/…/di/FirebaseCloudModule.kt`. Keep `AppleGrant.kt`, models, `CloudAuth`, `CloudBackupStore`, `PlatformProviderAuth`, `CloudException` and constants in `commonMain`. Move the JVM_17 `compilerOptions` comment with them if still needed. In the same commit, keep the desktop graph resolvable (this task removes `cloudModule`, which was the only desktop binding of `CloudAuth` and `CloudBackupStore`): add `UnavailableCloudAuth` and `UnavailableCloudBackupStore` (one class per file) in `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/`. `UnavailableCloudAuth`: `provider = GOOGLE`, `currentUser = flowOf(null)`, `signIn` and `reauthenticate` throw `CloudException.Unknown`, `signOut` and `deleteUser` are no-ops. `UnavailableCloudBackupStore`: every member throws `CloudException.Unauthenticated`. Bind both in `CloudPlatformModule.desktop.kt`. Both are deleted in T096
- [X] T084 [US5] Bind `firebaseCloudModule` from the Android and iOS `cloudPlatformModule` actuals (`CloudPlatformModule.android.kt`, `CloudPlatformModule.ios.kt`: `includes(firebaseCloudModule)`); remove `cloudModule` from `allModules` and its import in `shared/src/commonMain/kotlin/PKG/shared/di/SharedKoin.kt`. Move any `androidHostTest` that referenced moved classes only if the source set requires it
- [X] T085 [US5] Checkpoint: `./gradlew :core:cloud:compileAndroidMain`, `./gradlew test`, `./gradlew :androidApp:assembleDebug`; on macOS `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`. Mobile behaviour must be unchanged. Desktop still compiles and `DesktopKoinGraphTest` (T030) stays green because of the `Unavailable*` bindings added in T083; also run `./gradlew :desktopApp:compileKotlin`

### Step 2 — the REST client

- [X] T086 [US5] `core/cloud/build.gradle.kts`: add `alias(libs.plugins.buildconfig)` generating `internal object DesktopCloudCredentials` in `desktopMain` (package `com.nikolasguillen.questlog.core.cloud`) from `local.properties` keys `FIREBASE_WEB_API_KEY`, `FIREBASE_PROJECT_ID`, `GOOGLE_DESKTOP_CLIENT_ID`, `GOOGLE_DESKTOP_CLIENT_SECRET` (default `""`; same `Properties` loading as `core/network/build.gradle.kts`); add `desktopMain` dependencies `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-client-core`, `ktor-serialization-kotlinx-json`; `desktopTest` adds `ktor-client-mock`. Missing keys must not fail the build (R6)
- [X] T087 [P] [US5] `core/cloud/src/desktopTest/kotlin/PKG/core/cloud/FirestoreRestCodecTest.kt` + `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/FirestoreRestCodec.kt`: pure encode/decode of `BackupManifestDocument`, `BackupStreamDocument` and `BackupPartDocument` to Firestore REST `fields` per `contracts/firestore-rest.md` (`stringValue`; `integerValue` as a decimal **string**; `arrayValue.values` of `mapValue.fields`). Decoding accepts `integerValue` and defensively `doubleValue`, and an empty array as `{"arrayValue": {}}`. Test a round trip and a fixture document in the shape GitLive writes (`BackupPartDocument.data` base64 up to 700,000 characters)
- [X] T088 [P] [US5] `core/cloud/src/desktopTest/kotlin/PKG/core/cloud/RestErrorMapperTest.kt` + `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/RestErrorMapper.kt`: Identity Toolkit `error.message` (text before ` : `) → `CloudException` exactly per the table in `contracts/firestore-rest.md` (`needConfirmation: true` and `FEDERATED_USER_ID_ALREADY_LINKED` → `CredentialCollision`; `CREDENTIAL_TOO_OLD_LOGIN_AGAIN` → `RecentLoginRequired`; `TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN`, `INVALID_ID_TOKEN`, `USER_DISABLED`, `USER_NOT_FOUND` → `Unauthenticated`; `INVALID_IDP_RESPONSE`, `INVALID_API_KEY`, anything else → `Unknown`); HTTP 401/403 from Firestore → `Unauthenticated`; other non-2xx → `Unknown`; `java.net` transport failures → `Connectivity`; calls over `CLOUD_CALL_TIMEOUT` → `Timeout`; `CancellationException` always rethrown first
- [X] T089 [P] [US5] `core/cloud/src/desktopTest/kotlin/PKG/core/cloud/GoogleInstalledAppSignInTest.kt` + `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/GoogleInstalledAppSignIn.kt`: PKCE (`code_verifier` 43–128 unreserved characters, `code_challenge = BASE64URL(SHA256(verifier))` without padding), auth-URL builder (`scope=openid email profile`, `code_challenge_method=S256`, `prompt=select_account`), loopback `com.sun.net.httpserver.HttpServer` on `127.0.0.1` with an ephemeral port, redirect parsing (mismatched `state` → `Unknown`; `error=access_denied` → `SignInCancelled`), the "You can return to QuestLog" HTML response, **5-minute** wait then `SignInCancelled`, server stopped on cancellation, `onAccountPicked` called when the redirect arrives, and the token exchange (`oauth2.googleapis.com/token`, form fields from the contract). Opens the browser with `Desktop.browse`. Test everything except the real browser/network with `ktor-client-mock` and a fake browser launcher
- [X] T090 [P] [US5] `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/DesktopCloudSessionStore.kt` + `DesktopCloudSession.kt` (one class per file) + `core/cloud/src/desktopTest/.../DesktopCloudSessionStoreTest.kt`: persist `uid`, `email`, `providers`, `refreshToken` as JSON in `appDataDirectory()/cloud-session.json`; write atomically (temp file in the same directory, then `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`); on macOS set `rw-------` (`PosixFilePermissions`); delete on clear; corrupt or missing file → signed out, never a crash; the refresh token is never logged. `:core:cloud` takes `appDataDirectory()` through a constructor `Path` parameter so tests use a temp directory (it must not depend on `:core:common` internals)
- [X] T091 [US5] `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/IdentityToolkitClient.kt`: Ktor client for `accounts:signInWithIdp` (`requestUri=http://localhost`, `postBody=id_token=<token>&providerId=google.com`, `returnSecureToken`, `returnIdpCredential`), `securetoken.googleapis.com/v1/token` (refresh), `accounts:lookup`, `accounts:delete`; JSON with `ignoreUnknownKeys`; errors through T088
- [X] T092 [US5] `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/RestCloudAuth.kt` implementing `CloudAuth` (`provider = CloudProvider.GOOGLE`): `currentUser` flow from the stored session at start, `null` after sign-out/delete or a refresh refused with `TOKEN_EXPIRED`/`USER_DISABLED`/`USER_NOT_FOUND`; `signIn(onAccountPicked)` = T089 flow then `signInWithIdp`, saves the session; the ID token is kept in memory and refreshed when **under 5 minutes** are left; `reauthenticate` = fresh sign-in, sets the in-memory `reauthenticated` flag; `signOut` clears the session file; `deleteUser` throws `RecentLoginRequired` unless `reauthenticated`, calls `accounts:delete`, clears the session; every member throws only `CloudException` (+ `CancellationException`). Internal `idToken()` supplier for the Firestore store
- [X] T093 [P] [US5] `core/cloud/src/desktopTest/kotlin/PKG/core/cloud/RestCloudAuthTest.kt`: with `ktor-client-mock` and a fake session store/sign-in — sign-in stores the session and emits a `CloudUser`; a refresh near expiry happens once; a refresh answered `TOKEN_EXPIRED` signs out; `deleteUser` without reauthentication throws `RecentLoginRequired`; `needConfirmation` yields `CredentialCollision`; sign-out removes the session
- [X] T094 [US5] `core/cloud/src/desktopMain/kotlin/PKG/core/cloud/RestBackupStore.kt` implementing `CloudBackupStore` against the Firestore REST endpoints in `contracts/firestore-rest.md` (`readManifest`/`readPart` GET with 404 → `null`; `writeManifest`/`writePart` PATCH with the full document and **no `updateMask`**; `listPartIds` with `pageSize=300`, `mask.fieldPaths=snapshotId`, following `nextPageToken`; `deleteParts` via `documents:commit` with at most **400** delete writes per call; `deleteManifest` DELETE), `Authorization: Bearer <idToken>`, every call bounded by `CLOUD_CALL_TIMEOUT` and failing as in `FirestoreBackupStore.call`
- [X] T095 [P] [US5] `core/cloud/src/desktopTest/kotlin/PKG/core/cloud/RestBackupStoreTest.kt`: each method's request (method, path, body fields, header), 404 → `null`, pagination of `listPartIds`, 401 → `Unauthenticated`, batching of 401 ids into 400 + 1 delete commits
- [X] T096 [US5] Replace the T021 stub: `CloudPlatformModule.desktop.kt` binds `DesktopCloudSessionStore` (path from `appDataDirectory()`; it lives in `:core:common`, which `:core:cloud` may not import — pass the directory via a Koin property set by `QuestLogDesktop.kt`, same pattern as Android's `google_web_client_id`), the Ktor client, `RestCloudAuth` as `CloudAuth` and `RestBackupStore` as `CloudBackupStore`; delete the stub `PlatformProviderAuth` and the `UnavailableCloudAuth.kt` / `UnavailableCloudBackupStore.kt` files from T083. Update `QuestLogDesktop.kt` to set the property
- [X] T097 [US5] Run `DesktopKoinGraphTest` (T030) and fix newly missing bindings; confirm `:desktopApp` still has no `:core:cloud` dependency
- [ ] T098 [US5] Verify US5: quickstart §5 rows 22–25 against the real Firebase project with the Desktop OAuth client keys set in `local.properties`; confirm a backup made on Android restores on desktop and the reverse (SC-004), and that Apple is not offered (provider is Google only, R4). If sign-in cannot be made to work, apply the FR-024 fallback: ship local-only by hiding the account entry on desktop — do not leave a dead control — and report to the owner

**Checkpoint**: Desktop shares data with the phones.

---

## Phase 8: User Story 6 — Install it like a normal desktop app (Priority: P3)

**Goal**: A `.msi` on Windows and a `.dmg` on macOS install QuestLog; one documented command runs it from the repo.

**Independent Test**: quickstart §5 rows 27–28.

- [ ] T099 [P] [US6] Create icons in `desktopApp/icons/`: `QuestLog.ico` (16–256 px), `QuestLog.icns`, `notification.png` (256 px), from the launcher artwork under `androidApp/src/main/res/` (document the source asset in a one-line comment in `desktopApp/build.gradle.kts`)
- [ ] T100 [US6] `desktopApp/build.gradle.kts` `nativeDistributions`: `targetFormats(TargetFormat.Msi, TargetFormat.Dmg)`, `packageName = "QuestLog"`, `packageVersion = "1.0.0"`, `vendor = "Nikolas Guillen"`, `windows { menu = true; shortcut = true; perUserInstall = true; upgradeUuid = "<fixed UUID, generated once and committed>"; iconFile = icons/QuestLog.ico }`, `macOS { bundleID = "com.nikolasguillen.questlog"; iconFile = icons/QuestLog.icns; infoPlist { extraKeysRawXml = "<key>LSMinimumSystemVersion</key><string>13.0</string>" } }`, `modules("java.desktop", "jdk.httpserver", …)` from `suggestRuntimeModules` output (add `java.naming`, `java.sql`, `jdk.unsupported` as that task reports), and pass `-Dquestlog.version=<packageVersion>` through `jvmArgs` so the installed copy reports its version (FR-028, FR-030a)
- [ ] T101 [US6] Bundle the macOS dylib: in `desktopApp/build.gradle.kts` configure `appResourcesRootDir = project.layout.projectDirectory.dir("resources")` and make `packageDmg`/`createDistributable` depend on `buildMacNotifications` (macOS only); ensure `MacUserNotifications` (T061) resolves the dylib from `compose.application.resources.dir`
- [ ] T102 [US6] Build and install on Windows: `.\gradlew.bat :desktopApp:packageMsi` → `desktopApp\build\compose\binaries\main\msi\QuestLog-1.0.0.msi`; install on a clean user account, launch from the Start menu, confirm the JLink runtime has every module (the app starts and signs in, and the loopback server works). Add any missing module to T100's list
- [ ] T103 [US6] Build on macOS: `./gradlew :desktopApp:packageDmg` → `desktopApp/build/compose/binaries/main/dmg/QuestLog-1.0.0.dmg`; mount, drag to Applications, launch (unsigned: right-click → Open on first run). State in the walkthrough that this is Apple silicon only (R9). If no Mac is at hand, say so in the final report instead of assuming it works
- [ ] T104 [US6] Document the one command per OS in the root `CLAUDE.md` **Commands** section: `./gradlew :desktopApp:run` (macOS) and `.\gradlew.bat :desktopApp:run` (Windows), plus `packageMsi`, `packageDmg`, `runDistributable` and the desktop test/compile commands (`compileKotlinDesktop`, `desktopTest`); also state that the desktop OAuth keys and Firebase keys belong in `local.properties`
- [ ] T105 [US6] Verify US6: quickstart §5 rows 27–29 (installed copy runs; row 10 holds from the installed copy; uninstall result recorded)

**Checkpoint**: All six stories work; installers exist.

---

## Phase 9: Polish and cross-cutting concerns (FR-031–FR-033)

**Purpose**: Documentation and governance in the same change, final regression, walkthrough record.

- [ ] T106 [P] Update `specs/010-kmp-migration/contracts/platform-contracts.md`: add a **Desktop** column to every table that has Android/iOS columns using `contracts/desktop-platform-contracts.md`; add the new contracts (`DesktopNotifications`, the Swift ABI, `TextSharer.copiesToClipboard`, `ReleaseReminderCoordinator.beforeFirstSync`, the desktop shell row, REST cloud) and the eight new rows of the capability exception register; update the "Cloud account and backup" section for `mobileMain`/`desktopMain` (FR-032, FR-033)
- [ ] T107 [P] Update the root `CLAUDE.md` to make desktop the third supported platform: the header/stack line, the module list (21 modules incl. `:desktopApp`), the module graph (`:desktopApp → :shared`, `:core:common`), "Working in a multiplatform codebase" (replace "iOS is the only platform besides Android (no desktop, no web)"), the `commonMain`/`desktopMain` rule, and the sentence about `src/androidMain` / `src/iosMain` source sets. Delete rules that this change makes false instead of annotating them (FR-033)
- [ ] T108 [P] Update `core/cloud/CLAUDE.md`: GitLive is `mobileMain` only; desktop uses the REST client; `desktopMain` imports no `dev.gitlive`/`com.google.firebase`; provider is Google only on desktop; the secrets keys of R6
- [ ] T109 [P] Update `docs/roadmap.md`: remove "iOS only" wording; record follow-ups — Apple sign-in on desktop (paid Apple account + HTTPS redirect domain), macOS signing and notarization (and Intel/universal builds), Windows uninstall cleanup of pending toasts and HKCU keys (MSI custom action, compose-multiplatform#1972), Windows code signing
- [ ] T110 [P] Check `docs/tech-debt.md` for entries made false or newly created by this change (for example the "no desktop" wording, `SearchBarScrollBehaviorCompat` now having three `actual`s); update by deleting or adding entries, never annotating
- [ ] T111 Draft the constitution amendment in `.specify/memory/constitution.md`: Version **1.3.5 → 1.4.0** (MINOR), Last Amended = the commit date; update the Platform paragraph (add the desktop stack: JNA, Ktor REST for cloud on desktop), the Kotlin Multiplatform paragraph ("Android, iOS and desktop (Windows and macOS)"), `commonMain`/`desktopMain` rule, and the Principle I `:core:cloud` line (GitLive is `mobileMain` only). **Show the diff to the owner and commit it only after their approval** (Governance)
- [ ] T112 Run the full automated gate once more: `./gradlew :androidApp:assembleDebug`, `./gradlew test`, `./gradlew :desktopApp:compileKotlin`, and on macOS `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the xcodebuild command from `CLAUDE.md`. All green (SC-012)
- [ ] T113 Run the complete quickstart §5 walkthrough (rows 1–36) on Windows and the macOS-applicable rows on a Mac; copy every **record** result into the capability exception register in `platform-contracts.md` and `research.md` R14. State plainly in the final report which rows were not verified (macOS closed-app reminders; anything on a machine that was not at hand)
- [ ] T114 Final review against the spec: FR-001–FR-033 each have a task above or an existing behaviour that the walkthrough covers; `grep` the desktop source sets for `android.`, `Co-Authored`-style leaks and any import of `:core:cloud`/`:core:data` from `:desktopApp`; confirm Linux appears nowhere as a target (FR-030)

---

## Dependencies and execution order

### Phase dependencies

- **Phase 1 (Setup)**: none. T004 depends on nothing; T005 after T004.
- **Phase 2 (Foundational)**: after Phase 1. Internal order: T007–T010 → T011, T012; T015–T019 → T020; T025–T027; T028 → T029; T030 after T027 and T020; T031 after all actuals; T032 → T033; spikes T034–T036 after T033 (T034 gates Windows US2/US3; T036 gates US5).
- **US1 (Phase 3)**: after Phase 2. No dependency on other stories.
- **US2 (Phase 4)**: after Phase 2 and spike T034. Uses `Main.kt` from US1 (T043), so schedule after T043.
- **US3 (Phase 5)**: after US2 (needs the toast stack and `Main.kt` click wiring).
- **US4 (Phase 6)**: after US2 (T065 authorization cache); independent of US3.
- **US5 (Phase 7)**: after Phase 2 and spike T036; independent of US2–US4 and may run in parallel with them (different module: `:core:cloud`).
- **US6 (Phase 8)**: after US1; the Windows/macOS reminder checks in T102–T105 need US2 and US3 done.
- **Polish (Phase 9)**: after the stories you intend to ship. T111 waits for the owner's approval.

### Within a story

- Interfaces and pure functions (with their tests) before the OS bindings; bindings before Koin wiring; Koin wiring before `Main.kt` changes; verification last.
- Tasks that edit the same file are sequential: `Main.kt` (T029, T043, T067, T074), `DataPlatformModule.desktop.kt` (T020, T062), `QuestLogDesktop.kt` (T027, T043, T067, T096), `AumidRegistration.kt` (T056, T071), `desktopApp/build.gradle.kts` (T028, T060, T100, T101).

### Parallel opportunities

- Phase 1: T002, T003, T006.
- Phase 2: T007–T010; T012–T014; T016–T019; T022–T024; T026; T030; spikes T035, T036.
- US1: T037–T039 and T040, T042, T044.
- US2: T049–T051; T052, T053, T059, T065. Windows binding (T054–T058) and macOS binding (T059–T061) are independent tracks.
- US5 can run beside US2–US4 on a second track: T087–T090 in parallel, then T091–T096.
- Polish: T106–T110.

### Parallel example: User Story 2

```text
Track Windows:  T054 → T055 → T056 → T057 → T058
Track macOS:    T059 → T060 → T061
Shared pieces:  T052, T053, T065 (parallel) → T062 → T063 → T064 → T066 → T067
Tests (parallel anytime after their subject): T049, T050, T051
```

---

## Implementation strategy

### MVP first

US1 and US2 are both P1: the smallest shippable desktop app is **Phase 1 + Phase 2 + US1 + US2**. Stop after T068 and
validate with quickstart rows 1–14. Add US3 and US4 next (they finish the reminder experience), then US5 (cloud) and
US6 (installers) — US6 can move earlier if you want to distribute before cloud is ready.

### Incremental delivery

1. Setup + Foundational → desktop runs locally (T033).
2. US1 → usable local app. 3. US2 → reminders. 4. US3 + US4 → click-through and permission handling.
5. US5 → cloud. 6. US6 → installers. 7. Polish → documentation and the owner-approved constitution amendment.

### Rules to keep in mind while executing

- A task that touches `commonMain` is not done until Android builds and `./gradlew test` passes.
- Do not weaken or delete a test to make something pass.
- Anything only a Mac can verify is said so, never assumed. macOS closed-app reminders are built but not accepted
  (no Apple account).
- If spike T034 fails, stop and ask the owner before introducing a .NET helper.

## Notes

- `[P]` tasks touch different files and have no unfinished dependency.
- Each story is a point the work can pause on, with Android green.
- Commit after each task or logical group, using `type(scope): subject`; this repository's commits end with the
  `Co-Authored-By` trailer given in the session's attribution instructions.
