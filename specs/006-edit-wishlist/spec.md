# Feature Specification: Edit Wishlist

**Feature Branch**: `006-edit-wishlist`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "let's add the possibility of editing a wishlist. We should show a pencil button inside the lists' detail screen. Pressing the button should open the same BS that we use for creating lists, but with updated button labels and pre-filled text edits. After confirming the update, the user should immediatly see the changes. Any list is editable, also the default one."

## Clarifications

### Session 2026-10-06

- Q: If a newly picked cover image can't be saved, what should the user experience be? → A: Save the other
  changes, keep the previous cover, and tell the user the cover could not be saved (same behaviour as
  creating a list).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit a wishlist's details (Priority: P1)

A user opens one of their wishlists and realises its name no longer fits ("Co-op Picks" is now mostly
single-player games), or that it never got a description, icon or cover image. They tap a pencil button
on the list's detail screen, the same form they used to create the list opens with the list's current
values already filled in, they change what they want, confirm, and the list is updated.

**Why this priority**: This is the whole feature. Today a list's details are frozen at creation time, so
fixing a typo or renaming a list means deleting it and rebuilding it by hand. Everything else in this
spec refines this one flow.

**Independent Test**: Can be fully tested by opening any wishlist's detail screen, tapping the pencil
button, changing the name, confirming, and checking that the detail screen shows the new name.

**Acceptance Scenarios**:

1. **Given** a user is on a wishlist's detail screen, **When** they look at the top bar, **Then** a pencil
   (edit) button is visible.
2. **Given** a user is on a wishlist's detail screen, **When** they tap the pencil button, **Then** the
   same form used for creating a list opens, with the list's current name, description, icon and cover
   image already filled in.
3. **Given** the edit form is open, **When** the user reads its title and its confirm button, **Then**
   both are worded for editing (not "new list" / "create").
4. **Given** the edit form is open, **When** the user changes one or more fields and confirms, **Then**
   the form closes and the detail screen immediately shows the updated name, description, icon and cover
   image, without leaving or reopening the screen.
5. **Given** the user has just confirmed an edit, **When** they go back to the overview of all wishlists,
   **Then** that list's row shows the same updated details.
6. **Given** a list has a description, an icon or a cover image, **When** the user opens the edit form,
   **Then** each of those is pre-selected / pre-filled exactly as the list currently has it.

---

### User Story 2 - Edit the default wishlist (Priority: P1)

A user wants to rename or restyle the wishlist that is currently their default. Being the default must
not lock the list's details.

**Why this priority**: The default list is the one every user is guaranteed to have and the one most
likely to still carry its original name. If it were not editable, every user would hit the limitation.
It is also the case most likely to be missed, because the detail screen's other actions (set as default,
delete) are already hidden for the default list.

**Independent Test**: Can be fully tested by opening the default wishlist's detail screen, confirming the
pencil button is shown there, and completing an edit.

**Acceptance Scenarios**:

1. **Given** a user is on the default wishlist's detail screen, **When** they look at the top bar,
   **Then** the pencil button is visible even though the other list actions are not.
2. **Given** a user edits the default wishlist and confirms, **When** the detail screen updates, **Then**
   the list shows the new details and is still the default (the "Default" indicator remains).
3. **Given** a user has only one wishlist, **When** they open its detail screen, **Then** the pencil
   button is still offered.

---

### User Story 3 - Back out of an edit without changing anything (Priority: P2)

A user opens the edit form by mistake, or changes their mind halfway through typing. They must be able to
leave without altering the list.

**Why this priority**: Not delivering value on its own, but it keeps editing safe. Without it, opening
the form is a commitment.

**Independent Test**: Can be fully tested by opening the edit form, typing a different name, dismissing
the form, and checking that the list still has its original name.

**Acceptance Scenarios**:

1. **Given** the edit form is open with changes typed in, **When** the user taps Cancel or dismisses the
   form, **Then** the form closes and the list keeps its previous details.
2. **Given** the user cancelled an edit that contained changes, **When** they reopen the edit form,
   **Then** it shows the list's saved details, not the abandoned changes.

---

### Edge Cases

- **Blank name**: The user clears the name field. The confirm button is disabled, as it is when creating
  a list, so a list can never end up without a name.
- **Whitespace**: Leading and trailing whitespace in the name and description is trimmed on confirm, as
  it is on creation.
- **Cleared description**: The user empties a description that previously had text. After confirming, the
  list has no description and the detail screen no longer shows an empty description area.
- **Removed icon or cover image**: The user deselects the icon or removes the cover image. After
  confirming, the list falls back to the default icon / no cover, as a newly created list without them
  would.
- **Replaced cover image**: The user picks a new cover image. The new one replaces the old one everywhere
  the list is shown, and the old one is no longer used.
- **Cover image cannot be saved**: The user picks a new cover image but it cannot be stored. The other
  changes (name, description, icon) are still saved, the previous cover stays in place, and the user is
  told that the cover could not be saved.
- **No changes**: The user confirms without changing anything. The form closes and the list is unchanged;
  this is not an error.
- **Same name as another list**: Creating a list does not forbid it, so editing does not either.
- **Device rotation or process death while the form is open**: The form stays open and text typed into
  it is not lost.
- **Games in the list**: Editing never adds, removes or reorders the list's games, and never changes
  their statuses.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The wishlist detail screen MUST show a pencil (edit) button in its top bar for every
  wishlist, including the default wishlist and a user's only wishlist.
- **FR-002**: The edit button MUST be shown independently of the other top-bar actions (set as default,
  delete): hiding those for the default list MUST NOT hide the edit button.
- **FR-003**: Tapping the edit button MUST open the same form used to create a wishlist, not a separate
  or duplicated form.
- **FR-004**: When opened for editing, the form MUST be pre-filled with the list's current name,
  description, icon and cover image.
- **FR-005**: When opened for editing, the form's title and confirm button MUST use editing wording
  instead of the creation wording; the cancel button is unchanged.
- **FR-006**: The user MUST be able to change any combination of name, description, icon and cover
  image, and to clear the description, the icon and the cover image.
- **FR-007**: The confirm button MUST be disabled while the name is blank, and the name and description
  MUST be trimmed on confirm, exactly as when creating a list.
- **FR-008**: On confirm, the system MUST save the updated details to the same list; it MUST NOT create
  a new list, change the list's identity, or alter the list's games or their statuses.
- **FR-009**: On confirm, the form MUST close and the detail screen MUST show the updated details
  immediately, without the user leaving or reloading the screen.
- **FR-010**: The updated details MUST also appear everywhere else the list is shown, including the
  overview of all wishlists and the list picker used when adding a game to a list.
- **FR-011**: Editing MUST NOT change whether a list is the default. A default list stays the default
  after being edited; a non-default list stays non-default.
- **FR-012**: Cancelling or dismissing the form MUST leave the list untouched, and reopening the form
  MUST show the saved details rather than abandoned changes.
- **FR-013**: Editing MUST be persistent: the updated details MUST still be there after the app is
  closed and reopened.
- **FR-014**: The edit button MUST have an accessible description naming its action, and all new
  user-visible wording MUST come from the app's string resources.
- **FR-015**: If a newly picked cover image cannot be saved, the system MUST still save the other changes,
  MUST keep the list's previous cover, and MUST tell the user that the cover could not be saved.

### Key Entities

- **Wishlist**: A named collection of games. Its editable attributes are name (required), description
  (optional), icon (optional) and cover image (optional). Its identity, its games and their statuses, and
  its default / non-default designation are not editable through this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can rename a wishlist in under 15 seconds from the moment they are on its detail
  screen, without deleting or recreating it.
- **SC-002**: After confirming an edit, 100% of the changed details are visible on the detail screen
  without any additional navigation or manual refresh.
- **SC-003**: 100% of wishlists, including the default one and a user's only one, expose the edit button
  on their detail screen.
- **SC-004**: Zero games are lost, moved or have their status changed as a result of editing a wishlist.
- **SC-005**: A user who has used the create-list form can complete their first edit with no instruction,
  because the edit form looks and behaves the same apart from its pre-filled values and wording.

## Assumptions

- The editable attributes are exactly the four the creation form already collects: name, description,
  icon and cover image. No new attribute is introduced.
- Validation matches creation: the only rule is a non-blank name. There is no uniqueness rule on names
  today, so editing does not add one.
- The edit button lives only on the wishlist detail screen. Editing from the overview list (swipe action,
  long press) is out of scope.
- A successful edit applies immediately and silently: no confirmation dialog and no snackbar. The
  updated screen is the feedback, and the action is non-destructive and trivially repeatable. Only a
  cover image that cannot be saved is announced (FR-015).
- Changing the default designation stays a separate action ("Set as default"); it is not added to the
  edit form.
- Because the app is unpublished and stores lists locally, no data migration for existing lists is
  needed: every existing list simply becomes editable.
- Depends on the existing wishlist creation form, whose behaviour (fields, validation, cover-image
  picking) the edit flow reuses rather than reimplements.
