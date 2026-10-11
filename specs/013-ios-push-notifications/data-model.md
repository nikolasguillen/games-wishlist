# Data Model: iOS Release Notifications

No Room entity changes. The database stays at `version = 1`, and no schema export is regenerated. Every value
below is either an in-memory domain value or the one new iOS `DataStore` key.

## Existing, unchanged

| Entity | Where | Role here |
|---|---|---|
| `ReleaseNotificationEntity(gameId, notifiedForDate)` | `:core:database` | The opt-in (row present = on) and the date a reminder was delivered for. iOS now writes `notifiedForDate` too, from the ledger (research §3) |
| Saved game + `ReleaseDate(date, precision, platformId)` | `:core:model` | Read through `resolveReleaseDates` / `resolveNotificationInstant`; no second copy |
| `NOTIFICATION_HOUR = 9` | `ReleaseDateResolver.kt` | Fire hour on both platforms |

## New: `ReleaseReminder` — `:core:domain/notification/ReleaseReminder.kt`

The reminder the reconciler hands to a platform scheduler.

| Field | Type | Rule |
|---|---|---|
| `gameId` | `Int` | The opted-in, saved game |
| `gameName` | `String` | Data-source text, so `String`, not `UiText`. iOS bakes it into the content; Android ignores it |
| `releaseDateEpochSeconds` | `Long` | The resolved earliest release date the reminder is for. It is the ledger key on iOS, and Android ignores it |
| `fireAt` | `Instant` | `resolveNotificationInstant(...)`. It is never `null` here: a `null` instant means `cancel`, not `schedule` |

Built only by `SyncReleaseNotificationsUseCase`.

## New: armed-reminder ledger (iOS only)

| | |
|---|---|
| Storage | Existing iOS settings `DataStore<Preferences>`, key `release_reminders_armed` (`stringSetPreferencesKey`) |
| Entry | `"<gameId>:<releaseDateEpochSeconds>"`, at most one per `gameId` |
| Owner | The iOS scheduler (`LocalNotificationReleaseScheduler`), nothing else reads or writes it |

**Transitions** (per game):

```text
             schedule accepted                    request no longer pending
 (none) ───────────────────────────▶ ARMED(D) ───────────────────────────────▶ FIRED(D)
   ▲                                 │   │                                       │
   │      cancel / trimmed by cap    │   │ schedule for D' ≠ D (date moved)      │ takeFiredReminders()
   └─────────────────────────────────┘   └──▶ ARMED(D')                          │   → markReleaseNotificationDelivered(gameId, D)
   ▲                                                                             │   → entry removed
   └─────────────────────────────────────────────────────────────────────────────┘
```

- `schedule(reminder)` with a ledger entry for the same date *D* whose request is no longer pending is a **no-op**.
  That reminder has fired, and re-adding it would post it twice (research §3).
- An add that the system rejects (permission not granted) writes no entry. The next sync tries again.
- `cancel(gameId)` removes the pending request and the entry.
- Trimming under the cap removes the request and the entry, and does **not** count as fired.

## New: notification request (iOS only)

| Part | Value |
|---|---|
| Identifier | `release-reminder-<gameId>`: one per game, and adding it again replaces the pending one |
| Content title / body | `ReleaseReminderText.title(gameName)` / `.body()`, the same text as Android |
| `userInfo` | `{"gameId": <Int>}`, which is all the tap handler reads |
| Sound | Default |
| Trigger | Floating `UNCalendarNotificationTrigger` (Y/M/D/h/m in the current zone, no zone set), or a 1 s `UNTimeIntervalNotificationTrigger` when `fireAt ≤ now` |

## New: notification authorization (iOS only)

A process-wide cache in `:core:ui` `iosMain`, mapped from `UNAuthorizationStatus` (research §8):

| Status | Meaning to the UI |
|---|---|
| `NOT_DETERMINED` | Can ask: `request()` shows the system prompt |
| `DENIED` | Permanently denied: only iOS Settings can fix it |
| `AUTHORIZED` | Can deliver (also covers `provisional` / `ephemeral`) |

A change from anything else to `AUTHORIZED` is one of the coordinator's resync triggers.

## Removed

| Removed | Why |
|---|---|
| `ReleaseRemindersAvailability`, `StaticReleaseRemindersAvailability` | Always `true` after this feature (research §10) |
| `NoOpReleaseNotificationScheduler`, `NoOpReleaseNotifier` | iOS has real implementations |
| `OnboardingPage.RadarWithoutReminders`, `onboarding_radar_without_reminders_body` | Reminders exist on every platform |
| `ReleaseNotifier.canDeliver()` | Has no callers (research §5) |
| `release_notification_title_format`, `release_notification_body` in `:core:data` Android `strings.xml` | Moved to `:core:ui` Compose resources (research §9) |
