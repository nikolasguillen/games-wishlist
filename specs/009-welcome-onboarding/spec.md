# Feature Specification: Welcome Onboarding

**Feature Branch**: `009-welcome-onboarding`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "We need to implement a tutorial/welcome flow that needs to be shown the first time a user installs the app. We need to discuss about what to include in this flow"

## Clarifications

### Session 2026-10-07

- Q: If a user picks some platforms in the flow and then skips, or the app is closed before the end, should
  those picks be kept? → A: Kept. Every pick is saved the moment it is made, as in the Settings picker, so
  skipping or closing the app mid-flow never loses a pick.
- Q: In the platforms step, should the user see the full searchable list of platforms or a short list of
  common ones? → A: The same full, searchable list as the Settings picker, with the user's current picks
  shown first.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Understand the app on first launch (Priority: P1)

A user opens QuestLog for the very first time. Instead of landing cold on an empty search screen, they
see a short welcome flow that explains what the app is for (keeping a wishlist of videogames) and how its
main areas fit together: finding games (Search and the Discover suggestions), saving them into lists
(Lists, with a play status per game), and following their release dates (Radar, with optional release
reminders). When they finish, they land in the app knowing where to start.

**Why this priority**: This is the feature. A first-time user with nothing saved sees the app at its
emptiest — Discover has no taste profile to work from, Radar and Lists are empty — so the welcome flow is
the only place that explains why saving a game is worth doing.

**Independent Test**: Install the app fresh, launch it, page through the welcome flow to the end, and
confirm the user lands on the app's normal start screen; relaunch and confirm the flow does not appear
again.

**Acceptance Scenarios**:

1. **Given** the app has never been opened on this device, **When** the user launches it, **Then** the
   welcome flow is shown before any other screen.
2. **Given** the user is on any page of the welcome flow, **When** they move forward or back, **Then** the
   flow shows the next or previous page and indicates their position in the flow (e.g. page 2 of 4).
3. **Given** the user is on the last page, **When** they confirm (e.g. "Get started"), **Then** they land on
   the app's normal start screen with its bottom navigation.
4. **Given** the user has completed the welcome flow, **When** they launch the app again (including after
   a device restart or an app update), **Then** the app opens directly on its normal start screen.

---

### User Story 2 - Skip the flow at any point (Priority: P1)

A user who does not want a tour — or who has used the app before on another device — wants out
immediately. From any page they can skip the rest of the flow and go straight into the app.

**Why this priority**: A welcome flow that cannot be dismissed is a barrier, not a help. Skipping must work
from the first page or the flow will be resented by exactly the users who need it least.

**Independent Test**: Launch a fresh install, skip from the first page, confirm the app's start screen is
shown, then relaunch and confirm the flow is not shown again.

**Acceptance Scenarios**:

1. **Given** the user is on any page of the welcome flow, **When** they choose "Skip", **Then** the flow
   closes and they land on the app's normal start screen.
2. **Given** the user skipped the flow, **When** they launch the app again, **Then** the flow is not shown
   — skipping counts as completing it.

---

### User Story 3 - Set up platforms and release reminders (Priority: P2)

While the welcome flow has the user's attention, it offers the one setting that changes what a brand-new
user sees: which platforms they own. Picking them up front means Discover suggestions and Radar release
dates are filtered to the user's platforms from the first search, instead of the user discovering the
setting buried in Settings weeks later.

The flow then offers release reminders. A dedicated page explains why the app wants to send notifications
(a heads-up on the day a saved game comes out) and lets the user explicitly choose to allow them. If the
user declines, the flow tells them reminders will stay off and that they can change this later from
Settings.

**Why this priority**: Valuable but not essential — every setting offered here remains available in
Settings, and the flow already delivers its value through User Stories 1 and 2 without it.

**Independent Test**: Launch a fresh install, select two platforms in the welcome flow's setup step,
continue to the reminders page, allow notifications, finish the flow, open Settings and confirm the same
two platforms are shown as owned and notifications are enabled. Repeat with a fresh install, decline on
the reminders page, and confirm the flow explains how to change this later and still finishes.

**Acceptance Scenarios**:

1. **Given** the user reaches the setup step, **When** they select one or more platforms, **Then** each is
   saved as owned the moment it is selected, exactly as if it had been picked from Settings — including if
   the user then skips the flow or closes the app.
2. **Given** the user reaches the setup step, **When** they continue without selecting anything, **Then**
   no platform filter is applied and the flow proceeds — the step is never a blocker.
3. **Given** the device has no connection and no platform list has been fetched yet, **When** the user
   reaches the setup step, **Then** the step explains that platforms cannot be loaded right now and that
   they can be set later from Settings, and the user can still continue.
4. **Given** the user reaches the reminders page, **When** it is shown, **Then** it explains what the
   notifications are for (a reminder on the release day of a saved game) and offers an explicit "Allow
   notifications" action alongside a "Not now" action; the system permission request is shown only after
   the user chooses to allow, never before.
5. **Given** the user is on the reminders page, **When** they allow notifications in the system prompt,
   **Then** the flow confirms reminders are available and proceeds.
6. **Given** the user is on the reminders page, **When** they decline — either "Not now" or denying the
   system prompt — **Then** the flow tells them reminders are off and that they can change this later from
   Settings, and lets them continue.
7. **Given** notifications are already allowed on the device, or the device needs no permission to show
   them, **When** the flow reaches the point of the reminders page, **Then** the page is not shown.

---

### User Story 4 - Revisit the tour later (Priority: P3)

A user who skipped the flow on day one, or who wants a reminder of what Radar does, can open the welcome
flow again on demand.

**Why this priority**: Nice to have. It recovers users who skipped too eagerly, but the app is fully usable
without it.

**Independent Test**: Complete the flow, open the replay entry point, confirm the flow is shown again from
its first page, finish it, and confirm the user returns to where they started it from.

**Acceptance Scenarios**:

1. **Given** the user has completed or skipped the flow, **When** they choose "Show welcome tour" from
   Settings, **Then** the flow is shown from its first page.
2. **Given** the user is replaying the flow, **When** they finish or skip it, **Then** they return to the
   screen they opened it from, and any setting they leave untouched keeps its current value.

---

### Edge Cases

- The app is closed or killed part-way through the flow (before finishing or skipping): the next launch
  shows the flow again from its first page, since it was never completed. Platforms picked before the
  app closed are kept, and the platforms step shows them as already selected.
- The device is rotated, or the app is sent to the background and restored, mid-flow: the user stays on the
  same page with any selections they had made.
- The user presses the system back action on the first page of a first-launch flow: the app closes (as it
  would on any root screen) and the flow is shown again on the next launch. On later pages, back returns
  to the previous page.
- The app's stored data is cleared, or the app is uninstalled and reinstalled: the flow is shown again,
  as for any first launch. The exception is a reinstall where the system restores a backup of the app's
  data: that user is a returning one — their settings and lists come back with it — so the flow is not
  shown.
- The app is opened from a release-notification link before the flow has ever been completed (only
  possible after a data clear, since reminders require a saved game): the flow is shown first; the link is
  not honoured, because the reminder it belongs to no longer exists.
- The user permanently denied the notification permission earlier (a replay, or a re-install that kept the
  device's choice): the system prompt will not appear, so the reminders page's allow action cannot work.
  The page then behaves as a decline — it explains that reminders are off and how to change that from
  Settings — rather than offering an action that does nothing.
- The user replays the tour after completing it: the platforms step shows their current selection, and the
  reminders page follows FR-018.
- The user chose a light or dark appearance and later replays the flow: the flow follows the current
  appearance. On a first launch it follows the device's light/dark setting.
- Large font sizes or a small screen: every page's text remains fully readable (scrollable if needed) and
  the forward, back and skip controls stay reachable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST show the welcome flow on the first launch of the app on a device, before the
  app's normal start screen.
- **FR-002**: The system MUST remember that the flow was completed or skipped, and MUST NOT show it
  automatically on any later launch — including after a device restart or an app update.
- **FR-003**: A flow that was neither completed nor skipped (the app closed mid-flow) MUST be shown again on
  the next launch.
- **FR-004**: Users MUST be able to skip the remainder of the flow from every page in a single action.
  Skipping MUST be treated the same as completing it for FR-002.
- **FR-005**: Users MUST be able to move forward and back between pages, and each page MUST show the
  user's position within the flow.
- **FR-006**: The flow MUST end on the app's normal start screen (Search, with the bottom navigation
  visible), and pressing back from there MUST NOT return into the flow.
- **FR-007**: The flow MUST contain informational pages that introduce, at minimum: what QuestLog is for;
  finding games (Search and Discover); saving games into lists with a play status (Lists); and following
  release dates with optional reminders (Radar).
- **FR-008**: Each page's text MUST be short enough to read at a glance — one headline and no more than two
  sentences — and MUST be translatable, never hard-coded.
- **FR-009**: Beyond the informational pages, the flow MUST include two optional setup steps: choosing
  owned platforms, and a reminders page about notification permission (FR-016 to FR-018). The platforms
  step MUST offer the same full, searchable list of platforms as the Settings picker, with the user's
  current picks shown first, rather than a reduced set.
- **FR-010**: Any setup step in the flow MUST be optional: continuing without making a choice leaves the
  corresponding setting at its default, and the flow never blocks on it.
- **FR-011**: Any choice made in a setup step MUST be stored exactly as if it had been made from Settings,
  and MUST be visible and editable there afterwards. A choice MUST be saved the moment it is made — there is
  no confirm step — so skipping the flow or closing the app mid-flow never discards it.
- **FR-012**: A setup step that needs data from the network MUST degrade gracefully when that data is
  unavailable: it explains the situation, points to Settings for later, and lets the user continue.
- **FR-013**: The flow MUST NOT start the offline translation model download, ask for an account, or require
  any network access to be completed.
- **FR-014**: The flow MUST be presented as a sequence of full-screen pages shown before the app. It MUST
  NOT add overlays or hints to the app's own screens.
- **FR-015**: The flow MUST render correctly in both the light and the dark appearance, and MUST be usable
  with a screen reader (every page's headline, text and controls are announced).
- **FR-016**: The reminders page MUST explain, before any system prompt appears, why the app wants to send
  notifications, and MUST offer an explicit action to allow them and a separate action to decline. The
  system permission request MUST be made only as the result of the user choosing to allow.
- **FR-017**: If the user declines — through "Not now" or by denying the system prompt — the flow MUST tell
  them that reminders are off and that they can change this later from Settings, then let them continue.
  Declining MUST NOT prevent the flow from completing, and MUST NOT turn off or discard any other choice.
- **FR-018**: The reminders page MUST NOT be shown when notifications are already allowed, or when the
  device does not require a permission to show them.
- **FR-019**: The welcome tour MUST be replayable on demand from a "Show welcome tour" row in Settings. A
  replay starts from the first page, ends back on the screen it was opened from, and does not alter any
  setting the user leaves untouched.

### Key Entities

- **Onboarding completion**: A per-device record of whether the welcome flow has been completed or skipped.
  Absent on a fresh install; set once, the first time the user finishes or skips the flow. Not tied to any
  saved game or list, and not cleared by anything other than clearing the app's data.
- **Welcome page**: One step of the flow — a headline, a short explanation and an illustration, describing
  one area of the app. Pages are fixed content shipped with the app, not data the user edits.
- **Notification permission**: The device-level permission, owned by the system and reused from
  `003-release-notifications`. The flow only asks for it and reads its state; it keeps no copy.
- **Owned platforms**: Reused from Settings — the user's set of owned platforms that filters Discover
  suggestions and Radar release dates. The flow writes to the same setting; it does not keep its own copy.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can read through the whole flow and reach the app's start screen in under
  60 seconds.
- **SC-002**: A user can leave the flow from any page in one action.
- **SC-003**: After the flow has been completed or skipped once, it is shown automatically on 0% of later
  launches.
- **SC-004**: Every platform chosen during the flow appears as owned in Settings, with no extra action
  from the user.
- **SC-005**: A user who has just finished the flow can name the app's three main areas (Search, Lists,
  Radar) and what each is for, when asked in an informal walkthrough.
- **SC-006**: The flow can be completed with no network connection.
- **SC-007**: A user who declines notifications in the flow is told, on the same page, that reminders are
  off and where to change that, in 100% of cases; none is left believing reminders are on.
- **SC-008**: A user can replay the welcome tour from Settings in two taps or fewer.

## Assumptions

- "The first time a user installs the app" is read as "the first launch on a device with no app data".
  The app is unpublished, so there is no existing user base that would need to be shown the flow (or
  spared it) after an update.
- The flow is shown regardless of how the app was launched, since it must appear before any other screen.
- Appearance (light/dark) is not offered as a setup step: the device's setting is a sensible default and
  the choice is one tap away in Settings.
- The offline translation model is not offered as a setup step: it is several gigabytes, Wi-Fi only, and
  only useful once the user is reading game descriptions. The informational pages may mention it exists.
- The notification permission is requested from the flow, on the owner's decision, behind an explanatory
  page rather than cold. This does not replace the existing behaviour from `003-release-notifications`: if
  the user declines here, the permission is still requested the first time they turn on a release reminder,
  and Settings still surfaces a blocked permission with a way to fix it.
- Allowing notifications in the flow does not opt any game into reminders — reminders remain a per-game
  choice. The page therefore promises only that the user can be reminded, not that they will be.
- Illustrations for the pages are built from the app's own visual language (e.g. stylised game cards and
  the existing icons); producing bespoke artwork is out of scope.
- The flow is presented in the app's current language only, like the rest of the app; it introduces no new
  localisation.
