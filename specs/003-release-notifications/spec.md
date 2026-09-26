# Feature Specification: Release Notifications

**Feature Branch**: `003-release-notifications`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Implement Phase 3 of the roadmap: opt-in release notifications for saved games. Each saved game gets a 'Notify me' opt-in, toggleable from wherever the game is presented in Radar and/or its detail screen. The notification fires on the game's release date, using the same refreshed dates Radar already maintains — no separate date source. If a game's release date shifts after the user has opted in, the notification schedule follows the refreshed date, not the original one. Add a 'Notifications' group to the Settings screen with a way to see/manage which games have notifications enabled, and system notification permission handling. Only saved games are eligible."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Opt in to a release reminder (Priority: P1)

A user browsing their Radar timeline or a game's detail screen sees an upcoming, unreleased game they
care about and turns on "Notify me" so they don't have to remember to check back. On release day, they
get a notification instead of having to open the app.

**Why this priority**: This is the entire payoff of the feature — everything else (settings management,
rescheduling, permission handling) exists to support this one moment.

**Independent Test**: Save a game with a future release date, toggle "Notify me" on from either Radar or
the detail screen, and confirm a notification is delivered on the game's release date.

**Acceptance Scenarios**:

1. **Given** a saved game with a known future release date shown in Radar, **When** the user turns on
   "Notify me" for that game, **Then** the opt-in is recorded and reflected as "on" wherever that game is
   shown (Radar and its detail screen).
2. **Given** a game with "Notify me" on, **When** its release date arrives, **Then** the user receives a
   notification identifying that game.
3. **Given** a game with "Notify me" on, **When** the user opens the game's detail screen, **Then** the
   toggle shows the same "on" state set from Radar (and vice versa).
4. **Given** a game with "Notify me" on, **When** the user turns it back off from either surface, **Then**
   no notification is delivered for that game and the "off" state is reflected everywhere the game appears.

---

### User Story 2 - Notification follows a shifted release date (Priority: P1)

A user has opted in to a game whose release date later moves (a common occurrence for unreleased games).
Radar's existing background refresh picks up the new date; the user's notification must silently follow
it rather than firing on the stale date or not firing at all.

**Why this priority**: Without this, the feature actively misleads users — a reminder that fires on a
delayed game's original date, or never fires because the date moved, is worse than no reminder. This is
core to the feature's trust, not a refinement.

**Independent Test**: Opt in to a game, simulate a refreshed release date further in the future, and
confirm the notification now fires on the new date instead of the old one (and not on the old one).

**Acceptance Scenarios**:

1. **Given** a game with "Notify me" on and a scheduled notification for date D1, **When** the periodic
   refresh updates that game's release date to D2, **Then** the notification is rescheduled for D2 and no
   notification fires on D1.
2. **Given** a game with "Notify me" on whose date moves earlier (already passed) before the refresh
   catches it, **When** the refresh resolves the new, past date, **Then** the system does not fire a
   notification for a date that has already elapsed.

---

### User Story 3 - Manage notifications from Settings (Priority: P2)

A user wants an overview of which saved games they'll be reminded about, without having to hunt through
Radar entry by entry, and needs a way to grant or fix the system notification permission if it was denied
or later revoked.

**Why this priority**: Necessary for the feature to be manageable at scale (a dozen opted-in games are
hard to audit one row at a time in Radar) and to recover from a denied permission, but the feature already
delivers its core value via User Story 1 without it.

**Independent Test**: Opt in to two or three games, open Settings, and confirm all of them are listed with
a way to turn each off individually; separately, deny the system notification permission and confirm
Settings surfaces that state with a way to resolve it.

**Acceptance Scenarios**:

1. **Given** one or more saved games have "Notify me" on, **When** the user opens Settings, **Then** a
   "Notifications" group lists those games and lets the user turn any of them off.
2. **Given** the system notification permission has not been granted, **When** the user opens Settings'
   "Notifications" group, **Then** the permission's status is visible and the user is given a way to grant
   it (or is directed to the system settings if it was permanently denied).
3. **Given** the user turns a game's notification off from Settings, **When** they then check that game in
   Radar or its detail screen, **Then** the toggle there also shows "off".

---

### Edge Cases

- A game the user has opted into is unsaved (removed from the wishlist/collection): no notification will
  ever be delivered for it, and it disappears from the Settings management list immediately — but the
  underlying schedule and opt-in record are cleared on the next periodic reconcile, not instantly.
- The system notification permission is denied when the user tries to opt in: the opt-in is not silently
  dropped — the user is prompted to grant permission, and Settings reflects the pending/denied state.
- A game's release date passes without ever resolving to a concrete day (stays TBA): no notification is
  ever scheduled for it, and any "Notify me" control for it communicates that there's nothing to schedule
  yet.
- The device is off, or the app is force-stopped, at the exact moment a notification should fire: it is
  delivered as soon as the system can next run the scheduled work, per platform norms for background
  delivery — it is not silently dropped.
- The user opts in to a game that, unknown to them, already released (e.g., its date resolves to today or
  earlier once refreshed): no notification fires for a date that has already elapsed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to turn a per-game "Notify me" opt-in on or off from wherever that game
  appears in the Radar timeline.
- **FR-002**: Users MUST be able to turn the same per-game opt-in on or off from that game's detail screen.
- **FR-003**: The opt-in state for a given game MUST be consistent across every surface that shows it —
  toggling it in one place is immediately reflected everywhere else it's presented.
- **FR-004**: Only saved games are eligible for the opt-in; the system MUST NOT offer or honor it for games
  that are not saved.
- **FR-005**: When a saved game is unsaved, the system MUST guarantee no notification is ever delivered
  for it afterward, and MUST remove it from the Settings management list immediately. The underlying
  WorkManager schedule and opt-in record MAY persist until the next periodic reconcile (at most one
  Radar refresh cycle) rather than being cancelled at the instant of unsaving.
- **FR-006**: The system MUST deliver a notification identifying the game on the release date of each
  opted-in game, using the same release-date data Radar's periodic refresh already maintains — no
  independent source of release dates is introduced for this feature.
- **FR-007**: When Radar's refresh updates an opted-in game's release date, the system MUST reschedule that
  game's notification to the new date automatically, without requiring the user to re-opt-in.
- **FR-008**: The system MUST NOT deliver a notification for a date that has already elapsed by the time
  it is scheduled or rescheduled.
- **FR-009**: Tapping a delivered release notification MUST take the user to that game's detail screen.
- **FR-010**: The Settings screen MUST include a "Notifications" group that lists every saved game with
  "Notify me" currently on and lets the user turn each one off from that list.
- **FR-011**: The Settings "Notifications" group MUST show the current state of the system notification
  permission and give the user a way to grant it, or to reach the system settings to fix it if it has been
  permanently denied.
- **FR-012**: The first time a user opts in and the system notification permission has not been granted,
  the system MUST request it; if the user declines, the opt-in MUST NOT silently behave as "on" without
  ever being able to notify.
- **FR-013**: A game whose release date has no known day precise enough to notify on MUST NOT have a
  notification scheduled for it yet, and its opt-in control MUST make clear that no reminder will fire
  until a precise date is known.
- **FR-014**: Users MUST be able to turn "Notify me" on for a saved game even while its release date is
  only known to a coarser precision than an exact day (month, quarter, year-only, or TBD). The opt-in is
  recorded immediately; the system MUST automatically schedule the notification, with no further user
  action, the moment a later refresh resolves that game's date to an exact day.
- **FR-015**: When a saved game has more than one relevant release date (it releases on more than one
  platform the user owns, each with its own date), the system MUST fire a single notification timed to the
  earliest of those resolved dates. The per-game opt-in does not need to be tracked separately per
  platform.

### Key Entities

- **Notification Opt-In**: The per-saved-game record of whether release reminders are on for that game.
  Tied to a single saved game; cleared when the game is unsaved.
- **Release Notification**: The one-time reminder delivered to the user on (or as soon as possible after)
  an opted-in game's resolved release date. Carries enough identity to link back to the game it's about.
- **Saved Game / Release Date**: Reused from Radar (Phase 2) — a saved game's release date(s), including
  precision, refreshed periodically by the existing background job. This feature reads that data; it does
  not introduce a second copy of it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can turn "Notify me" on for a saved game in two actions or fewer from either Radar or
  the game's detail screen.
- **SC-002**: Every game with "Notify me" on and a precise, future release date produces exactly one
  notification on its release day, with no duplicate and no missed notification, across normal app usage.
- **SC-003**: When a game's release date shifts after opt-in, the user is reminded on the new date, not
  the original one, without having to revisit the opt-in themselves.
- **SC-004**: A user can see every game they currently have notifications enabled for, and turn any of
  them off, within a single Settings screen.
- **SC-005**: A user who has denied the system notification permission can still tell, from within the
  app, that their opt-ins won't fire and how to fix that — they are never left silently believing a
  reminder is scheduled when it cannot be delivered.

## Assumptions

- "Notify me" is offered on both surfaces named in the request (Radar and the detail screen), not a
  choice between them — the request's "and/or" is read as "everywhere the game is presented," consistent
  with how the rest of the app keeps state consistent across surfaces (e.g., saved state itself).
- A release notification is a one-time event per game, delivered once on (or, if the device could not
  fire it exactly on time, as soon as possible after) the resolved release date — not a repeating or
  escalating reminder.
- The Settings "Notifications" group is a management surface (view + turn off), not the primary place
  users opt in; opting in happens where the game itself is shown (Radar, detail screen), per the request.
- Denying or later revoking the system notification permission does not delete a user's existing opt-ins;
  it only prevents delivery until permission is restored, at which point previously opted-in games resume
  being eligible for their next scheduled notification.
- No notification is scheduled for a game that has no saved game state (e.g., an item only appearing in a
  non-saved list elsewhere in the app) — this mirrors Radar's existing "saved games only" scope and is not
  a new restriction introduced by this feature.
- Unsaving a game does not synchronously cancel its scheduled work or delete its opt-in row — that
  happens on the next periodic reconcile — but no notification is ever delivered for it in the meantime,
  since delivery re-verifies the saved state at fire time.
