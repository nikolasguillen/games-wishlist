# Feature Specification: Light Appearance & Theme Preference

**Feature Branch**: `004-light-dark-theme`

**Created**: 2026-09-30

**Status**: Draft

**Input**: User description: "we should create a light variant theme for this app. We should keep the primary color and its accents, but we should turn the various surfaces to a light theme. The user should be able to choose between dark/light/follow system settings in app's settings section."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manually choose Light or Dark appearance (Priority: P1)

A user who prefers a specific look opens Settings, finds an Appearance option, and picks either Light or
Dark. The app's screens immediately switch to the chosen look: backgrounds, cards, navigation bar, and
dialogs adopt the selected appearance, while the app's brand color and its accent colors look the same as
they always have.

**Why this priority**: This is the core ask — without a way to pick a specific appearance, there is no
feature. It also delivers value on its own: even before "follow system" exists, users can already choose
Light or Dark.

**Independent Test**: Can be fully tested by opening Settings, selecting "Light", and confirming every
screen in the app renders with light surfaces and unchanged brand colors; then selecting "Dark" and
confirming the app returns to its current look.

**Acceptance Scenarios**:

1. **Given** the app is running in Dark appearance, **When** the user selects "Light" in Settings, **Then**
   all visible surfaces (backgrounds, cards, navigation bar, dialogs, top bars) switch to a light color
   scheme without needing to restart the app.
2. **Given** the app is in Light appearance, **When** the user views any screen, **Then** the primary brand
   color and its accent colors appear the same hue as they do in Dark appearance.
3. **Given** the user has selected Light appearance, **When** the user navigates across different features
   (search, wishlist, lists, game detail, radar, settings), **Then** every screen renders fully readable
   text and icons with no leftover dark-only elements.
4. **Given** the user has selected an appearance, **When** the user closes and reopens the app, **Then** the
   app launches directly in the previously selected appearance.

---

### User Story 2 - Follow the device's system appearance (Priority: P2)

A user who does not want to manage the setting manually selects "Follow System" in Settings. The app then
matches whatever light/dark mode the device itself is set to, and switches automatically if the user
changes their device-wide setting later (for example, via a scheduled night mode).

**Why this priority**: This is the "hands-off" option the request calls out explicitly, and it is the
expected default for most users, but it depends on Light appearance already existing (User Story 1), so it
is layered on top.

**Independent Test**: Can be fully tested by selecting "Follow System", setting the device to light mode
and confirming the app shows Light, then switching the device to dark mode and confirming the app shows
Dark without any action inside the app.

**Acceptance Scenarios**:

1. **Given** the user selects "Follow System" and the device is set to light mode, **When** the user opens
   the app, **Then** the app renders in Light appearance.
2. **Given** the user selects "Follow System" and the device is set to dark mode, **When** the user opens
   the app, **Then** the app renders in Dark appearance.
3. **Given** the app is open with "Follow System" selected, **When** the device-wide appearance setting
   changes, **Then** the app updates to match without the user needing to restart it.

---

### User Story 3 - Appearance choice is remembered (Priority: P3)

A user picks an appearance once and never has to pick it again: the choice survives app restarts, device
reboots, and app updates until the user changes it themselves.

**Why this priority**: This is expected baseline behavior for any preference screen, and it depends on User
Stories 1 and 2 existing first. Without it, the feature works but is annoying, which is why it is a
follow-on rather than a P1.

**Independent Test**: Can be fully tested by selecting an appearance, force-closing the app, reopening it,
and confirming the same appearance and the same Settings selection are shown.

**Acceptance Scenarios**:

1. **Given** the user selected "Light", **When** the app is fully closed and reopened later, **Then** the
   app opens directly in Light appearance and Settings shows "Light" as selected.
2. **Given** the user never changed the setting, **When** they open the app for the first time, **Then** the
   Appearance setting shows "Follow System" as the selected option.

### Edge Cases

- What happens when the user changes the device system appearance while the app is running in the
  background (not currently visible)? The app should reflect the new system appearance the next time it is
  brought to the foreground.
- What happens on a device where "system dark mode" is not supported or cannot be detected? "Follow System"
  should fall back to Light appearance.
- What happens if the user switches appearance rapidly (e.g., taps Light, Dark, Light in quick succession)?
  Each selection should be applied and reflected without visual glitches or the app getting stuck showing a
  stale appearance.
- What happens to in-progress content (open dialogs, expanded search bar, scroll position) when the
  appearance changes while a screen is visible? It should remain functionally unaffected — only the visual
  appearance changes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Settings section MUST offer an "Appearance" control with exactly three options: Light,
  Dark, and Follow System.
- **FR-002**: The default Appearance selection, when the user has never made a choice, MUST be "Follow
  System".
- **FR-003**: Selecting "Light" MUST render all app surfaces (screen backgrounds, cards, navigation bar,
  top bars, dialogs, and other chrome) in a light color scheme.
- **FR-004**: Selecting "Light" MUST preserve the app's existing primary brand color and its accent colors
  unchanged in hue from what is shown in Dark appearance.
- **FR-005**: Selecting "Dark" MUST continue to render the app exactly as it does today, with no visual
  regression.
- **FR-006**: Selecting "Follow System" MUST make the app's appearance match the device's current
  system-level light/dark setting.
- **FR-007**: When "Follow System" is selected, the app MUST update its appearance automatically if the
  device's system-level setting changes, without requiring the user to restart the app.
- **FR-008**: Changing the Appearance selection in Settings MUST apply to the whole app immediately, without
  requiring an app restart.
- **FR-009**: The user's Appearance selection MUST persist across app restarts and device reboots until the
  user changes it again.
- **FR-010**: Every screen and feature in the app MUST remain fully readable (sufficient contrast between
  text/icons and their background) in Light appearance, matching the readability already provided in Dark
  appearance.
- **FR-011**: The Settings screen MUST visually indicate which of the three Appearance options is currently
  selected.

### Key Entities

- **Appearance Preference**: The user's chosen theme mode for the app. One of three values — Light, Dark,
  or Follow System. Belongs to the user/device and is read whenever the app determines how to render its
  screens.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can change the app's appearance from Settings in under 3 taps, with the new appearance
  visible on screen within 1 second.
- **SC-002**: 100% of app screens display fully readable text and icons when Light appearance is active,
  matching the readability standard of the existing Dark appearance.
- **SC-003**: The chosen Appearance preference is correctly restored in 100% of app relaunches, including
  after a full device restart.
- **SC-004**: When "Follow System" is selected, the app's displayed appearance matches the device's
  system-level setting every time the app is opened or brought to the foreground.
- **SC-005**: The primary brand color and its accents are visually recognizable as the same color identity
  in both Light and Dark appearance, as confirmed by design review.

## Assumptions

- The app currently ships with a single, dark-only appearance; Light appearance is new, not a fix to an
  existing light mode.
- "Surfaces" refers to backgrounds, containers, cards, bars, and dialog chrome. It does not include the
  primary brand color or its accent colors, which stay the same hue across both appearances per the user's
  request.
- The Appearance preference is a device-local setting, consistent with how other settings in this app
  behave today; it is not expected to sync across a user's devices.
- "Follow System" relies on the standard OS-level light/dark signal already available on the platforms this
  app targets today.
- No additional appearance variants (e.g., high-contrast, custom accent picker) are in scope — only Light,
  Dark, and Follow System.
- Existing users who upgrade into this feature will see "Follow System" as their starting selection, same
  as a first-time install, since no appearance choice has been made yet.
