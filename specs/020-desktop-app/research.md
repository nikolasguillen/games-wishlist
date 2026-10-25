# Research: Desktop App (Windows and macOS)

Each entry records the decision, why it was taken and what else was weighed. **(owner)** marks a decision the
owner made during clarification or planning.

## R1. The desktop target: one JVM target named `desktop` in every multiplatform module

- **Decision**: `questlog.kmp.library` adds `jvm("desktop")` next to the Android and iOS targets, so every
  multiplatform module gains `desktopMain` and `desktopTest` source sets. `desktopMain` compiles to JVM 17 bytecode
  (the runtime the installers bundle is 21). Room's compiler is added for the target as `kspDesktop` in
  `:core:database`.
- **Rationale**: Compose Multiplatform's desktop runtime is the JVM, and every library the shared code uses publishes
  a JVM variant: checked against Maven for Compose 1.12.1, JetBrains Material 3 1.12.0-alpha03 and its adaptive
  navigation suite, adaptive 1.2.0, Navigation 3 (androidx runtime 1.1.7, JetBrains UI 1.1.2), navigationevent
  1.1.0, lifecycle 2.11.0 (ViewModel, runtime-compose, navigation3), Koin 4.2.2, Room 2.8.5, sqlite-bundled 2.6.2,
  DataStore 1.2.1, Coil 3.6.3, Haze 2.0.0-rc02, material-icons 1.7.3 and Ktor's OkHttp engine 3.6.0. The name
  `desktop` (not `jvm`) says what the target is for, and the source sets read `desktopMain` next to `iosMain`.
- **Alternatives considered**: putting the target only in the modules that need platform code. Rejected: a module
  without the target cannot be a dependency of one that has it, and every module ends up in the app. Sharing
  `java.*` code between `androidMain` and `desktopMain` through an intermediate `jvmCommon` source set was
  rejected too: the overlap is four short files (date rendering, connectivity classification, `runBlocking`, the
  OkHttp engine binding). One intermediate set in every module costs more in build configuration than the
  duplication costs, and the Android copies call Android APIs in places.

## R2. The desktop entry point: a new `:desktopApp` module

- **Decision**: a JVM-only module `:desktopApp` (plugins `org.jetbrains.kotlin.jvm`, `org.jetbrains.compose`,
  `org.jetbrains.kotlin.plugin.compose`) holds `main()`, the window, single-instance handling, the native
  libraries and the `compose.desktop.application { nativeDistributions { … } }` block. It depends on `:shared`
  (desktop variant) and on `:core:common`, for the desktop OS bindings (R7). Its build file is hand-written, like
  `:core:ai`'s, because it is the only module of its kind.
- **Rationale**: it mirrors `:androidApp` and `iosApp/`: `:shared` keeps the navigation, the Koin assembly and the
  root composable, and the shell only knows its platform. Packaging, icons and native library bundling belong to
  the shell, not to `:shared`.
- **Alternatives considered**: `compose.desktop.application` inside `:shared` (the shape of the KMP wizard's
  `composeApp`). Rejected: `:shared` would carry installer settings, and the Android and iOS shells already sit
  outside it. A `questlog.desktop.application` convention plugin was rejected for a single module.

## R3. Cloud account and backup on desktop: Firebase REST over Ktor **(owner)**

- **Finding**: GitLive 2.7.0 publishes `jvm` variants of `firebase-auth` and `firebase-firestore`, backed by
  GitLive's `firebase-java-sdk`. In that SDK `FirebaseAuth.signInWithCredential` is `TODO()`: only anonymous,
  custom-token and email/password sign-in exist. The app signs in with Google or Apple credentials only, so the
  current `FirebaseCloudAuth` cannot sign anyone in on desktop. Firestore through the same SDK takes its token from
  that SDK's `FirebaseAuth`, so it would run unauthenticated and the rules (`request.auth.uid == uid`) refuse it.
- **Decision (owner)**: desktop gets its own `CloudAuth` and `CloudBackupStore` in `:core:cloud` `desktopMain`,
  over Ktor:
  - **Google sign-in** is OAuth 2.0 for installed apps: the system browser opens Google's consent page with PKCE and a
    loopback redirect (`http://127.0.0.1:<random port>/`), served by the JDK's `com.sun.net.httpserver`. The
    authorization code is exchanged for a Google ID token at `oauth2.googleapis.com/token` with a **Desktop app**
    OAuth client of the Firebase project.
  - **Firebase Auth** is the Identity Toolkit REST API: `accounts:signInWithIdp` (Google ID token → Firebase ID token,
    refresh token, uid, email, `needConfirmation` for a collision), `securetoken.googleapis.com/v1/token` to refresh,
    `accounts:lookup` for the providers, and `accounts:delete`.
  - **Firestore** is the REST API (`firestore.googleapis.com/v1/projects/{id}/databases/(default)/documents/…`) with
    the Firebase ID token as the bearer, so the existing rules apply unchanged. The document shapes are in
    [contracts/firestore-rest.md](contracts/firestore-rest.md).
  - Android and iOS keep GitLive. `FirebaseCloudAuth`, `FirestoreBackupStore`, `FirebaseErrorClassifier` and the
    GitLive dependency move from `commonMain` to a new `mobileMain` source set (Android + iOS) in `:core:cloud`, so
    neither `firebase-java-sdk` nor its transitive libraries reach the desktop installer. Their bindings move from
    `cloudModule` into the two mobile `cloudPlatformModule`s, and `cloudModule` disappears.
- **Rationale**: the Firestore documents are flat (strings, integers, one list of maps), so encoding them as
  Firestore REST values is small and testable on the JVM with `ktor-client-mock`. Ktor is already the project's HTTP
  client.
- **Alternatives considered**: waiting for the Java SDK to implement `signInWithCredential` (desktop would ship
  local-only, as FR-024 allows); a custom-token server (a backend, which the spec rules out); seeding the Java SDK's
  persisted user with REST tokens (depends on that SDK's private storage format).

## R4. Sign-in providers on desktop: Google only

- **Decision**: desktop offers Google. `CloudAuth.provider` is `GOOGLE` on desktop, as on Android. The account screen
  already shows only the platform's one provider, so Apple is absent with no UI change.
- **Rationale**: Sign in with Apple on the web needs a Services ID and an HTTPS redirect on a domain registered with
  Apple. Both need a paid Apple Developer account, which the owner does not have (spec clarification), and Apple does
  not accept loopback redirects. **Reported to the owner (FR-024).** Consequence: a user who signs in only with Apple
  on iOS reaches the same account on desktop only if the Apple account's email is the same as their Google
  account's. Firebase links same-email trusted providers by itself, as it already does between Android and iOS.
- **Alternatives considered**: none that work without a paid account.

## R5. The desktop session: tokens in a per-user file

- **Decision**: the Firebase refresh token, uid, email and providers are stored in a JSON file `cloud-session.json`
  in the app data directory (R10). It is written atomically (temporary file, then a move). On macOS it is limited to
  the owner (`rw-------`); on Windows `%LOCALAPPDATA%` is already per-user. The ID token is kept in memory and
  refreshed when it has less than five minutes left. Sign-out and delete remove the file.
- **Rationale**: this is how Firebase's own SDKs keep the session (SharedPreferences on Android, IndexedDB on the web),
  so the exposure is the same as on mobile: anything running as the user can read it.
- **Alternatives considered**: Windows Credential Manager and the macOS Keychain. Two more native bindings for no
  difference against a process running as the same user. They can be added later behind the same storage class.

## R6. Desktop cloud credentials: generated from `local.properties`

- **Decision**: `FIREBASE_WEB_API_KEY`, `FIREBASE_PROJECT_ID`, `GOOGLE_DESKTOP_CLIENT_ID` and
  `GOOGLE_DESKTOP_CLIENT_SECRET` live in `local.properties` and are generated into an
  `internal object DesktopCloudCredentials` under `build/` by `core/cloud/build.gradle.kts`, with the same
  `buildconfig` plugin `:core:network` uses for IGDB. They are never committed. Google documents that an
  installed app's client secret is not confidential: PKCE is what protects the exchange.
- **Behaviour when missing**: the build still succeeds. A sign-in attempt then fails with `CloudException.Unknown`,
  which the account screen shows as its generic error. Only a developer can be in this state; quickstart §1 lists the
  keys.

## R7. Where the desktop OS bindings live: `:core:common` `desktopMain`

- **Decision**: the native layer that both `:core:ui` (the permission state) and `:core:data` (the scheduled
  notifications) need sits once in `:core:common` `desktopMain`. This is `DesktopNotifications`, implemented by
  `WindowsToastNotifications` and `MacUserNotifications` and picked once per process from `os.name`, together with
  the OS helpers (`appDataDirectory()`, `openSystemNotificationSettings()`). `:desktopApp` reaches the same
  bindings for the Windows activator and the macOS click callback. JNA 5.19.1 (`jna`, `jna-platform`) is a
  `desktopMain` dependency of `:core:common`.
- **Rationale**: on iOS both modules call `UNUserNotificationCenter`, which the platform provides to everyone. On
  desktop the equivalent "platform API" is our binding. Putting it in the module both already depend on avoids two
  copies and a new module.
- **Alternatives considered**: a new `:core:desktop` module (one more module and two more edges for a handful of
  files); one copy in `:core:ui` and one in `:core:data` (two COM/Objective-C bindings to keep in step).

## R8. Windows release reminders: scheduled toasts through WinRT, from the JVM via JNA

- **Decision**:
  - **Scheduling** uses `Windows.UI.Notifications.ToastNotifier.AddToSchedule` with a `ScheduledToastNotification`.
    Windows delivers it at its time with no app process running and keeps it across restarts. The calls are WinRT
    vtable calls made with JNA (`RoInitialize`, `WindowsCreateString`, `RoGetActivationFactory`,
    `RoActivateInstance`), so no extra toolchain is needed. The interfaces used are `IToastNotificationManagerStatics`
    (`CreateToastNotifierWithId`), `IToastNotifier` (`AddToSchedule`, `RemoveFromSchedule`,
    `GetScheduledToastNotifications`, `Show`, `get_Setting`), `IScheduledToastNotificationFactory`,
    `IScheduledToastNotification` (`Id`, `DeliveryTime`), `IToastNotificationFactory`, `IXmlDocumentIO` and the
    `IVectorView` of scheduled toasts.
  - **Identity.** The AUMID is `com.nikolasguillen.questlog`. At every launch the app writes (idempotently)
    `HKCU\Software\Classes\AppUserModelId\com.nikolasguillen.questlog` with `DisplayName` = QuestLog, `IconUri`
    (a PNG shipped in the app resources) and `CustomActivator` = the app's activator CLSID. It also calls
    `SetCurrentProcessExplicitAppUserModelID`. This is the registration Microsoft's toolkit performs for unpackaged
    apps, and it needs no Start menu shortcut property, which jpackage cannot set.
  - **Click-through** uses a COM activator. `HKCU\Software\Classes\CLSID\{CLSID}\LocalServer32` points at the installed
    launcher (`jpackage.app-path`) with `-ToastActivated`. The running app registers an `INotificationActivationCallback`
    class object (`CoRegisterClassObject`, `REGCLS_MULTIPLEUSE`, implemented with JNA callbacks). A click while the app
    runs reaches that object in the running process. A click while it is closed makes Windows start the launcher with
    `-ToastActivated`, and `main` registers the class object before anything else, so the click is delivered to the
    new process. The toast's `launch` argument is `gameId=<id>`.
  - **Toast id**: `g<gameId>` (scheduled toast ids are limited to 16 characters; an IGDB id has at most 10 digits).
  - **Time**: the floating 09:00 `LocalDateTime` is converted to an instant with the current time zone when it is
    armed. Every full sync re-arms, so the next launch follows a time zone change (FR-019, R14).
  - **Immediate delivery** (`fireAt == null`): `Show` instead of a 1 s schedule. It is then not pending, which the
    shared scheduler already reads as "fired".
  - **Permission**: Windows has no prompt. `get_Setting` returns `Enabled` → authorized. `DisabledForApplication`,
    `DisabledForUser`, `DisabledByGroupPolicy` and `DisabledByManifest` → denied. `add` refuses while it is not
    `Enabled`, as the iOS adapter does.
- **Rationale**: this is the OS's own scheduler (FR-012) with nothing resident. JNA keeps the "one documented
  command" true on a Windows machine with only a JDK.
- **Alternatives considered**:
  - A C# helper executable using `ToastNotificationManagerCompat`. Simplest activation code, but it adds the .NET SDK to
    every Windows build. **Fallback** if the JNA activator spike (T-spike in the tasks) fails; that would be an owner
    decision, because it adds a toolchain.
  - Protocol activation (`activationType="protocol"`). It needs a registered URL scheme, which the spec forbids
    (clarification 5).
  - Windows App SDK `AppNotificationManager`. It has no scheduled notifications.
- **Dev run**: scheduling and delivery work from `./gradlew :desktopApp:run`, because the AUMID is registered for any
  launcher. Click-through from a closed app needs `jpackage.app-path`, so it is verified from the installed copy
  (or `runDistributable`).

## R9. macOS release reminders: `UserNotifications` through a Swift library

- **Decision**: a small Swift dynamic library, `libQuestLogNotifications.dylib`, is built by `swiftc` in a Gradle
  `Exec` task that runs only on macOS. It is bundled as a macOS app resource and loaded with JNA. Its C entry points
  (`@_cdecl`) are: `ql_authorization_status`, `ql_request_authorization(callback)`,
  `ql_add_calendar(id, title, body, y, m, d, h, min, callback)`, `ql_add_immediate(id, title, body, callback)`,
  `ql_pending(callback)`, `ql_remove(ids, count)`, `ql_set_click_handler(callback)`. Requests use the same floating
  `UNCalendarNotificationTrigger` at 09:00 as iOS, the same identifier scheme and the same 64-request cap. The click
  handler is installed as the `UNUserNotificationCenter` delegate at the very start of `main`, before Compose starts
  AppKit.
- **Rationale**: Xcode is already required on the Mac for the iOS app, so the Swift toolchain is free. Calling
  `UserNotifications` from JNA directly would mean building Objective-C blocks by hand for every completion handler.
- **Limits**:
  - `UNUserNotificationCenter` works only inside an app bundle with a bundle identifier, so on macOS reminders work
    from `runDistributable` or the `.dmg`, never from `./gradlew :desktopApp:run`. There `DesktopNotifications` reports
    `UNSUPPORTED`, which the app treats as denied, with no crash. The dev run shows the blocked state.
  - The bundle is ad-hoc signed by jpackage and not notarized (the owner has no Apple account). Closed-app reminders on
    macOS are built but **not part of acceptance** (spec clarification 1). If they work locally on the build Mac, the
    walkthrough records it.
  - The `.dmg` contains the architecture of the Mac that builds it (Apple silicon on the owner's Mac). An Intel build
    needs an Intel Mac or a universal runtime. Not in this feature; reported to the owner.

## R10. Local data on desktop

- **Decision**: one per-user directory, `appDataDirectory()`: `%LOCALAPPDATA%\QuestLog` on Windows and
  `~/Library/Application Support/QuestLog` on macOS. The Room file (`questlog.db`, `BundledSQLiteDriver`), DataStore
  (`datastore/settings.preferences_pb`), list covers (`covers/`), the cloud session, the single-instance lock and the
  socket all live in it. The dev run and the installed copy share it, as one user's app should.
- **Rationale**: FR-007. `LOCALAPPDATA` rather than `APPDATA` (Roaming), because a database does not belong in a
  roaming profile.

## R11. Single instance and the window lifecycle

- **Decision**:
  - The first process takes an exclusive `FileChannel.tryLock()` on `instance.lock` and listens on a Unix-domain
    socket `instance.sock` in the app data directory (Java 16+, supported on Windows 10 1803+ and macOS).
  - A second process fails to take the lock, connects, sends one line (`FOCUS`, or `OPEN_GAME <id>`) and exits.
    Before handing over on Windows it calls `AllowSetForegroundWindow(ASFW_ANY)`, so the running window may come to
    the front instead of only flashing in the taskbar.
  - The running instance un-minimizes the window, calls `toFront()`, requests focus and, for `OPEN_GAME`, emits the id
    to the same pending-deep-link holder the iOS shell uses.
  - On macOS LaunchServices already keeps one copy of a bundle, and a click on a notification is delivered to the
    running process through the delegate. The lock still covers dev runs.
  - Closing the window calls `exitApplication()`: the process ends on both OSes (FR-006a). There is no tray icon and no
    Dock-only state.
  - The minimum window size is 400 × 640 (the narrow layout). Size and position are not persisted.
- **Alternatives considered**: a TCP loopback port. Rejected: it needs a port file and is reachable by every local
  user.

## R12. Release-date refresh on desktop

- **Decision**: `InAppReleaseRefreshScheduler` (`:core:data` `desktopMain`) implements `ReleaseRefreshScheduler` over
  the existing `ReleaseRefreshRunner`. `schedulePeriodicRefresh()` runs it with `onlyIfDue = true` at launch and then
  rechecks every hour while the process lives. `scheduleImmediateRefresh()` runs it with `onlyIfDue = false`. The
  runner already joins concurrent requests, records only successes and runs a full reminder sync after a successful
  refresh (FR-013). Nothing runs while the app is closed (FR-026).
- **Finding**: no string in the app mentions background refresh, so FR-026's "no mention" needs no UI change.

## R13. Unsupported features on desktop

- **Translation**: `UnsupportedDescriptionTranslator` (`:core:data` `desktopMain`) reports
  `TranslationModelStatus.UNSUPPORTED` and never translates. The Settings row, the game detail action and the
  download flow already hide on `UNSUPPORTED`, as they do on an iOS device without Apple Intelligence (FR-025). The
  `TranslationEngineBadge` desktop `actual` draws nothing; it is unreachable.
- **Share**: there is no share sheet the JVM can reach on Windows. The desktop `TextSharer` copies the text to the
  clipboard and sets `copiesToClipboard = true`. Game detail then shows a "Copied to clipboard" snackbar, a new
  string, so the action does what it says (FR-027).
- **Cover picker**: `java.awt.FileDialog` (the native picker on both OSes), filtered to JPEG, PNG, GIF and BMP, which
  `javax.imageio` decodes. `source` is the picked file's absolute path. Unlike iOS, the desktop storage **never
  deletes the source**, because it is the user's own file.
- **Unmetered network**: `NetworkStatusProvider.isUnmeteredNetworkAvailable` is `true` on desktop. Its only consumer
  is the translation download, which never appears.
- **Welcome flow**: `requiresRuntimePermission` is `true` on macOS (there is a prompt) and `false` on Windows (there is
  none, as on Android below API 33). The existing mapper therefore shows the reminders page on macOS when undecided
  or blocked, and skips it on Windows, which is the Android-below-33 behaviour (FR-022: no "unavailable" page
  exists anywhere).

## R14. Reminder behaviour that differs by OS, and what each guarantees

| Topic | Windows | macOS |
|---|---|---|
| Delivery with the app closed | Yes, scheduled toast, from a dev run and the installed copy | Yes, from a bundle only (`runDistributable`, `.dmg`); not part of acceptance (R9) |
| Pending cap | 64, kept equal to iOS through the shared `MAX_PENDING_REMINDERS` (Windows allows more; 64 keeps one rule) | 64, the system's limit |
| Time zone change after arming | Fires at the old instant until the next launch re-arms | Floating trigger, follows the new zone |
| Machine off or asleep at 09:00 | Expected: shown when the machine wakes or signs in. **Recorded in the walkthrough** (quickstart §5) | Expected: shown on wake. Recorded if verifiable |
| Focus Assist / Focus | The OS's rules; the toast goes to the notification centre | The OS's rules |
| Uninstall | The MSI removes the app. **Pending scheduled toasts and the HKCU AUMID keys stay**, so a stale toast can still appear on its date, with no click action. Removing them needs an uninstall custom action, and Compose's `packageMsi` only allows one by overwriting jpackage's private `main.wxs`, which is undocumented ([compose-multiplatform#1972](https://github.com/JetBrains/compose-multiplatform/issues/1972)). Not done; listed as a follow-up | No uninstaller (drag to Trash). Pending requests stay with the system and are expected not to be shown once the bundle is gone; not guaranteed |
| Reinstall | First launch: `ReleaseReminderCoordinator` sweeps pending reminders that the ledger does not know, then runs the full sync | Same |

This table goes into the platform contracts' capability exception register (FR-021, FR-032).

## R15. Orphan sweep at launch (desktop only)

- **Decision**: `ReleaseReminderCoordinator` gains an optional `beforeFirstSync: suspend () -> Unit = {}`. The
  desktop binding passes a sweep that removes every pending reminder whose game id is not in `ArmedReminderLedger`.
  iOS keeps the default no-op, so its behaviour does not change.
- **Rationale**: on desktop the app data and the OS's pending notifications can drift apart (data deleted by hand, a
  reinstall, a dev run and an installed copy). The spec requires that no stale notification from an earlier install
  is delivered after a reinstall.

## R16. Back navigation, keyboard and mouse

- **Decision**: the desktop window maps **Escape** and the **mouse back button** (button 4) to a back event through
  the navigation-event dispatcher that Navigation 3 and the two-pane back handler already listen to. Escape is not
  consumed while a text field has a selection or an open menu or dialog takes it first. Clipboard and selection
  shortcuts, wheel and trackpad scrolling and hover come from Compose Desktop with no code.
- **Rationale**: FR-005. The on-screen back arrows already exist.

## R17. Packaging

- **Decision**: Compose's `nativeDistributions` with `targetFormats(Msi, Dmg)`. `packageMsi` runs only on Windows and
  downloads WiX itself; `packageDmg` runs only on macOS. Settings:
  - `packageName = "QuestLog"`, `packageVersion = "1.0.0"` (MSI and DMG need `MAJOR.MINOR.BUILD` with `MAJOR > 0`)
  - a fixed `upgradeUuid`, so a newer MSI replaces the old one
  - `perUserInstall = true`, so no admin rights are needed and the app is in the user's Start menu
  - `menu = true`, `shortcut = true`
  - `bundleID = "com.nikolasguillen.questlog"`
  - icons (`.ico`, `.icns`) generated from the existing launcher artwork
  - `modules(...)` from `suggestRuntimeModules`, which must include `java.desktop` and `jdk.httpserver` (R3)
  - the version passed to the app as `-Dquestlog.version`, read by the desktop `AppVersionProvider`
  - a minimum OS of Windows 10 22H2 / macOS 13 (`LSMinimumSystemVersion` in the bundle's `Info.plist`)
- **Signing**: none in this feature. Windows shows an unknown-publisher prompt (spec assumption). The macOS bundle is
  only ad-hoc signed (R9).

## R18. Tests on the desktop target

- **Decision**: new desktop-only logic gets JUnit4 tests in the owning module's `desktopTest`: the Firestore REST
  value codec, the Identity Toolkit error mapping, `RestCloudAuth` and `RestBackupStore` against `ktor-client-mock`,
  the PKCE and loopback parsing, the toast XML builder (escaping, `launch` argument), the single-instance message
  parser, `InAppReleaseRefreshScheduler`, the orphan sweep and a `DesktopKoinGraphTest` in `:shared` that verifies
  the desktop graph. The root `test` task also depends on every module's `desktopTest`, so `./gradlew test` stays
  "every JVM unit test" on Windows and macOS. The native bindings themselves are covered by the walkthrough, not by
  unit tests.
- **Rationale**: Principle V. The JVM is the desktop runtime, so these tests run exactly where the code runs.
