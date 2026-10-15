# Data Model: Cloud Backup and Restore

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Research**: [research.md](research.md)

Signatures are the agreed shape, not finished code. One type per file, except the sealed hierarchies the root
`CLAUDE.md` allows (`RepositoryError`, the feature's state/event/effect files).

---

## `:core:model`

### `AuthProvider` (new, `AuthProvider.kt`)

```kotlin
enum class AuthProvider { GOOGLE, APPLE }
```

### `Account` (new, `Account.kt`)

```kotlin
/** The signed-in identity the cloud backup belongs to. */
data class Account(
    val id: String,                    // the backend's user id; never shown
    val email: String?,                // null when no linked method shares one (rare)
    val providers: Set<AuthProvider>   // linked sign-in methods; never empty while signed in
)
```

### `BackupInfo` (new, `BackupInfo.kt`)

```kotlin
data class BackupInfo(
    val createdAt: kotlin.time.Instant,
    val listCount: Int,
    val gameCount: Int
)
```

### `RepositoryError` (changed, cases added)

| New case | When | Spec |
|---|---|---|
| `NotSignedIn` | No account, or the session was revoked/expired during an operation | Edge case "signed out mid-operation", FR-025 |
| `SignInCancelled` | The user closed a provider's sign-in UI. The ViewModel shows nothing | FR-027 |
| `AccountExistsWithOtherMethod` | Sign-in collided with an account owned by another method (R3) | FR-004 |
| `MethodLinkedToOtherAccount` | Linking a method that belongs to another account | FR-006 |
| `BackupNotFound` | Restore with no manifest | FR-023 |
| `BackupFromNewerVersion` | `formatVersion` above what this build reads | FR-024 |
| `BackupUnreadable` | Missing or short part, undecodable JSON, no default list | Edge cases, FR-025 |
| `BackupTooLarge` | More than `MAX_BACKUP_PARTS` parts | Edge case "very large collections" |

The existing `NoNetwork`, `RequestTimeout`, `FileStorage` and `Unknown` cover connectivity, timeouts, cover files and
the rest.

---

## `:core:domain`

### `AccountRepository` (new, `repository/AccountRepository.kt`), owner-approved second repository

```kotlin
interface AccountRepository {
    fun observeAccount(): Flow<Account?>
    suspend fun signIn(provider: AuthProvider): AppResult<Account>
    suspend fun linkProvider(provider: AuthProvider): AppResult<Account>
    suspend fun signOut(): AppResult<Unit>
    /** A fresh sign-in with Apple when linked, else Google. Required before [deleteAccount] (R8). */
    suspend fun reauthenticate(): AppResult<Unit>
    /** Revokes Apple's grant (best effort), then deletes the account. Call [reauthenticate] first. */
    suspend fun deleteAccount(): AppResult<Unit>
}
```

### `BackupRepository` (new, `repository/BackupRepository.kt`), owner-approved second repository

```kotlin
interface BackupRepository {
    /** `Success(null)`: signed in, no backup yet. */
    suspend fun getBackupInfo(): AppResult<BackupInfo?>
    suspend fun backUp(): AppResult<BackupInfo>
    suspend fun restore(): AppResult<BackupInfo>
    suspend fun deleteBackup(): AppResult<Unit>
    /** DB-only, hence no AppResult: anything beyond a fresh install's seeded default wishlist. */
    suspend fun hasLocalUserData(): Boolean
}
```

Every member except `hasLocalUserData` touches the network, so it returns `AppResult` (Constitution II).

### Use cases (new, `usecase/account/`, `usecase/backup/`)

| Use case | Does | Test |
|---|---|---|
| `ObserveAccountUseCase` | `accountRepository.observeAccount()` | delegation, not tested |
| `SignInUseCase(provider)` | `signIn` | delegation |
| `LinkProviderUseCase(provider)` | `linkProvider` | delegation |
| `SignOutUseCase` | `signOut` | delegation |
| `DeleteAccountUseCase` | `reauthenticate` → `backupRepository.deleteBackup()` → `deleteAccount()`, stopping at the first failure (R8) | **yes**: order, short-circuit at each step |
| `GetBackupInfoUseCase` | `getBackupInfo` | delegation |
| `BackUpUseCase` | `backUp` | delegation |
| `RestoreBackupUseCase` | `restore` | delegation |
| `HasLocalUserDataUseCase` | `hasLocalUserData` | delegation |

---

## `:core:cloud` (new module)

Owns the Firestore document shapes and the provider sign-in. Knows nothing about games or lists: it stores and
returns opaque parts. See [contracts/cloud-contracts.md](contracts/cloud-contracts.md) for the interfaces.

### `CloudUser` (new)

```kotlin
data class CloudUser(val uid: String, val email: String?, val providers: Set<CloudProvider>)
enum class CloudProvider { GOOGLE, APPLE }   // own enum: :core:cloud does not depend on :core:model
```

### `BackupManifestDocument` (new, `@Serializable`, stored at `users/{uid}`)

```kotlin
@Serializable
data class BackupManifestDocument(
    val snapshotId: String,
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersion: String,
    val listCount: Int,
    val gameCount: Int,
    val streams: List<BackupStreamDocument>   // in restore order; the data stream first
)

@Serializable
data class BackupStreamDocument(
    val name: String,          // "data" or "cover-<listId>"
    val byteLength: Long,      // decoded length, checked on restore
    val partIds: List<String>  // in order
)
```

### `BackupPartDocument` (new, `@Serializable`, stored at `users/{uid}/parts/{partId}`)

```kotlin
@Serializable
data class BackupPartDocument(val snapshotId: String, val stream: String, val index: Int, val data: String)
```

`partId` = `"$snapshotId-$stream-$index"`. Validation: `data.length <= PART_MAX_CHARS`, set by `:core:data`.

### Exceptions (new, `CloudException.kt` and one file per subclass)

`CloudConnectivityException`, `CloudTimeoutException`, `CloudUnauthenticatedException`,
`CloudSignInCancelledException`, `CloudCredentialCollisionException`, `CloudCredentialAlreadyLinkedException`,
`CloudRecentLoginRequiredException`. All extend `CloudException : Exception`. Mapping is in R11.

---

## `:core:database`

### `UserDataDao` (new, `dao/UserDataDao.kt`): abstract class, see R10

```kotlin
@Dao
abstract class UserDataDao(private val database: QuestLogDatabase) {
    @Transaction open suspend fun readUserData(): UserDataRows
    @Transaction open suspend fun replaceUserData(rows: UserDataRows)
    @Query("...") abstract suspend fun hasUserData(): Boolean
    // plus the abstract @Query/@Insert primitives the two methods use
}
```

Added to `QuestLogDatabase` as `abstract fun userDataDao(): UserDataDao` and bound in `databaseModule`. **No
entity change, so no schema change.**

### `UserDataRows` (new, `userdata/UserDataRows.kt`), not a table and not a query result

```kotlin
data class UserDataRows(
    val lists: List<ListEntity>,
    val defaultListId: Long,
    val memberships: List<GameListCrossRef>,
    val ownedPlatformIds: List<Int>,
    val ownedPlatforms: List<PlatformEntity>,   // so the picker can name them before the catalogue syncs
    val savedGames: List<GameWithAllDetails>
)
```

It lives in its own `userdata/` package because `entity/` must match the exported schema and `relation/` holds
query results (`core/database/CLAUDE.md`).

`replaceUserData` clears and writes in this order, inside one transaction: `DELETE default_wishlist` →
`DELETE game_list_cross_ref` → `DELETE wishlists` → `DELETE owned_platforms` →
`UPDATE games SET status = NULL, priority = NULL, notes = ''` → insert lists (with their ids) → insert the
`default_wishlist` row → per saved game, `gameDao().saveGame(...)` with `lastViewedAt` taken from the existing row →
insert memberships → upsert owned `PlatformEntity` rows and insert `OwnedPlatformEntity` rows. The
`default_wishlist` row is deleted first and inserted after the lists, because of its `RESTRICT` foreign key.

---

## `:core:data`

### Snapshot DTOs (new, `backup/model/`, `@Serializable`, format version 1)

| Type | Fields |
|---|---|
| `BackupSnapshot` | `formatVersion`, `createdAtEpochMillis`, `appVersion`, `lists`, `defaultListId`, `games`, `memberships`, `ownedPlatformIds`, `ownedPlatforms` |
| `BackupList` | `id`, `name`, `description`, `icon: String?`, `hasCover: Boolean` (its bytes are stream `cover-<id>`) |
| `BackupGame` | every `GameEntity` field except `lastViewedAt`, plus `platforms`, `genres`, `companies`, `engines`, `artworks`, `relatedGames` |
| `BackupGamePlatform` | `PlatformEntity` fields + `releaseDate`, `releaseDatePrecision` from the cross-ref |
| `BackupGenre`, `BackupEngine` | id, name |
| `BackupCompany` | id, name, `isDeveloper`, `isPublisher` |
| `BackupArtwork` | `position`, `url` |
| `BackupRelatedGame` | `relatedGameId`, `name`, `coverUrl`, `relationType` |
| `BackupPlatform` | `PlatformEntity` fields |
| `BackupMembership` | `gameId`, `listId` |

`status` and `icon` are names (`GameStatus.name`, `WishlistIcon.name`), mapped back with a `null` fallback (R7).

### Mappers (new, `mapper/BackupMapper.kt`)

`UserDataRows.toBackupSnapshot(createdAt, appVersion, coverListIds)`, `BackupSnapshot.toUserDataRows(coverPaths)`.
These are top-level extensions, per `core/data/CLAUDE.md`. Round-trip tested.

### Chunking (new, `backup/BackupChunker.kt`)

`ByteArray.toParts(maxChars): List<String>` (base64, then cut) and `List<String>.joinParts(expectedLength): ByteArray`
(join, decode, check the length, else `BackupUnreadable`). Pure and unit-tested.

### Repositories (new)

- `AccountRepositoryImpl(cloudAuth: CloudAuth)`: maps `CloudUser` ↔ `Account`, `CloudProvider` ↔ `AuthProvider`,
  and errors.
- `BackupRepositoryImpl(cloudAuth, cloudBackupStore, userDataDao, coverStorage, appVersionProvider, clock, json)`:
  R6 and R9. A `Mutex` makes backup, restore and delete mutually exclusive.

### `WishlistCoverImageStorage` (changed contract)

```kotlin
interface WishlistCoverImageStorage {
    suspend fun persist(source: String): String?                // unchanged
    suspend fun delete(path: String)                            // unchanged
    suspend fun read(path: String): ByteArray?                  // new: the stored bytes, null if missing
    suspend fun import(bytes: ByteArray): String?               // new: write already-downscaled bytes as a new file
    suspend fun deleteAllExcept(paths: Set<String>)             // new: sweep files no list references
}
```

### Error mapping (changed, `repository/RepositoryErrorMapper.kt`)

The `Cloud*Exception` branches go above the catch-all: `Connectivity → NoNetwork`, `Timeout → RequestTimeout`,
`Unauthenticated → NotSignedIn`, `SignInCancelled → SignInCancelled`, `CredentialCollision →
AccountExistsWithOtherMethod`, `CredentialAlreadyLinked → MethodLinkedToOtherAccount`. `RecentLoginRequired` never
escapes `AccountRepositoryImpl`, which re-authenticates and retries once.

---

## `:core:common`

`formatDateTime(instant: Instant, dateStyle: DateStyle, timeStyle: TimeStyle): String` (new, in `DateUtils`), backed
by `internal expect fun renderLocalDateTime(...)`, plus `enum class TimeStyle { SHORT, MEDIUM }`.

---

## `:core:navigation`

`AccountBackupRoute` (new `data object` in `Routes.kt`) and its `subclass(...)` line in
`GameNavSavedStateConfiguration.kt`.

---

## `:feature:settings`

### `AccountBackupUiState` (new, `model/AccountBackupUiState.kt`)

```kotlin
@Immutable
internal data class AccountBackupUiState(
    val account: AccountUiModel? = null,              // null: signed out
    val backup: BackupStatusUiModel = BackupStatusUiModel.Unknown,
    val runningOperation: CloudOperation? = null,     // one at a time (FR-026)
    val dialog: AccountBackupDialog? = null
)
```

| Type | Shape |
|---|---|
| `AccountUiModel` | `email: String?`, `linkedProviders: List<AuthProvider>`, `linkableProviders: List<AuthProvider>` |
| `BackupStatusUiModel` (sealed) | `Unknown` (loading or offline), `None`, `Available(lastBackup: UiText /* formatted date+time */, summary: UiText)` |
| `CloudOperation` (enum) | `SIGN_IN_GOOGLE`, `SIGN_IN_APPLE`, `LINK_GOOGLE`, `LINK_APPLE`, `BACK_UP`, `RESTORE`, `SIGN_OUT`, `DELETE_ACCOUNT` |
| `AccountBackupDialog` (sealed) | `ConfirmRestore`, `ConfirmEmptyBackup`, `ConfirmDeleteAccount` |

### `AccountBackupUiEvent` (new, sealed)

`SignIn(provider)`, `Link(provider)`, `BackUp`, `ConfirmEmptyBackup`, `Restore`, `ConfirmRestore`, `SignOut`,
`DeleteAccount`, `ConfirmDeleteAccount`, `DismissDialog`, `Retry`.

### `AccountBackupUiEffect` (new, sealed)

`ShowMessage(text: UiText)`, for success ("Backup complete", "Restored", "Account deleted", "Google linked") and for
every failure except `SignInCancelled`.

### `SettingsUiState` (changed)

Gains `accountSummary: UiText?`: the signed-in address, or "Not signed in", shown as the new row's subtitle. It is
`null` until the first emission, like `ownedPlatformsSummary`.

### Strings (`feature/settings/.../values/strings.xml`)

`settings_group_backup`, `settings_account_backup`, `settings_account_not_signed_in`, `account_backup_title`,
`account_backup_explainer` (FR-031), `account_backup_continue_with_google`, `account_backup_sign_in_with_apple`,
`account_backup_link_google`, `account_backup_link_apple`, `account_backup_linked_methods`,
`account_backup_last_backup` (`%1$s`), `account_backup_summary` (`%1$d` lists, `%2$d` games),
`account_backup_no_backup`, `account_backup_back_up_now`, `account_backup_restore`, `account_backup_sign_out`,
`account_backup_delete_account`, the three dialogs' titles, bodies and confirm labels, and the success messages.

`:core:ui` gains `error_not_signed_in`, `error_account_exists_other_method`, `error_method_linked_other_account`,
`error_backup_not_found`, `error_backup_newer_version`, `error_backup_unreadable` and `error_backup_too_large`.
`SignInCancelled` maps to `error_unknown` only for exhaustiveness and is never shown.

---

## State transitions (`AccountBackupViewModel`)

```
            ┌──────────── signIn ok ────────────┐
SignedOut ──┤                                   ▼
            └── cancelled / failed ─▶ SignedOut   SignedIn(backup = Unknown) ── getBackupInfo ─▶ None | Available
                                                     │
   BackUp ── hasLocalUserData? ─ no ─▶ dialog ConfirmEmptyBackup ── confirm ─┐
                    └─ yes ──────────────────────────────────────────────────┴▶ running BACK_UP ─▶ Available | message
   Restore ── hasLocalUserData? ─ yes ─▶ dialog ConfirmRestore ── confirm ─┐
                    └─ no ─────────────────────────────────────────────────┴▶ running RESTORE ─▶ message
   DeleteAccount ─▶ dialog ConfirmDeleteAccount ── confirm ─▶ running DELETE_ACCOUNT ─▶ SignedOut | message
   SignOut ─▶ running SIGN_OUT ─▶ SignedOut
   any failure with NotSignedIn ─▶ observeAccount emits null ─▶ SignedOut, message "sign in again"
```

`runningOperation != null` disables every cloud action. It is cleared in a `finally`, so a cancellation never
leaves the screen stuck busy.
