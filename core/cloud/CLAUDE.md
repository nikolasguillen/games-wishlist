# CLAUDE.md — core:cloud

The cloud account and the cloud backup's documents: Firebase Auth and Firestore through GitLive's multiplatform SDK
(`dev.gitlive:firebase-auth`, `firebase-firestore`), and the one sign-in method each platform offers. It knows nothing
about the app's data: `:core:data` turns a backup into documents and back. Only `:core:data` and `:shared` depend on it.

**Nothing outside this module imports `dev.gitlive`, `com.google.firebase`, `androidx.credentials` or the Firebase
Swift types.**

## Contracts

- `CloudAuth` and `CloudBackupStore` are the interfaces `:core:data` uses; `FirebaseCloudAuth` and
  `FirestoreBackupStore` implement them in `commonMain`.
- `PlatformProviderAuth` (internal) is the per-platform part of signing in: Google through Credential Manager in
  `androidMain/AndroidProviderAuth`, Apple through the Swift bridge in `iosMain/IosProviderAuth`. One provider per
  platform, on purpose: do not add Apple to Android or Google to iOS.
- iOS reaches Apple's sign-in through `IdentityProviderBridge`, implemented in Swift under `iosApp/` and passed in by
  `startQuestLog`. It is callback-based because a `suspend` member implemented in Swift cannot be cancelled.
- `CurrentActivityTracker` (Android) holds the resumed Activity, which Credential Manager needs; no Activity means
  the sign-in counts as cancelled.

## Errors

Every member throws only `CloudException` subtypes (in `error/`) and `CancellationException`, which is always
rethrown. `FirebaseErrorClassifier` is where Firebase's codes become those types; it takes the code as a string,
because Firestore's own enum cannot be initialised in a JVM test. `:core:data` maps the exceptions to
`RepositoryError`, so it never names a Firebase type. A new failure worth telling apart gets its own exception here
first.

`CloudException` is **sealed**, with every subtype in `error/CloudException.kt`, so the mapper in `:core:data` switches over it
exhaustively: a new subtype fails to compile there until it has a `RepositoryError`. Throw `CloudException.Unknown` for a
failure nothing needs to tell apart; the base class cannot be instantiated.

## Things that look wrong and are not

- **The module compiles for JVM 17**, unlike every other module (Java 11). GitLive's inline `set` and `data` are built
  for 17 and cannot be inlined into 11 bytecode. Keep the exception in this build file.
- **iOS Firebase is pinned to 11.x** in Xcode (`11.15.0 ..< 12.0.0`) because GitLive 2.7.0's bindings are generated
  from 11.x headers. Do not raise it without raising GitLive.
- **Reads that must be fresh go to the server** (`readManifest`), and the memory cache is the only cache: nothing is
  persisted on the device. Every call is bounded by `CLOUD_CALL_TIMEOUT`, because a Firestore call made offline
  would otherwise wait.
- **`reauthenticate()` also keeps Apple's revocation grant in memory** until `deleteUser()` or `signOut()`, because
  Apple requires the token to be revoked when the account is deleted and Firebase stores none.
- `firebase/firestore.rules` is the rules file; the project has no Firebase CLI, so the owner pastes it into the
  console.
