# Feature Specification: Desktop App (Windows and macOS)

**Feature Branch**: `020-desktop-app`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Make QuestLog available as a full desktop app on Windows and macOS, alongside the existing Android and iOS apps. Desktop becomes the third supported platform. Release reminders work the same as on mobile, including when the app is closed, using each operating system's own notification scheduling. Cloud account and backup work, so desktop and phone share data. On-device translation and background refresh while the app is closed are not supported and not offered. Linux is out of scope." (Full brief in the conversation that opened this feature.)

## Background

QuestLog runs on Android and iOS from one shared codebase: about 94 % of the code is shared, and every
platform-specific piece sits behind a contract with one implementation per platform. Spec 010 settled the
scope as "iOS only, no desktop". This feature reverses that decision: **Windows and macOS become the third
supported platform**, as a full app and not a read-only companion. Linux is not part of it.

The goal is parity of experience, not a redesign. A desktop user gets Search, game detail, saved games and
lists, Radar, Settings and the welcome flow, with the same data, wording and rules as on a phone. The
adaptive layouts that already exist (the navigation rail and the two-pane Search and game detail layout on
wide windows) are the desktop layout; there are no desktop-only screens.

Two things from mobile do not carry over, and they are absent on desktop rather than shown disabled:
on-device description translation, and refreshing release dates in the background while the app is closed.

Release reminders do carry over, including delivery **while the app is closed**, which is the demanding part:
the operating system, not a resident process, has to deliver them on the release day.

Windows is the primary development and verification platform; macOS must work equally.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Use QuestLog as a desktop app (Priority: P1)

A user opens QuestLog on a Windows PC or a Mac. They search for a game, open its page, save it, put it in a
list, look at Radar, change settings and walk through the welcome flow, exactly as they would on a phone. They
resize the window and the layout adapts. They quit and reopen the app and everything they saved is still
there.

**Why this priority**: Without a working full app there is nothing else to deliver. Every other story builds
on it, and it is the smallest slice that is useful by itself.

**Independent Test**: On a clean Windows machine and on a Mac, start the app, complete the welcome flow, search
for a game, save it, add it to a list, open Radar and Settings, close the app, reopen it, and confirm the data
persisted and every screen worked against the live game database.

**Acceptance Scenarios**:

1. **Given** a fresh install on Windows or macOS, **When** the user opens the app for the first time, **Then**
   the welcome flow is shown and, once finished, the app opens on Search with the same navigation destinations
   as on a phone.
2. **Given** the app is open, **When** the user searches, opens a game, saves it and adds it to a list,
   **Then** each step behaves as on mobile (same results, same wording, same states), including the
   character and developer search and their detail pages.
3. **Given** the window is narrow, **When** the user widens it past the wide-window threshold used on mobile
   tablets, **Then** Search and game detail show side by side, and narrowing it again returns to the single
   pane without losing the user's place.
4. **Given** saved games, lists and settings, **When** the user quits and reopens the app, **Then** all of it
   is still there, including the "welcome flow done" state.
5. **Given** the user has no network connection, **When** they open the app, **Then** their saved data is
   shown and network-dependent screens show the same offline and error states as on mobile.
6. **Given** the user has only a mouse and keyboard, **When** they use the app, **Then** every action
   available on mobile can be reached, text fields accept the standard clipboard and selection shortcuts, and
   the list-like screens scroll with the mouse wheel or trackpad.

---

### User Story 2 - Release reminders arrive, even when the app is closed (Priority: P1)

A user turns on "Notify me" for an upcoming saved game and closes the app. On the game's release day the
operating system shows a notification naming the game, at the same time of day the iOS app uses. The app does
not have to be running, and no helper process is left running in the background to make this happen.

**Why this priority**: This is the payoff of reminders and the hardest requirement of the feature. A reminder
that only works while the app is open is not a reminder. It shares top priority with Story 1 because the owner
asked for it in this feature rather than as a follow-up.

**Independent Test**: On Windows and on macOS, save a game with a release date a few minutes away (or shift a
stored date for the test), turn "Notify me" on, grant permission, quit the app completely, and confirm the
notification appears at the expected time with the app not running.

**Acceptance Scenarios**:

1. **Given** an opted-in saved game with an exact future release day and the app fully closed, **When** that
   day arrives, **Then** the operating system delivers exactly one notification naming the game, at the same
   time of day the iOS app uses.
2. **Given** the app is open when a reminder comes due, **When** it fires, **Then** the user still sees it as a
   system notification.
3. **Given** a game with "Notify me" on, **When** the user turns it off or unsaves the game, **Then** no
   notification is delivered for it afterward, including from a closed app.
4. **Given** a game with "Notify me" on and a reminder for day D1, **When** a refresh run while the app is
   open moves its release date to D2, **Then** the reminder is for D2 and nothing is delivered on D1.
5. **Given** the app was closed while a game's release date changed on another device, **When** the user next
   launches the app, **Then** the refreshed date is picked up and the reminders are re-synced to it. Until
   then a reminder fires on the date known when it was armed (see Edge Cases).
6. **Given** a game whose release date is TBA or only known to month, quarter or year, **When** the user opts
   in, **Then** the opt-in is recorded, the control says no reminder will fire until an exact day is known, and
   the reminder is armed automatically once a refresh resolves an exact day.
7. **Given** a game has several release dates (platforms), **When** the reminder is armed, **Then** a single
   notification is timed to the earliest resolved date.

---

### User Story 3 - Tapping a reminder opens the game (Priority: P2)

When a release notification arrives and the user clicks it, QuestLog comes forward on that game's detail
screen. If the app was closed it launches first; if it was already open it does not start a second copy.

**Why this priority**: The notification is a prompt to act, and landing on the right game completes the
moment. The reminder is still useful without it, so it ranks below delivery.

**Independent Test**: Deliver a release notification for a saved game, click it with the app closed and again
with the app open in the background, and confirm the game's detail screen is shown each time in a single app
window.

**Acceptance Scenarios**:

1. **Given** a delivered notification and the app closed, **When** the user clicks it, **Then** the app
   launches on that game's detail screen.
2. **Given** a delivered notification and the app open (focused or not), **When** the user clicks it, **Then**
   the existing window comes forward on that game's detail screen and no second window or process is created.
3. **Given** the notification refers to a game the user has since unsaved, **When** it is clicked, **Then** the
   app opens normally and shows neither a crash nor an empty screen.

---

### User Story 4 - Manage reminders and fix permission from Settings (Priority: P2)

A user opens Settings and sees the same "Notifications" group as on mobile: every saved game with its own
"Notify me" toggle, and the state of the operating system's notification permission for QuestLog. If
notifications are blocked, the app says so and takes the user to the system's settings for the app.

**Why this priority**: It makes the feature manageable and recoverable. The core value of Story 2 does not
depend on it.

**Independent Test**: Opt in two games, open the Notifications screen and confirm both are listed and
toggleable; block notifications for the app in the operating system and confirm the app shows the blocked
state and offers a way to fix it.

**Acceptance Scenarios**:

1. **Given** saved games, **When** the user opens the Notifications screen from Settings, **Then** every saved
   game is listed with its own toggle and flipping it does not remove the row.
2. **Given** the first opt-in without permission, **When** the user turns "Notify me" on, **Then** the app
   asks for permission through whatever mechanism the operating system provides; if it is declined, the opt-in
   does not appear as an active reminder and the user can tell why.
3. **Given** notifications are blocked for the app in the operating system, **When** the user opens the
   Notifications group, **Then** the blocked state is visible and the user is taken to the system settings for
   the app.
4. **Given** permission is granted again after being blocked, **When** the app next sees it, **Then**
   previously opted-in games resume getting their reminders without the user re-opting in.
5. **Given** the welcome flow, **When** the user goes through it on desktop, **Then** the reminders page
   behaves as on mobile, including the blocked-permission case, and no page says reminders are unavailable.

---

### User Story 5 - Sign in and share data with the phone (Priority: P2)

A user signs in to their QuestLog cloud account on desktop and sees the backup they made on their phone;
a backup made on desktop restores on the phone. Both directions work and use the same data and rules as the
mobile apps.

**Why this priority**: It is what makes desktop part of the same product instead of a separate island, but
the app is fully usable locally without it.

**Independent Test**: Make a backup on a phone, sign in with the same account on desktop and restore it, then
change the wishlist on desktop, back up, and restore on the phone.

**Acceptance Scenarios**:

1. **Given** a backup made on Android or iOS, **When** the user signs in on desktop and restores, **Then** the
   desktop shows the same saved games, lists and settings the backup holds.
2. **Given** a backup made on desktop, **When** the user restores it on a phone, **Then** the phone shows the
   same data.
3. **Given** the user signs in on desktop, **When** the sign-in completes, **Then** they are signed in to the
   same account they use on mobile, using the same account providers mobile offers.
4. **Given** the user signs out or deletes the account on desktop, **When** they do, **Then** it behaves as on
   mobile, including clearing the provider state.

---

### User Story 6 - Install it like a normal desktop app (Priority: P3)

A user gets an installer for their operating system, installs QuestLog, and finds it in the Start menu or the
Applications folder like any other app. A developer can start the app straight from the repository with one
documented command to work on it.

**Why this priority**: Distribution matters for real users, but a developer can already verify Stories 1 to 5
from the repository, so it ranks after them.

**Independent Test**: Build the installer on a Windows machine and on a Mac, install it on a clean user
account, launch from the Start menu or Applications, and confirm the app runs and reminders from Story 2 work
from the installed copy.

**Acceptance Scenarios**:

1. **Given** the repository, **When** a developer runs the documented command on Windows or macOS, **Then**
   the app starts.
2. **Given** a Windows machine, **When** the installer is built, **Then** a `.msi` is produced; on a Mac,
   **When** the installer is built, **Then** a `.dmg` is produced.
3. **Given** an installed copy, **When** the user uninstalls it, **Then** the app and the reminders it armed
   are removed (see Edge Cases for what the platform can guarantee).

---

### Edge Cases

- **Reminder armed before a date change, app closed.** The operating system fires on the date known when the
  reminder was armed; nothing runs to correct it while the app is closed. The next launch re-syncs reminders
  to the refreshed dates. This is accepted by the owner (see Assumptions).
- **Date refreshed to a day in the past.** No notification is delivered for an elapsed date.
- **Game already released when opted in.** No notification is delivered for it.
- **Many opted-in games.** If the operating system caps pending notifications per app, the soonest releases
  are the ones armed and the rest are armed as earlier ones fire or are removed, as on iOS.
- **Computer off, asleep or in Do Not Disturb / Focus at the time.** Delivery follows the operating system's
  normal rules for notifications; the app does not drop the reminder. Whether the OS delivers a missed one when
  the machine wakes is recorded in the plan for each platform.
- **Permission blocked or never granted.** Opt-ins are kept, nothing is delivered, and the Settings group
  shows the state; see User Story 4.
- **Time zone or clock changes.** A reminder aimed at a calendar day lands on that day in the user's current
  time zone.
- **Second launch of the app.** Starting the app again, or clicking a notification, while it is running brings
  the existing window forward and does not open a second instance, which would otherwise open the local
  database twice.
- **Reinstall, data cleared or uninstall.** No stale notification from a previous install is delivered. Where
  the platform cannot remove pending notifications on uninstall, the plan says so and the first launch after a
  reinstall clears them.
- **Backup restore on a machine that has opt-ins armed.** Reminders end up matching the restored data; none
  remain armed for games the restore removed.
- **Offline at launch.** Release-date refresh is skipped without blocking the app; the staleness check runs
  again at the next launch or while the app is open.
- **Window very small or very large.** The layout stays usable at the smallest supported window size and uses
  the wide-window layout on large ones; the user's window size and position are not required to persist.
- **Unsupported features.** On-device translation and its Settings row, its badge and its download flow, and
  any "background refresh" wording do not appear anywhere on desktop.

## Requirements *(mandatory)*

### Functional Requirements

**The app**

- **FR-001**: The system MUST provide a desktop app for Windows and macOS containing every user-facing screen
  and flow that exists on Android and iOS: Search (games, characters, developers, with their detail pages),
  game detail, saved games and lists, Radar, Settings, the account and backup screens, the welcome flow and
  the deep-link routes that open a game.
- **FR-002**: The desktop app MUST reuse the existing adaptive layouts, including the navigation rail and the
  two-pane Search and game detail layout on wide windows, and MUST NOT introduce desktop-only screens.
- **FR-003**: The desktop app MUST behave as mobile does for the same data: the same wording, rules, states
  and error and offline handling. Text MUST come from the same resources as the other platforms.
- **FR-004**: The desktop window MUST be resizable and the layout MUST adapt as it changes size, without
  losing the user's place.
- **FR-005**: Every action available on mobile MUST be reachable with a mouse and keyboard, a back action MUST
  have a desktop equivalent, and text input MUST support the standard clipboard and selection shortcuts.
- **FR-006**: Only one instance of the app MUST run per user at a time. A second launch MUST bring the
  running window forward, including when it carries a game to open.

**Local data**

- **FR-007**: Saved games, lists, settings and the "welcome flow done" state MUST persist on the machine,
  in the operating system's per-user application data location, and survive quitting and reopening the app.
- **FR-008**: Desktop MUST use the same data model and the same release-date sources as mobile and MUST NOT
  add a second source of release dates.

**Reminders**

- **FR-009**: Users MUST be able to turn a per-game "Notify me" opt-in on or off from the same places as on
  mobile (Radar, game detail and the Settings Notifications screen), and the state MUST be the same on every
  surface that shows it.
- **FR-010**: Only saved games MUST be eligible for the opt-in. When a game is unsaved, no notification MUST
  be delivered for it afterward.
- **FR-011**: The system MUST deliver exactly one notification naming the game on the release day of each
  opted-in game with an exact release day, at the same local time of day the iOS app uses, using only the
  release dates the app already maintains.
- **FR-012**: Delivery MUST work while the app is closed. It MUST rely on the operating system's own
  notification scheduling and MUST NOT depend on a process that stays resident in the background.
- **FR-013**: When a refresh run while the app is open changes an opted-in game's release date, the pending
  reminder MUST move to the new date without the user re-opting in. On each launch the app MUST re-sync all
  armed reminders to the current dates.
- **FR-014**: The system MUST NOT deliver a notification for a date that has already elapsed when it is armed
  or re-armed, nor for a game already released.
- **FR-015**: A game with more than one relevant release date MUST get a single notification timed to the
  earliest resolved date. A game whose date is only known to month, quarter, year or TBA MUST accept the
  opt-in, say that no reminder fires until an exact day is known, and be armed automatically once one is.
- **FR-016**: Clicking a release notification MUST open the app on that game's detail screen, from a closed
  or running app, in a single window. A click for a game that no longer exists MUST open the app normally.
- **FR-017**: The first opt-in without notification permission MUST ask for it through the operating
  system's mechanism. If it is declined the opt-in MUST NOT behave or appear as an active reminder.
- **FR-018**: Settings MUST show the state of the operating system's notification permission for the app and,
  when it is blocked, take the user to the system settings for the app. When permission is granted again,
  previously opted-in games MUST resume getting reminders without re-opting in.
- **FR-019**: A reminder MUST land on the release calendar day in the user's current time zone.
- **FR-020**: When the operating system limits how many notifications an app can have pending, the soonest
  releases MUST be the ones armed, and the rest MUST be armed as capacity frees up.
- **FR-021**: Reminders armed by the app MUST be cancelled when the user uninstalls it, to the extent the
  platform allows, and the plan MUST state what each platform guarantees.
- **FR-022**: The welcome flow on desktop MUST show the same reminders introduction as on mobile and MUST NOT
  show any page saying reminders are unavailable.

**Cloud account and backup**

- **FR-023**: Users MUST be able to sign in, sign out, back up and restore on desktop, with the same data
  model, rules and account as on mobile, so a backup made on one platform restores on any other.
- **FR-024**: Desktop MUST offer the same sign-in providers as mobile. If a provider cannot be offered on a
  platform, the account screen MUST NOT show it, and the planning phase MUST report this to the owner.

**Platform features that are not supported**

- **FR-025**: On-device description translation MUST NOT be offered on desktop: no translate action, badge,
  download row or Settings entry. Descriptions are shown in their original language.
- **FR-026**: Release-date refresh MUST run when the app starts and while it is open (the existing 24-hour
  staleness check) and MUST NOT run while the app is closed. The desktop UI MUST NOT mention a background
  refresh.
- **FR-027**: No control on desktop MUST exist that does nothing. Anything not supported is absent.

**Distribution and development**

- **FR-028**: The project MUST produce a `.msi` installer on Windows and a `.dmg` installer on macOS, each
  built on its own operating system, and MUST document one command that starts the app from the repository on
  each.
- **FR-029**: The macOS build MUST be signed and notarized to the extent the operating system requires for
  notifications to work. The plan MUST state what the owner has to provide (for example an Apple developer
  account and certificates) and what, if anything, is still not verifiable without it.
- **FR-030**: Linux MUST NOT be a target, an installer or a tested platform.

**Compatibility and documentation**

- **FR-031**: Android and iOS MUST keep building and passing their tests at every commit. Desktop-specific
  code MUST NOT leak into the other platforms and the module boundaries in `CLAUDE.md` still hold.
- **FR-032**: Each new platform capability desktop needs MUST be added to the platform contracts first, with
  what each platform does, and be reached through a contract owned by shared code with one implementation per
  platform.
- **FR-033**: The project's documentation that states "iOS is the only platform besides Android"
  (`CLAUDE.md`, the constitution, `docs/roadmap.md`, the platform contracts) MUST be updated to make desktop
  the third supported platform, in the same change. A constitution amendment needs the owner's approval.

### Key Entities

- **Desktop App**: The Windows and macOS build of QuestLog: one window, one instance per user, the existing
  screens and layouts.
- **Release Reminder**: The one-time notification armed for an opted-in saved game on its resolved release
  day. It is scheduled with the operating system, carries enough identity to open the game's detail screen,
  and exists only while the game is saved and opted in.
- **Notification Opt-In**: The per-saved-game record of whether the user wants a reminder. Already exists;
  desktop reads and writes the same record.
- **Notification Permission State**: Whether the operating system allows, blocks or has not yet been asked
  about notifications for the app.
- **Saved Game / Release Date**: Reused from Radar, including the date's precision. Desktop holds no second
  copy.
- **Cloud Account / Backup**: The user's signed-in identity and the backup they share across platforms.
  Already exists; desktop is a new client of it.
- **Installer**: The distributable package per supported operating system (`.msi`, `.dmg`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a Windows machine and on a Mac, a developer can start the app from the repository with a
  single documented command, and 100 % of the screens and flows listed in FR-001 open and work against the
  live game database.
- **SC-002**: A user can go from first launch to a saved game in a list in the same number of steps as on a
  phone.
- **SC-003**: Data saved, then the app quit and reopened, is present 100 % of the time.
- **SC-004**: A backup made on a phone restores on desktop and a backup made on desktop restores on a phone,
  with every saved game, list and setting the backup holds present afterward.
- **SC-005**: Every opted-in game with an exact future release day produces exactly one notification on that
  day on both operating systems with the app fully closed, with no duplicate and no miss across normal use
  including quitting and reopening.
- **SC-006**: Clicking a release notification lands the user on the right game's detail screen in 100 % of
  attempts from a closed or running app, always in a single window.
- **SC-007**: After a release date is refreshed while the app is open, 100 % of opted-in games are reminded on
  the new day and none on the old one; after a launch following a closed-app change, the same holds.
- **SC-008**: Turning a reminder off, or unsaving the game, results in no notification for that game in 100 %
  of test runs, including when the app is closed.
- **SC-009**: A user who has blocked notifications can tell from inside the app that reminders will not fire
  and reach the system settings in one action from the Settings Notifications group.
- **SC-010**: A `.msi` is produced on Windows and a `.dmg` on macOS, and the reminder behavior of SC-005
  holds from the installed copy, not only from a development run.
- **SC-011**: No screen on desktop offers translation, mentions background refresh, or shows a control that
  does nothing.
- **SC-012**: All existing Android and iOS tests still pass, and Android builds at every commit.

## Assumptions

- "Desktop" means Windows and macOS only. Linux, web, tablets and TV targets are out of scope.
- The same reminder rules as mobile apply: one notification per game, on its release day, not repeated or
  escalated, with the same wording per language and the same local time of day iOS uses.
- Release dates on desktop are kept current by the existing in-app refresh (at launch and while the app is
  open). With the app closed nothing refreshes them, so a date change can take until the next launch to reach
  an armed reminder; this is accepted by the owner.
- Closed-app delivery uses the operating system's own scheduling (scheduled toast notifications on Windows,
  user notifications on macOS) and no resident background process; this is the owner's decision. Whether
  each one works from a development run, an installed copy, or only a signed one is a planning question.
- A macOS build that the system accepts for notifications has to be signed and notarized. If the owner has no
  Apple developer account, closed-app reminders on macOS cannot be verified end to end, and SC-005, SC-006
  and SC-010 are verified on Windows only for the time being. The plan states what the owner needs to supply.
- The Windows installer is not code-signed in this feature; the operating system may show an unrecognised
  publisher warning on first run. Signing it is a later decision.
- Desktop is verified on Windows first and on a Mac when one is at hand; anything only the Mac can verify is
  said so rather than assumed.
- Cloud backup keeps the data model, security rules and account system already in place. No new backend is
  introduced.
- Window size and position are not required to persist between launches.
- The first-time experience, strings and layouts are reused unchanged; any desktop-specific copy (for example
  the way the system settings are named on each operating system) is localised through the same resources.

### Points for the planning phase (not decided here)

1. How sign-in works on desktop, since the mobile mechanisms (Android's account picker, Sign in with Apple's
   native sheet) do not exist there; likely a system-browser sign-in. This decides FR-024 for each provider.
2. Whether the cloud library the project uses supports the account and database features on the desktop
   runtime. If it does not, the plan must propose an alternative; that is a library swap the owner decides.
3. How the app reaches each operating system's notification scheduling, what identity or packaging each
   needs, and what that adds to the build and installers.
4. Whether a missed reminder (computer off at the time) is delivered when it wakes, per platform.
5. New module or dependency edges, if any, called out in the plan's Complexity Tracking.
