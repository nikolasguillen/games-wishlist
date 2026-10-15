# Contract: Cloud Account and Storage

The interfaces `:core:cloud` exposes to `:core:data`, the platform pieces behind them, and the existing contracts
this feature extends. These are the agreed shape, not finished code. Everything here is added to
`specs/010-kmp-migration/contracts/platform-contracts.md` in the same commit as the code that introduces it.

The platform-contracts rules apply: no platform type in any signature, each implementation bound in its module's
platform Koin module, `:shared` assembling them, and fakes in the consuming module's test source set.

## `:core:cloud` → `:core:data`

### `CloudAuth`: `commonMain`, implemented in `commonMain` (`FirebaseCloudAuth`, GitLive)

```kotlin
interface CloudAuth {
    val provider: CloudProvider                // the one method this platform offers: GOOGLE on Android, APPLE on iOS
    val currentUser: Flow<CloudUser?>          // Firebase.auth.authStateChanged, mapped
    suspend fun signIn(): CloudUser
    suspend fun reauthenticate()               // a fresh sign-in with `provider`; keeps Apple's revocation grant
    suspend fun signOut()                      // also clears the provider's cached credential and any grant
    suspend fun deleteUser()                   // revokes the held Apple grant (best effort), then deletes
}
```

- Throws only `CloudException` subtypes, plus `CancellationException`, which is always rethrown.
- Each platform offers one method (2026-10-16 clarification in the spec), so there is no `link`: the other platform's
  method reaches the same account only when Firebase links it by email.
- `deleteUser` without a fresh `reauthenticate` throws `CloudRecentLoginRequiredException`. `AccountRepositoryImpl`
  never lets that happen, because `DeleteAccountUseCase` always re-authenticates first.
- The provider-specific half is delegated to `PlatformProviderAuth`, which is the only thing that differs per platform.

### `CloudBackupStore`: `commonMain`, implemented in `commonMain` (`FirestoreBackupStore`, GitLive)

```kotlin
interface CloudBackupStore {
    suspend fun readManifest(uid: String): BackupManifestDocument?            // server read, never the cache
    suspend fun writePart(uid: String, partId: String, part: BackupPartDocument)
    suspend fun readPart(uid: String, partId: String): BackupPartDocument?
    suspend fun writeManifest(uid: String, manifest: BackupManifestDocument)  // the commit point
    suspend fun listPartIds(uid: String): List<String>
    suspend fun deleteParts(uid: String, partIds: Collection<String>)
    suspend fun deleteManifest(uid: String)
}
```

- Each call is bounded by `CLOUD_CALL_TIMEOUT` (`CloudTimeoutException`), and an `UNAVAILABLE` code becomes
  `CloudConnectivityException`. `UNAUTHENTICATED` and `PERMISSION_DENIED` become `CloudUnauthenticatedException`.
- The Firestore instance uses the **memory** cache, so nothing is persisted on the device (R11).
- Document paths and the rules are in [firestore.md](firestore.md).

## Platform: `PlatformProviderAuth` (`:core:cloud`, `internal`)

```kotlin
internal interface PlatformProviderAuth {
    val provider: CloudProvider                                  // GOOGLE on Android, APPLE on iOS
    suspend fun signIn()                                         // ends with Firebase signed in
    suspend fun reauthenticate(): AppleGrant?                    // non-null only where a grant must be revoked (Apple)
    suspend fun revokeApple(grant: AppleGrant)
    suspend fun clearProviderState()                             // on sign-out
}

/** Opaque to common code: the authorization code Apple issues on iOS. */
class AppleGrant internal constructor(internal val value: String)
```

| | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| `provider` | `GOOGLE` | `APPLE` |
| Sign-in, re-auth | Credential Manager `GetSignInWithGoogleOption(webClientId)` on the tracked Activity → `GoogleAuthProvider.credential(idToken, null)` → GitLive `signInWithCredential` / `reauthenticate` | `IdentityProviderBridge.signInWithApple` → identity token + raw nonce + authorization code → `OAuthProvider.credential("apple.com", idToken, rawNonce)` → GitLive |
| Grant | none (`reauthenticate` returns `null`) | the authorization code |
| `revokeApple` | no-op: Google keeps nothing to revoke | `IdentityProviderBridge.revokeAppleToken(code, …)` (Swift: `Auth.auth().revokeToken(withAuthorizationCode:)`) |
| `clearProviderState` | `CredentialManager.clearCredentialState` | no-op: Apple holds nothing on the device |
| Cancelled by the user | `GetCredentialCancellationException`, no Google account → `CloudSignInCancelledException` | the bridge's `CANCELLED` result → same |

### `CurrentActivityTracker` (Android only, `androidMain`)

An `Application.ActivityLifecycleCallbacks` that keeps the resumed Activity in a `WeakReference`. It is
registered by a Koin `single(createdAtStart = true)` in `cloudPlatformModule`. With no resumed Activity,
`PlatformProviderAuth` throws `CloudSignInCancelledException` (the user left the app mid-tap).

### Configuration

| | Android | iOS |
|---|---|---|
| Firebase start-up | google-services plugin (content provider, before `Application.onCreate`) | `FirebaseApp.configure()`, the first line of `AppDelegate.didFinishLaunching` |
| Google client id | Koin property `google_web_client_id`, set by `QuestLogApp` from `R.string.default_web_client_id` | not used: iOS offers no Google sign-in |
| Apple | Services ID, Team ID, key and key id configured in the Firebase console's Apple provider | **Sign in with Apple** capability on the `iosApp` target |

## Swift → Kotlin: `IdentityProviderBridge` (`:core:cloud` `iosMain`, implemented in `iosApp/`)

```kotlin
interface IdentityProviderBridge {
    fun signInWithApple(onResult: (AppleSignInResult) -> Unit)
    fun revokeAppleToken(authorizationCode: String, onResult: (Boolean) -> Unit)
}

class AppleSignInResult(
    val outcome: ProviderOutcome,
    val idToken: String?, val rawNonce: String?, val authorizationCode: String?
)
enum class ProviderOutcome { SUCCESS, CANCELLED, FAILED }
```

- **Nothing is `suspend`**: the same reasons as `AppleLanguageModelBridge`. Kotlin wraps each call in
  `suspendCancellableCoroutine`.
- `onResult` is called exactly once, on any thread.
- Swift owns presentation (the top `UIViewController`, `ASAuthorizationControllerPresentationContextProviding`) and the
  Apple nonce (random, with its SHA-256 sent to Apple).
- `startQuestLog(languageModelBridge:appRefreshTaskBridge:identityProviderBridge:)` binds it, like the other two.

## Existing contracts this feature changes

### `WishlistCoverImageStorage` (`:core:data`): three members added

| | Android | iOS |
|---|---|---|
| `read(path)` | `File(path).readBytes()` if the file is inside the covers directory, else `null` | `NSData(contentsOfFile:)` under the same check |
| `import(bytes)` | Write as-is to `<covers dir>/<UUID>.<jpg\|png>`, the extension sniffed from the magic bytes | same, `NSUUID` |
| `deleteAllExcept(paths)` | Delete every file in the covers directory not in `paths` | same |

The bytes are already downscaled, because they were written by `persist` on some device, so `import` does not
re-encode.

### `DateUtils` platform rendering (`:core:common`): one `expect` added

```kotlin
enum class TimeStyle { SHORT, MEDIUM }
internal expect fun renderLocalDateTime(dateTime: LocalDateTime, dateStyle: DateStyle, timeStyle: TimeStyle): String
```

| Android | iOS |
|---|---|
| `DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle)` with the default locale | `NSDateFormatter` with `dateStyle` / `timeStyle` mapped, `NSLocale.currentLocale` |

### Capability exception register

No row is added. Both platforms offer the same capability: sign-in, backup and restore. The sign-in method differs by
design (Google on Android, Apple on iOS, 2026-10-16 clarification in the spec), and the mechanisms differ (above).
