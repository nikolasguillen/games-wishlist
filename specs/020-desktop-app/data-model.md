# Data Model: Desktop App (Windows and macOS)

Desktop adds **no Room entity, no DAO and no schema change**: the database stays at `version = 1`. Saved games,
release dates and their precision, notification opt-ins, lists and settings are the existing records, read and
written by the same DAOs and DataStore keys as on mobile (FR-008). What is new is state outside Room: what the OS
holds for reminders, the cloud session file, and the files and sockets in the app data directory.

## 1. App data directory (`appDataDirectory()`, `:core:common` `desktopMain`)

| OS | Root |
|---|---|
| Windows | `%LOCALAPPDATA%\QuestLog` |
| macOS | `~/Library/Application Support/QuestLog` |

| Path under the root | Owner | Content |
|---|---|---|
| `questlog.db` (+ `-wal`, `-shm`) | `:core:database` | The Room database, `BundledSQLiteDriver` |
| `datastore/settings.preferences_pb` | `:core:data` | DataStore: settings, onboarding flag, `release_dates_last_refresh_epoch_ms`, `release_reminders_armed` (the ledger) |
| `covers/<uuid>.jpg` | `:core:data` | Downscaled list covers (`WishlistCoverImageStorage`) |
| `cloud-session.json` | `:core:cloud` | The cloud session (§4). `rw-------` on macOS |
| `instance.lock`, `instance.sock` | `:desktopApp` | Single instance (§6) |

The directory is created on first use. Nothing else writes to it.

## 2. Notification permission (`DesktopNotificationStatus`, `:core:common` `desktopMain`)

```text
enum DesktopNotificationStatus { NOT_DETERMINED, AUTHORIZED, DENIED, UNSUPPORTED }
```

| Status | Windows (`ToastNotifier.Setting`) | macOS (`UNAuthorizationStatus`) |
|---|---|---|
| `NOT_DETERMINED` | never | `notDetermined` |
| `AUTHORIZED` | `Enabled` | `authorized`, `provisional`, `ephemeral` |
| `DENIED` | `DisabledForApplication`, `DisabledForUser`, `DisabledByGroupPolicy`, `DisabledByManifest` | `denied` |
| `UNSUPPORTED` | the WinRT call failed | not running from an app bundle (dev run), or the library failed to load |

`:core:ui` `desktopMain` maps it to the existing `NotificationPermissionState`:

| | `canDeliver` | `isPermanentlyDenied` | `requiresRuntimePermission` | `request()` |
|---|---|---|---|---|
| Windows | `AUTHORIZED` | `!AUTHORIZED` | `false` | opens `ms-settings:notifications` |
| macOS | `AUTHORIZED` | `DENIED` or `UNSUPPORTED` | `true` | prompt while `NOT_DETERMINED`, otherwise opens the app's notification settings |

The cache (`DesktopNotificationAuthorization`, process-wide like iOS's `NotificationAuthorization`) is refreshed at
launch, when the window gains focus and after a prompt.

## 3. A reminder as the OS holds it

The shared `LocalReminderRequest`, `PendingReminder` and `ArmedReminderLedger` are reused unchanged. The desktop
`LocalNotificationCenter` (`DesktopLocalNotificationCenter`, `:core:data` `desktopMain`) maps them as follows:

| Field | Windows scheduled toast | macOS `UNNotificationRequest` |
|---|---|---|
| Identity | `Id = "g<gameId>"` (≤ 16 chars) | `identifier = "release-reminder-<gameId>"`, the iOS scheme |
| When | `DeliveryTime` = `fireAt` (09:00 floating) converted with the current zone; `fireAt == null` → `Show` now | `UNCalendarNotificationTrigger` (y, m, d, 09:00, floating); `null` → 1 s interval trigger |
| Text | Toast XML `ToastGeneric`: title, body from `ReleaseReminderText`, XML-escaped | `title`, `body` |
| Click payload | `launch="gameId=<id>"` | `userInfo["gameId"] = <id>` |
| `pendingReminders()` | `GetScheduledToastNotifications` whose `Id` parses as `g<int>` → `PendingReminder(gameId, DeliveryTime)` | `getPendingNotificationRequests` whose identifier parses → next trigger date |
| `remove(ids)` | `RemoveFromSchedule` for each matching toast | `removePendingNotificationRequests(withIdentifiers:)` |
| `add` returns `false` | status is not `AUTHORIZED`, or the call failed | status is not `AUTHORIZED`, or `add` reported an error |

**Lifecycle** (unchanged from iOS, plus one desktop step):

```text
launch ─► orphan sweep (pending ids not in the ledger are removed)        [desktop only, R15]
       ─► full sync: takeFiredReminders → mark delivered → schedule/cancel each opted-in saved game
focus gained / permission newly granted ─► full sync (conflated)
game unsaved ─► sync(gameId) ─► cancel
refresh succeeded (in app) ─► full sync
```

## 4. Cloud session (`DesktopCloudSession`, `:core:cloud` `desktopMain`)

Persisted as `cloud-session.json`:

| Field | Type | Rule |
|---|---|---|
| `uid` | String | Firebase `localId` |
| `email` | String? | From `signInWithIdp`, refreshed by `accounts:lookup` |
| `providers` | Set<String> | Firebase provider ids (`google.com`, `apple.com`) |
| `refreshToken` | String | Never logged |

In memory only: `idToken` and its expiry (refreshed when under 5 minutes are left), and the `reauthenticated` flag
that `deleteUser` requires, as in `FirebaseCloudAuth`. If Firebase still answers `CREDENTIAL_TOO_OLD_LOGIN_AGAIN`,
that maps to `RecentLoginRequired`. `currentUser` emits
`CloudUser(uid, email, providers mapped to CloudProvider)` from this state: the stored session at start, `null` after
sign-out, delete or a refresh refused with `TOKEN_EXPIRED`, `USER_DISABLED` or `USER_NOT_FOUND`.

**States**: `SignedOut` → (browser sign-in) → `SignedIn` → (reauthenticate) → `SignedIn(recent)` → (delete) →
`SignedOut`; `SignedIn` → (sign-out, or the refresh token refused) → `SignedOut`.

## 5. Backup documents over REST

`BackupManifestDocument`, `BackupStreamDocument` and `BackupPartDocument` are unchanged and remain the only shapes.
Their Firestore REST encoding is in [contracts/firestore-rest.md](contracts/firestore-rest.md). A backup written by
desktop is byte-for-byte the same document a phone writes, and the reverse (FR-023, SC-004).

## 6. Single-instance message (`:desktopApp`)

One UTF-8 line over `instance.sock`, then the second process exits:

| Line | Meaning |
|---|---|
| `FOCUS` | Bring the window forward |
| `OPEN_GAME <id>` | Bring the window forward and open game `<id>` (a positive integer) |

Anything else is ignored. Notification clicks reach the running process directly (the COM activator on Windows, the
delegate on macOS), so this channel is only for a second launch by the user, or by Windows' `-ToastActivated` while a
first instance is still starting.

## 7. Pending deep link

`ReminderDeepLinks` (now in `:shared` `commonMain`, moved from `iosMain`) holds the game id a click asked to open
until `QuestLogRoot` consumes it. iOS and desktop both write to it. A game that is no longer saved still opens its
detail screen from IGDB, and a failed load shows the screen's existing error state (FR-016, US3 #3).
