# Implementation Plan: Cloud Backup and Restore

**Branch**: `016-cloud-backup-restore` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-cloud-backup-restore/spec.md`

## Summary

A signed-in user can back up their lists (with covers), saved games (status, priority, notes, details), list
membership, default wishlist and owned platforms to Firebase, and restore them on any Android or iOS device. Sign-in
is Google or Apple on both platforms. The app stays fully usable signed out.

Technically:
- **A new `:core:cloud` module** wraps Firebase through GitLive's Kotlin SDK (R1). It plays the role `:core:network`
  plays for IGDB: it talks to the backend, throws its own exception types, and knows nothing about games.
  `:core:data` uses it, builds the snapshot from Room, maps errors and implements two new repositories,
  `AccountRepository` and `BackupRepository`, **(owner)**.
- **Sign-in is the only platform code**: Credential Manager and Firebase's Apple web flow on Android (R4); a Swift
  `IdentityProviderBridge` with GoogleSignIn-iOS and AuthenticationServices on iOS (R5), the shape the
  FoundationModels bridge already has.
- **Same-email linking is Firebase's default** "one account per email" with trusted providers. There is no Identity
  Platform and no billing (R3). The spec was amended.
- **Storage is a manifest plus chunked parts** in Firestore. A backup is committed by writing the manifest last, so
  a failed backup never touches the previous one (R6). Covers are included **(owner)**, each as its own stream.
- **Restore downloads and validates everything, then replaces the data in one Room transaction** in a new
  `UserDataDao`, writing games through `GameDao.saveGame` (R9, R10).
- **The iOS Firebase SDK is re-pinned to 11.x** to match GitLive 2.7.0 and GoogleSignIn-iOS (R2). **A spike proves
  the iOS link before any feature code** (R13).
- **The UI is a new Account & backup screen** in `:feature:settings`, reached from a new row in the Settings hub
  (R12).

## Technical Context

**Language/Version**: Kotlin 2.4.20 (JVM toolchain 21, Java 11 bytecode); Swift 5 for the iOS bridge

**Primary Dependencies**: existing Compose Multiplatform 1.12.1, Koin 4.2.2, Room 2.8.5, kotlinx-serialization 1.11.0.
**New** (R14), with each edge called out under Complexity Tracking:
- `dev.gitlive:firebase-auth`, `dev.gitlive:firebase-firestore` 2.7.0 (`:core:cloud` `commonMain`)
- `androidx.credentials:credentials` and `credentials-play-services-auth` 1.6.0, and
  `com.google.android.libraries.identity.googleid:googleid` 1.2.1 (`:core:cloud` `androidMain`)
- `kotlinx-serialization-json` and the serialization plugin in `:core:data` and `:core:cloud`
- the `com.google.gms.google-services` 4.5.0 plugin (`:androidApp`)
- Swift packages: `firebase-ios-sdk` 11.15.0 ..< 12 (re-pinned from 13.0.1) and `GoogleSignIn-iOS` 10.0.0

**Storage**: Room: no entity or schema change. One new DAO (`UserDataDao`), so the DB version is untouched.
Firestore: `users/{uid}` and `users/{uid}/parts/*` ([contracts/firestore.md](contracts/firestore.md)). Cover files are
in the existing covers directory, through three new `WishlistCoverImageStorage` members.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test in `androidHostTest`:
- `:core:data`: mapper round trip, chunker, `BackupRepositoryImpl` and `AccountRepositoryImpl` against fakes, error mapping
- `:core:cloud`: classification of Firebase codes
- `:core:domain`: `DeleteAccountUseCase`
- `:feature:settings`: `AccountBackupViewModel`
- `:shared`: `KoinGraphTest`

No Room DAO tests can run on the host (existing gap, `docs/tech-debt.md`). The transaction is covered by quickstart
§4 rows 11–12.

**Target Platform**: Android (minSdk 29, compile/targetSdk 37) and iOS 16+ (`ASAuthorizationController` is iOS 13+,
GoogleSignIn-iOS 10 supports iOS 12+)

**Project Type**: Kotlin Multiplatform mobile app (19 → **20** Gradle modules)

**Performance Goals**: SC-001 and SC-002: a 500-game backup in under 30 s, and a restore including sign-in in under
2 min on mobile data. Expected volume is ~10 Firestore parts of ≤ 700 KB, each written sequentially.

**Constraints**:
- Nothing half-written, locally or in the cloud (FR-014, FR-020).
- Free plan only.
- No Firebase type outside `:core:cloud`.
- No platform type in a contract signature.
- `commonMain` has no `android.*`/`java.*`.
- Every string is a resource.
- Both config files stay git-ignored.

**Scale/Scope**:
- 1 new module, 2 new repositories, 9 use cases, 1 new route and screen, 1 new DAO, and 1 Swift bridge.
- About 60 new Kotlin files (one type per file), 3 Swift files, and about 15 changed files.

No unknowns remain. R1–R14 in [research.md](research.md) resolve every question. The one outcome still open is the
spike (R13), and its failure path is decided.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design. Both pass, with the items under
Complexity Tracking owner-approved.*

| Principle | Check | Result |
|-----------|-------|--------|
| **I. Module boundaries** | A **new module `:core:cloud`** with **two new edges**: `:core:data → :core:cloud` and `:shared → :core:cloud` (Koin assembly and the iOS bridge binding). `:core:cloud` depends on no project module. `feature/*` gains no edge: `:feature:settings` reaches accounts and backup through `:core:domain` use cases only. `:core:model` gains three plain types (no Android, no Compose). Navigation: one route, added in `Routes.kt`, `GameNavSavedStateConfiguration.kt` and `QuestLogNavDisplay`'s `entryProvider`. Every new item is listed under Complexity Tracking. | ✅ (justified) |
| **II. Typed errors** | `:core:cloud` throws only its own `Cloud*Exception` types; `:core:data` maps them in `toRepositoryError()` and never names a Firebase type. `CancellationException` is rethrown first. The account and backup methods touch the network, so they return `AppResult`; `hasLocalUserData` is DB-only and returns `Boolean`. All rendering goes through `RepositoryError.toUiText()`, which gains eight cases (R11). | ✅ |
| **III. UI renders** | `AccountBackupViewModel` decides: which dialog, which operation is running, whether to confirm (via `HasLocalUserDataUseCase`), and which messages. The screen collects with `collectAsStateWithLifecycle()` and emits `AccountBackupUiEvent`s through one `onEvent`. The state is `@Immutable`, and effects go through `Channel(BUFFERED)`. Dates and summaries are built as `UiText` in the mapper. | ✅ |
| **IV. Reuse first** | Reused: `CustomAlertDialog`, `SettingsGroup`/`SettingsRow`, `MaterialTheme.spacing`, `RepositoryError.toUiText`, `AppVersionProvider`, `WishlistCoverImageStorage` (extended rather than duplicated), `GameDao.saveGame` (called by the restore), `DateUtils` (extended). **Two new repositories**: the constitution requires the owner's agreement, which was **given on 2026-10-15** (Complexity Tracking). | ✅ (owner-approved) |
| **V. Local verification** | `:core:data`, `:core:cloud`, `:core:domain`, `:feature:settings` and `:shared` `testAndroidHostTest`; `./gradlew test`; `:androidApp:assembleDebug`; `xcodebuild` on the Mac. No CI or lint is assumed. The iOS rows of the quickstart are recorded as not run until run. | ✅ |
| **Persistence** | No entity change, no version bump, no schema regeneration. Games are persisted through `GameDao.saveGame`. No list-shaped column. | ✅ |
| **Injection** | `cloudModule` + `expect val cloudPlatformModule` in `:core:cloud`, assembled in `:shared`'s `allModules`. `AccountBackupViewModel` is registered with `viewModelOf`. The iOS bridge is bound in `startQuestLog`. No `SavedStateHandle`. `KoinGraphTest` covers the new graph. | ✅ |
| **Secrets** | `google-services.json` and `GoogleService-Info.plist` stay ignored (already true). No credential in source. The web client id comes from the generated resource, not from a literal. | ✅ |
| **KMP** | Shared logic is in `commonMain`, and the platform code is only the provider sign-in (`PlatformProviderAuth`, two implementations) and the Swift bridge, each added to `platform-contracts.md` first. `expect`/`actual` is used for one function (`renderLocalDateTime`) and the platform Koin module. Library choice (GitLive) is decided here, in the plan. | ✅ |

**Post-design re-check**: the contracts and data model introduce nothing beyond the items above. The abstract-class
DAO is a Room pattern and is recorded under Complexity Tracking, because `core/database/CLAUDE.md` describes DAOs as
interfaces.

## Project Structure

### Documentation (this feature)

```text
specs/016-cloud-backup-restore/
├── plan.md               # this file
├── research.md           # R1–R14
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── cloud-contracts.md
│   └── firestore.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks, not created here
```

### Source Code (repository root)

```text
settings.gradle.kts                              + include(":core:cloud")
build.gradle.kts                                 + google-services plugin, apply false
gradle/libs.versions.toml                        + gitlive, credentials, googleid, google-services
firebase/firestore.rules                         new (contracts/firestore.md)

core/cloud/                                      NEW MODULE (questlog.kmp.library + serialization)
  build.gradle.kts
  src/commonMain/kotlin/com/nikolasguillen/questlog/core/cloud/
    CloudAuth.kt, FirebaseCloudAuth.kt
    CloudBackupStore.kt, FirestoreBackupStore.kt
    PlatformProviderAuth.kt, AppleGrant.kt
    model/  CloudUser.kt, CloudProvider.kt, BackupManifestDocument.kt, BackupStreamDocument.kt, BackupPartDocument.kt
    error/  CloudException.kt (+ one file per subclass), FirebaseErrorClassifier.kt
    di/     CloudKoin.kt (cloudModule), CloudPlatformModule.kt (expect)
  src/androidMain/kotlin/.../core/cloud/
    AndroidProviderAuth.kt, CurrentActivityTracker.kt, di/CloudPlatformModule.android.kt
  src/iosMain/kotlin/.../core/cloud/
    IdentityProviderBridge.kt, GoogleSignInResult.kt, AppleSignInResult.kt, ProviderOutcome.kt,
    IosProviderAuth.kt, di/CloudPlatformModule.ios.kt
  src/androidHostTest/kotlin/.../core/cloud/FirebaseErrorClassifierTest.kt

core/model/src/commonMain/.../core/model/        + AuthProvider.kt, Account.kt, BackupInfo.kt; RepositoryError.kt (+8 cases)

core/domain/src/commonMain/.../core/domain/
  repository/  + AccountRepository.kt, BackupRepository.kt
  usecase/account/  + ObserveAccountUseCase, SignInUseCase, LinkProviderUseCase, SignOutUseCase, DeleteAccountUseCase
  usecase/backup/   + GetBackupInfoUseCase, BackUpUseCase, RestoreBackupUseCase, HasLocalUserDataUseCase
  di/DomainKoin.kt  (registrations)
core/domain/src/androidHostTest/.../usecase/account/DeleteAccountUseCaseTest.kt

core/database/src/commonMain/.../core/database/
  dao/UserDataDao.kt (new, abstract class)   userdata/UserDataRows.kt (new)
  QuestLogDatabase.kt (+ userDataDao())       di/DatabaseKoin.kt (+ binding)

core/data/
  build.gradle.kts                           + :core:cloud, serialization json + plugin
  src/commonMain/.../core/data/
    backup/  BackupChunker.kt, BackupConstants.kt, model/ (11 @Serializable DTOs, one per file)
    mapper/BackupMapper.kt, mapper/AccountMapper.kt
    repository/AccountRepositoryImpl.kt, BackupRepositoryImpl.kt, RepositoryErrorMapper.kt (Cloud* branches)
    local/WishlistCoverImageStorage.kt (+3 members)
    di/DataKoin.kt (+ the two repositories)
  src/androidMain/.../local/WishlistCoverImageStorageImpl.kt  (+3 members)
  src/iosMain/.../local/WishlistCoverImageStorageImpl.kt      (+3 members)
  src/androidHostTest/.../ BackupMapperTest, BackupChunkerTest, BackupRepositoryImplTest,
                           AccountRepositoryImplTest, RepositoryErrorMapperTest (extended)

core/common/   DateUtils.kt (+ formatDateTime, TimeStyle), renderLocalDateTime expect/actuals
core/ui/       mapper/ErrorMapper.kt (+8 branches), composeResources strings (+7)
core/navigation/  Routes.kt (+ AccountBackupRoute), GameNavSavedStateConfiguration.kt (+ subclass)

feature/settings/src/commonMain/.../feature/settings/
  AccountBackupScreen.kt, AccountBackupViewModel.kt
  components/  AccountCard.kt, BackupCard.kt, ProviderSignInButton.kt, AccountBackupDialogs.kt
  mapper/AccountBackupUiMapper.kt
  model/  AccountBackupUiState.kt, AccountBackupUiEvent.kt, AccountBackupUiEffect.kt, AccountUiModel.kt,
          BackupStatusUiModel.kt, CloudOperation.kt, AccountBackupDialog.kt
  SettingsScreen.kt / SettingsViewModel.kt / SettingsUiState.kt (+ Backup group row and accountSummary)
  composeResources/values/strings.xml (+ strings), drawable/ (Google "G", Apple logo)
feature/settings/src/androidHostTest/.../AccountBackupViewModelTest.kt, SettingsViewModelTest.kt (extended)

shared/
  build.gradle.kts                           + :core:cloud
  src/commonMain/.../di/SharedKoin.kt         + cloudModule, cloudPlatformModule
  src/commonMain/.../di/ViewModelModule.kt    + AccountBackupViewModel
  src/commonMain/.../QuestLogNavDisplay.kt    + AccountBackupRoute entry; Settings → route lambda
  src/iosMain/.../QuestLogIos.kt              + identityProviderBridge parameter and binding

androidApp/
  build.gradle.kts                           + google-services plugin
  src/main/java/.../QuestLogApp.kt           + Koin property google_web_client_id

iosApp/
  iosApp.xcodeproj                           firebase-ios-sdk → 11.15.0..<12; + GoogleSignIn-iOS 10.0.0; capability
  iosApp/AppDelegate.swift                   FirebaseApp.configure(); pass the bridge to startQuestLog
  iosApp/iOSApp.swift                        .onOpenURL → GIDSignIn
  iosApp/IdentityProviderBridge.swift        new
  iosApp/Info.plist                          + REVERSED_CLIENT_ID URL type
  iosApp/iosApp.entitlements                 new: Sign in with Apple

docs and instructions                        CLAUDE.md (module graph, :core:cloud rule, where-things-live),
                                             core/data/CLAUDE.md (repositories), core/database/CLAUDE.md (UserDataDao),
                                             specs/010-kmp-migration/contracts/platform-contracts.md,
                                             .specify/memory/constitution.md (PATCH: 20 modules, :core:cloud in Principle I),
                                             README.md (Firebase setup), docs/tech-debt.md (test gaps)
```

**Structure Decision**: The existing layered multi-module layout. One module is added at the same tier as
`:core:network`, and the feature lives in the existing `:feature:settings`.

## Implementation order

Each step leaves Android building and `./gradlew test` green (constitution, KMP section).

1. **Spike** (R13, quickstart §1): `:core:cloud` skeleton + GitLive, the google-services plugin, Firebase iOS re-pinned
   to 11.x, `FirebaseApp.configure()`, and a debug-only Firebase read. **Gate**: both platforms build and reach
   Firebase. On failure, stop and re-plan around native SDKs.
2. **Model and domain**: the new `:core:model` types and `RepositoryError` cases with `:core:ui` strings, the
   repository interfaces, the use cases, and `DeleteAccountUseCaseTest`.
3. **Cloud**: `CloudAuth`/`FirebaseCloudAuth`, `CloudBackupStore`/`FirestoreBackupStore`, error classification and its
   test, the Android `PlatformProviderAuth` + `CurrentActivityTracker`, and the iOS bridge interface +
   `IosProviderAuth`.
4. **Database**: `UserDataDao` + `UserDataRows`, confirming the cross-DAO transaction (R10) on Android, then iOS.
5. **Data**: DTOs, mapper and chunker (with tests), cover storage members, both repositories (with tests), error
   mapping, Koin.
6. **UI**: the route, ViewModel (with tests), screen, components, Settings hub row, strings and brand assets.
7. **iOS shell**: Swift bridge, GoogleSignIn package, URL scheme, entitlement, and `startQuestLog` parameter.
8. **Docs and rules**: `firebase/firestore.rules`, README, CLAUDE.md files, platform contracts, constitution PATCH,
   tech-debt.
9. **Validation**: quickstart §2–§5.

## Test cases

### `BackupRepositoryImplTest` (fakes for `CloudAuth`, `CloudBackupStore`, the DAO and cover storage)
- Backup writes every part **before** the manifest, and the manifest last.
- A part write failure leaves the manifest unwritten and returns the mapped error.
- After a successful backup, stale parts are deleted. A failed delete still returns success.
- More than `MAX_BACKUP_PARTS` → `BackupTooLarge`, with nothing written.
- Restore with no manifest → `BackupNotFound`, with no part read.
- Restore with `formatVersion` above the supported one → `BackupFromNewerVersion`, with no part read.
- Restore with a missing part or a wrong length → `BackupUnreadable`, `replaceUserData` not called, imported covers
  deleted.
- `replaceUserData` throwing → imported covers deleted, error returned.
- Successful restore → old cover files deleted after the transaction, and the sweep runs first.
- Signed out → `NotSignedIn`, with no store call.
- Backup and restore are mutually exclusive (a second call waits).

### `BackupMapperTest` / `BackupChunkerTest`
- `UserDataRows → BackupSnapshot → UserDataRows` is identity, except `lastViewedAt` and cover paths.
- An unknown `GameStatus`/`WishlistIcon` name decodes to `null`.
- Chunks are ≤ `PART_MAX_CHARS`, rejoin to the same bytes, and handle non-ASCII titles and an empty stream. A
  length mismatch is rejected.

### `AccountRepositoryImplTest`, `RepositoryErrorMapperTest`, `FirebaseErrorClassifierTest`
- Provider and user mapping.
- Collision → `AccountExistsWithOtherMethod`; already linked → `MethodLinkedToOtherAccount`.
- Cancel → `SignInCancelled`; `RecentLoginRequired` → one re-authentication and retry.
- Firestore `UNAVAILABLE` → connectivity, `UNAUTHENTICATED`/`PERMISSION_DENIED` → unauthenticated,
  `DEADLINE_EXCEEDED` → timeout.

### `DeleteAccountUseCaseTest`
- Order: reauthenticate → delete backup → delete account.
- Each step's failure stops the later steps.

### `AccountBackupViewModelTest`
- Signed out and signed in states come from `ObserveAccountUseCase`; backup info is fetched on sign-in.
- Backup with no local data opens `ConfirmEmptyBackup`; with data it runs directly.
- Restore with local data opens `ConfirmRestore`; cancel runs nothing.
- `runningOperation` blocks a second action and clears after success, failure and cancellation.
- `SignInCancelled` emits no message; any other failure emits its `toUiText()`.
- Delete asks for confirmation and ends signed out.

## Follow-ups (not in this feature)

- The backup reminder dialog (`docs/tech-debt.md`). It needs the last backup time stored on the device, which this
  feature deliberately does not keep.
- GitLive 3.0 stable → revisit the Firebase iOS pin (R2).
- App Check, before the first public release (it blocks unauthorised clients from the project).

## Complexity Tracking

| Item | Why needed | Simpler alternative rejected because |
|---|---|---|
| **New module `:core:cloud`** **(owner)** | Isolates Firebase and the sign-in SDKs, as `:core:network` isolates Ktor; keeps `:core:data` free of Firebase types (Principle II) | Firebase inside `:core:data`: the error boundary would name Firebase types, and every `:core:data` build and test would carry Firebase |
| **Edge `:core:data → :core:cloud`** | The repositories are in `:core:data`, the error boundary | No alternative keeps the boundary in one place |
| **Edge `:shared → :core:cloud`** | `:shared` assembles every Koin module and binds the Swift bridges in `startQuestLog` | Re-exporting through `:core:data` would hide a real dependency |
| **Two new repositories** (`AccountRepository`, `BackupRepository`) **(owner, 2026-10-15)** | Account and backup are unrelated to games; each gets its own fakes | `GameRepository` would grow sign-in methods; a single combined repository was offered and declined |
| **Abstract-class DAO** (`UserDataDao`) | One transaction must also call `GameDao.saveGame` (R10) | An interface DAO cannot call another DAO; duplicating `saveGame` breaks its "always through `saveGame`" rule; orchestrating from the repository breaks `core/database/CLAUDE.md` |
| **Eight new `RepositoryError` cases** | FR-025's categories: account and backup failures the user must tell apart | Folding them into `Unknown` loses FR-004/006/023/024's messages |
| **Firebase iOS re-pin 13.0.1 → 11.x** | GitLive 2.7.0 is built against 11.8.0; GoogleSignIn-iOS needs `app-check` 11 (R2) | Keeping 13.x: unresolvable with GoogleSignIn, and untested with GitLive |
| **Constitution PATCH + CLAUDE.md edits** | Module count (19 → 20), `:core:cloud` added to Principle I's forbidden-for-features list, the repository rule's approval recorded | Leaving the rules stale; the constitution's governance requires the amendment in the same commit as the change |
