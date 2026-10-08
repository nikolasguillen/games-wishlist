# Feature Specification: Multiplatform Migration

**Feature Branch**: `010-kmp-migration`

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "I want to migrate this whole project to kotlin multi platform"

## Clarifications

### Session 2026-10-08

- Q: Which additional platforms are in scope? → A: iOS only. Desktop and web are out of scope for this
  feature.
- Q: Must on-device translation and release reminders reach parity on iOS before it ships? → A: No. iOS
  launches without both and their entry points are hidden there. Implementing them for iOS is deferred
  follow-up work and must stay on record (see FR-008 and the Assumptions).

## User Scenarios & Testing *(mandatory)*

<!--
  The "users" of this feature are the people who use QuestLog (existing Android users, and the new
  audience on the additional platform(s)) and the owner, who maintains the code. A migration has no new
  screens: its value is that the same app, with the same behaviour, exists on more than one platform
  from one body of shared code, without breaking what already works.
-->

### User Story 1 - Android keeps working, unchanged (Priority: P1)

An existing Android user updates the app to the first multiplatform-based version. Everything they already
do — searching for games, browsing Discover, saving games into lists with a play status, following release
dates in Radar, receiving release reminders, translating a game description, choosing a light or dark
appearance, going through onboarding on a fresh install — behaves exactly as before. They never notice the
migration happened.

**Why this priority**: Android is the only platform QuestLog runs on today. A migration that regresses it
has taken value away to create value elsewhere, so "no regression" is the gate every other story has to
pass through.

**Independent Test**: Run the full existing JVM test suites, then walk through every shipped screen on an
Android device and compare against the behaviour recorded before the migration started; confirm a
database created by the pre-migration build is still usable (or, while the app is unpublished, that the
documented destructive reset is the only data loss).

**Acceptance Scenarios**:

1. **Given** the migrated Android build, **When** the user performs any flow that existed before the
   migration (search, save, change status, edit a list, view Radar, open game detail, change settings),
   **Then** the outcome is identical to the pre-migration build.
2. **Given** the migrated Android build, **When** the existing automated test suites are run, **Then** all
   of them pass with no test deleted or weakened to make them pass.
3. **Given** a user with release reminders enabled, **When** a tracked game's release date approaches,
   **Then** they still receive the reminder at the same time as before.

---

### User Story 2 - Use QuestLog on iOS (Priority: P2)

A person who owns an iPhone installs QuestLog and gets the same wishlist
experience: they can search for games, see Discover suggestions, save games into lists, track release
dates in Radar, and open a game's detail page. The look and feel follows the same design (colors,
typography, spacing, light and dark appearance), and the app respects that platform's conventions where
they differ (back navigation, system appearance, permissions prompts).

**Why this priority**: This is the reason for the migration. It is P2 only because it must not start
until Story 1's safety net exists, and because it is delivered in slices (see Story 3).

**Independent Test**: Install the app on a device of the additional platform, sign in to nothing (the app
has no account), search for a game, save it to a list, restart the app, and confirm the game is still
there and appears in Radar if it has a release date.

**Acceptance Scenarios**:

1. **Given** a fresh install on the additional platform, **When** the user opens the app, **Then** they see
   the welcome flow once and then land on the normal start screen, exactly as on Android.
2. **Given** the app on the additional platform with network access, **When** the user searches for a
   game and saves it to a list, **Then** it appears in that list and survives closing and reopening the
   app.
3. **Given** the system is set to dark appearance and the user has chosen "follow system", **When** the app
   is opened, **Then** it renders in the dark scheme; changing the app's own appearance setting overrides
   the system.
4. **Given** a feature that cannot work identically on the additional platform, **When** the user reaches
   it, **Then** the app either offers the platform's equivalent or hides the entry point — it never shows a
   broken or dead control.

---

### User Story 3 - Migrate in verifiable steps, never one big switch (Priority: P3)

The owner moves the code over one module or layer at a time. After each step the Android app still
builds, still passes its tests and can still be run; the work can be paused between any two steps without
leaving the project in a half-converted state. The existing module boundaries (feature modules never
depend on data, network, database or AI modules; the AI module is reachable only from the data module;
only the app module knows about navigation) are preserved throughout and after the migration.

**Why this priority**: The project has no CI and a single maintainer, so the safety of the migration
comes from its sequencing, not from a pipeline. This story is about how the work is cut, which is what
makes Stories 1 and 2 achievable.

**Independent Test**: Pick any completed step, check out that commit, and confirm the Android app builds
and its test suites pass.

**Acceptance Scenarios**:

1. **Given** any commit on the migration branch, **When** the Android debug build and the unit tests are
   run, **Then** both succeed.
2. **Given** a module that has been migrated, **When** its dependencies are inspected, **Then** it
   depends only on what the module-boundary rules allow, and it contains no platform-specific code outside
   the places designated for it.
3. **Given** the migration is partly done, **When** the owner decides to stop for a while, **Then** the
   repository is in a releasable Android state, with no dead scaffolding left behind.

---

### User Story 4 - Platform-only capabilities stay isolated behind contracts (Priority: P3)

Capabilities that exist only on one platform — scheduling release-refresh work in the background,
posting release notifications, on-device translation of a game description, the splash screen, runtime
permission prompts — are reached from shared code through a contract that the shared code owns. Each
platform supplies its own implementation, and a platform without one degrades gracefully (see
FR-008) instead of blocking the build or the other platform.

**Why this priority**: Two of these capabilities (background scheduling and on-device translation) are
already behind contracts, which is what makes the migration tractable. Keeping that pattern is cheaper
than retrofitting it later.

**Independent Test**: Replace the platform implementation of any one capability with a stub and confirm
that shared code compiles and that its tests still pass without touching shared code.

**Acceptance Scenarios**:

1. **Given** a shared use case that needs a platform capability, **When** it is built and tested without
   any platform implementation present, **Then** it compiles and its tests run against a test double.
2. **Given** a platform that has no implementation of a capability, **When** the user reaches the
   related feature, **Then** the behaviour follows FR-008.

---

### Edge Cases

- What happens to a user's saved games and lists when they update from the last non-multiplatform build
  to the first multiplatform one? While the app is unpublished the database is allowed to reset, but the
  spec must make that outcome explicit rather than accidental.
- How does the app behave on a platform where the on-device translation model does not exist? The
  translate control must be absent or replaced, never present and failing.
- How are dates and times shown when the device is in a different time zone or locale from the one the
  release date was stored in? Radar's timeline must show the same day on both platforms for the same
  game.
- What happens to the IGDB credentials, which are injected at build time from a git-ignored local file? A
  build for the additional platform must obtain them the same way, and they must never end up committed
  or in shared source.
- How are strings, icons and fonts that are today Android resources shown on the additional platform,
  including every existing translation of the app's text?
- What happens when a screen's behaviour depends on a platform-native gesture (for example the Android
  system back gesture) that the additional platform does not have?
- What if a library the project relies on has no equivalent on the additional platform? The capability
  must be listed as an exception (FR-008), not silently dropped.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST continue to ship on Android with feature parity to the pre-migration build:
  every shipped screen, setting, notification and background behaviour keeps working as it does today.
- **FR-002**: The app MUST be installable and usable on iOS, in addition to Android. No other platform
  (desktop, web) is in scope.
- **FR-003**: Business logic MUST live in code shared across platforms: domain models, use cases,
  repository contracts and implementations, mappers, error mapping, and the logic that drives each
  screen. Only genuinely platform-specific behaviour (see FR-007) may live outside it.
- **FR-004**: The user interface MUST be shared across platforms and MUST render the same design system
  (colors, typography, spacing tokens, light and dark schemes, user-selectable appearance) on every
  supported platform.
- **FR-005**: All user-visible text MUST come from a single shared set of strings, in every language the
  app already supports, on every supported platform. No display text may be hardcoded in a model, mapper
  or screen.
- **FR-006**: The existing module-boundary rules MUST still hold after the migration: feature modules
  depend only on the common, model, domain, UI, navigation and design-system modules; the AI module is
  reachable only from the data module; only the app module knows about navigation. Any new module or
  dependency edge MUST be called out and justified explicitly.
- **FR-007**: Capabilities that exist on only some platforms (background release refresh, release
  notifications and their permission prompt, on-device description translation, splash screen, secure
  build-time credential injection) MUST be reached from shared code through a contract owned by shared
  code, with one implementation per platform.
- **FR-008**: When a platform cannot provide a capability, the app MUST either provide that platform's
  equivalent or hide the entry point for it; it MUST NOT show a control that cannot work. The set of
  capabilities affected and the chosen behaviour for each MUST be recorded. For the first iOS release,
  on-device description translation and release reminders (including their permission prompt and any
  release-date refresh that runs while the app is closed) are NOT required. Their entry points MUST be
  hidden on iOS, and the Settings rows that configure them MUST NOT appear there. Release dates shown in
  Radar MUST still stay current while the app is in use. Both deferred features remain Android-only until they are
  implemented for iOS in follow-up work, which MUST be kept on record (FR-016).
- **FR-016**: The deferred iOS work for on-device translation and for release reminders MUST stay
  recorded in `docs/roadmap.md` as planned features (it is, under "iOS follow-ups after the multiplatform
  migration"), so the gap is a tracked decision and not something forgotten. Each entry is deleted only
  when its iOS implementation ships.
- **FR-009**: The same game, with the same release date, MUST appear on the same day in Radar on every
  platform, regardless of device time zone or locale.
- **FR-010**: User data (saved games, lists, play status, settings, onboarding-seen flag) MUST persist
  across app restarts on every platform. Data stays local to the device; the app has no account and no
  sync.
- **FR-011**: The Android debug build and the full set of unit test suites MUST succeed at every commit
  of the migration, and no existing test may be deleted or weakened to achieve that.
- **FR-012**: Shared logic that is migrated MUST keep its automated test coverage, and the tests MUST run
  without a device or emulator.
- **FR-013**: The IGDB credentials MUST remain in the git-ignored local properties file and be injected
  at build time for every platform; they MUST NOT be committed or placed in shared source.
- **FR-014**: The project documentation (root and per-directory instruction files, the constitution,
  `docs/tech-debt.md`, `docs/roadmap.md`) MUST be updated in the same commit as each change that makes a
  rule or note stale — rules about things that no longer exist are deleted, not annotated.
- **FR-015**: The migration MUST NOT change the database version, add migration objects, or alter what
  the app stores, other than what is needed to make storage work on every platform. While the app is
  unpublished, a one-time local data reset on the first multiplatform build is acceptable.

### Key Entities *(include if feature involves data)*

- **Supported platform**: A target the app is built and shipped for (Android today; one or more others
  after this feature). Each has its own set of capabilities and conventions.
- **Platform capability**: Something shared code needs but that only a platform can provide — background
  scheduling, notifications, on-device translation, permission prompts, splash screen. Defined by a
  contract in shared code and implemented once per platform.
- **Capability exception**: A recorded decision that a capability is unavailable, replaced or deferred on
  a given platform, including what the user sees instead.
- **Migration step**: One independently verifiable unit of the move (a module or layer). Each step ends
  with the Android build and tests green and can be the last step before a pause.
- **Parity baseline**: The recorded pre-migration behaviour of the Android app, against which Story 1 is
  checked.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the automated test suites that exist before the migration pass after it, with none
  removed or disabled.
- **SC-002**: 100% of the screens and flows listed in the parity baseline behave identically on Android
  after the migration, verified by a manual walkthrough with no regressions logged.
- **SC-003**: A new user on each supported additional platform can install the app, complete the welcome
  flow, find a game, save it to a list and see it still saved after restarting the app, in under 3
  minutes.
- **SC-004**: At least 90% of the application's non-UI logic and of its screen logic is written once and
  used by every platform, with platform-specific code limited to the capabilities enumerated in FR-007.
- **SC-005**: At every commit on the migration branch the Android app builds and its tests pass, so the
  work can be paused at any point without an unreleasable state (verified by checking out a sample of
  commits).
- **SC-006**: Zero controls are visible on any platform that cannot perform their function.
- **SC-007**: A change to a shared rule (for example, how a game's release date is computed) takes effect
  on every platform from a single edit, with no second copy to update.

## Assumptions

- "The additional platform" throughout this spec means iOS. Release reminders and on-device translation
  are deliberately absent from the first iOS release and are tracked as follow-up work (FR-008, FR-016).
  The contracts from FR-007 exist for them from day one, so adding the iOS implementations later does not
  touch shared code.
- The user interface will be shared across platforms rather than rebuilt natively per platform; this is
  the direction already recorded in the project's instructions, and "the whole project" in the request is
  read as including the UI layer.
- Android remains a first-class, shipping target for the whole migration; it is not retired or demoted.
- The migration is done incrementally by one maintainer, with no CI; verification is local builds and the
  existing JVM unit test suites.
- The app stays account-free and local-first: no sync between devices is introduced by this feature.
- The IGDB service remains the sole data source; the app's data model and screens do not change as part
  of the migration.
- The app is unpublished, so a one-time reset of local data is acceptable if a storage change requires it
  (this does not lift the rule against bumping the database version).
- Library replacements that are required by the move (networking, dependency injection, image loading,
  persistence access, dates) are decisions for the planning phase; this spec only requires that the
  outcomes above hold.
- The project's current instruction files and constitution state that no multiplatform work should start
  yet. This feature is the owner's explicit decision to start it; those documents will need to be
  amended as part of the work (see FR-014).
