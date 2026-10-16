# Research: Cloud Backup and Restore

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-15

Every unknown from the Technical Context is resolved here. Owner decisions taken during planning are marked
**(owner)**.

---

## R1. Library layer: GitLive's Firebase Kotlin SDK, with native escape hatches

**Decision**: `dev.gitlive:firebase-auth` and `dev.gitlive:firebase-firestore` **2.7.0** in the new `:core:cloud`
(`commonMain`). The two flows GitLive does not wrap are reached through its `.android` / `.ios` properties
(Android) or a Swift bridge (iOS); see R4 and R5.

**Rationale**: GitLive's common API covers every account and storage call the spec needs (read from its sources
on 2026-10-15):

| Need | GitLive common API |
|---|---|
| Sign in / link / re-authenticate with an ID token | `Firebase.auth.signInWithCredential`, `FirebaseUser.linkWithCredential`, `FirebaseUser.reauthenticate` |
| Google and Apple credentials | `GoogleAuthProvider.credential(idToken, accessToken)`, `OAuthProvider.credential(providerId = "apple.com", idToken, rawNonce)` |
| Current account and its methods | `Firebase.auth.authStateChanged: Flow<FirebaseUser?>`, `FirebaseUser.uid`, `.email`, `.providerData` |
| Sign out, delete | `FirebaseAuth.signOut()`, `FirebaseUser.delete()` |
| Typed failures | `FirebaseAuthUserCollisionException`, `FirebaseAuthRecentLoginRequiredException`, `FirebaseFirestoreException.code` (`UNAVAILABLE`, `UNAUTHENTICATED`, `PERMISSION_DENIED`, …) |
| Documents | `DocumentReference.get(Source)`, `.set(strategy, data)`, `.delete()`, `CollectionReference.get()`, `WriteBatch` |

Not wrapped, so platform code: the Android Apple flow (`startActivityFor{SignIn,Link,Reauthenticate}WithProvider`),
Apple token revocation (`revokeAccessToken` on Android, `revokeToken(withAuthorizationCode:)` on iOS), and every
provider's own sign-in UI (Credential Manager, GoogleSignIn-iOS, AuthenticationServices).

**Alternatives considered**:
- *Native Firebase SDKs on each platform behind our own interfaces* (Android SDK in `androidMain`, iOS SDK called
  from Swift). Full control and no wrapper lag, but every account and storage call is written twice and the iOS
  half is Swift, outside the JVM test suite. Kept as the **fallback** if the spike (R13) fails. The `:core:cloud`
  interfaces (`CloudAuth`, `CloudBackupStore`) are designed so that this swap stays inside `:core:cloud`.
- *Firebase REST APIs over Ktor*. No native dependency at all, but token refresh, Apple's nonce flow and error
  mapping would be ours to write and keep correct. Rejected.

## R2. Firebase versions: the iOS SDK goes back to 11.x

**Decision**: Pin `firebase-ios-sdk` in Xcode to **11.15.0, up to the next major** (`11.15.0 ..< 12.0.0`). This
replaces the 13.0.1 pin committed in `9022d3f4`. On Android, GitLive's transitive Firebase BOM (**34.18.0**)
is used as is, with no BOM of our own.

**Rationale**:
- GitLive 2.7.0 is built against Firebase iOS **11.8.0** (`firebase-cocoapods` in its version catalog). Its
  Kotlin/Native bindings were generated from 11.x headers. 13.x is two majors ahead, the likeliest way to get a
  link error or a crash on the first call.
- `GoogleSignIn-iOS` 10.0.0 depends on `app-check` from 11.0.0 (11.x). Firebase 13.0.1 needs `app-check` 12.x,
  so the two cannot resolve together. Firebase 11.x and 12.x both use `app-check` 11.x.
- 11.15.0 is the newest 11.x tag.

**Alternatives considered**: keep 13.0.1 and wait for GitLive 3.0 (only `3.0.0-alpha02` exists, published
2026-09-03, and it would still clash with GoogleSignIn). Pin 12.19.2: newer, but still a major release that GitLive
was not built against. Revisit both when GitLive 3.0 is stable.

## R3. Same-email linking: Firebase's default setting, no Identity Platform, no billing

**Decision**: Keep Firebase Authentication's default **"One account per email address"**. Handle
`FirebaseAuthUserCollisionException` on sign-in by showing `RepositoryError.AccountExistsWithOtherMethod` (FR-004),
and on linking by showing `RepositoryError.MethodLinkedToOtherAccount` (FR-006).

**Rationale**: Firebase treats **Apple** (always) and **Google for `@gmail.com`** as trusted email providers.
"Trusted → trusted: both providers will be linked without errors." So a Gmail user who signs in with Google on one
device and Apple on the other (same address) lands in one account with no app code. A Google account on another
domain (a work address) is untrusted for that address, and Firebase raises a collision. The app cannot learn
which method owns the address (`fetchSignInMethodsForEmail` is deprecated and returns nothing under email
enumeration protection), so the message is generic: sign in with the method you used before, then link this one.

This corrects the planning conversation, which assumed an Identity Platform upgrade and possibly a billing
account. Neither is needed. The spec's Assumptions and FR-004 were amended.

**Alternatives considered**: *"Multiple accounts per email address"*: the opposite of FR-004. *Identity Platform's
linking setting*: it only changes which error is returned and adds a billing dependency.

## R4. Sign-in on Android

**Decision**:
- **Google**: Credential Manager (`androidx.credentials:credentials` + `credentials-play-services-auth` **1.6.0**)
  with `GetSignInWithGoogleOption(serverClientId = webClientId)` from `googleid` **1.2.1**. Its ID token becomes
  `GoogleAuthProvider.credential(idToken, null)`, passed to GitLive. `GetCredentialCancellationException` maps to
  `CloudSignInCancelledException`. Sign-out also calls `CredentialManager.clearCredentialState`.
- **Apple**: Firebase's own web flow: `OAuthProvider.newBuilder("apple.com").setScopes(listOf("email"))`, then
  `FirebaseAuth.startActivityForSignInWithProvider`, `FirebaseUser.startActivityForLinkWithProvider` or
  `startActivityForReauthenticateWithProvider`, through GitLive's `.android`. `getPendingAuthResult()` is checked
  first, for an Activity that was recreated mid-flow.
- **Revocation** (R8): `FirebaseAuth.revokeAccessToken((authResult.credential as OAuthCredential).accessToken)`
  from the re-authentication result.
- **The Activity**: `CurrentActivityTracker` in `:core:cloud` `androidMain`, an
  `Application.ActivityLifecycleCallbacks` registered from a Koin `single(createdAtStart = true)`, holding the
  resumed Activity weakly. Both flows need an Activity. No contract signature carries one (platform-contracts
  rule: no platform types in signatures).
- **The web client ID**: the google-services plugin generates `R.string.default_web_client_id` into
  `:androidApp`. `QuestLogApp` passes it to Koin as the property `google_web_client_id`, and `:core:cloud` reads it
  with `getProperty`. Reading it in the app module keeps it a compile-time reference, so resource shrinking
  (`isShrinkResources = true`) cannot strip it, which `getIdentifier` would allow.

**Alternatives considered**: a composable launcher in `:core:ui` (the `rememberCoverImagePicker` shape). Rejected
because the Apple flow is Firebase's own Activity flow, and `:core:ui` must not depend on Firebase. A legacy
`GoogleSignInClient`: deprecated in favour of Credential Manager.

## R5. Sign-in on iOS: a Swift bridge, like FoundationModels

**Decision**: `IdentityProviderBridge`, a Kotlin interface in `:core:cloud` `iosMain`. Swift implements it
(`iosApp/iosApp/IdentityProviderBridge.swift`), and `startQuestLog` receives and binds it, the same shape as
`AppleLanguageModelBridge`. It is callback-based with no `suspend` member, for the reason recorded in the
platform contracts (cancellation, thrown Swift errors).
- **Google**: `GoogleSignIn-iOS` **10.0.0** (SPM), `GIDSignIn.sharedInstance.signIn(withPresenting:)` from the top
  view controller, with `clientID` from `FirebaseApp.app()?.options.clientID`. The ID token and access token go
  back to Kotlin. URL scheme: `REVERSED_CLIENT_ID` in `Info.plist`, and `.onOpenURL { GIDSignIn.sharedInstance.handle($0) }`.
- **Apple**: `ASAuthorizationController` with a random raw nonce whose SHA-256 is sent to Apple (CryptoKit). The
  identity token, the raw nonce and the authorization code go back to Kotlin, which builds
  `OAuthProvider.credential("apple.com", idToken, rawNonce)`.
- **Revocation** (R8): Swift calls `Auth.auth().revokeToken(withAuthorizationCode:)` with the code from the
  re-authentication. The Firebase iOS SDK is linked into the app (`9022d3f4`), so Swift can reach it directly.
- **Firebase start-up**: `FirebaseApp.configure()` is the first line of `AppDelegate.didFinishLaunching`, before
  `startQuestLog`. On Android the google-services plugin's content provider initialises Firebase before
  `Application.onCreate`.
- Xcode: the **Sign in with Apple** capability (an entitlements file) on the `iosApp` target.

**Alternatives considered**: Kotlin/Native cinterop with AuthenticationServices. Possible, but the nonce,
delegate and presentation-anchor code is idiomatic in Swift and awkward through cinterop, and GoogleSignIn is
not cinterop-friendly. The bridge pattern already exists twice in the app.

## R6. Storage layout: one manifest, chunked parts, a pointer flip

**Decision**: per user, one manifest document and a collection of opaque parts:

```
users/{uid}                    the manifest: the current snapshot's id, format version, counts, part ids
users/{uid}/parts/{partId}     { snapshotId, stream, index, data }   data ≤ 700,000 ASCII characters
```

`:core:data` serializes the snapshot to JSON (UTF-8), base64-encodes it and cuts it into parts. Each cover file is
its own stream, cut the same way. A backup writes every part of a **new** snapshot id, then writes the manifest
(**the commit point**), then deletes the parts the new manifest does not list. Restore reads the manifest, then
exactly the parts it lists. See [contracts/firestore.md](contracts/firestore.md).

**Rationale**:
- **Limits**: a document is at most 1 MiB, and a request at most 10 MiB. A saved game with its details (description,
  platforms with release dates, genres, companies, engines, artworks, related games) is roughly 3–6 KB of JSON,
  so 500 games is ~1.5–3 MB. A cover is downscaled to 1440 px at JPEG 85% (PNG when it has alpha), and a PNG can
  exceed 1 MiB. Chunking is unavoidable for both.
- **Why base64**: a 700 KB chunk of a JSON string can exceed 1 MiB in UTF-8 when titles are non-Latin. Base64 makes
  every part ASCII, so characters equal bytes, and covers and data share one chunker.
- **Atomicity (FR-014)**: until the manifest write succeeds, the old manifest still points at the old, untouched
  parts. A crash or a failed part write leaves the previous backup intact. Orphaned parts are swept at the next
  backup by listing `users/{uid}/parts`.
- **Free tier**: a 500-game backup with five covers is ~10 parts plus one manifest, so about 11 writes, 11 reads per
  restore and ~10 deletes per backup. That is far inside 20,000 writes and 50,000 reads a day.
- **Bytes as base64**: GitLive documents no `Blob` type, and strings are portable across both platforms.

**Alternatives considered**: *one document with the whole snapshot*: breaks at 1 MiB. *Cloud Storage for
Firebase*: the natural blob store, but new default buckets require the Blaze plan. *A batched write of all parts plus
the manifest*: atomic too, but bounded by the 10 MiB request limit, which large collections would hit. *Compression*:
no deflate in Kotlin `commonMain` without a new dependency, and not needed at this size.

## R7. Backup format and versioning

**Decision**: our own `@Serializable` DTOs in `:core:data` (`backup/model/`), format version **1**, encoded with
`kotlinx-serialization-json` (`ignoreUnknownKeys = true`, `explicitNulls = false`). Enums (`GameStatus`,
`WishlistIcon`) are written as their names and read back with a fallback to `null` for a name this build does
not know. Restore refuses a manifest whose `formatVersion` is above `BACKUP_FORMAT_VERSION` with
`RepositoryError.BackupFromNewerVersion` (FR-024) before downloading any part.

**Rationale**: Room entities are a storage schema that changes freely while the app is unpublished (destructive
migrations). Serializing them directly would make every entity change a silent backup-format change. Separate
DTOs make the format a deliberate contract. Additive changes stay at version 1 (unknown keys are ignored). A change
an older build cannot read raises the version.

**Saved game**: exactly `GameDao.getSavedGames`'s definition: in some list, **or** with a status, **or** with a
priority. Every row `GameWithAllDetails` carries is backed up, except `lastViewedAt`, which belongs to the Recently
viewed list (search activity, FR-012). A restore keeps the device's own value.

## R8. Account deletion order

**Decision** (`DeleteAccountUseCase`):
1. `AccountRepository.reauthenticate()`: a fresh sign-in with **Apple when it is linked**, otherwise Google. This
   satisfies "recent login" and yields Apple's revocation grant (an authorization code on iOS, an access token on
   Android), held in memory by `:core:cloud` until step 3 or until the account is signed out.
2. `BackupRepository.deleteBackup()`: every part, then the manifest.
3. `AccountRepository.deleteAccount()`: revoke the Apple grant (**best effort**: a failure is logged, not fatal),
   then `FirebaseUser.delete()`.

A failure at step 1 or 2 changes nothing visible: the account and, after step 1, the backup are intact or fully
removed, and the user can retry. A failure at step 3 after step 2 leaves an account with no backup. Settings shows
exactly that ("No backup") and offers Delete again, which is the "visible and retryable" state FR-008 and
acceptance scenario US4-5 allow.

**Rationale**: Apple requires apps that offer Sign in with Apple to revoke the user's tokens on account deletion,
and Firebase stores no Apple token. A fresh Apple prompt is the only way to get one, and it doubles as the recent
login. Revoking before deleting the data would leave a live account the user can no longer reach with Apple if the
data delete then failed. The order above never strands the user.

## R9. Restore: download everything, then one transaction

**Decision**:
1. `deleteAllExcept(referencedCoverPaths)` sweeps cover files a killed earlier restore left behind.
2. Read the manifest from the **server** (`Source.SERVER`). No manifest → `BackupNotFound`. Version too new →
   `BackupFromNewerVersion`.
3. Download every listed part, join each stream, check its length against the manifest, decode the JSON. Any
   mismatch → `BackupUnreadable`.
4. Write each cover's bytes to a **new** file (`WishlistCoverImageStorage.import(bytes)`).
5. **One Room transaction**: `UserDataDao.replaceUserData(rows)` (R10).
6. After the commit, delete the cover files of the replaced lists (best effort).

Before step 5, any failure deletes the files from step 4 and leaves the database untouched. A kill before step 5
leaves only orphaned files, swept by step 1 next time. A kill inside step 5 is rolled back by SQLite. FR-020 holds
in every case.

## R10. The Room side: `UserDataDao`, an abstract DAO class

**Decision**: a new `@Dao abstract class UserDataDao(private val database: QuestLogDatabase)` in
`:core:database`, with:
- `@Transaction open suspend fun readUserData(): UserDataRows`: the lists, the default list id, the memberships,
  the owned platform ids with their `PlatformEntity` rows, and the saved games as `GameWithAllDetails`, read in one
  transaction so the snapshot is consistent.
- `@Transaction open suspend fun replaceUserData(rows: UserDataRows)`: clears the user's state (`default_wishlist`,
  `game_list_cross_ref`, `wishlists`, `owned_platforms`, and `status`/`priority`/`notes` on every game), inserts
  the lists with their original ids, the default pointer, the memberships and the owned platforms, and writes each
  saved game through **`database.gameDao().saveGame(...)`**, keeping the device's `lastViewedAt` for a game it
  already has.
- `suspend fun hasUserData(): Boolean`: any saved game, or more than one list, or any owned platform. That is "not
  a fresh install", because the seeded default wishlist alone is not user data. It drives the confirmations in
  FR-015 and FR-019.

**Rationale**: `core/database/CLAUDE.md` puts multi-table writes in a DAO's `@Transaction` method, never
orchestrated from the repository, and requires every game write to go through `GameDao.saveGame`. An interface
DAO cannot call another DAO. Room's documented abstract-class form, with the database as its only constructor
parameter, can: "a function that would make multiple DAO calls … succeed or fail as a whole", and a nested
`@Transaction` becomes a savepoint. The first restore task confirms this on Room KMP (Android and iOS). If it does
not hold, the fallback is `useWriterConnection { it.immediateTransaction { … } }` inside the same
`:core:database` method. The orchestration then still lives in the database module.

**Outcome (Android, 2026-10-16, Pixel 10 Pro, T073)**: the nested `@Transaction` does roll back as a whole. With
`error("rollback check")` placed right after `clearUserFieldsOnGames()` (so the default-list pointer, the memberships,
the lists, the owned platforms and the status/priority/notes had already been cleared), a restore failed with the
generic error, and afterwards both lists, the default marker, the cover and the membership counts (13 and 7) were
exactly as before. The `useWriterConnection { immediateTransaction { … } }` fallback is not needed on Android. The
iOS check (T074) has not been run.

Untouched by a restore (FR-022): search history, `lastViewedAt`, translations, the Discover caches, release
reminders. The existing reminder sync already reacts when a game leaves the saved set (iOS
`ReleaseReminderCoordinator`; the Android sync on its usual triggers). That is the app's normal behaviour, not the
restore editing reminders.

## R11. Error model

**Decision**: `:core:cloud` translates every Firebase, Credential Manager and bridge failure into its own exception
types (`CloudConnectivityException`, `CloudTimeoutException`, `CloudUnauthenticatedException`,
`CloudSignInCancelledException`, `CloudCredentialCollisionException`, `CloudCredentialAlreadyLinkedException`,
`CloudRecentLoginRequiredException`). `:core:data`'s `toRepositoryError()` maps those, so `:core:data` never names
a Firebase type. This mirrors `:core:network` and Ktor (Constitution II).

`RepositoryError` gains: `NotSignedIn`, `SignInCancelled`, `AccountExistsWithOtherMethod`,
`MethodLinkedToOtherAccount`, `BackupNotFound`, `BackupFromNewerVersion`, `BackupUnreadable`, `BackupTooLarge`.
Each gets a string in `:core:ui`'s `ErrorMapper` (FR-025). `SignInCancelled` is a failure the ViewModel swallows
(FR-027).

**Timeouts**: Firestore keeps queued writes pending while offline instead of failing. Each remote call is wrapped in
`withTimeout(CLOUD_CALL_TIMEOUT)` (30 s), and every backup and restore **starts with a server read of the
manifest**, which fails fast with `UNAVAILABLE` when offline. The persistent cache is disabled (memory cache only),
so no stale manifest is served from disk.

The classification of Firebase codes is a pure function (`classifyFirestoreCode`, `classifyAuthError`) so it is
unit-tested on the JVM without constructing Firebase exceptions.

## R12. UI placement

**Decision**: a new route `AccountBackupRoute` and screen in `:feature:settings`, with `AccountBackupViewModel`.
The Settings hub gets a "Backup" group with one row ("Account & backup"), whose subtitle is the signed-in address
or "Not signed in". The screen shows:
- **Signed out**: what is stored and where (FR-031), then "Continue with Google" and "Sign in with Apple".
- **Signed in**: the account (address, linked methods, a "Link Google" / "Link Apple" action for the missing one);
  the backup (last backup date and time, or "No backup yet"; "Back up now"; "Restore"); then "Sign out" and
  "Delete account".
- Confirmations through `CustomAlertDialog`: restore over local data (FR-019), back up an empty device (FR-015),
  delete the account (FR-008).
- One running operation at a time, held in the state, with its button busy and every cloud action disabled
  (FR-026). Outcomes are one-shot effects shown as snackbars.

Button styles follow Google's and Apple's branding rules (the Google "G" mark, the Apple logo, the approved
labels). The two marks are vector resources in `:feature:settings`.

**Date and time**: `:core:common` gains `formatDateTime(instant, DateStyle, TimeStyle)`, with an `expect`
`renderLocalDateTime` next to the existing `renderLocalDate` (java.time on Android, `NSDateFormatter` on iOS).
This extends an existing compatibility seam; it adds no new capability.

**Alternatives considered**: rows inside the Settings hub itself (too much for one screen with dialogs and busy
states). A new `:feature:account` module: a new module for one screen that belongs to Settings.

## R13. The spike comes first

**Decision**: the first implementation step is a throwaway-sized but committed slice: `:core:cloud` with the GitLive
dependencies, Firebase iOS pinned to 11.15.0, `FirebaseApp.configure()` in Swift, the google-services plugin on
`:androidApp`, and a debug-only call that reads `Firebase.auth.currentUser` and the caller's manifest. It passes
when `:androidApp:assembleDebug` builds, `xcodebuild` links, and both apps reach Firebase on a device or simulator.
If it fails on iOS, the design switches to R1's fallback before any feature code is written. That decision is hard
to undo later, so it is made here.

**Outcome (passed)**: both platforms built, started Firebase and reached Firestore. GitLive reports the same
`FirestoreExceptionCode` names on both: a server read with no user is `PERMISSION_DENIED`, and the same read offline
is `UNAVAILABLE` (Android message "Failed to get document because the client is offline.", iOS the same text inside
an `NSError` for `FIRFirestoreErrorDomain` code 14). The spike exercised Firestore only, so the Auth code strings
are still to be verified on both platforms by T021.

**Risk noted**: an iOS **test** binary for a module that depends on `:core:cloud` would need the Firebase frameworks
to link. No such test exists (`:core:network` is the only module with iOS tests), and `:core:cloud` gets none.

## R14. Build and repository changes

- `gradle/libs.versions.toml`: `gitliveFirebase = "2.7.0"`, `androidxCredentials = "1.6.0"`, `googleId = "1.2.1"`,
  `googleServices = "4.5.0"`, with their libraries and the `google-services` plugin.
- Root `build.gradle.kts`: `alias(libs.plugins.google.services) apply false`. `androidApp/build.gradle.kts` applies
  it (it owns `google-services.json` and the application id), as discussed in the planning conversation, not
  through the convention plugin.
- `settings.gradle.kts`: `include(":core:cloud")`.
- **A fresh clone needs both config files** to build: the google-services task fails without
  `androidApp/google-services.json`, and Xcode without `GoogleService-Info.plist`. The README's Getting Started gains
  the Firebase step. The owner has both files locally.
- `firebase/firestore.rules` is committed as the rules' source of truth and pasted into the console (no Firebase
  CLI in the toolchain).

## Values fixed by this plan

| Value | Where | Why |
|---|---|---|
| `BACKUP_FORMAT_VERSION = 1` | `:core:data` `backup/` | R7 |
| `PART_MAX_CHARS = 700_000` | `:core:data` `backup/` | ~33% headroom under 1 MiB for the other fields and encoding overhead (R6) |
| `MAX_BACKUP_PARTS = 40` | `:core:data` `backup/` | ~28 MB of base64, ~21 MB raw: far beyond a plausible collection, refused with `BackupTooLarge` |
| `CLOUD_CALL_TIMEOUT = 30.seconds` | `:core:cloud` | R11 |
| Firebase iOS `11.15.0 ..< 12.0.0`, GitLive `2.7.0`, GoogleSignIn-iOS `10.0.0`, Credential Manager `1.6.0`, `googleid` `1.2.1`, google-services `4.5.0` | catalog, Xcode | R2, R4, R5 |

## Sources

- GitLive Firebase Kotlin SDK: [repository](https://github.com/GitLiveApp/firebase-kotlin-sdk),
  [`auth.kt`](https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-auth/src/commonMain/kotlin/dev/gitlive/firebase/auth/auth.kt),
  [`credentials.kt`](https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-auth/src/commonMain/kotlin/dev/gitlive/firebase/auth/credentials.kt),
  [`user.kt`](https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-auth/src/commonMain/kotlin/dev/gitlive/firebase/auth/user.kt),
  [`firestore.kt`](https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-firestore/src/commonMain/kotlin/dev/gitlive/firebase/firestore/firestore.kt)
- [Firestore quotas and limits](https://firebase.google.com/docs/firestore/quotas)
- [Users in Firebase projects: trusted providers](https://firebase.google.com/docs/auth/users),
  [Identity Platform: linking providers](https://cloud.google.com/identity-platform/docs/link-accounts)
- [Authenticate using Apple on Android](https://firebase.google.com/docs/auth/android/apple),
  [Authenticate using Apple (Apple platforms)](https://firebase.google.com/docs/auth/ios/apple)
- Apple token revocation with Firebase:
  [Deleting user account & revoke access token](https://medium.com/firebase-developers/deleting-user-account-revoke-access-token-0e30d7a351bb)
- [Firebase pricing](https://firebase.google.com/pricing)
- Room transactions: [Access data using Room DAOs](https://developer.android.com/training/data-storage/room/accessing-data),
  [Transactions and Room](https://commonsware.com/Room/pages/chap-dao-005.html),
  [Set up Room for KMP](https://developer.android.com/kotlin/multiplatform/room)
