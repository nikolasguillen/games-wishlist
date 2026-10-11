# Feature Specification: iOS Release Notifications

**Feature Branch**: `013-ios-push-notifications`

**Created**: 2026-10-11

**Status**: Draft

**Input**: User description: "we should implement push notifications for iOS"

## Background

Android users can turn on "Notify me" for a saved game and get a notification on its release day. The
reminder follows the game's release date when the date shifts, a tap on it opens the game, and Settings
lists every saved game with its own toggle plus the state of the system notification permission.

On iOS none of that exists. The reminder controls are hidden everywhere (Radar, game detail, Settings), the
welcome flow shows a page explaining that reminders are unavailable, and nothing is ever delivered. The
roadmap lists this as "Release reminders on iOS". This feature brings iOS to parity with Android: the same
opt-in, the same notification, the same management screen.

The release dates themselves are already kept current on iOS by the in-app refresh and by the background
refresh of spec 012. This feature reads those dates and adds no second source of them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Opt in to a release reminder on iOS (Priority: P1)

An iPhone user browsing Radar or a game's detail screen sees an upcoming, unreleased saved game and turns
on "Notify me". The first time, the system asks for permission to send notifications. On the game's release
day the user gets a notification naming the game, without having to open the app.

**Why this priority**: This is the whole payoff of the feature. Everything else (rescheduling, tapping
through, Settings) supports this one moment.

**Independent Test**: On an iOS device or simulator, save a game with a release date a few minutes away (or
use a date shifted for testing), turn "Notify me" on, grant permission, close the app, and confirm a
notification naming the game arrives at the expected time.

**Acceptance Scenarios**:

1. **Given** a saved game with a known future release day shown in Radar, **When** the user turns on
   "Notify me", **Then** the opt-in is recorded and shown as "on" in Radar, on the game's detail screen and
   in Settings.
2. **Given** a game with "Notify me" on, **When** its release day arrives, **Then** the user receives a
   notification identifying that game, even if the app is closed.
3. **Given** the user has never been asked for notification permission, **When** they turn "Notify me" on
   for the first time, **Then** the system permission prompt appears, and if they allow it the reminder is
   scheduled.
4. **Given** a game with "Notify me" on, **When** the user turns it off from any surface, **Then** no
   notification is delivered for that game and "off" is shown everywhere the game appears.
5. **Given** a saved game whose release date is only known to a month, quarter, year or is TBA, **When**
   the user turns "Notify me" on, **Then** the opt-in is recorded, the control makes clear that no reminder
   will fire until an exact day is known, and the reminder is scheduled automatically once a later refresh
   resolves an exact day.

---

### User Story 2 - The reminder follows a shifted release date (Priority: P1)

A user has opted in to a game whose release date later moves. The app picks up the new date, and the
reminder silently moves with it: it does not fire on the old date and does not stop firing.

**Why this priority**: Release dates for unreleased games shift often. A reminder on a stale date misleads
the user, which is worse than no reminder.

**Independent Test**: Opt in to a game, change its stored release date to a later day through a refresh,
and confirm the pending reminder is now for the new day only.

**Acceptance Scenarios**:

1. **Given** a game with "Notify me" on and a reminder for day D1, **When** a refresh (in-app or
   background) changes its release date to D2, **Then** the reminder is for D2 and nothing is delivered on
   D1.
2. **Given** a game with "Notify me" on whose refreshed date is already in the past, **When** the refresh
   resolves it, **Then** no notification is delivered for the elapsed date.
3. **Given** a saved game with "Notify me" on, **When** the user unsaves it, **Then** no notification is
   ever delivered for it afterward, and it leaves the Settings list.
4. **Given** a game has more than one release date (several platforms), **When** the reminder is
   scheduled, **Then** a single notification is timed to the earliest resolved date.

---

### User Story 3 - Tapping a reminder opens the game (Priority: P2)

When a release notification arrives and the user taps it, the app opens on that game's detail screen,
whether the app was closed, in the background or in the foreground.

**Why this priority**: The notification is a prompt to act. Landing on the Radar or the home screen instead
of the game wastes the moment, but the reminder is still useful without it.

**Independent Test**: Deliver a release notification for a saved game, tap it from the lock screen and from
the notification centre with the app closed, and confirm the game's detail screen is shown each time.

**Acceptance Scenarios**:

1. **Given** a delivered release notification and the app closed, **When** the user taps it, **Then** the
   app launches on that game's detail screen.
2. **Given** a delivered release notification and the app running in the background, **When** the user taps
   it, **Then** the app comes forward on that game's detail screen.
3. **Given** the app is open in the foreground when a reminder is due, **When** it fires, **Then** the user
   still sees it as a banner and can tap it to open the game.
4. **Given** the notification refers to a game the user has since unsaved, **When** it is tapped, **Then**
   the app opens normally and does not crash or show an empty screen.

---

### User Story 4 - Manage reminders and fix permission from Settings (Priority: P2)

A user opens Settings on iOS and sees the same "Notifications" group Android has: every saved game with its
own "Notify me" toggle, and the state of the system notification permission with a way to fix it if it was
denied.

**Why this priority**: It makes the feature manageable when many games are opted in and lets a user recover
from a denied permission. The core value of User Story 1 does not depend on it.

**Independent Test**: Opt in to two games, open Settings and confirm both are listed and toggleable
without leaving the list; deny permission in iOS Settings and confirm the app says so and offers a way to
resolve it.

**Acceptance Scenarios**:

1. **Given** saved games, **When** the user opens the Notifications screen from Settings, **Then** every
   saved game is listed with its own toggle, and flipping a toggle does not remove the row.
2. **Given** notification permission was denied or later revoked, **When** the user opens the Notifications
   group in Settings, **Then** the denied state is visible and the user is taken to the system settings for
   the app to fix it.
3. **Given** permission was revoked while games were opted in, **When** it is granted again, **Then** the
   previously opted-in games resume getting their reminders without the user re-opting in.
4. **Given** the user declines the permission prompt on first opt-in, **When** the decision is made,
   **Then** the opt-in is not shown as "on" as if a reminder were scheduled, and the user can tell why.

---

### User Story 5 - The welcome flow stops saying reminders are unavailable (Priority: P3)

A new iOS user going through the welcome flow no longer sees the page that says release reminders are not
available on iOS. The flow matches the one on Android, including the page that introduces reminders.

**Why this priority**: Leaving a "not available" message once the feature exists is wrong, but it is a
copy and flow change with no effect on whether reminders work.

**Independent Test**: Reset the welcome flow on iOS and walk through it; no page says reminders are
unavailable, and the reminders introduction appears.

**Acceptance Scenarios**:

1. **Given** a fresh install on iOS, **When** the user goes through the welcome flow, **Then** the
   "reminders unavailable" page is not shown and the reminders page from Android's flow is.

---

### Edge Cases

- **Many opted-in games.** iOS holds only a limited number of pending notifications per app (64). With more
  opted-in games than that, the soonest ones MUST be scheduled and the rest scheduled as earlier ones
  fire or are removed, so the nearest release is never the one left out.
- **App reinstalled or data cleared.** The opt-ins live with the app's data; after a reinstall nothing is
  scheduled and nothing is opted in. No stale notification from a previous install is delivered.
- **Permission denied at opt-in.** The opt-in is not silently dropped and not shown as working; see User
  Story 4.
- **Permission revoked later in iOS Settings.** Existing opt-ins are kept, nothing is delivered, and the
  Settings group shows the denied state.
- **Device date, time zone or clock changes.** A reminder aimed at a calendar day still lands on that day in
  the user's current time zone; a user who travels is not notified a day early or late.
- **Device off or in Focus / Do Not Disturb at the time.** Delivery follows the system's normal rules for
  notifications; the reminder is not dropped by the app.
- **Release date that is TBA or imprecise.** No notification is scheduled until an exact day is known.
- **Date refreshed to a day in the past.** No notification is delivered for an elapsed date.
- **Game opted in that was already released.** No notification is delivered for it.
- **Notification arrives for a game since unsaved.** It is not delivered: delivery re-checks that the game
  is still saved.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On iOS, release reminders MUST be scheduled on the user's device from the release dates the
  app already holds, as on Android, and delivered for opted-in saved games. No server is involved: nothing
  is sent to the device from outside it.
- **FR-002**: Users MUST be able to turn a per-game "Notify me" opt-in on or off from wherever the game
  appears in the Radar timeline, from the game's detail screen, and from the Settings Notifications screen.
- **FR-003**: The opt-in state for a game MUST be the same on every surface that shows it, and changing it
  in one place MUST be reflected immediately in the others.
- **FR-004**: Only saved games MUST be eligible for the opt-in. When a game is unsaved, no notification
  MUST ever be delivered for it afterward.
- **FR-005**: The system MUST deliver one notification identifying the game on the release day of each
  opted-in game with an exact release day, using the release dates the app already maintains and no other
  date source.
- **FR-006**: When a refresh changes an opted-in game's release date, the pending reminder MUST move to the
  new date without the user re-opting in. This MUST hold for the in-app refresh and for the background
  refresh.
- **FR-007**: The system MUST NOT deliver a notification for a date that has already elapsed when it is
  scheduled or rescheduled.
- **FR-008**: A game with more than one relevant release date MUST get a single notification timed to the
  earliest resolved date.
- **FR-009**: Users MUST be able to opt in a game whose date is only known to month, quarter, year or TBA.
  The opt-in is recorded at once, the control MUST say that no reminder will fire until an exact day is
  known, and the reminder MUST be scheduled automatically when a later refresh resolves an exact day.
- **FR-010**: The first time a user opts in without notification permission granted, the system MUST ask
  for it. If the user declines, the opt-in MUST NOT behave or appear as an active reminder.
- **FR-011**: The Settings Notifications group MUST show the state of the system notification permission
  and, when it is denied, take the user to the system settings for the app.
- **FR-012**: When permission is granted again after being denied or revoked, previously opted-in games
  MUST resume getting their reminders without the user re-opting in.
- **FR-013**: Tapping a release notification MUST open the app on that game's detail screen, from a closed,
  backgrounded or foreground app. A tap for a game that no longer exists MUST open the app normally.
- **FR-014**: A reminder that comes due while the app is open MUST still be shown to the user.
- **FR-015**: When more games are opted in than the system can hold as pending notifications, the soonest
  releases MUST be the ones scheduled, and the rest MUST be scheduled as capacity frees up.
- **FR-016**: A reminder MUST land on the release calendar day in the user's current time zone, at the same
  time of day Android uses.
- **FR-017**: The iOS welcome flow MUST stop showing the "reminders unavailable" page and MUST show the
  same reminders introduction as Android.
- **FR-018**: Android behaviour MUST NOT change. The reminder controls, copy and rules the two platforms
  share MUST be the same on both.

### Key Entities

- **Notification Opt-In**: The per-saved-game record of whether the user wants a release reminder. Tied to
  one saved game and cleared when the game is unsaved. Already exists; iOS reads and writes the same record.
- **Release Notification**: The one-time reminder delivered on a game's resolved release day. It carries
  enough identity to open the game's detail screen when tapped.
- **Notification Permission State**: Whether the user has allowed, denied or not yet been asked about
  notifications for the app, as the system reports it.
- **Saved Game / Release Date**: Reused from Radar, including the date's precision. This feature reads it
  and holds no second copy.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can turn "Notify me" on for a saved game in two actions or fewer from Radar or the
  game's detail screen, not counting the one-time system permission prompt.
- **SC-002**: Every opted-in game with an exact future release day produces exactly one notification on
  that day on a test device, with no duplicate and no miss, across normal use including closing and
  reopening the app.
- **SC-003**: After a release date shifts, 100% of opted-in games are reminded on the new day and none on
  the old one, once the app has refreshed that date.
- **SC-004**: Tapping a release notification lands the user on the right game's detail screen in 100% of
  attempts, from a closed, backgrounded or foreground app.
- **SC-005**: A user who has denied permission can tell from inside the app that their reminders will not
  fire and reach the system settings to fix it in one tap from the Settings Notifications group.
- **SC-006**: With more than 64 opted-in games, the 64 soonest releases are always the ones scheduled.
- **SC-007**: No screen on iOS tells the user that release reminders are unavailable, and the Android
  reminder behaviour is unchanged (all existing tests still pass).

## Assumptions

- "Push notifications" means release reminders scheduled on the device, in line with the roadmap's
  "Release reminders on iOS" (confirmed by the owner). Server-sent push through Apple's push service is out
  of scope; it would need a backend that QuestLog does not have, and can be its own feature later.
- The feature follows the Android rules for what a reminder is: one notification per game, on its release
  day, not repeated or escalated, and with the same wording per language.
- Release dates on iOS are kept current by the existing in-app refresh and the background refresh of spec
  012. Because the system decides when a background refresh runs, a date change can take until the next
  app launch to reach the pending reminder; this is accepted.
- The release-reminder abstraction in shared code already exists. The iOS work is a platform
  implementation of it, plus making the entry points visible there.
- iOS distribution to a physical device (signing, provisioning, TestFlight) stays out of scope. Delivery
  is verified on the simulator, and on a physical device when one is available.
- Notification text is localised through the same resources as the rest of the app.
- Anything only a Mac can verify (simulator delivery, the permission prompt, tapping a notification) is
  verified there and called out as such.
