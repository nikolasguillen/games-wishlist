# Implementation Plan: Desktop App (Windows and macOS)

**Branch**: `020-desktop-app` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-desktop-app/spec.md`

## Summary

QuestLog becomes a desktop app on Windows and macOS, the third platform next to Android and iOS, with the same
screens, data and rules. Release reminders are delivered by the OS while the app is closed. The cloud account and
backup are shared with the phones. Translation and closed-app refresh are absent.

Technically:
- **One JVM target, `desktop`, in every multiplatform module**, added by `questlog.kmp.library` (R1). Every library
  the shared code uses already ships a JVM build. Each existing `expect` and platform Koin module gets a
  `desktopMain` `actual` ([contracts/desktop-platform-contracts.md](contracts/desktop-platform-contracts.md)).
- **A new `:desktopApp` shell** holds `main`, the window, single-instance handling, the native libraries and the
  installer configuration, as `:androidApp` and `iosApp/` do for their platforms (R2, R11).
- **Reminders reuse the iOS pipeline as it stands.** The shared scheduler, coordinator and ledger run over a desktop
  `LocalNotificationCenter`. On Windows that is WinRT scheduled toasts with a COM activator for clicks, called
  through JNA. On macOS it is `UserNotifications` through a small Swift library (R8, R9). A launch-time orphan sweep
  handles reinstalls (R15).
- **Cloud on desktop is Firebase's REST API over Ktor (owner).** GitLive's JVM build cannot sign in with a Google
  credential. Google sign-in runs in the system browser with PKCE and a loopback redirect. Firestore documents are
  identical to the mobile ones. Apple sign-in is not offered on desktop because it needs a paid Apple account (R3–R6).
  GitLive moves to a `mobileMain` source set in `:core:cloud`.
- **Refresh runs at launch and hourly while open** over the existing runner (R12). Translation reports
  `UNSUPPORTED`, which hides it everywhere. Share copies to the clipboard (R13).
- **Installers**: `.msi` built on Windows and `.dmg` built on macOS through Compose's `nativeDistributions`. Neither
  is signed (R17).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (JVM toolchain 21; Android stays Java 11 bytecode, `desktopMain` targets JVM 17).
Swift 5 for the macOS notifications library.

**Primary Dependencies**: the existing Compose Multiplatform 1.12.1, Koin 4.2.2, Room 2.8.5 (+ `kspDesktop`), Ktor 3.6.0,
DataStore 1.2.1, Coil 3.6.3 and Navigation 3. **New** (each one is under Complexity Tracking):
- `compose.desktop.currentOs` and `org.jetbrains.kotlinx:kotlinx-coroutines-swing` 1.11.0 (`:desktopApp`; Swing
  provides `Dispatchers.Main` on desktop)
- `net.java.dev.jna:jna` and `jna-platform` 5.19.1 (`:core:common` `desktopMain`, `:desktopApp`)
- `io.ktor:ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`
  (`:core:cloud` `desktopMain`), plus the `buildconfig` plugin in `:core:cloud` for `DesktopCloudCredentials`
- `io.ktor:ktor-client-okhttp` in `desktopMain` of `:core:network` and `:shared` (the engine Android already uses)

**Storage**: no Room change; the version stays 1. Files go in the per-user app data directory
([data-model.md §1](data-model.md)). The OS notification stores hold the scheduled reminders.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test, plus `ktor-client-mock`, in each module's new `desktopTest`
(R18). Covered there: the REST codec and error mapping, `RestCloudAuth` and `RestBackupStore`, PKCE and redirect
parsing, the toast XML builder, the single-instance parser, `InAppReleaseRefreshScheduler`, the orphan sweep and
`DesktopKoinGraphTest`. The existing `androidHostTest` suites stay green, including `KoinGraphTest` and the
coordinator tests updated for `beforeFirstSync`. The root `test` task gains every `desktopTest`. The native bindings
are covered by the walkthrough ([quickstart.md §5](quickstart.md)).

**Target Platform**: Windows 10 22H2 and Windows 11 (x64); macOS 13+ (the architecture of the build Mac, Apple silicon
here). Android and iOS unchanged.

**Project Type**: Kotlin Multiplatform mobile and desktop app (Compose Multiplatform).

**Performance Goals**: no new targets. The desktop window should reach its first screen in about the time the phones
take (JVM start plus the same root load). Not measured as a gate.

**Constraints**:
- Android builds and passes at every commit.
- Nothing resident when the app is closed (FR-012, FR-006a).
- No URL scheme registered (FR-001).
- No signing.
- Windows is the primary verification platform. macOS closed-app reminders are built but not accepted.

**Scale/Scope**: 20 multiplatform modules gain a target. About 25 `desktopMain` `actual`s and bindings. One new
module. Two native bindings (Windows via JNA, macOS via Swift). Two REST clients (Firebase Auth, Firestore). One new
string (`share_copied_to_clipboard`). No new screen.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule | Status | Notes |
|---|---|---|
| I. Module boundaries | **Justified** | New `:desktopApp` (→ `:shared`, `:core:common`). Features still depend only on the six allowed modules. `:core:cloud` is still reached only from `:core:data` and `:shared`: `:desktopApp` never names it. `:core:ai` is untouched and stays Android-only (desktop does not link it). See Complexity Tracking |
| II. Typed errors | Pass | `RestCloudAuth` and `RestBackupStore` throw only `CloudException` subtypes and rethrow `CancellationException`; `:core:data`'s mapper is unchanged. Desktop connectivity classification matches Android's |
| III. UI renders | Pass | No new screen. The one UI change (the clipboard snackbar) is driven by the sharer's flag in the existing effect handler. Permission state comes through the existing `NotificationPermissionState` |
| IV. Reuse | Pass | `CustomAlertDialog` for the desktop denied dialog. No new component. The reminder scheduler, coordinator, ledger, refresh runner and reminder text are reused as they are |
| V. Local verification | Pass | Real commands only (quickstart §4). `./gradlew test` grows to include `desktopTest`. No CI or lint gate is assumed |
| Persistence | Pass | No entity change, version 1, no migration |
| Injection | Pass | ViewModels unchanged in `ViewModelModule`. Desktop bindings live in each module's `*PlatformModule` `actual`. `:desktopApp` supplies only what `main` knows. `KoinGraphTest` stays green and the desktop graph gets its own test |
| Secrets | Pass | Four new keys in `local.properties`, generated under `build/` (R6); never committed |
| KMP: platform code implements a listed capability | **Amendment** | `desktopMain` joins `androidMain`/`iosMain`. Every desktop file implements a capability in the updated platform contracts (FR-032) |
| KMP: "iOS is the only platform besides Android" | **Amendment (owner)** | The constitution's Platform and KMP sections, `CLAUDE.md`, `docs/roadmap.md` and the platform contracts are updated in the same change (FR-033). Constitution 1.3.5 → **1.4.0** (MINOR: a materially expanded section), which needs the owner's approval |
| Library swaps decided in the plan | Pass | The Firebase REST client on desktop is decided here, with the owner (R3). GitLive is unchanged on mobile |
| Hard-to-undo decisions | Flagged | (1) The constitution amendment. (2) Shipping a REST client next to GitLive. (3) The HKCU registration (AUMID, COM CLSID) that installed copies leave behind. All three are listed under Owner decisions |

**Post-design re-check (after Phase 1)**: unchanged. The contracts keep platform types out of every signature
(`DesktopNotifications` uses `kotlinx-datetime` and `kotlinx-io` types). The new source set in `:core:cloud`
(`mobileMain`) adds no module edge.

## Project Structure

### Documentation (this feature)

```text
specs/020-desktop-app/
├── plan.md                 # This file
├── research.md             # Phase 0: R1–R18
├── data-model.md           # Phase 1: files, permission, OS reminder mapping, cloud session, messages
├── quickstart.md           # Phase 1: setup, run, package, walkthrough
├── contracts/
│   ├── desktop-platform-contracts.md   # the desktop column + new contracts + register rows
│   └── firestore-rest.md               # Google OAuth, Identity Toolkit, Firestore REST wire format
└── tasks.md                # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
build-logic/convention/src/main/kotlin/
└── KmpLibraryConventionPlugin.kt        # + jvm("desktop"), JVM 17 for desktop

settings.gradle.kts                      # + include(":desktopApp")
build.gradle.kts                         # root `test` also depends on every `desktopTest`
gradle/libs.versions.toml                # + jna, jna-platform, kotlinx-coroutines-swing

desktopApp/                              # NEW: the desktop shell
├── build.gradle.kts                     # kotlin("jvm") + compose; compose.desktop.application { nativeDistributions }
├── icons/                               # QuestLog.ico, QuestLog.icns, notification.png
├── native/macos/QuestLogNotifications.swift   # the Swift library (research R9)
├── resources/macos-arm64/               # built dylib lands here (git-ignored), bundled as an app resource
└── src/main/kotlin/com/nikolasguillen/questlog/desktop/
    ├── Main.kt                          # main(): click handler, single instance, initKoin, start-up, window
    ├── SingleInstance.kt                # lock + Unix-domain socket (data-model §6)
    ├── SingleInstanceMessage.kt
    ├── DesktopBackKeys.kt               # Escape / mouse back → back event
    └── WindowFocusEvents.kt             # focus → DesktopReminderSyncTriggers

core/common/src/desktopMain/kotlin/.../core/common/
├── AppVersionProviderImpl.kt, NetworkStatusProviderImpl.kt, DateRendering.desktop.kt
├── di/CommonPlatformModule.desktop.kt
└── desktop/                             # DesktopNotifications, DesktopNotificationStatus, DesktopNotificationRequest,
    ├── …                                # DesktopPendingNotification, AppDataDirectory, OpenSystemNotificationSettings
    ├── windows/                         # WinRT/COM via JNA: WinRt, ToastNotifierBinding, ToastXml,
    │                                    # NotificationActivator, AumidRegistration, WindowsToastNotifications
    └── macos/                           # MacUserNotifications (JNA over the Swift library)

core/database/src/desktopMain/.../di/DatabasePlatformModule.desktop.kt
core/network/src/desktopMain/.../        # PlatformTransportFailure.desktop.kt, di/NetworkPlatformModule.desktop.kt
core/designsystem/src/desktopMain/.../theme/SystemBarsAppearance.desktop.kt

core/data/src/desktopMain/.../core/data/
├── di/DataPlatformModule.desktop.kt
├── notification/DesktopLocalNotificationCenter.kt
├── scheduler/InAppReleaseRefreshScheduler.kt
├── translation/UnsupportedDescriptionTranslator.kt
├── local/WishlistCoverImageStorageImpl.kt
└── repository/RepositoryErrorMapper.desktop.kt
core/data/src/commonMain/.../notification/ReleaseReminderCoordinator.kt   # + beforeFirstSync

core/cloud/
├── build.gradle.kts                     # + mobileMain (android + ios) for GitLive; desktopMain Ktor; buildconfig
└── src/
    ├── commonMain/                      # interfaces, models, CloudException, constants (GitLive-free)
    ├── mobileMain/                      # MOVED: FirebaseCloudAuth, FirestoreBackupStore, FirebaseErrorClassifier,
    │                                    #        firebaseCloudModule (was cloudModule)
    └── desktopMain/                     # RestCloudAuth, RestBackupStore, GoogleInstalledAppSignIn, IdentityToolkitClient,
                                         # FirestoreRestCodec, DesktopCloudSessionStore, RestErrorMapper,
                                         # di/CloudPlatformModule.desktop.kt

core/ui/src/desktopMain/.../core/ui/
├── util/                                # NotificationPermission.desktop.kt, DesktopNotificationAuthorization,
│                                        # CoverImagePicker.desktop.kt, TextSharer.desktop.kt, ReadableBitmaps.desktop.kt
└── component/                           # FullScreenDialogProperties.desktop.kt, NotificationPermissionDeniedDialog.desktop.kt,
                                         # translation/TranslationEngineBadge.desktop.kt
core/ui/src/commonMain/.../util/TextSharer.kt          # + copiesToClipboard
core/ui/src/commonMain/composeResources/values/strings.xml   # + share_copied_to_clipboard

feature/search/src/desktopMain/.../components/SearchBarScrollBehaviorCompat.desktop.kt
feature/game-detail/src/commonMain/.../GameDetailScreen.kt   # snackbar when the sharer copies

shared/src/
├── commonMain/.../ReminderDeepLinks.kt  # MOVED from iosMain
├── commonMain/.../di/SharedKoin.kt      # cloudModule removed from allModules
└── desktopMain/.../                     # DesktopReminderSyncTriggers, RunBlocking.desktop.kt, QuestLogDesktop.kt
                                         # (startQuestLogDesktop: the Koin + start-up half of main)

docs & governance (same change, FR-033): CLAUDE.md (root, plus core/cloud/CLAUDE.md for mobileMain/desktopMain),
.specify/memory/constitution.md (1.4.0, owner approval), docs/roadmap.md,
specs/010-kmp-migration/contracts/platform-contracts.md (Desktop column, new contracts, register rows)
```

**Structure Decision**: the existing modules each get a `desktopMain`. The one new module is the `:desktopApp`
shell. The desktop OS bindings are shared from `:core:common` `desktopMain` (R7). `:core:cloud` gains a `mobileMain`
intermediate source set, so the Firebase SDK stays off desktop.

## Build Order

Each step leaves Android building and passing (quickstart §4) and is a point the work can pause on.

1. **Spikes**, throwaway code on the branch, removed or folded into step 4/5:
   - (a) Windows: from an installed `runDistributable` image, a JNA scheduled toast fires with the app closed, and its
     click reaches a JNA COM activator in a freshly started process. If (a) fails, stop and ask the owner about the
     C# helper fallback (R8).
   - (b) macOS: the Swift library schedules from `runDistributable`, and a click reaches the delegate.
   - (c) The REST Google sign-in reaches `signInWithIdp` with the Desktop client.
2. **Desktop target and stubs**: the convention plugin, `kspDesktop`, every `actual` (reminders and cloud are
   temporarily inert, with no UI entry pointing at them), `:desktopApp` with the window, and the root `test` task.
   The app opens and works locally on both OSes (US1 core).
3. **Desktop shell behaviour**: app data directory, single instance, quit on close, minimum size, back keys, clipboard
   share and snackbar, cover picker, translator, refresh scheduler (US1 complete, FR-025–027).
4. **Reminders**: `DesktopNotifications` (Windows, then macOS), `DesktopLocalNotificationCenter`, the permission state
   and denied dialog, sync triggers, orphan sweep, click-through via `ReminderDeepLinks` (US2–US4).
5. **Cloud over REST**: the `:core:cloud` `mobileMain` move (mobile unchanged, its tests green), then the desktop REST
   client, session store and bindings (US5).
6. **Packaging**: icons, `nativeDistributions`, runtime modules, Swift library bundling, `.msi` on Windows and `.dmg`
   on macOS (US6).
7. **Documentation and governance** (FR-033): platform contracts, `CLAUDE.md` files, roadmap, and the constitution
   amendment once the owner approves. Then the walkthrough, with its records written into the register.

## Owner Decisions

Already decided: no Apple account, so macOS closed-app reminders are not accepted. Local-only is the fallback, not
taken. The window quits on close. Minimum OS versions. No URL scheme. REST for desktop cloud.

Needed during implementation:

1. **Constitution amendment 1.3.5 → 1.4.0**: desktop as the third platform and `desktopMain` as a platform source set.
   Hard to undo in spirit, because it sets the platform scope. Drafted in step 7 and committed only with approval.
2. **The C# helper fallback for Windows**, only if spike 1a fails. It adds the .NET SDK to Windows builds.
3. **Reported, no action needed**:
   - Desktop offers Google sign-in only (R4).
   - The `.dmg` is Apple silicon only (R9).
   - A Windows uninstall leaves pending toasts and the HKCU keys behind (R14). Fixing that means overriding
     jpackage's WiX template, which Compose does not support.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| The JNA COM activator does not receive closed-app clicks | US3 on Windows | Spike 1a first. C# helper fallback (owner) |
| A JVM app on macOS misses the click that launched it (AppKit finishes launching before the delegate is set) | US3 on macOS, not accepted | The delegate is installed first in `main`. Recorded in walkthrough row 32 |
| Firebase refuses the Desktop client's ID token in `signInWithIdp` | US5 | Spike 1c. The client is in the same Google Cloud project as Firebase. If needed, pass the access token instead |
| `jlink` runtime misses a module (`jdk.httpserver`, `java.naming`, …) | The installed copy fails where `run` works | `suggestRuntimeModules`. Walkthrough row 27 runs from the installed copy |
| A library's JVM build behaves differently (Haze blur, the adaptive navigation suite) | Visual differences | Walkthrough rows 1–3. No desktop-only screens to maintain |
| The scheduled-toast id limit or the WinRT `DateTime` epoch is handled wrong | Wrong or missing toasts | Unit tests for id and XML. `DateTime` is 100 ns ticks since 1601-01-01 UTC, tested |

## Complexity Tracking

| Addition | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| New module `:desktopApp` | The desktop entry point, window, single instance, native libraries and installers, mirroring `:androidApp` and `iosApp/` | `compose.desktop.application` in `:shared` would put installer and shell concerns into the navigation and DI module (R2) |
| Edges `:desktopApp → :shared`, `:desktopApp → :core:common` | The root composable and Koin start come from `:shared`. The click handler, activator and OS helpers come from `:core:common` `desktopMain` | Re-exporting the OS bindings through `:shared` would widen `:shared`'s API for one caller |
| `jvm("desktop")` in every multiplatform module | Desktop is a target of the whole app | Partial targets cannot depend on each other (R1) |
| `mobileMain` source set in `:core:cloud` | Keeps GitLive and `firebase-java-sdk` out of desktop, where they cannot sign in | Leaving GitLive in `commonMain` ships an unusable SDK in the installer (R3) |
| Firebase REST client on desktop **(owner)** | GitLive's JVM build has no credential sign-in | Local-only desktop, waiting for the Java SDK, or a token server (R3) |
| JNA in `:core:common` `desktopMain` | WinRT, COM, the registry and the Swift library are reached from the JVM | A .NET helper adds a toolchain. Java's FFM is final only after JDK 21 (R8) |
| Swift library built by Gradle on macOS | `UserNotifications` needs Objective-C blocks and a delegate | JNA-only Objective-C requires hand-built blocks (R9) |
| `ReleaseReminderCoordinator.beforeFirstSync` | Desktop must sweep orphaned OS notifications at launch (R15) | A desktop-only coordinator copy would duplicate the conflation logic |
| `TextSharer.copiesToClipboard` | Desktop has no share sheet, and FR-027 forbids a control that silently does something else | Hiding share on desktop would remove an action mobile has (FR-001) |
