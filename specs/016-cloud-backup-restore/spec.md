# Feature Specification: Cloud Backup and Restore

**Feature Branch**: `016-cloud-backup-restore`

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "Cloud backup and restore of the user's data, tied to a Google or Apple account. Today the wishlist lives only on the device, so losing or replacing the phone loses every list. The user wants to back it up and get it back, including on a device of the other platform (Android <-> iPhone). Sign in with Google or Sign in with Apple on both platforms; the app stays fully usable without an account. A Google account and an Apple account that share the same verified email resolve to the same user, with a manual link for when Apple's 'Hide My Email' gives a different address. Sign out and delete account. Back up the lists (including the default wishlist), the saved games and their status, which list each game belongs to, and the owned platforms; not search history, settings or anything re-fetchable. Manual 'Back up now' and 'Restore' only; restore replaces local data after a confirmation, all-or-nothing. Settings shows the signed-in account and the last successful backup time. Network failures, being signed out mid-operation and a missing backup are shown as clear errors with nothing half-written."

## Clarifications

### 2026-10-16: one sign-in method per platform

The owner decided each platform offers exactly one sign-in method: **Google on Android, Apple on iOS**. A device never
shows the other platform's button. The consequences, accepted by the owner's decision:

- Cross-platform data sharing relies on the two accounts sharing a verified email address, which Firebase links by
  itself (FR-004). There is **no manual link**: a device cannot sign in with the method it does not offer, so User
  Story 3's linking half (FR-005, FR-006) is dropped.
- An Apple ID that hides its email (private relay) gets a separate, empty account that cannot be joined to a Google
  account later. The user can still back up and restore between iPhones with it.
- Sign in with Apple is the only third-party sign-in on iOS, so App Store guideline 4.8 is satisfied without offering
  Google there. Apple token revocation on account deletion applies on iOS only.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Back up my wishlist to my account (Priority: P1)

A user who has built up lists and saved games opens Settings, signs in with their Google or Apple account, and
taps "Back up now". The app saves a copy of their data to their account and then shows when the last successful
backup happened. Backing up again replaces the previous copy with the current state of the device.

**Why this priority**: This is the whole point of the feature: without a backup there is nothing to restore.
Everything else builds on a user being able to put their data somewhere safe.

**Independent Test**: On a device with several lists and saved games, sign in from Settings, tap "Back up now",
and confirm the screen reports success and shows a last-backup time. Change the data, back up again, and
confirm the time moves forward and the earlier backup is no longer the one kept.

**Acceptance Scenarios**:

1. **Given** a user who is not signed in, **When** they open the backup section of Settings, **Then** they see
   the sign-in options (Google and Apple) and no backup or restore action is offered.
2. **Given** a signed-in user with data on the device, **When** they tap "Back up now" and it succeeds,
   **Then** the screen confirms it and shows the time of this backup as the last successful backup.
3. **Given** a signed-in user who already has a backup, **When** they back up again, **Then** the new backup
   replaces the previous one and only the newest state is kept.
4. **Given** a signed-in user whose device has no lists or saved games, **When** they tap "Back up now",
   **Then** the app asks them to confirm first, because an existing backup would be replaced with an empty one.
5. **Given** a backup in progress, **When** the user looks at the screen, **Then** the action is visibly busy
   and cannot be started a second time.

---

### User Story 2 - Restore my data on a new device, on either platform (Priority: P1)

A user gets a new phone, which may be the other platform (Android to iPhone or the reverse). They install the
app, sign in with the same account, and tap "Restore". If the device already holds data the app first says that
restoring will replace it, and only proceeds after they confirm. After restoring, their lists, saved games,
game statuses, list membership, default wishlist and owned platforms are what they were at the time of the
backup.

**Why this priority**: A backup that cannot be restored has no value, and restoring after losing a device is
the moment the feature pays off. It is the other half of the P1 pair.

**Independent Test**: Back up on one device, install on a second device (ideally the other platform), sign in
with the same account, tap "Restore", and confirm every list, saved game, status, list assignment, the default
wishlist and the owned platforms match the first device.

**Acceptance Scenarios**:

1. **Given** a signed-in user with a backup and an empty device, **When** they tap "Restore", **Then** the
   data from the backup appears on the device and the screen confirms the restore.
2. **Given** a signed-in user with a backup and a device that already has lists or saved games, **When** they
   tap "Restore", **Then** a confirmation dialog states that the data on this device will be replaced and lost,
   and nothing changes until they confirm.
3. **Given** that confirmation dialog, **When** the user cancels, **Then** the device data is exactly as it
   was.
4. **Given** a backup made on Android, **When** it is restored on iOS (or the reverse), **Then** the result is
   the same as restoring on the original platform.
5. **Given** a restore that fails partway for any reason, **When** the user returns to the app, **Then** the
   device data is exactly what it was before the restore started, with no half-restored state.
6. **Given** a signed-in user with no backup, **When** they tap "Restore", **Then** they are told there is no
   backup to restore and nothing on the device changes.

---

### User Story 3 - One account whether I sign in with Google on Android or Apple on iPhone (Priority: P2)

A user who signs in with Google on their Android phone and with Apple on their iPhone, using the same verified
email address, reaches the same backup from both. (There is no manual link: see the 2026-10-16 clarification. If
Apple hides their email behind a private relay address the two do not match, and the iPhone has its own account.)

**Why this priority**: This is what makes a cross-platform restore possible for people who use a different
provider on each platform. It is not needed for a single-platform user, so it ranks below the basic loop.

**Independent Test**: Sign in with Google and back up. On another device sign in with Apple using the same
verified email and confirm the same backup is offered. Then repeat with an Apple account that hides its email,
confirm it starts with no backup, link it from Settings while signed in with Google, and confirm the backup
becomes reachable with either provider.

**Acceptance Scenarios**:

1. **Given** a user who backed up after signing in with Google, **When** they sign in with Apple using the same
   verified email address, **Then** they are in the same account and see the same last-backup time and data.
2. **Given** a user signed in with one provider, **When** they choose to link the other provider from Settings
   and complete its sign-in, **Then** both providers lead to the same account from then on.
3. **Given** an Apple sign-in that shares a private relay address, **When** the user signs in, **Then** they
   get a separate, empty account until they link it manually, and the app does not claim the accounts are
   linked.
4. **Given** a provider already belongs to a different account, **When** the user tries to link it,
   **Then** the app explains that it cannot be linked and changes nothing.

---

### User Story 4 - Sign out and delete my account (Priority: P2)

A signed-in user can sign out, which leaves the data on the device untouched and keeps the cloud backup. They
can also delete their account, which asks for confirmation, removes their cloud backup and the account itself,
and returns the app to the signed-out state with the local data still on the device.

**Why this priority**: Deleting an account from inside the app is required by the platform store's rules for
any app that offers account creation, and users need a way to leave. It is secondary to the backup and
restore loop itself.

**Independent Test**: Sign in, back up, then sign out and confirm the local data is unchanged and signing back
in finds the backup. Then delete the account and confirm it asks for confirmation, that the backup is gone, and
that signing in again starts with no backup.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **When** they sign out, **Then** the app returns to the signed-out state, the
   local data is unchanged, and the cloud backup is kept.
2. **Given** a signed-in user, **When** they choose to delete their account, **Then** a confirmation dialog
   states that the account and the cloud backup will be permanently removed, and nothing is deleted until they
   confirm.
3. **Given** the user confirms deletion, **When** it completes, **Then** the cloud backup and the account are
   gone, the app is signed out, and the data on the device is unchanged.
4. **Given** the user cancels the deletion dialog, **When** they return to Settings, **Then** they are still
   signed in and the backup is unchanged.
5. **Given** a deletion that cannot complete, **When** the failure is shown, **Then** the account and the
   backup are not left in a half-deleted state the user cannot see or retry.

---

### Edge Cases

- **No network**: backing up, restoring, signing in, linking and deleting each fail with a clear "no
  connection" message; local data and the cloud backup are untouched.
- **Signed out mid-operation** (the session expires, or the account is removed elsewhere while a backup or
  restore runs): the operation stops with a message asking the user to sign in again; nothing is half-written
  locally or in the cloud.
- **The user cancels the platform sign-in screen**: the app stays signed out without an error banner.
- **App closed or killed during a restore**: on the next launch the device data is either entirely the old data
  or entirely the restored data, never a mix.
- **App closed or killed during a backup**: the previous backup is still intact and restorable.
- **Two devices back up in turn**: the last backup wins; there is no merging. Restoring on a device that backed
  up earlier replaces its data with whatever the account holds.
- **Backup made by a newer version of the app** than the one restoring it: the restore refuses with a message
  asking the user to update the app, and changes nothing.
- **Account deletion needs a recent sign-in**: if the platform requires the user to have signed in recently, the
  app asks them to sign in again and then continues, instead of failing with no explanation. When Apple is one
  of the account's sign-in methods, that fresh Apple sign-in is also what lets the app withdraw the app's
  access to the Apple ID, which Apple requires on deletion.
- **Same email, provider that cannot vouch for it**: signing in with a Google work address that is already
  registered through Apple does not create a second account; the user is told to sign in with Apple and link
  Google from there.
- **Restore replaces local data only**: release reminders, search history, settings and cached game data are
  not part of a backup, so they are not touched by a restore.
- **Lists the user deleted after backing up** come back on restore; the backup is a snapshot, not a log.
- **Backup of very large collections**: a collection far beyond what a person plausibly builds may be refused
  with a clear message; it never silently truncates.

## Requirements *(mandatory)*

### Functional Requirements

**Accounts**

- **FR-001**: The app MUST remain fully usable without an account; signing in is only required for backup,
  restore, linking and account deletion.
- **FR-002**: On Android, users MUST be able to sign in with a Google account, and it is the only method offered.
- **FR-003**: On iOS, users MUST be able to sign in with an Apple account, and it is the only method offered.
- **FR-004**: Signing in with a Google account and with an Apple account that share the same verified email
  address MUST lead to the same user account and the same backup. Where the sign-in service cannot vouch for
  the address on its own (a Google account on a domain Google does not host, such as a work address), the
  app MUST NOT create a second account: it MUST tell the user that the address is already registered with
  another sign-in method and that they can sign in with that method and link this one (FR-005).
- **FR-005**: *(Dropped, 2026-10-16 clarification.)* There is no manual linking: each platform offers one method.
- **FR-006**: *(Dropped, 2026-10-16 clarification.)*
- **FR-007**: Users MUST be able to sign out; signing out MUST leave the data on the device and the cloud
  backup unchanged.
- **FR-008**: Users MUST be able to delete their account from within the app after an explicit confirmation;
  deletion MUST remove the cloud backup and the account, and MUST leave the data on the device unchanged.
- **FR-009**: Settings MUST show whether the user is signed in, and when signed in, which account (and which
  sign-in methods) it is.

**Backup**

- **FR-010**: A signed-in user MUST be able to start a backup from Settings with a "Back up now" action. There
  MUST be no automatic or background backup.
- **FR-011**: A backup MUST contain the user's lists (including which list is the default wishlist and each
  list's custom cover photo), the saved games with their status, priority and notes, which list(s) each saved
  game belongs to, and the platforms the user owns.
- **FR-012**: A backup MUST NOT contain search history, app settings, or data that can be re-fetched from the
  game database (cached game details, Discover feed, translations, release-reminder entries).
- **FR-013**: A new backup MUST replace the user's previous backup; only one backup per account is kept.
- **FR-014**: A backup MUST be all-or-nothing: if it fails, the previous backup MUST remain intact and usable.
- **FR-015**: If the device holds no lists or saved games, the app MUST ask for confirmation before backing up,
  because it would replace an existing backup with an empty one.
- **FR-016**: Settings MUST show the time of the last successful backup for the signed-in account, and MUST
  update it when a backup succeeds.

**Restore**

- **FR-017**: A signed-in user MUST be able to start a restore from Settings with a "Restore" action.
- **FR-018**: A restore MUST replace the local lists, saved games, statuses, list membership, default wishlist
  and owned platforms with those in the backup; it MUST NOT merge the two.
- **FR-019**: If the device already holds lists or saved games, the app MUST show a confirmation dialog stating
  that the data on this device will be replaced and lost, and MUST NOT change anything until the user
  confirms.
- **FR-020**: A restore MUST be all-or-nothing: if it fails or is interrupted, the local data MUST be exactly
  what it was before the restore began.
- **FR-021**: A backup made on one platform MUST be restorable on the other, with the same result.
- **FR-022**: A restore MUST NOT modify data outside the backup's scope (search history, settings, reminders,
  cached data).
- **FR-023**: If the account has no backup, the app MUST say so and change nothing.
- **FR-024**: If a backup was made by a newer version of the app than the one restoring it, the app MUST refuse
  the restore with a message asking the user to update, and change nothing.

**Errors and feedback**

- **FR-025**: Failed operations (sign-in, backup, restore, link, sign-out, delete) MUST show a clear,
  localized message that distinguishes at least: no network connection, session expired or signed out, no
  backup found, an unreadable or too-new backup, and an unexpected failure.
- **FR-026**: While backup, restore, or deletion is running, the action MUST be visibly busy and MUST NOT be
  startable a second time.
- **FR-027**: A user cancelling a platform sign-in screen MUST leave the app signed out without an error.
- **FR-028**: All user-facing text for this feature MUST be localized like the rest of the app, with no text
  hard-coded in logic or models.

**Privacy and security**

- **FR-029**: A user MUST only be able to read, write, or delete their own backup; no other account can access
  it.
- **FR-030**: The app MUST NOT store the user's credentials; it relies on the platform sign-in providers.
- **FR-031**: Before the first backup, or on the sign-in screen, the app MUST tell the user what is stored in
  the cloud (their lists, saved games and owned platforms) and under which account.

### Key Entities

- **Account**: the person's identity for the cloud backup. Has one or more linked sign-in methods (Google,
  Apple) and exactly one backup slot. Exists only once the user signs in; the app works without it.
- **Linked sign-in method**: a Google or Apple identity attached to an account. An identity belongs to at most
  one account.
- **Backup**: a single snapshot of the user's data at a point in time, owned by one account. Contains the
  lists (with the default-wishlist marker), the saved games with their status, the list-membership links, and
  the owned platforms. Carries the time it was made and the app version that made it. Replaced as a whole by
  each new backup.
- **Local data**: what the device holds today in the same scope (lists, saved games, memberships, owned
  platforms). The thing a backup copies from and a restore replaces.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A signed-in user can finish a backup in no more than two taps from the Settings screen, and for
  a collection of up to 500 saved games it completes in under 30 seconds on a typical mobile connection.
- **SC-002**: A user with a backup can restore it on a new device, from opening Settings to seeing their data,
  in under 2 minutes including sign-in, for a collection of up to 500 saved games.
- **SC-003**: After a restore, 100% of the lists, saved games, statuses, list assignments, owned platforms and
  the default-wishlist choice match the backed-up device, including when restored on the other platform.
- **SC-004**: In every tested failure case (no network, session expired, process killed, interrupted restore,
  too-new backup), the local data and the stored backup are identical to their state before the operation
  started, in 100% of runs.
- **SC-005**: A user who signs in with Google and with Apple using the same verified email reaches the same
  backup in 100% of tested cases; a user with a differing Apple relay address can reach it after linking in
  under 1 minute.
- **SC-006**: Account deletion removes the cloud backup: after deleting, signing in again shows no backup in
  100% of tested cases.
- **SC-007**: No test account can read, change, or delete another account's backup.
- **SC-008**: Every failure the user can hit shows a message in their language that names the cause category
  (connection, sign-in, missing backup, outdated app, or unexpected), with no raw technical error text.

## Assumptions

- **Platforms and providers**: both Android and iOS ship with both Google and Apple sign-in in the first
  version. Sign in with Apple requires a paid Apple Developer account, which the owner must have before the
  Apple half can be built and tested.
- **Backend (owner's decision)**: Firebase Authentication and Firestore are the chosen backend, using
  Firebase Authentication's default "one account per email address" setting. Under it, Apple and Google
  (for addresses Google hosts) are trusted providers and are linked automatically when the verified email
  matches, on the free plan; no Identity Platform upgrade or billing account is needed. The plan decides the
  library layer and the data layout; the Firebase project, its sign-in providers and its security rules are
  configured by the owner in the console.
- **Identity matching**: two providers are treated as the same person only when both report the same *verified*
  email address. An unverified address never merges accounts.
- **Backup scope**: the saved games in a backup carry what the app stores for them, so that a restored
  wishlist looks the same without re-downloading each game. Release reminders are not part of a backup; after a
  restore the user sets reminders again where they want them.
- **Single slot**: one backup per account, replaced on each backup. There is no backup history and no way to
  roll back to an earlier one.
- **Manual only**: the app never backs up or restores by itself. A reminder dialog for users who have not
  backed up in a while is a recorded follow-up in `docs/tech-debt.md`, not part of this feature.
- **No merge**: restore replaces local data; merging local and cloud data, and live sync between devices, are
  out of scope.
- **Out of scope**: backing up search history or settings, email/password sign-in, sharing a backup between
  different accounts, and any web or desktop client.
- **Cost**: expected usage is far inside the free allowances of the chosen backend, so no per-user cost is
  assumed. A budget alert is the safeguard if the project is moved to a paid plan.
- **Offline**: the app's existing offline behaviour is unchanged; only the cloud actions in this feature need a
  connection.
- **Open for the plan**: the plan must prove on a real build for each platform that every sign-in and linking
  flow in this spec works with the chosen tooling before the design is final, and must say what changes if it
  does not.
