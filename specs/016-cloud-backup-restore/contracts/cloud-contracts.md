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
    val currentUser: Flow<CloudUser?>          // Firebase.auth.authStateChanged, mapped
    suspend fun signIn(provider: CloudProvider): CloudUser
    suspend fun link(provider: CloudProvider): CloudUser
    suspend fun reauthenticate()               // Apple if linked, else Google; keeps Apple's revocation grant
    suspend fun signOut()                      // also clears the provider's cached credential and any grant
    suspend fun deleteUser()                   // revokes the held Apple grant (best effort), then deletes
}
```

- Throws only `CloudException` subtypes, plus `CancellationException`, which is always rethrown.
- `link` on a provider that is already linked returns the current user unchanged.
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
    suspend fun signIn(provider: CloudProvider)                  // ends with Firebase signed in
    suspend fun link(provider: CloudProvider)                    // links to Firebase's current user
    suspend fun reauthenticate(provider: CloudProvider): AppleGrant?   // non-null only for APPLE
    suspend fun revokeApple(grant: AppleGrant)
    suspend fun clearProviderState()                             // on sign-out
}

/** Opaque to common code: an authorization code (iOS) or an OAuth access token (Android). */
class AppleGrant internal constructor(internal val value: String)
```

| | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| Google sign-in, link, re-auth | Credential Manager `GetSignInWithGoogleOption(webClientId)` on the tracked Activity → `GoogleAuthProvider.credential(idToken, null)` → GitLive `signInWithCredential` / `linkWithCredential` / `reauthenticate` | `IdentityProviderBridge.signInWithGoogle` → tokens → the same GitLive calls |
| Apple sign-in, link, re-auth | `OAuthProvider.newBuilder("apple.com")` + `startActivityFor{SignIn,Link,Reauthenticate}WithProvider` through GitLive's `.android`, after `pendingAuthResult` | `IdentityProviderBridge.signInWithApple` → identity token + raw nonce + authorization code → `OAuthProvider.credential("apple.com", idToken, rawNonce)` → GitLive |
| Apple grant | `(authResult.credential as OAuthCredential).accessToken` | the authorization code |
| `revokeApple` | `FirebaseAuth.revokeAccessToken(token)` | `IdentityProviderBridge.revokeAppleToken(code, …)` (Swift: `Auth.auth().revokeToken(withAuthorizationCode:)`) |
| `clearProviderState` | `CredentialManager.clearCredentialState` | `IdentityProviderBridge.signOutGoogle()` |
| Cancelled by the user | `GetCredentialCancellationException`, the Apple flow's cancel → `CloudSignInCancelledException` | the bridge's `CANCELLED` result → same |

### `CurrentActivityTracker` (Android only, `androidMain`)

An `Application.ActivityLifecycleCallbacks` that keeps the resumed Activity in a `WeakReference`. It is
registered by a Koin `single(createdAtStart = true)` in `cloudPlatformModule`. With no resumed Activity,
`PlatformProviderAuth` throws `CloudSignInCancelledException` (the user left the app mid-tap).

### Configuration

| | Android | iOS |
|---|---|---|
| Firebase start-up | google-services plugin (content provider, before `Application.onCreate`) | `FirebaseApp.configure()`, the first line of `AppDelegate.didFinishLaunching` |
| Google client id | Koin property `google_web_client_id`, set by `QuestLogApp` from `R.string.default_web_client_id` | `FirebaseApp.app()?.options.clientID`, read in Swift; `REVERSED_CLIENT_ID` URL scheme in `Info.plist` |
| Apple | Services ID, Team ID, key and key id configured in the Firebase console's Apple provider | **Sign in with Apple** capability on the `iosApp` target |

## Swift → Kotlin: `IdentityProviderBridge` (`:core:cloud` `iosMain`, implemented in `iosApp/`)

```kotlin
interface IdentityProviderBridge {
    fun signInWithGoogle(onResult: (GoogleSignInResult) -> Unit)
    fun signInWithApple(onResult: (AppleSignInResult) -> Unit)
    fun revokeAppleToken(authorizationCode: String, onResult: (Boolean) -> Unit)
    fun signOutGoogle()
}

class GoogleSignInResult(val outcome: ProviderOutcome, val idToken: String?, val accessToken: String?)
class AppleSignInResult(
    val outcome: ProviderOutcome,
    val idToken: String?, val rawNonce: String?, val authorizationCode: String?
)
enum class ProviderOutcome { SUCCESS, CANCELLED, FAILED }
```

- **Nothing is `suspend`**: the same reasons as `AppleLanguageModelBridge`. Kotlin wraps each call in
  `suspendCancellableCoroutine`.
- `onResult` is called exactly once, on any thread.
- Swift owns presentation (the top `UIViewController`, `ASAuthorizationControllerPresentationContextProviding`), the
  Apple nonce (random, with its SHA-256 sent to Apple) and the Google `clientID`.
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

No row is added. Both platforms offer the same capability: Google and Apple sign-in, backup and restore. The
mechanisms differ (above), but nothing the user sees differs.
