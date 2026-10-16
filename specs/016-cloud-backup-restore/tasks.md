---

description: "Task list for cloud backup and restore"
---

# Tasks: Cloud Backup and Restore

**Input**: Design documents from `/specs/016-cloud-backup-restore/`

**Prerequisites**: plan.md, spec.md, research.md (R1–R14), data-model.md, contracts/cloud-contracts.md,
contracts/firestore.md, quickstart.md

**Tests**: Included. The constitution (principle V) requires a test in the module's own `androidHostTest` for new
ViewModel, mapper, use-case and error-mapping logic, and this feature is mostly that logic (the snapshot mapper, the
chunker, the two repositories, the error classification, the account-deletion order, the screen's state machine).
They are JUnit4 + MockK + `kotlinx-coroutines-test` with a `StandardTestDispatcher`, mocking use cases for
ViewModels and using hand-written fakes for the cloud contracts (a fake lives in the test source set of the module that
consumes it, never in `commonMain`). Tests come before the code they cover; a test that names a type that does not exist
yet fails to compile until the code lands, so commit a test and its code together. Not covered by JVM tests, and
checked by previews and the quickstart walkthrough instead: the composables (the project has no Compose UI tests,
`docs/tech-debt.md`), the Room DAO (no DAO tests exist, same file), the Firebase and provider calls, and the Swift
bridge.

**Organization**: Grouped by user story. Setup ends with a **gate** (the iOS spike, research R13). Foundational builds
what every story needs and nothing story-specific: the model types, the whole `:core:cloud` module including both
platforms' provider sign-in, and the error plumbing. Each story then adds its own slice of the two repositories (an
interface member is added by the story that needs it, so no story compiles against a stub), its use cases, its tests
and its UI. Tasks marked **(Mac only)** cannot be compiled or run without a Mac and must not be reported as verified
unless they were run on one.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: Which user story the task belongs to (US1–US4)
- Every path is relative to the repository root. `<pkg>` stands for `com/nikolasguillen/questlog`.

## Rules that apply to every task

- Android must build and pass its tests at every commit: `./gradlew :<module>:compileAndroidMain --console=plain -q`
  after a change, `./gradlew :<module>:testAndroidHostTest --console=plain -q` for a module with tests,
  `./gradlew test` and `./gradlew :androidApp:assembleDebug` at each checkpoint. Never delete or weaken an existing
  test. Where a task says "extend" a test, only cases are added.
- One `data class` / `class` / `enum` / `interface` per file, except a sealed hierarchy keeping its cases in the same
  file (`RepositoryError`, the `AccountBackupUi*` state/event/effect files). New types are `internal` unless another
  module needs them.
- Module boundaries (Constitution I): **no `dev.gitlive`, `com.google.firebase`, `androidx.credentials` or
  `com.google.android.libraries.identity` import outside `:core:cloud`**, and no `feature/*` build file mentions
  `:core:cloud`. `:core:cloud` depends on no project module.
- Exceptions stop at `:core:data` (Constitution II): `:core:cloud` throws only `Cloud*Exception` types, every `catch`
  rethrows `CancellationException` first, and results are built with `AppResult.success` / `AppResult.failure`, never
  the constructors.
- Every user-visible string is a resource (`UiText`), nothing is hardcoded in a model, mapper or composable. Use
  `MaterialTheme.spacing` tokens, never a `dp` literal. Dialogs use `CustomAlertDialog`. Check `core/ui/component/`
  and `core/ui/util/modifiers/` before writing a component or modifier.
- Do not touch `androidApp/google-services.json` or `iosApp/iosApp/GoogleService-Info.plist` beyond reading them, and
  never `git add` them (both are ignored). If Xcode stages the plist, `git restore --staged` it.
- No `Migration`, no entity change, no schema version bump: this feature adds no table (`core/database/CLAUDE.md`).
- Comments and KDoc are in English and say *why*. Every new composable gets a private `@QuestLogPreviews` wrapped in
  `QuestLogTheme { }`.
- Commit messages follow `type(scope): subject` (scopes in `.githooks/commit-msg`: `cloud`, `data`, `domain`,
  `database`, `model`, `ui`, `settings`, `common`, `navigation`, `shared`, `docs`, `build`) and end with the
  `Co-Authored-By` trailer given for this repository.
- Do not fix any entry in `docs/tech-debt.md`, and do not add: automatic or background backup, merge, history,
  search-history or settings backup, email/password sign-in, the reminder dialog.

---

## Phase 1: Setup (build, module, spike)

**Purpose**: Know the starting point, add the module and dependencies, and **prove the riskiest assumption before any
feature code exists**: that GitLive's Firebase wrapper builds and reaches Firebase on both platforms (research R13).

**Prerequisite from the owner** (quickstart "Prerequisites" steps 1, 2 and 4): the Google provider enabled, both config
files re-downloaded after that (they must carry an OAuth client: `CLIENT_ID`/`REVERSED_CLIENT_ID` in the plist, a
`client_type: 3` entry in the json), and a Firestore database created with the rules published. Do not start T007 until
the owner confirms.

- [X] T001 Run `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q` on the
  branch before any change and confirm both are green. If anything is already red, stop and tell the owner instead of
  continuing. Then ask the owner to confirm the prerequisite above.
- [X] T002 In `gradle/libs.versions.toml` add under `[versions]`: `gitliveFirebase = "2.7.0"`,
  `androidxCredentials = "1.6.0"`, `googleId = "1.2.1"`, `googleServices = "4.5.0"`; under `[libraries]`:
  `gitlive-firebase-auth` and `gitlive-firebase-firestore` (`dev.gitlive:firebase-auth` / `firebase-firestore`,
  `version.ref = "gitliveFirebase"`), `androidx-credentials` (`androidx.credentials:credentials`),
  `androidx-credentials-play-services-auth` (`androidx.credentials:credentials-play-services-auth`) on
  `androidxCredentials`, and `googleid` (`com.google.android.libraries.identity.googleid:googleid`, `googleId`);
  under `[plugins]`: `google-services` (`com.google.gms.google-services`, `googleServices`). Keep the file's
  alphabetical-by-group style.
- [X] T003 In the root `build.gradle.kts` add `alias(libs.plugins.google.services) apply false` to the `plugins`
  block, and apply `alias(libs.plugins.google.services)` in `androidApp/build.gradle.kts` (not in the convention
  plugin: `:androidApp` owns `google-services.json` and the application id, research R14). Run
  `./gradlew :androidApp:assembleDebug --console=plain -q`: it must still pass, and the task that merges the json's
  values into resources must have run.
- [X] T004 In `settings.gradle.kts` add `include(":core:cloud")` next to the other `:core:*` includes, and add `cloud` to
  `SCOPES` in `.githooks/commit-msg` (CLAUDE.md: the hook's list is authoritative), keeping the list's format.
- [X] T005 Create `core/cloud/build.gradle.kts` applying `id("questlog.kmp.library")` and
  `alias(libs.plugins.jetbrains.kotlin.plugin.serialization)` (copy the shape of `core/network/build.gradle.kts`).
  `commonMain.dependencies`: `libs.gitlive.firebase.auth`, `libs.gitlive.firebase.firestore`, `libs.koin.core`,
  `libs.kotlinx.coroutines.core`, `libs.kotlinx.serialization.core`, `libs.kotlinx.serialization.json`; **no
  `project(...)` dependency**. `androidMain.dependencies`: `libs.androidx.credentials`,
  `libs.androidx.credentials.play.services.auth`, `libs.googleid`, `libs.koin.android`,
  `libs.kotlinx.coroutines.android`. `androidHostTest.dependencies`: `libs.junit`, `libs.kotlinx.coroutines.test`,
  `libs.mockk` (as `core/data/build.gradle.kts`). Create the directory
  `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/` with a placeholder `package-info`-free file only if Gradle
  needs a source to compile. Run `./gradlew :core:cloud:compileAndroidMain --console=plain -q`.
- [X] T006 [P] **(Mac only)** Re-pin the Firebase iOS SDK (research R2). In Xcode, Project → Package Dependencies →
  `firebase-ios-sdk`: set the rule to **Up to Next Major Version** with `11.15.0`, then **File → Packages → Reset
  Package Caches** and **Resolve Package Versions** if resolution fails (the cache issue seen earlier). Confirm
  `iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved` lists `firebase-ios-sdk`
  `11.15.0` and `app-check` `11.x`, and that `FirebaseAuth` and `FirebaseFirestore` are still on the `iosApp` target.
  Commit `project.pbxproj` and `Package.resolved` only (`build: re-pin firebase-ios-sdk to 11.x for GitLive`). Verify
  with `git status` that the plist is not staged.
- [X] T007 Create the temporary probe `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/CloudSpike.kt`: a public
  `object CloudSpike { suspend fun run(): String }` that returns a one-line report of
  `Firebase.auth.currentUser?.uid`, and the outcome of
  `Firebase.firestore.document("users/spike").get(Source.SERVER)` (exists, or the caught
  `FirebaseFirestoreException.code`, or the exception class name for anything else). It exists only to pass the gate:
  T011 deletes it.
- [X] T008 In `shared/build.gradle.kts` add `implementation(project(":core:cloud"))` to `commonMain.dependencies` (the
  permanent edge, plan Complexity Tracking). In `shared/src/commonMain/kotlin/<pkg>/shared/di/SharedKoin.kt`, at the
  end of `initKoin`, add a temporary `if (isDebugBuild)` block that launches
  `println("CloudSpike: " + CloudSpike.run())` on `koin.get<CoroutineScope>()` (the app scope `dataModule` registers).
  Run `./gradlew :androidApp:assembleDebug --console=plain -q`.
- [X] T009 **(Mac only to verify)** In `iosApp/iosApp/AppDelegate.swift` add `import FirebaseCore` and make
  `FirebaseApp.configure()` the first statement of `application(_:didFinishLaunchingWithOptions:)`, before
  `ReleaseRefreshTask.register()` and `startQuestLog`. This line is permanent. If `import FirebaseCore` does not
  resolve, add the `FirebaseCore` product to the `iosApp` target. Update the type's KDoc comment to mention that
  Firebase starts first and why (providers and the notification delegate may need it at launch).
- [X] T010 **GATE (Mac for iOS)**. Run quickstart §1 in full: `./gradlew :androidApp:assembleDebug`,
  `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`, then the `xcodebuild` command. Run the Android app and the
  iOS app (simulator is enough) and read the `CloudSpike:` line on each. **Pass**: both builds succeed and both lines
  show Firebase answered (a missing document or a `PERMISSION_DENIED` is a valid answer; a link error, a crash on the
  first Firebase call, or `FirebaseApp` not configured is not). Also record, for later tasks, the `code` strings
  GitLive reports for a Firestore failure on each platform by running once in airplane mode. **If iOS fails, STOP**:
  report the exact error to the owner. The plan then switches to native Firebase SDKs behind the same `CloudAuth` and
  `CloudBackupStore` interfaces (research R1) before anything else is built, which changes `:core:cloud`'s
  dependencies and this task list.
- [X] T011 Delete `CloudSpike.kt` and the temporary block in `SharedKoin.kt` (keep the `:core:cloud` dependency from
  T008 and the Swift line from T009). Run `./gradlew :androidApp:assembleDebug --console=plain -q` and commit
  (`build(cloud): add the cloud module and Firebase dependencies`) with T002–T005, T008's dependency line and T009.

**Checkpoint**: Android builds, iOS links and reaches Firebase, the module exists and is empty.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: Everything every story needs: the `:core:model` types, the error cases and their text, and the complete
`:core:cloud` module (contracts, Firebase implementations, Android and iOS provider sign-in, Koin), so that no story
touches Firebase code again.

**⚠️ CRITICAL**: No user story starts until this phase is complete.

### Model and errors

- [X] T012 [P] Create `core/model/src/commonMain/kotlin/<pkg>/core/model/AuthProvider.kt`:
  `enum class AuthProvider { GOOGLE, APPLE }`, with a one-line KDoc.
- [X] T013 [P] Create `core/model/src/commonMain/kotlin/<pkg>/core/model/Account.kt`:
  `data class Account(val id: String, val email: String?, val providers: Set<AuthProvider>)` with KDoc quoting the
  data model: `id` is the backend's user id and is never shown; `email` is null when no linked method shares one;
  `providers` is never empty while signed in.
- [X] T014 [P] Create `core/model/src/commonMain/kotlin/<pkg>/core/model/BackupInfo.kt`:
  `data class BackupInfo(val createdAt: kotlin.time.Instant, val listCount: Int, val gameCount: Int)`. `:core:model`
  keeps no Android or Compose dependency.
- [X] T015 Add eight cases to `core/model/src/commonMain/kotlin/<pkg>/core/model/RepositoryError.kt`, each a
  `data object` with a KDoc that says *when* it occurs (data-model.md table): `NotSignedIn`, `SignInCancelled`,
  `AccountExistsWithOtherMethod`, `MethodLinkedToOtherAccount`, `BackupNotFound`, `BackupFromNewerVersion`,
  `BackupUnreadable`, `BackupTooLarge`. The file does not compile elsewhere until T016 handles them, so land T015 and
  T016 together.
- [X] T016 In `core/ui/src/commonMain/kotlin/<pkg>/core/ui/mapper/ErrorMapper.kt` add the eight branches to the
  exhaustive `when` (`SignInCancelled` maps to `error_unknown` only for exhaustiveness and is never shown, with a
  comment saying so). Add seven strings to `core/ui/src/commonMain/composeResources/values/strings.xml`:
  `error_not_signed_in` ("Sign in again to continue"), `error_account_exists_other_method` ("This email is already
  registered with another sign-in method. Sign in with that method, then link this one from Settings."),
  `error_method_linked_other_account` ("That sign-in method already belongs to another account."),
  `error_backup_not_found` ("There is no backup to restore."), `error_backup_newer_version` ("This backup was made by
  a newer version of the app. Update the app to restore it."), `error_backup_unreadable` ("The backup can't be read.
  Nothing was changed."), `error_backup_too_large` ("Your collection is too large to back up."). Wording is a
  starting point; keep it plain and name the cause category (SC-008).

### `:core:cloud`: types and errors

- [X] T017 [P] Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/model/CloudProvider.kt`
  (`enum class CloudProvider { GOOGLE, APPLE }`, its own enum because `:core:cloud` does not depend on `:core:model`)
  and `.../model/CloudUser.kt` (`data class CloudUser(val uid: String, val email: String?, val providers:
  Set<CloudProvider>)`).
- [X] T018 [P] Create the three `@Serializable` documents in `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/model/`,
  one file each, with exactly the fields in data-model.md: `BackupManifestDocument(snapshotId: String,
  formatVersion: Int, createdAtEpochMillis: Long, appVersion: String, listCount: Int, gameCount: Int, streams:
  List<BackupStreamDocument>)`, `BackupStreamDocument(name: String, byteLength: Long, partIds: List<String>)`
  (`name` is `"data"` or `"cover-<listId>"`; `byteLength` is the decoded length, checked on restore) and
  `BackupPartDocument(snapshotId: String, stream: String, index: Int, data: String)`. KDoc on
  `BackupPartDocument.data` quotes the constraint: base64, **at most 700,000 characters**; and the part id format
  `"$snapshotId-$stream-$index"`. The 700,000 bound is enforced by `:core:data` (T050), not here.
- [X] T019 [P] Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/error/CloudException.kt`
  (`open class CloudException(message: String?, cause: Throwable? = null) : Exception(message, cause)`) and one file per
  subclass in the same package: `CloudConnectivityException`, `CloudTimeoutException`,
  `CloudUnauthenticatedException`, `CloudSignInCancelledException`, `CloudCredentialCollisionException`,
  `CloudCredentialAlreadyLinkedException`, `CloudRecentLoginRequiredException`. They extend `Exception`, **not**
  `IOException`, so `RepositoryErrorMapper`'s ordering rule about `IOException` is unaffected.
- [X] T020 Write `core/cloud/src/androidHostTest/kotlin/<pkg>/core/cloud/error/FirebaseErrorClassifierTest.kt` first
  (JUnit4, no Firebase objects: the classifier takes plain codes). Cases: Firestore `UNAVAILABLE` →
  `CloudConnectivityException`; `DEADLINE_EXCEEDED` → `CloudTimeoutException`; `UNAUTHENTICATED` and
  `PERMISSION_DENIED` → `CloudUnauthenticatedException`; any other code → `null` (the caller rethrows it). Auth:
  `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL` and `ERROR_EMAIL_ALREADY_IN_USE` → collision;
  `ERROR_CREDENTIAL_ALREADY_IN_USE` and `ERROR_PROVIDER_ALREADY_LINKED` → already linked;
  `ERROR_REQUIRES_RECENT_LOGIN` → recent login required; `ERROR_NETWORK_REQUEST_FAILED` → connectivity;
  `ERROR_USER_TOKEN_EXPIRED`, `ERROR_INVALID_USER_TOKEN`, `ERROR_USER_NOT_FOUND`, `ERROR_USER_DISABLED` →
  unauthenticated; an unknown or `null` code → `null`. Add the iOS-side code strings recorded in T010 as extra cases.
- [X] T021 Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/error/FirebaseErrorClassifier.kt`: pure
  `internal fun classifyFirestoreCode(code: FirestoreExceptionCode): CloudException?` and
  `internal fun classifyAuthCode(code: String?): CloudException?` (and a `Throwable.toCloudException()` extension that
  applies them to `FirebaseFirestoreException` / `FirebaseAuthException`, rethrows `CancellationException` first, and
  returns the original throwable unchanged when nothing matches). Verify the auth code strings against GitLive's
  `FirebaseAuthException.code` on **both** platforms (the strings from T010's run); if iOS differs, classify both.
  T020 passes.

### `:core:cloud`: contracts and Firebase implementations

- [X] T022 Create the contracts in `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/`: `CloudAuth.kt` (exactly the
  interface in contracts/cloud-contracts.md: `currentUser: Flow<CloudUser?>`, `signIn`, `link`, `reauthenticate`,
  `signOut`, `deleteUser`), `AppleGrant.kt` (`class AppleGrant internal constructor(internal val value: String)`) and
  `PlatformProviderAuth.kt` (`internal interface`, five members as in the contract). Public KDoc on every `CloudAuth`
  member states what it throws and the "`link` on an already linked provider returns the current user unchanged" rule.
- [X] T023 Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/FirebaseCloudAuth.kt` implementing `CloudAuth` with
  GitLive: `currentUser` = `Firebase.auth.authStateChanged` mapped to `CloudUser` (provider ids `"google.com"` →
  `GOOGLE`, `"apple.com"` → `APPLE`, from `FirebaseUser.providerData`; ignore `"firebase"`); `signIn`/`link` call
  `PlatformProviderAuth` then return the current user; `reauthenticate` picks Apple when `providerData` contains it,
  else Google, and keeps the returned `AppleGrant` in a private field; `signOut` calls `Firebase.auth.signOut()` then
  `clearProviderState()` and drops the grant; `deleteUser` calls `revokeApple(grant)` (a failure is logged with
  `println`-free logging the module already uses, never thrown), then `FirebaseUser.delete()`, throwing
  `CloudRecentLoginRequiredException` when no re-authentication happened. Every GitLive call is wrapped with
  `Throwable.toCloudException()` (T021).
- [X] T024 Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/CloudBackupStore.kt`: the interface in
  contracts/cloud-contracts.md (`readManifest`, `writePart`, `readPart`, `writeManifest`, `listPartIds`,
  `deleteParts`, `deleteManifest`, all taking `uid`).
- [X] T025 Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/FirestoreBackupStore.kt` implementing it over
  GitLive with the exact paths in contracts/firestore.md: manifest at `users/{uid}`, parts at
  `users/{uid}/parts/{partId}`, serialized with the documents' `@Serializable` strategies. `readManifest` uses
  `Source.SERVER`. Each call is wrapped in `withTimeout(CLOUD_CALL_TIMEOUT)` (a `internal val CLOUD_CALL_TIMEOUT =
  30.seconds` in `CloudConstants.kt`) mapping the timeout to `CloudTimeoutException`, and in
  `Throwable.toCloudException()`. `deleteParts` deletes in `WriteBatch`es of at most 400 documents. Disable on-device
  persistence (memory cache only) through GitLive's Firestore settings if it exposes the option, otherwise through
  `.android` / `.ios`; if neither is practical, leave it on, rely on `Source.SERVER`, and record the deviation in
  research.md R11.
- [X] T026 Create `core/cloud/src/commonMain/kotlin/<pkg>/core/cloud/di/CloudKoin.kt` (`val cloudModule = module { ... }`
  binding `CloudAuth` → `FirebaseCloudAuth` and `CloudBackupStore` → `FirestoreBackupStore` as `single`s) and
  `.../di/CloudPlatformModule.kt` (`expect val cloudPlatformModule: Module`, KDoc listing what each platform binds).

### `:core:cloud`: Android provider sign-in

- [X] T027 [P] Create `core/cloud/src/androidMain/kotlin/<pkg>/core/cloud/CurrentActivityTracker.kt`: an
  `Application.ActivityLifecycleCallbacks` holding the resumed `Activity` in a `WeakReference`, with
  `val current: Activity?`. KDoc says why it exists (both provider flows need an Activity, and no contract signature
  may carry one).
- [X] T028 Create `core/cloud/src/androidMain/kotlin/<pkg>/core/cloud/AndroidProviderAuth.kt` implementing
  `PlatformProviderAuth`, **Google part**: `signIn`, `link` and `reauthenticate(GOOGLE)` get an ID token with
  Credential Manager (`CredentialManager.create(context).getCredential(activity, GetCredentialRequest(listOf(
  GetSignInWithGoogleOption.Builder(webClientId).build())))`, `GoogleIdTokenCredential.createFrom(...)`), build
  `GoogleAuthProvider.credential(idToken, null)` and call GitLive's `signInWithCredential` / `linkWithCredential` /
  `reauthenticate`. `GetCredentialCancellationException` and "no resumed Activity" →
  `CloudSignInCancelledException`; `NoCredentialException` → `CloudSignInCancelledException` too (no Google account on the
  device, shown as nothing, the user adds one). `clearProviderState` calls `clearCredentialState`.
- [X] T029 In the same file add the **Apple part**: `OAuthProvider.newBuilder("apple.com")` with scopes `email` and
  `name`, through GitLive's `.android` properties: `startActivityForSignInWithProvider` /
  `startActivityForLinkWithProvider` / `startActivityForReauthenticateWithProvider` on the tracked Activity, checking
  `pendingAuthResult` first for an Activity recreated mid-flow. `reauthenticate(APPLE)` returns
  `AppleGrant((authResult.credential as OAuthCredential).accessToken)`; `revokeApple` calls
  `FirebaseAuth.getInstance().revokeAccessToken(token)`. Cancellation (`FirebaseAuthWebException` with a cancelled
  code, or a dismissed Custom Tab) → `CloudSignInCancelledException`. Wrap each Firebase `Task` with
  `suspendCancellableCoroutine` (or `kotlinx-coroutines-play-services` only if it is already a dependency; do not add
  one).
- [X] T030 Create `core/cloud/src/androidMain/kotlin/<pkg>/core/cloud/di/CloudPlatformModule.android.kt`
  (`actual val cloudPlatformModule`): `single(createdAtStart = true) { CurrentActivityTracker().also {
  (androidContext() as Application).registerActivityLifecycleCallbacks(it) } }` and `single<PlatformProviderAuth> {
  AndroidProviderAuth(androidContext(), get(), getProperty("google_web_client_id")) }`. In
  `androidApp/src/main/java/<pkg>/QuestLogApp.kt`, pass the property from the generated resource inside the existing
  `initKoin { }` block: `properties(mapOf("google_web_client_id" to getString(R.string.default_web_client_id)))`
  (a compile-time reference, so resource shrinking cannot strip it, research R4).

### `:core:cloud`: iOS provider sign-in

- [X] T031 [P] Create in `core/cloud/src/iosMain/kotlin/<pkg>/core/cloud/` one file each: `IdentityProviderBridge.kt`
  (the interface in contracts/cloud-contracts.md, **nothing `suspend`**, KDoc quoting the reason from the platform
  contracts: a `suspend` member implemented in Swift has no cancellation and crashes on a thrown Swift error),
  `GoogleSignInResult.kt`, `AppleSignInResult.kt`, `ProviderOutcome.kt` (`SUCCESS`, `CANCELLED`, `FAILED`). `onResult`
  is documented as called exactly once, on any thread.
- [X] T032 Create `core/cloud/src/iosMain/kotlin/<pkg>/core/cloud/IosProviderAuth.kt` implementing
  `PlatformProviderAuth` over the bridge: each call wrapped in `suspendCancellableCoroutine`, `CANCELLED` →
  `CloudSignInCancelledException`, `FAILED` → an `Exception` the caller maps to `Unknown`. Google result →
  `GoogleAuthProvider.credential(idToken, accessToken)`; Apple result → `OAuthProvider.credential(providerId =
  "apple.com", idToken = ..., rawNonce = ...)`; both go to GitLive's `signInWithCredential` / `linkWithCredential` /
  `reauthenticate`. `reauthenticate(APPLE)` returns `AppleGrant(authorizationCode)`; `revokeApple` calls
  `bridge.revokeAppleToken`. Also create `.../di/CloudPlatformModule.ios.kt` (`actual val cloudPlatformModule`) binding
  `single<PlatformProviderAuth> { IosProviderAuth(get<IdentityProviderBridge>()) }`; the bridge itself is bound by
  `startQuestLog` (T034), like `AppleLanguageModelBridge`.
- [X] T033 **(Mac only)** Create `iosApp/iosApp/IdentityProviderBridge.swift` implementing Kotlin's
  `IdentityProviderBridge`: Google through `GoogleSignIn-iOS` (`GIDSignIn.sharedInstance.signIn(withPresenting:)` from the
  top view controller, `clientID` from `FirebaseApp.app()?.options.clientID`, returning ID and access tokens), Apple
  through `ASAuthorizationController` with a random raw nonce (a 32-character cryptographically random string; send
  its SHA-256 hex via CryptoKit as `request.nonce`; return the raw nonce, the identity token and the authorization code),
  `revokeAppleToken` through `Auth.auth().revokeToken(withAuthorizationCode:)`, and `signOutGoogle` through
  `GIDSignIn.sharedInstance.signOut()`. Add the `GoogleSignIn-iOS` package (`https://github.com/google/GoogleSignIn-iOS`,
  Up to Next Major from `10.0.0`, product `GoogleSignIn`) to the `iosApp` target. In `iosApp/iosApp/Info.plist` add a URL
  type whose scheme is the plist's `REVERSED_CLIENT_ID`. Add the **Sign in with Apple** capability (creates
  `iosApp/iosApp/iosApp.entitlements`). In `iosApp/iosApp/iOSApp.swift` handle the redirect with
  `.onOpenURL { GIDSignIn.sharedInstance.handle($0) }`. Presentation, the nonce and the client id stay in Swift; no
  token is logged.
- [X] T034 **(Mac only to verify)** In `shared/src/iosMain/kotlin/<pkg>/shared/QuestLogIos.kt` add an
  `identityProviderBridge: IdentityProviderBridge` parameter to `startQuestLog` and bind it in the `modules(module { })`
  block next to the other bridges (`single<IdentityProviderBridge> { identityProviderBridge }`); update the KDoc. In
  `iosApp/iosApp/AppDelegate.swift` pass `IdentityProviderBridge()` to the call. Run
  `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command from CLAUDE.md.

### `:shared` and `:core:data` plumbing

- [X] T035 In `shared/src/commonMain/kotlin/<pkg>/shared/di/SharedKoin.kt` add `cloudModule` and `cloudPlatformModule`
  (imports from `<pkg>.core.cloud.di`) to `allModules`, before `dataModule`. In
  `shared/src/androidHostTest/kotlin/<pkg>/shared/di/KoinGraphTest.kt` add `Application::class` and `Activity::class`
  to `extraTypes` if `verify()` reports them missing (Android framework objects supplied at runtime, like `Context`);
  add nothing else. `./gradlew :shared:testAndroidHostTest --console=plain -q` stays green.
- [X] T036 Extend `core/data/src/androidHostTest/kotlin/<pkg>/core/data/repository/RepositoryErrorMapperTest.kt` first with
  the six mappings in data-model.md: `CloudConnectivityException` → `NoNetwork`, `CloudTimeoutException` →
  `RequestTimeout`, `CloudUnauthenticatedException` → `NotSignedIn`, `CloudSignInCancelledException` →
  `SignInCancelled`, `CloudCredentialCollisionException` → `AccountExistsWithOtherMethod`,
  `CloudCredentialAlreadyLinkedException` → `MethodLinkedToOtherAccount`; and that a `CancellationException` is still
  rethrown.
- [X] T037 In `core/data/build.gradle.kts` add `implementation(project(":core:cloud"))` and the serialization plugin
  (`alias(libs.plugins.jetbrains.kotlin.plugin.serialization)`) plus `libs.kotlinx.serialization.core` and
  `libs.kotlinx.serialization.json` to `commonMain.dependencies`. In
  `core/data/src/commonMain/kotlin/<pkg>/core/data/repository/RepositoryErrorMapper.kt` add the six `Cloud*Exception`
  branches (above the catch-all; `CloudCredentialCollisionException` and
  `CloudCredentialAlreadyLinkedException` map as in T036). `RecentLoginRequired` is deliberately **not** mapped: it never
  escapes `AccountRepositoryImpl` (T087). Update the "Mapped cases" paragraph of `core/data/CLAUDE.md` in T094, not here.
  T036 passes.
- [X] T038 Checkpoint: `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`
  green; **(Mac only)** `:shared:linkDebugFrameworkIosSimulatorArm64` and `xcodebuild` green. `git grep -nE
  "dev\.gitlive|com\.google\.firebase|androidx\.credentials" -- ':!core/cloud'` prints nothing.

**Checkpoint**: Foundation ready. The cloud module is complete and bound, nothing uses it yet. User stories can start.

---

## Phase 3: User Story 1 - Back up my wishlist to my account (Priority: P1) 🎯 MVP

**Goal**: A user signs in with Google or Apple from a new Account & backup screen, taps "Back up now", and sees when
the last backup happened. Backing up again replaces the previous copy. The app is usable without signing in.

**Independent Test**: Quickstart §4 rows 1–8 and 20 on Android: signed-out explainer and buttons (rows 1–2), a cancelled
sign-in leaves no error (3), a completed sign-in (4), a backup with covers shows the date and counts (5), a second
backup replaces the first in the console (6), a double tap runs one backup (7), airplane mode fails cleanly and the
manifest is unchanged (8), and an empty device asks first (20).

### Tests for User Story 1 (write first)

- [X] T039 [P] [US1] Create `core/data/src/androidHostTest/kotlin/<pkg>/core/data/backup/BackupChunkerTest.kt`: chunks are
  at most `PART_MAX_CHARS` (**700,000** characters), rejoin to the identical bytes, an empty byte array yields one empty
  part (or zero parts, whichever T051 documents, asserted either way), non-ASCII text in the source survives, and
  `joinParts` rejects a result whose decoded length differs from `expectedLength` with `BackupUnreadable`-mapped
  failure (assert on the exception type T051 throws).
- [X] T040 [P] [US1] Create `core/data/src/androidHostTest/kotlin/<pkg>/core/data/mapper/BackupMapperTest.kt`:
  `UserDataRows.toBackupSnapshot(...)` carries every field (a list with an icon and a cover, a game with every child
  collection, an owned platform, the default list id, a membership); `lastViewedAt` is **not** in the snapshot; a
  `GameStatus` or `WishlistIcon` name this build does not know decodes to `null`; a list whose cover file is missing is
  written with `hasCover = false`. (The inverse mapping is added in US2.)
- [X] T041 [P] [US1] Create `core/data/src/androidHostTest/kotlin/<pkg>/core/data/repository/AccountRepositoryImplTest.kt`
  with a hand-written `FakeCloudAuth` in the same source set: `observeAccount` maps `CloudUser` → `Account` and `null`
  → `null`; `signIn(GOOGLE)` / `signIn(APPLE)` return the mapped account; a `CloudSignInCancelledException` becomes
  `RepositoryError.SignInCancelled`; a `CloudCredentialCollisionException` becomes `AccountExistsWithOtherMethod`; a
  `CancellationException` is rethrown, not mapped.
- [X] T042 [P] [US1] Create `core/data/src/androidHostTest/kotlin/<pkg>/core/data/repository/BackupRepositoryImplTest.kt`
  with `FakeCloudAuth`, a `FakeCloudBackupStore` (records the order of calls, can be told to fail the nth write), a
  fake cover storage and a MockK `UserDataDao`: **every part is written before the manifest, and the manifest last**; a
  part write that fails leaves `writeManifest` uncalled and returns the mapped error; after a successful backup the
  parts the new manifest does not list are deleted, and a failing delete still returns success; more than
  `MAX_BACKUP_PARTS` (**40**) parts returns `BackupTooLarge` with nothing written; signed out returns `NotSignedIn`
  with no store call; `getBackupInfo` returns `Success(null)` when there is no manifest and the mapped `BackupInfo`
  when there is; `hasLocalUserData` delegates to the DAO; two concurrent `backUp` calls run one after the other (the
  `Mutex`).
- [X] T043 [P] [US1] Create `feature/settings/src/androidHostTest/kotlin/<pkg>/feature/settings/AccountBackupViewModelTest.kt`
  (KDoc header, `StandardTestDispatcher`, `Dispatchers.setMain`/`resetMain`, use cases mocked as
  `SettingsViewModelTest` does): signed-out state when `ObserveAccountUseCase` emits `null`; sign-in success; sign-in
  failure emits `ShowMessage(error.toUiText())`; `SignInCancelled` emits **no** message (FR-027); once signed in the
  backup info is fetched and exposed as `None` or `Available`; "Back up now" with local data runs `BackUpUseCase`
  directly; with **no** local data it opens `ConfirmEmptyBackup` and runs nothing until `ConfirmEmptyBackup`;
  `DismissDialog` closes it; `runningOperation` is set while a backup runs, a second `BackUp` event during it is
  ignored, and it is cleared after success, after failure and after cancellation (FR-026); a backup failure with
  `NotSignedIn` shows its message.

### Implementation for User Story 1

- [X] T044 [US1] Create `core/domain/src/commonMain/kotlin/<pkg>/core/domain/repository/AccountRepository.kt` with
  `fun observeAccount(): Flow<Account?>` and `suspend fun signIn(provider: AuthProvider): AppResult<Account>`, and
  `.../repository/BackupRepository.kt` with `suspend fun getBackupInfo(): AppResult<BackupInfo?>` ("`Success(null)`:
  signed in, no backup yet"), `suspend fun backUp(): AppResult<BackupInfo>` and `suspend fun hasLocalUserData(): Boolean`
  ("DB-only, hence no `AppResult`: anything beyond a fresh install's seeded default wishlist"). KDoc on each says what
  it returns for a signed-out caller. These are the owner-approved second and third repositories (plan Complexity
  Tracking); later stories add their members here.
- [X] T045 [P] [US1] Create the use cases in `core/domain/src/commonMain/kotlin/<pkg>/core/domain/usecase/account/`
  (`ObserveAccountUseCase`, `SignInUseCase`) and `.../usecase/backup/` (`GetBackupInfoUseCase`, `BackUpUseCase`,
  `HasLocalUserDataUseCase`): plain classes with `operator fun invoke(...)`, no base type. Register each with
  `factoryOf` in `core/domain/src/commonMain/kotlin/<pkg>/core/domain/di/DomainKoin.kt`, next to its neighbours.
- [X] T046 [US1] Create `core/database/src/commonMain/kotlin/<pkg>/core/database/userdata/UserDataRows.kt` exactly as in
  data-model.md (`lists: List<ListEntity>`, `defaultListId: Long`, `memberships: List<GameListCrossRef>`,
  `ownedPlatformIds: List<Int>`, `ownedPlatforms: List<PlatformEntity>`, `savedGames: List<GameWithAllDetails>`). It
  lives in `userdata/`, not `entity/` or `relation/`: `entity/` must match the exported schema and `relation/` holds
  query results.
- [X] T047 [US1] Create `core/database/src/commonMain/kotlin/<pkg>/core/database/dao/UserDataDao.kt` as
  `@Dao abstract class UserDataDao(private val database: QuestLogDatabase)` (research R10: an interface DAO cannot call
  another DAO). Read half: `@Transaction open suspend fun readUserData(): UserDataRows` reading, in one transaction,
  every `ListEntity`, the default list id, every `GameListCrossRef`, the owned platform ids with their `PlatformEntity`
  rows, and the saved games with **exactly `GameDao.getSavedGames`'s predicate** (in some list, **or** with a status,
  **or** with a priority) as a `suspend` `@Transaction @Query` returning `List<GameWithAllDetails>`; and
  `suspend fun hasUserData(): Boolean` true when there is a saved game, **or more than one list**, or any owned platform.
  KDoc explains why the seeded default wishlist alone is not user data. Add `abstract fun userDataDao(): UserDataDao` to
  `core/database/src/commonMain/kotlin/<pkg>/core/database/QuestLogDatabase.kt` and bind it in
  `core/database/src/commonMain/kotlin/<pkg>/core/database/di/DatabaseKoin.kt` next to the other DAOs. **No entity
  changes.** Run `./gradlew :core:database:compileAndroidMain --console=plain -q` and confirm
  `core/database/schemas/` is unchanged (`git status`).
- [X] T048 [P] [US1] In `core/common/src/commonMain/kotlin/<pkg>/core/common/DateUtils.kt` add
  `formatDateTime(instant: Instant, dateStyle: DateStyle, timeStyle: TimeStyle): String`; create
  `.../common/TimeStyle.kt` (`enum class TimeStyle { SHORT, MEDIUM }`); declare
  `internal expect fun renderLocalDateTime(dateTime: LocalDateTime, dateStyle: DateStyle, timeStyle: TimeStyle): String`
  beside `renderLocalDate` in `.../common/DateRendering.kt`, with the `actual`s in
  `core/common/src/androidMain/kotlin/<pkg>/core/common/DateRendering.android.kt`
  (`DateTimeFormatter.ofLocalizedDateTime(...)` with the default locale) and
  `core/common/src/iosMain/kotlin/<pkg>/core/common/DateRendering.ios.kt` (`NSDateFormatter` with `dateStyle`/`timeStyle`
  mapped and `NSLocale.currentLocale`). Extend `core/common/src/androidHostTest/.../DateUtilsTest.kt` with one case that
  formats a fixed instant in a fixed time zone. Dates use `kotlinx-datetime`, not `java.time`, in `commonMain`.
- [X] T049 [P] [US1] Add `read(path: String): ByteArray?` to
  `core/data/src/commonMain/kotlin/<pkg>/core/data/local/WishlistCoverImageStorage.kt` ("the stored bytes, `null` if
  missing, and only for files inside the covers directory, the same guard `delete` uses") and implement it in
  `core/data/src/androidMain/kotlin/<pkg>/core/data/local/WishlistCoverImageStorageImpl.kt` and
  `core/data/src/iosMain/kotlin/<pkg>/core/data/local/WishlistCoverImageStorageImpl.kt` (`NSData(contentsOfFile:)`).
  Update any test double of the interface so existing tests still compile (their assertions do not change).
- [X] T050 [US1] Create `core/data/src/commonMain/kotlin/<pkg>/core/data/backup/BackupConstants.kt` with
  `internal const val BACKUP_FORMAT_VERSION = 1`, `internal const val PART_MAX_CHARS = 700_000` ("~33% headroom under
  1 MiB for the other fields and encoding overhead", research R6) and `internal const val MAX_BACKUP_PARTS = 40`
  ("about 28 MB of base64: beyond a plausible collection, refused with `BackupTooLarge`"). Create the `@Serializable`
  DTOs in `core/data/src/commonMain/kotlin/<pkg>/core/data/backup/model/`, one file each, fields exactly as in
  data-model.md: `BackupSnapshot` (`formatVersion`, `createdAtEpochMillis`, `appVersion`, `lists`, `defaultListId`,
  `games`, `memberships`, `ownedPlatformIds`, `ownedPlatforms`), `BackupList` (`id`, `name`, `description`, `icon:
  String?`, `hasCover: Boolean`), `BackupGame` (every `GameEntity` field **except** `lastViewedAt`, with `status` as a
  `String?` name, plus `platforms`, `genres`, `companies`, `engines`, `artworks`, `relatedGames`),
  `BackupGamePlatform` (`PlatformEntity` fields + `releaseDate`, `releaseDatePrecision`), `BackupGenre`, `BackupEngine`
  (id, name), `BackupCompany` (id, name, `isDeveloper`, `isPublisher`), `BackupArtwork` (`position`, `url`),
  `BackupRelatedGame` (`relatedGameId`, `name`, `coverUrl`, `relationType`), `BackupPlatform` (`PlatformEntity`
  fields), `BackupMembership` (`gameId`, `listId`). All are `internal`.
- [X] T051 [US1] Create `core/data/src/commonMain/kotlin/<pkg>/core/data/backup/BackupChunker.kt`:
  `internal fun ByteArray.toParts(maxChars: Int = PART_MAX_CHARS): List<String>` (base64, then cut so every part is at
  most `maxChars` characters; base64 is ASCII, so characters equal bytes) and
  `internal fun List<String>.joinParts(expectedLength: Long): ByteArray` (join, decode, throw a typed
  `BackupUnreadableException` (a private `internal class` in the same package, mapped to `BackupUnreadable` by the
  repository) when the decoded length differs). Use `kotlin.io.encoding.Base64` (stdlib), no new dependency. T039 passes.
- [X] T052 [US1] Create `core/data/src/commonMain/kotlin/<pkg>/core/data/mapper/BackupMapper.kt`:
  `internal fun UserDataRows.toBackupSnapshot(createdAt: Instant, appVersion: String, coverListIds: Set<Long>):
  BackupSnapshot` as top-level extensions (`core/data/CLAUDE.md`), enums written as `name`. Create
  `.../mapper/AccountMapper.kt`: `CloudUser.toAccount()`, `CloudProvider.toAuthProvider()` and
  `AuthProvider.toCloudProvider()`. T040 passes.
- [X] T053 [US1] Create `core/data/src/commonMain/kotlin/<pkg>/core/data/repository/AccountRepositoryImpl.kt`
  (`observeAccount`, `signIn`; every call in `try/catch` building results with `AppResult.success` /
  `AppResult.failure(e.toRepositoryError())`, `CancellationException` rethrown) and
  `.../repository/BackupRepositoryImpl.kt` with `getBackupInfo`, `backUp`, `hasLocalUserData` and a private `Mutex` the
  later operations share. `backUp` follows research R6 exactly: uid from `cloudAuth.currentUser.first()` (`null` →
  `NotSignedIn`); `readManifest` from the server first (fails fast offline); `readUserData()`; the snapshot JSON as one
  `data` stream plus one `cover-<listId>` stream per list whose `coverImagePath` reads back non-null; each stream cut
  with `toParts()`; if the total part count exceeds `MAX_BACKUP_PARTS` return `BackupTooLarge` **before any write**; a
  random `snapshotId`; write every part sequentially; then `writeManifest` (**the commit point**); then, best effort,
  delete the parts under `users/{uid}/parts` the new manifest does not list. Constructor takes `CloudAuth`,
  `CloudBackupStore`, `UserDataDao`, `WishlistCoverImageStorage`, `AppVersionProvider`, a `Clock` and a `Json`. Use
  `kotlin.uuid.Uuid.random()` for the id (opt in if it is still experimental in 2.4.20). T041 and T042 pass.
- [X] T054 [US1] In `core/data/src/commonMain/kotlin/<pkg>/core/data/di/DataKoin.kt` bind
  `singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }`, `singleOf(::BackupRepositoryImpl) {
  bind<BackupRepository>() }` and a `single { Json { ignoreUnknownKeys = true; explicitNulls = false } }` with a comment
  saying why (a newer backup's extra keys must not break reading, research R7). Run
  `./gradlew :shared:testAndroidHostTest --console=plain -q` (`KoinGraphTest`).
- [X] T055 [P] [US1] Add `data object AccountBackupRoute : GameNavKey` to
  `core/navigation/src/commonMain/kotlin/<pkg>/core/navigation/Routes.kt` (with its `@Serializable` and KDoc like its
  neighbours), its `subclass(...)` line in `.../navigation/GameNavSavedStateConfiguration.kt` (`RoutesSerializationTest`
  fails without it), and in `shared/src/commonMain/kotlin/<pkg>/shared/QuestLogNavDisplay.kt` an `entry<AccountBackupRoute>`
  branch obtaining the ViewModel with `koinViewModel<AccountBackupViewModel>()` and passing `onBackClick = {
  backStack.removeLastOrNull() }`, plus a new `onAccountBackupClick` lambda on the existing `SettingsScreen(...)` call
  that navigates with the same `if (backStack.lastOrNull() != AccountBackupRoute)` guard the other settings routes use.
  Add `viewModelOf(::AccountBackupViewModel)` to `shared/src/commonMain/kotlin/<pkg>/shared/di/ViewModelModule.kt`. These
  edits compile together with T056–T060.
- [X] T056 [P] [US1] Create the state files in `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/model/`,
  all `internal`, `@Immutable` where they hold a `List` or a `UiText`: `AccountBackupUiState` (fields and defaults as in
  data-model.md), `AccountBackupUiEvent` (sealed: `SignIn(provider)`, `BackUp`, `ConfirmEmptyBackup`, `DismissDialog`,
  `Retry`), `AccountBackupUiEffect` (sealed: `ShowMessage(text: UiText)`), `AccountUiModel` (`email: String?`,
  `linkedProviders: List<AuthProvider>`, `linkableProviders: List<AuthProvider>`), `BackupStatusUiModel` (sealed:
  `Unknown`, `None`, `Available(lastBackup: UiText, summary: UiText)`), `CloudOperation` (enum with all eight values
  from data-model.md so later stories add no value) and `AccountBackupDialog` (sealed; only `ConfirmEmptyBackup` now).
- [X] T057 [US1] In `feature/settings/src/commonMain/composeResources/values/strings.xml` add the strings this story
  renders, with positional placeholders (`%1$s`, `%2$d`): `settings_group_backup`, `settings_account_backup`,
  `settings_account_not_signed_in`, `account_backup_title`, `account_backup_explainer` (FR-031: says the user's lists,
  saved games and owned platforms are stored in the cloud under the signed-in account, and that nothing is backed up
  unless they tap Back up now), `account_backup_continue_with_google`, `account_backup_sign_in_with_apple`,
  `account_backup_linked_methods`, `account_backup_last_backup` (`%1$s`), `account_backup_summary` (`%1$d` lists,
  `%2$d` games), `account_backup_no_backup`, `account_backup_back_up_now`, the empty-backup dialog's title, body and
  confirm label, and `account_backup_backup_complete`. The Google and Apple button labels follow each provider's
  branding rules ("Sign in with Google" / "Continue with Google", "Sign in with Apple" exactly). Add the two brand marks
  as vector drawables in `feature/settings/src/commonMain/composeResources/drawable/` from the providers' **official**
  assets; if the owner has not supplied them, stop and ask rather than drawing approximations.
- [X] T058 [US1] Create `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/mapper/AccountBackupUiMapper.kt`:
  `Account.toUiModel()` (`linkableProviders` = the providers not in `providers`), `BackupInfo.toUiModel()` building
  `Available(lastBackup = UiText.StringResource(account_backup_last_backup, formatDateTime(...)), summary =
  UiText.StringResource(account_backup_summary, listCount, gameCount))`. The date is formatted here, never in a
  composable. The email stays a plain `String` (data from the provider, never a resource).
- [X] T059 [US1] Create `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/AccountBackupViewModel.kt` (state
  pipeline: `MutableStateFlow` + `_uiState.update { }`, with `ObserveAccountUseCase` collected in `init` for the
  ViewModel's whole life; effects through `Channel(Channel.BUFFERED)` + `receiveAsFlow()`; one exhaustive
  `onEvent`). Rules from data-model.md "State transitions": signing in fetches `GetBackupInfoUseCase`; `BackUp` asks
  `HasLocalUserDataUseCase` and opens `ConfirmEmptyBackup` when there is none; `runningOperation` is set before the call
  and cleared in a `finally` (a cancellation never leaves the screen busy); a second cloud event while it is set is
  ignored; `SignInCancelled` shows nothing, every other failure emits `ShowMessage(error.toUiText())`; a `NotSignedIn`
  failure leaves the observed account to emit `null`. Constructor takes the five use cases from T045. T043 passes.
- [X] T060 [US1] Create `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/AccountBackupScreen.kt` (public
  ViewModel-taking composable with `@Suppress("ParamsComparedByRef")`, collecting with `collectAsStateWithLifecycle()`
  and handling effects as `feature/wishlist/WishlistScreen.kt` does, snackbars for `ShowMessage`; and an `internal`
  stateless `AccountBackupContent(state, onEvent, modifier)`), plus components in
  `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/components/`: `ProviderSignInButton.kt`,
  `AccountCard.kt` (email and linked methods), `BackupCard.kt` (last backup and summary or "No backup yet", the "Back up
  now" button, busy while `runningOperation == BACK_UP`, every cloud action disabled while any operation runs) and
  `AccountBackupDialogs.kt` (the empty-backup `CustomAlertDialog`). Signed-out content shows the explainer and the two
  sign-in buttons only (US1-1). Previews: signed out, signed in with no backup, signed in with a backup, busy, and the
  dialog. Touch targets are at least 48dp through `minimumInteractiveComponentSize()`, as the Discover title row does.
- [X] T061 [US1] Add the Backup group to the Settings hub. In
  `feature/settings/src/commonMain/kotlin/<pkg>/feature/settings/SettingsScreen.kt` add an `onAccountBackupClick: () ->
  Unit` parameter and, in a new `SettingsGroup` titled `settings_group_backup`, one `SettingsRow` ("Account & backup")
  whose subtitle is `state.accountSummary`. In `SettingsUiState.kt` add `accountSummary: UiText? = null` with KDoc
  ("`null` until the first emission, like `ownedPlatformsSummary`"). In `SettingsViewModel.kt` inject
  `ObserveAccountUseCase` and map it to `UiText.DynamicString(email)` or
  `UiText.StringResource(settings_account_not_signed_in)`. Extend `SettingsViewModelTest.kt` with the two summary cases
  (signed in with an email, signed out).
- [ ] T062 [US1] Checkpoint: `./gradlew :core:data:testAndroidHostTest :feature:settings:testAndroidHostTest
  :core:domain:testAndroidHostTest :shared:testAndroidHostTest :core:navigation:testAndroidHostTest --console=plain -q`,
  `./gradlew test` and `./gradlew :androidApp:assembleDebug` green. Then walk quickstart §4 rows 1–8 and 20 on an
  Android device or emulator with a real Google account and record what was and was not walked. **(Mac only)** repeat
  on the iOS simulator, including the Apple sign-in sheet; anything not run is recorded as not run, never as passed.

**Checkpoint**: User Story 1 is complete: a user can sign in and back up, the data is in Firestore, nothing is restored
yet. This is the smallest release that is useful only together with US2 (a backup nobody can restore), so ship US1 and
US2 together.

---

## Phase 4: User Story 2 - Restore my data on a new device, on either platform (Priority: P1)

**Goal**: A signed-in user taps "Restore". If the device already has data, a dialog says it will be replaced first. After
restoring, lists (with covers), saved games, statuses, priorities, notes, list membership, the default wishlist and owned
platforms are what they were at backup time, from either platform's backup, and a failure at any point leaves the device
exactly as it was.

**Independent Test**: Quickstart §4 rows 10–13 and 22–23: restore on an empty device (10), the replace dialog and cancel
(11), a kill mid-restore (12), search history and Recently viewed untouched (13), a too-new backup refused (22), a
damaged backup refused (23).

### Tests for User Story 2 (write first)

- [X] T063 [P] [US2] Extend `core/data/src/androidHostTest/kotlin/<pkg>/core/data/mapper/BackupMapperTest.kt`:
  `BackupSnapshot.toUserDataRows(coverPaths)` is the inverse of `toBackupSnapshot` (a full round trip is identical except
  `lastViewedAt` and cover paths); a `hasCover = true` list gets the path supplied for it, a list without one gets `null`;
  a snapshot whose `defaultListId` is not among its lists fails with the `BackupUnreadable`-mapped exception.
- [X] T064 [P] [US2] Extend `.../repository/BackupRepositoryImplTest.kt` with the restore cases: no manifest →
  `BackupNotFound`, no part read; `formatVersion` above `BACKUP_FORMAT_VERSION` → `BackupFromNewerVersion`, no part read;
  a missing part or a stream whose decoded length differs from its manifest `byteLength` → `BackupUnreadable`,
  `replaceUserData` **not** called and every imported cover file deleted; `replaceUserData` throwing → imported covers
  deleted and the error returned; success → the sweep `deleteAllExcept(...)` runs **first**, the replaced lists' old
  cover files are deleted **after** the transaction, and the returned `BackupInfo` matches the manifest; signed out →
  `NotSignedIn` with no store call; restore and backup are mutually exclusive.
- [X] T065 [P] [US2] Extend `AccountBackupViewModelTest.kt`: `Restore` with local data opens `ConfirmRestore` and runs
  nothing; `DismissDialog` leaves the use case uncalled; `ConfirmRestore` runs `RestoreBackupUseCase`; `Restore` on an
  empty device runs it directly (no dialog); success emits the "restored" message and refreshes the backup info; a
  `BackupNotFound`, `BackupFromNewerVersion` or `BackupUnreadable` failure emits that error's text; `runningOperation`
  blocks a second action and is cleared on every exit path.

### Implementation for User Story 2

- [X] T066 [US2] Add `suspend fun restore(): AppResult<BackupInfo>` to `BackupRepository` ("replaces the local lists, saved
  games, list membership, default wishlist and owned platforms; all-or-nothing") and create
  `core/domain/src/commonMain/kotlin/<pkg>/core/domain/usecase/backup/RestoreBackupUseCase.kt`; register it with
  `factoryOf` in `DomainKoin.kt`.
- [X] T067 [P] [US2] Add `suspend fun import(bytes: ByteArray): String?` ("write already-downscaled bytes as a new file in the
  covers directory, the extension sniffed from the magic bytes: PNG `89 50 4E 47`, otherwise `.jpg`; returns the path or
  `null`") and `suspend fun deleteAllExcept(paths: Set<String>)` ("delete every file in the covers directory that is not
  in `paths`") to `WishlistCoverImageStorage` and implement both in the Android and iOS
  `WishlistCoverImageStorageImpl.kt`. The bytes were written by `persist` on some device, so `import` does not re-encode.
  **(Mac only to compile the iOS side.)**
- [X] T068 [US2] In `core/database/src/commonMain/kotlin/<pkg>/core/database/dao/UserDataDao.kt` add
  `@Transaction open suspend fun replaceUserData(rows: UserDataRows)` clearing and writing **in this order inside one
  transaction** (data-model.md): delete `default_wishlist` → delete `game_list_cross_ref` → delete `wishlists` → delete
  `owned_platforms` → `UPDATE games SET status = NULL, priority = NULL, notes = ''` → insert the lists **with their
  original ids** → insert the `default_wishlist` row (`id = 0`) → for each saved game call
  **`database.gameDao().saveGame(...)`** (never a second write path), carrying over the existing row's `lastViewedAt` for a
  game the device already has → insert the memberships → upsert the owned `PlatformEntity` rows, then insert the
  `OwnedPlatformEntity` rows. The `default_wishlist` row goes first and last because of its `RESTRICT` foreign key. KDoc
  states what a restore does **not** touch (search history, `lastViewedAt`, translations, the Discover caches, release
  reminders: FR-022). Run `./gradlew :core:database:compileAndroidMain --console=plain -q`; `core/database/schemas/` is
  unchanged.
- [X] T069 [US2] Add the inverse to `core/data/src/commonMain/kotlin/<pkg>/core/data/mapper/BackupMapper.kt`:
  `internal fun BackupSnapshot.toUserDataRows(coverPaths: Map<Long, String>): UserDataRows`, decoding `status` and `icon`
  names with the `null` fallback, and rejecting a snapshot whose `defaultListId` is not among its lists (T063 passes).
- [X] T070 [US2] Add `restore()` to `core/data/src/commonMain/kotlin/<pkg>/core/data/repository/BackupRepositoryImpl.kt`
  following research R9, under the shared `Mutex`: `deleteAllExcept(referencedCoverPaths)` first; read the manifest from the
  server (`null` → `BackupNotFound`; `formatVersion > BACKUP_FORMAT_VERSION` → `BackupFromNewerVersion`, before any part
  is read); read exactly the parts the manifest lists, join each stream with `joinParts(byteLength)`, decode the JSON
  (any mismatch or `SerializationException` → `BackupUnreadable`); `import` each cover into a **new** file; call
  `replaceUserData` once; after it commits, delete the replaced lists' old cover files, best effort. Before the
  transaction any failure deletes the files just imported and leaves the database untouched. T064 passes.
- [X] T071 [US2] Add `Restore` and `ConfirmRestore` to `AccountBackupUiEvent`, `ConfirmRestore` to `AccountBackupDialog`, and
  the handling to `AccountBackupViewModel` per data-model.md (confirm only when `HasLocalUserDataUseCase` is true; refresh
  the backup info after success; emit the success message). Add strings to
  `feature/settings/src/commonMain/composeResources/values/strings.xml`: `account_backup_restore`, the dialog's title
  ("Replace the data on this device?"), body (states that the lists, saved games and owned platforms on this device will be
  replaced by the backup and **cannot be recovered**) and confirm label, and `account_backup_restore_complete`. T065 passes.
- [X] T072 [US2] Add the "Restore" button (disabled with no backup, busy while `runningOperation == RESTORE`) to
  `components/BackupCard.kt` and the `ConfirmRestore` `CustomAlertDialog` to `components/AccountBackupDialogs.kt`; add a
  preview for the dialog.
- [X] T073 [US2] Verify the cross-DAO transaction on **Android** (research R10, the first thing to confirm about this DAO).
  Temporarily add `error("rollback check")` after the `UPDATE games SET status = NULL...` statement in
  `replaceUserData`, run a restore on a device with data, and confirm the lists, statuses and owned platforms are
  **exactly as before** (the clears rolled back). Remove the line. If the clears survive, the nested `@Transaction` did
  not join: switch `replaceUserData` to `database.useWriterConnection { it.immediateTransaction { ... } }` as research R10
  records, and repeat. Record the outcome in research.md R10.
- [ ] T074 [US2] **(Mac only)** Repeat T073 on the iOS simulator.
- [ ] T075 [US2] Checkpoint: the same suites as T062 plus quickstart §4 rows 10–13 and 22–23 (row 12's kill test:
  `adb shell am force-stop com.nikolasguillen.questlog` during a restore of a large backup), and, with a second device of the
  other platform, row 10 across platforms (SC-003). **(Mac only)** iOS rows; unrun rows are recorded as not run.

**Checkpoint**: User Stories 1 and 2 are complete: back up and restore work on both platforms. This is the shippable MVP.

---

## Phase 5: User Story 3 - One account across Google on Android and Apple on iPhone (Priority: P2)

**Superseded on 2026-10-16.** The owner decided each platform offers one sign-in method (Google on Android, Apple on iOS),
so there is nothing to link on a device and tasks T076–T081 (the `linkProvider` API, the `Link` event and the "Link Google /
Link Apple" actions) are **dropped, not deferred**. Firebase still links a Google and an Apple sign-in by itself when they
share a verified email, which needs no code. The per-platform change itself was made in the commit that introduced this
note: `CloudAuth.provider`, a parameterless `signIn()`, Google-only `AndroidProviderAuth`, Apple-only `IosProviderAuth` and
`IdentityProviderBridge`, and no `GoogleSignIn-iOS` package.

**Independent Test**: Quickstart §4 row 9 only: sign in with Google on Android and back up; sign in with Apple on an iPhone
using the same Gmail address and confirm the same backup is offered. A hidden-email Apple ID starts empty (row 14) and cannot
be joined later: record that, do not treat it as a failure.

**Checkpoint**: Both platforms reach the same account when their emails match.

---

## Phase 6: User Story 4 - Sign out and delete my account (Priority: P2)

**Goal**: A signed-in user can sign out (local data and cloud backup untouched) or delete their account after a confirmation:
the cloud backup and the account are removed, Apple's access is revoked when Apple is linked, and the local data stays.

**Independent Test**: Quickstart §4 rows 17–19 and 21: sign out keeps data and backup (17); cancelling the delete dialog changes
nothing (18); confirming removes the backup and account, revokes Apple, and signing in again shows no backup (19); a second
account cannot read the first's backup (21).

### Tests for User Story 4 (write first)

- [ ] T082 [P] [US4] Create `core/domain/src/androidHostTest/kotlin/<pkg>/core/domain/usecase/account/DeleteAccountUseCaseTest.kt`
  (MockK repositories): the calls happen in order **reauthenticate → `deleteBackup` → `deleteAccount`** (`coVerifyOrder`);
  a failure at `reauthenticate` stops everything after it; a failure at `deleteBackup` leaves `deleteAccount` uncalled; a
  failure at `deleteAccount` returns that failure; success returns `Success(Unit)`.
- [ ] T083 [P] [US4] Extend `AccountRepositoryImplTest.kt`: `signOut` maps success and failure; `reauthenticate` maps a
  cancelled sign-in to `SignInCancelled`; `deleteAccount` that first throws `CloudRecentLoginRequiredException`
  re-authenticates **once** and retries once, and a second `RecentLoginRequired` is returned as `NotSignedIn` (never
  mapped as its own case, never looped); a `CancellationException` is rethrown.
- [ ] T084 [P] [US4] Extend `BackupRepositoryImplTest.kt`: `deleteBackup` deletes **every part listed under the user's parts
  collection, then the manifest** (assert the order); a failing part delete stops before the manifest is deleted and returns
  the error (so the account never ends up with a manifest pointing at missing parts); signed out → `NotSignedIn`; with no
  manifest it still sweeps parts and returns success.
- [ ] T085 [P] [US4] Extend `AccountBackupViewModelTest.kt`: `SignOut` runs `SignOutUseCase` and leaves local data alone;
  `DeleteAccount` opens `ConfirmDeleteAccount` and runs nothing; `DismissDialog` leaves it uncalled; `ConfirmDeleteAccount`
  runs `DeleteAccountUseCase`; success ends signed out with the "account deleted" message; a failure emits its text and keeps
  the user signed in with the screen usable again; `runningOperation` blocks other actions during both.

### Implementation for User Story 4

- [X] T086 [US4] Add to `AccountRepository`: `suspend fun signOut(): AppResult<Unit>`, `suspend fun reauthenticate():
  AppResult<Unit>` ("a fresh sign-in with Apple when linked, else Google; required before `deleteAccount`") and
  `suspend fun deleteAccount(): AppResult<Unit>` ("revokes Apple's grant, best effort, then deletes the account; call
  `reauthenticate` first"); add `suspend fun deleteBackup(): AppResult<Unit>` to `BackupRepository`. Create
  `usecase/account/SignOutUseCase.kt` and `usecase/account/DeleteAccountUseCase.kt` (the order in research R8, stopping at the
  first failure, KDoc quoting why that order never strands the user) in
  `core/domain/src/commonMain/kotlin/<pkg>/core/domain/`, and register both in `DomainKoin.kt`. T082 passes.
- [X] T087 [US4] Implement `signOut`, `reauthenticate` and `deleteAccount` in `AccountRepositoryImpl.kt` (the single retry on
  `CloudRecentLoginRequiredException` lives in `deleteAccount`, never above it) and `deleteBackup` in `BackupRepositoryImpl.kt`
  under the shared `Mutex` (`listPartIds` → `deleteParts` → `deleteManifest`). T083 and T084 pass.
- [X] T088 [US4] Add `SignOut`, `DeleteAccount`, `ConfirmDeleteAccount` to `AccountBackupUiEvent` and `ConfirmDeleteAccount` to
  `AccountBackupDialog`; handle them in `AccountBackupViewModel` (T085 passes). Add strings: `account_backup_sign_out`,
  `account_backup_delete_account`, the dialog's title, body (states that the account **and the cloud backup** will be
  permanently deleted, that data on this device is kept, and, because an Apple sign-in will be asked for again, that a fresh
  sign-in is needed first) and confirm label, `account_backup_signed_out` and `account_backup_account_deleted`.
- [X] T089 [US4] Add "Sign out" and "Delete account" (the destructive style the design system already has for destructive
  actions; check `core/ui/component/` first) to the signed-in content, and the `ConfirmDeleteAccount` `CustomAlertDialog` to
  `components/AccountBackupDialogs.kt`; previews for both.
- [ ] T090 [US4] Checkpoint: the suites from T062 plus quickstart §4 rows 17–19 and 21 (row 21 in the console's Rules Playground).
  For row 19 with an Apple account, confirm in the device's Settings → Apple ID → Sign in with Apple that the app is gone.
  **(Mac only)** the iOS rows. Unrun rows are recorded as not run.

**Checkpoint**: All four user stories are complete.

---

## Phase 7: Polish & cross-cutting concerns

**Purpose**: Rules, documentation and instructions that must change in the same change set, the static checks, and the
verification record.

- [X] T091 [P] Create `firebase/firestore.rules` with exactly the rules in contracts/firestore.md (a signed-in user reads and writes
  `users/{uid}/{document=**}` only when `request.auth.uid == uid`; everything else denied). Ask the owner to paste it into the
  console's Rules tab (the project has no Firebase CLI) and confirm the published copy matches.
- [X] T092 [P] In `README.md` add a "Firebase configuration" step to Installation (and to Run on iOS): download
  `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist` from Firebase Console → Project settings → Your
  apps; both are git-ignored; a clone without them does not build the Android app or the iOS Firebase parts; point to
  quickstart "Prerequisites" for the provider and rules setup.
- [X] T093 [P] Update the root `CLAUDE.md`: "19 modules" → 20; add `:core:cloud` to the module list and the graph
  (`:core:data → :core:cloud`, `:shared → :core:cloud`); add the rule "`:core:cloud` is reachable only from `:core:data` and
  `:shared`; no `dev.gitlive`/Firebase/Credential Manager import outside it"; add `AccountRepository` and `BackupRepository` to
  the data-flow note (the second and third repositories are owner-approved); add a "Where things live" row for the Firebase
  config files and `firebase/firestore.rules`; add `core/cloud/` to the directory-specific instructions list **only if** a
  `core/cloud/CLAUDE.md` is created in this task (create it, short: what the module is, the throw-only error rule, the two
  platform provider files, the bridge, the spike's finding about versions). Rewrite nothing else.
- [X] T094 [P] Update `core/data/CLAUDE.md` (the "one repository" paragraph now names `AccountRepositoryImpl` and
  `BackupRepositoryImpl` as owner-approved additions; the "Mapped cases" paragraph gains the six `Cloud*` mappings and the
  `RecentLoginRequired`-never-escapes rule; a short "Backup" section: the format version, the chunk limits, the commit-point
  order) and `core/database/CLAUDE.md` (a short note that `UserDataDao` is an `abstract class` on purpose and why, that it is
  the only DAO that calls another, and what a restore does not touch). These are instructions, not a changelog.
- [X] T095 [P] Update `specs/010-kmp-migration/contracts/platform-contracts.md` with the new contracts from
  contracts/cloud-contracts.md (`CloudAuth`, `CloudBackupStore`, `PlatformProviderAuth`, `CurrentActivityTracker`,
  `IdentityProviderBridge`), the three `WishlistCoverImageStorage` members, the `renderLocalDateTime` seam, and the Platform shell
  table rows (Firebase start-up and `startQuestLog`'s third bridge). No capability-exception row is added (both platforms offer
  the same capability).
- [X] T096 Amend `.specify/memory/constitution.md` (PATCH, in the same commit as the code per its Governance section): Principle I
  "19 modules" → 20 and `:core:cloud` added to the list `feature/*` must never depend on; "Additional Constraints → Platform"
  gains the new libraries; bump **Version** to 1.3.5 and **Last Amended** to the date of the change. Do not rewrite other text.
- [X] T097 [P] Update `docs/tech-debt.md` (remove nothing, add only what is true after this work): the iOS Firebase SDK is pinned to
  11.x behind GitLive 2.7.0 (revisit when GitLive 3.0 is stable); `:core:cloud`, `UserDataDao`, the Swift bridge and the provider
  flows have no automated tests beyond the error classifier (the DAO is covered by the quickstart walkthrough); App Check is not
  enabled; any quickstart row recorded as not run. Keep the existing "cloud backup has no reminder" entry.
- [X] T098 [P] Accessibility pass on `AccountBackupScreen`: every button has a text label or `contentDescription` from a resource, the
  two sign-in buttons keep their branded contrast in light and dark (`MaterialTheme.isDarkTheme`), 48dp targets, and the busy state
  is announced (the button's label does not change, its enabled state and a progress indicator do).
- [X] T099 Static checks (quickstart §3): no feature build file mentions `:core:cloud`; no `dev.gitlive`, `com.google.firebase` or
  `androidx.credentials` import outside `:core:cloud` (`git grep -nE "dev\.gitlive|com\.google\.firebase|androidx\.credentials"
  -- ':!core/cloud' ':!*.md'`); no Firebase type in `RepositoryErrorMapper.kt`; `git ls-files | grep -iE
  'google-services|GoogleService-Info'` prints nothing; no new `dp` literal; every string in a `strings.xml`; `core/database/schemas/`
  unchanged.
- [X] T100 Full verification: `./gradlew test --console=plain -q` and `./gradlew :androidApp:assembleDebug --console=plain -q`. **(Mac only)**
  `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command from CLAUDE.md. Append a "Verification
  notes" section to this file in the style of spec 015's, listing exactly which quickstart rows were run on which device and OS
  and which were **not**; never tick a row that was not run.
- [ ] T101 Measure SC-001 and SC-002 once on a real device on mobile data with a 500-game collection and record the two timings in
  the PR description (quickstart §4, last paragraph). If either misses its target, record it as a finding for the owner; do not
  tune before reporting.

---

## Dependencies & execution order

### Phase dependencies

- **Setup (Phase 1)**: starts immediately. **T010 is a gate**: nothing in Phase 2 starts until it passes. T006 (Mac) and T007–T008
  can run in parallel with each other; T011 follows T010.
- **Foundational (Phase 2)**: after the gate; **blocks every story**. Inside it: T012–T014 → T015 + T016 (together) → the cloud tasks
  T017–T021 → T022–T026 → the two platform groups (T027–T030 Android, T031–T034 iOS) in parallel → T035–T038.
- **US1 (Phase 3)**: after Foundational. The MVP's first half.
- **US2 (Phase 4)**: after US1 (it needs signed-in state, the screen, `BackupRepositoryImpl` and `UserDataDao`).
- **US3 (Phase 5)** and **US4 (Phase 6)**: each after US1 only. They touch the same files as US2 (`AccountBackupViewModel`,
  `AccountBackupUiEvent`, `BackupRepositoryImpl`, `AccountRepositoryImpl`, `AccountCard`) but different members, so they can be
  developed in parallel on separate branches but not in parallel on one working tree.
- **Polish (Phase 7)**: after the stories being shipped.

### Within a story

- Tests first (they fail to compile until the code lands, so commit a test with its code).
- `:core:model` / `:core:domain` interfaces → `:core:database` → `:core:data` → `:feature:settings` state → ViewModel → screen.
- The `AccountBackup*` state, event and dialog files grow by story; a story adds only its own cases.

### Parallel opportunities

- Phase 2: T012, T013, T014 (three files); T017, T018, T019 (three groups of files); T027 with T031 (Android and iOS halves).
- US1: T039–T043 (five test files); T045, T048, T049, T055, T056 touch disjoint files.
- US2/US3/US4 tests: T063–T065, T076–T077 and T082–T085 are each a parallel group.
- Phase 7: T091–T095, T097, T098.

### Parallel example: Foundational

```text
T012 AuthProvider.kt   T013 Account.kt   T014 BackupInfo.kt          (:core:model)
T017 CloudProvider/CloudUser   T018 Backup*Document   T019 Cloud*Exception   (:core:cloud)
then in parallel:
  Android: T027 CurrentActivityTracker → T028/T029 AndroidProviderAuth → T030 module + QuestLogApp
  iOS:     T031 bridge types → T032 IosProviderAuth → T033 Swift (Mac) → T034 startQuestLog (Mac)
```

### Parallel example: User Story 1

```text
T039 BackupChunkerTest   T040 BackupMapperTest   T041 AccountRepositoryImplTest
T042 BackupRepositoryImplTest   T043 AccountBackupViewModelTest
then: T044 → T045, T046 → T047, T048, T049, T050 → T051 → T052 → T053 → T054
then: T055, T056 → T057 → T058 → T059 → T060, T061 → T062
```

## Implementation strategy

### MVP first (User Story 1 + 2)

1. Setup, including the iOS spike gate (T010). **If it fails, stop and re-plan around native SDKs.**
2. Foundational.
3. US1 and US2 together: a backup nobody can restore is not a release, and a restore needs a backup. After US2's checkpoint,
   the feature is usable on both platforms with Google or Apple sign-in.
4. Validate against quickstart §4 rows 1–13 and 20, 22, 23 before going further.

### Incremental delivery

1. Setup + Foundational → no visible change (the module exists and is bound).
2. + US1 → sign in and back up. Not shipped alone.
3. + US2 → restore: **the MVP**.
4. + US3 → cross-provider linking, the answer to Apple's hidden email.
5. + US4 → sign out and delete account. **Required before any App Store release that offers Sign in with Apple** (Apple
   requires in-app account deletion), so US4 ships with the first release that includes Apple sign-in even though it is P2.
6. Polish → documentation, rules, static checks, the verification record.

## Notes

- [P] tasks touch different files and have no unfinished dependency.
- **Apple Developer account**: every Apple-sign-in step (T029's end-to-end, T033, parts of T062/T075/T081/T090) needs the paid membership
  and the Services ID, key and Firebase Apple provider configured (quickstart "Prerequisites" step 3). Until then, implement and
  compile, and record those rows as not run; the Google paths can be completed and verified independently.
- `hasLocalUserData` (T047) treats a lone seeded default wishlist as no data. A user who only renamed or added a cover to that single
  list, with no saved games and no owned platforms, is restored over without the confirmation. That matches the plan (research
  R10); say so in the KDoc, and raise it with the owner if it matters.
- Release reminders are not backed up and are not touched by a restore (FR-012, FR-022), but the app's normal reminder sync reacts
  when saved games change, so a restore can cancel the reminders of games that left the saved set. That is the existing behaviour, not
  a restore side effect to fix.
- App Store submission is outside the repo but needs the owner: the privacy "nutrition label" must declare the email address and
  user content collected for backup, and the account-deletion path (US4) must be described in the review notes.
- Out of scope, do not add: automatic or background backup, merge, backup history or rollback, search-history or settings backup,
  email/password sign-in, the backup reminder dialog (`docs/tech-debt.md`), App Check, a new nav graph or module besides
  `:core:cloud`, or any change to `GameRepository`.

---

## Verification notes (Android only, 2026-10-16)

Run on two physical devices, with adb driving the taps and screenshots. **iOS was not run at all** (the owner paused it,
and Sign in with Apple fails in the simulator; a real iPhone and the paid membership are needed). No quickstart row is
ticked here that was not run, and T062, T074, T075, T090 and T101 stay open.

Static checks and suites: `./gradlew test`, `:androidApp:assembleDebug` and `:shared:linkDebugFrameworkIosSimulatorArm64`
pass, and so does the `xcodebuild` build of `iosApp` for the simulator. The static checks of quickstart §3 are clean:
no `dev.gitlive`, `com.google.firebase` or `androidx.credentials` import outside `:core:cloud`, no feature build file
mentions it, no Firebase type in `RepositoryErrorMapper.kt`, the two config files are untracked, `core/database/schemas`
is unchanged since 2026-10-02, and there is no new `dp` literal.

**Pixel 10 Pro, Android 17 (API 37), the owner's Gmail account, already signed in** (an emulator of the same model was
tried first and abandoned: it has no Google account and Google's add-account flow hung):

- Row 2 (one sign-in method): the screen offers "Continue with Google" only.
- Row 5 (part): Back up now with the owner's real data (2 lists, one with a cover and the default marker, 14 games, one
  owned platform): a spinner, then "Last backup" with date and time and "2 lists, 14 games". Not run with 20+ games or
  statuses and notes edited for the purpose.
- Row 6 (part): backing up again moved the time from 09:07 to 09:22. The console's manifest and the sweep were not looked at.
- Row 8: in airplane mode "No internet connection. Check your network and try again." appears and the time stays. The
  manifest in the console was not looked at.
- Row 10 (same device, not an empty one) and row 11: with a throwaway list added, Restore asked for confirmation
  ("Replace the data on this device?"), and after confirming the throwaway list was gone, with both real lists, the
  cover, the default marker and the counts (13 and 7) as backed up. Cancel on that dialog was not tapped. Offline,
  confirming shows the same "No internet connection" message and changes nothing.
- T073: with a temporary `error()` after the clears in `replaceUserData`, the restore failed with the generic error and
  the lists, cover, default marker and counts were exactly as before. The line was removed. Recorded in research R10.

**Samsung SM-A346B, Android 16 (API 36), a Capgemini work Google account** (the only Google account on that phone; the
owner chose it, and both Firebase users it created were deleted):

- Row 3: closing Google's consent screen returns to the signed-out screen with no message and a usable button.
- Row 4: signing in shows the address, "Google" and "No backup yet", with Restore disabled.
- Row 5 (part): Back up now on a nearly fresh install: "Backup complete", "1 list, 0 games". The empty-backup dialog did not
  appear, because the device had an owned platform set, which counts as user data.
- Row 17: Sign out, then sign in again: the 10:21 backup was still there.
- Row 18: Delete account, Cancel: still signed in, backup unchanged.
- Row 19 (part): Delete account, confirm: Google's account chooser appeared first (the fresh sign-in), the buttons were
  disabled with a spinner, then "Account deleted" and the signed-out screen; signing in again showed "No backup yet".
  That the console shows no `users/{uid}` and no parts was **not** checked, and "No backup yet" alone cannot prove it
  (a new sign-in gets a new user id). The delete of a user that has no backup also worked. The Apple part does not apply
  on Android.
- Observed: for about two seconds after signing out the sign-in button is disabled. It is the sign-out operation still
  running (it ends by clearing Credential Manager's cached credential), not a fixed delay.

**Not run:** rows 7 (double tap), 9 and 14 (Apple, cross-platform), 12 (kill during a restore), 13 (search history and
Recently viewed untouched), 15 and 16 (dropped with the one-method-per-platform decision), 20 (the empty-device
dialog is covered by `AccountBackupViewModelTest` only), 21 (the Rules Playground), 22 and 23 (a manifest edited in the
console), row 10 on a genuinely empty device, the Apple Settings check in row 19, SC-001 and SC-002 (T101, 500 games
on mobile data), and every iOS row.
