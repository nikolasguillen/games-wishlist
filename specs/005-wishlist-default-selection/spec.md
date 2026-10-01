# Feature Specification: Default Wishlist Selection

**Feature Branch**: `005-wishlist-default-selection`

**Created**: 2026-10-01

**Status**: Draft

**Input**: User description: "we need to improve the wishlist screen. We need to allow the user to pick it's default wishlist. The default wishlist must NOT be deletable. This means that there must always be at least one wishlist. The heart button in GameDetailActionPill.kt must add the game to the wishlist the user selected as default."

## Clarifications

### Session 2026-10-01

- Q: Should the list-detail screen still show its "⋮" options menu for the list that's currently the
  default, even though neither "Delete" nor "Set as default" would be clickable there? → A: Keep the
  menu's current hide-when-no-actions behavior untouched (it stays hidden for the default list, same as
  today); show a separate, always-visible, non-interactive "Default" label in that screen's top bar
  instead, next to the list name.
- Q: When a user taps "Set as default" on a non-default wishlist, should the app apply it immediately
  (with lightweight confirmation feedback), or should it first ask the user to confirm via a dialog, the
  way deleting a list already does? → A: Apply immediately and confirm with a snackbar; no confirmation
  dialog, since the action is non-destructive and instantly reversible.

### Session 2026-10-02

- Q: Should the overview list of all wishlists also show which one is the default? → A: Yes. The default
  wishlist's row there carries the same "Default" label as its detail screen. Setting the default stays on
  the detail screen's options menu; a swipe action on the overview rows remains a separate, later decision.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose a default wishlist (Priority: P1)

A user who has created several wishlists (e.g. "RPGs to Try", "Co-op Picks", "Finished") wants to tell
the app which one is their main, go-to wishlist, so that quick-save actions elsewhere in the app have an
obvious, predictable target instead of always landing in whichever list happened to be created first.

**Why this priority**: This is the capability the whole feature exists to deliver. Without it, there is no
way to express a preference, and every other requirement in this feature has nothing to attach to.

**Independent Test**: Can be fully tested by opening a non-default wishlist's detail screen, choosing
"Set as default" from its options menu, and confirming that wishlist's detail screen now shows the
"Default" indicator while the previous default's screen no longer does.

**Acceptance Scenarios**:

1. **Given** a user has two or more wishlists, **When** they open any one wishlist's detail screen, **Then**
   that screen clearly shows whether or not it is the current default.
2. **Given** a user has two or more wishlists and one is marked default, **When** they open a non-default
   wishlist's detail screen and choose "Set as default" from its options menu, **Then** that wishlist
   immediately becomes the default (no confirmation dialog), the previous default loses the designation,
   and a brief confirmation message is shown.
3. **Given** a user has only one wishlist, **When** they open its detail screen, **Then** it is shown as
   the default and no "Set as default" action is offered anywhere.
4. **Given** a user has two or more wishlists, **When** they view the overview list of all wishlists,
   **Then** exactly one row shows the "Default" label, and after a new default is chosen the label has
   moved to that wishlist's row.

---

### User Story 2 - Quick-save always targets the chosen default (Priority: P1)

A user browsing a game's detail screen taps the heart/favorite button to quickly save the game, without
opening any list picker. The game must land in whichever wishlist the user has designated as their
default, not in a fixed, unchangeable list.

**Why this priority**: This is the explicit, named trigger for the feature and the main place users will
feel the benefit day to day — it is the payoff for User Story 1.

**Independent Test**: Can be fully tested by marking a specific wishlist as default, tapping the heart
button on any game's detail screen, and confirming the game now appears in that wishlist (and only that
one). Repeating after switching the default to a different wishlist confirms the button follows the
change.

**Acceptance Scenarios**:

1. **Given** wishlist A is the current default, **When** the user taps the heart button on a game not yet
   saved anywhere, **Then** the game is added to wishlist A.
2. **Given** wishlist A is the current default and a game is already saved in it, **When** the user taps
   the heart button for that same game, **Then** the game is removed from wishlist A (the button toggles).
3. **Given** the user changes their default from wishlist A to wishlist B, **When** they tap the heart
   button on a game afterward, **Then** the game is added to wishlist B, not wishlist A.
4. **Given** a game was already saved in wishlist A before the default was switched to wishlist B, **When**
   the user views wishlist A afterward, **Then** the game is still present there (switching the default
   does not move or remove existing games).

---

### User Story 3 - The default wishlist can never be deleted (Priority: P2)

A user tries to delete the wishlist that is currently set as their default. The app must prevent this, so
the user never ends up with no wishlists at all and never loses the one list the heart button depends on.

**Why this priority**: This is a safety guard rail that protects the promise made in User Story 2. It
matters less on its own and more as a backstop once a default can be freely reassigned.

**Independent Test**: Can be fully tested by opening the wishlist currently marked as default and
confirming no delete action is offered for it, and that a delete requested directly is refused, while
deletion of any non-default wishlist succeeds normally.

**Acceptance Scenarios**:

1. **Given** a wishlist is currently the default, **When** the user opens its detail screen, **Then** no
   delete action is offered; and **When** a delete is requested by any other means, **Then** it is refused
   and nothing is removed.
2. **Given** a user has exactly one wishlist, **When** they attempt to delete it, **Then** the deletion is
   blocked, because it is necessarily the default.
3. **Given** a wishlist is not the current default, **When** the user deletes it, **Then** it is deleted
   normally and the default designation is unaffected.
4. **Given** a user reassigns the default from wishlist A to wishlist B, **When** they afterward attempt to
   delete wishlist A, **Then** deletion succeeds, because A is no longer the default.

---

### Edge Cases

- What happens if the user tries to set the already-current default as the default again? The system
  should treat this as a no-op with no visible change.
- What happens on first install, before the user has ever made a choice? The single wishlist the app
  starts with is the default until the user changes it.
- What happens if the user creates a brand-new wishlist? It is not automatically made the default; the
  existing default is unaffected until the user explicitly changes it.
- How does the system behave if a deletion is attempted on the default wishlist through any entry point?
  It must be blocked consistently everywhere deletion can be triggered, not only from one screen.
- What does the current default list's own detail screen show in place of the options menu, since it has
  no available Delete or Set-as-default action? A non-interactive "Default" label in its top bar, not an
  empty or disabled menu.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A wishlist's own detail screen MUST visibly indicate, via a persistent, non-interactive
  "Default" label in its top bar, when that wishlist currently holds the default designation.
- **FR-002**: Users MUST be able to designate a wishlist as the default via a "Set as default" action in
  that wishlist's detail-screen options menu — the same menu that already hosts "Delete list".
- **FR-002a**: The options menu on a wishlist's detail screen MUST follow the same show-only-when-actionable
  rule it follows today: it stays hidden for the current default (which has neither Delete nor Set-as-default
  available) and appears for any non-default wishlist, offering "Set as default" alongside "Delete list"
  (when deletion is otherwise allowed).
- **FR-002b**: Choosing "Set as default" MUST take effect immediately, without a confirmation dialog, and
  MUST show the user a brief confirmation message afterward.
- **FR-003**: Designating a new default MUST replace the previous default so that exactly one wishlist is
  the default at all times — never zero, never more than one.
- **FR-004**: The system MUST prevent deletion of whichever wishlist currently holds the default
  designation, regardless of which wishlist that is.
- **FR-005**: The system MUST allow deletion of any wishlist that does not currently hold the default
  designation.
- **FR-006**: The system MUST guarantee at least one wishlist always exists; since the default cannot be
  deleted, this guarantee holds automatically once FR-004 is enforced.
- **FR-007**: The heart/favorite action on a game's detail screen MUST add the game to whichever wishlist
  currently holds the default designation.
- **FR-008**: The heart/favorite action MUST toggle: if the game is already present in the current default
  wishlist, the action removes it from that wishlist instead of adding it.
- **FR-009**: Changing which wishlist is the default MUST NOT add, remove, or otherwise alter which games
  belong to any wishlist.
- **FR-010**: On first install, the wishlist the app is seeded with MUST be the default until the user
  changes it.
- **FR-011**: Every other quick-save action and saved-game indicator in the app (search results, Discover,
  recently viewed) MUST use the same default wishlist as the game-detail heart, so a game shows as saved
  exactly when it is in the default wishlist.
- **FR-012**: The overview list of all wishlists MUST show the same "Default" label on the row of
  whichever wishlist currently holds the default designation, and on no other row. It is informational
  only: it adds no action and does not change the order of the rows.

### Key Entities

- **Wishlist**: A user-created, named collection of games (existing entity). Gains a derived
  relationship to the default designation below; its own attributes (name, description, icon, cover
  image, game count) are unchanged by this feature.
- **Default designation**: A single, always-present pointer to exactly one of the user's wishlists. Moves
  between wishlists only when the user explicitly reassigns it; never absent, never on more than one
  wishlist at a time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can change their default wishlist in a single action from any non-default wishlist's
  detail screen.
- **SC-002**: 100% of attempts to delete the current default wishlist are blocked, across every entry
  point that can trigger a deletion.
- **SC-003**: After any change of default, 100% of subsequent heart-button taps save to the newly chosen
  default wishlist, with no games moved or lost from their existing wishlists.
- **SC-004**: Across the lifetime of an install, the app never reaches a state with zero wishlists.
- **SC-005**: A user can tell which wishlist is their default from the overview list alone, without
  opening any wishlist.

## Assumptions

- Picking a default is scoped to each wishlist's own detail screen for now (via its existing options
  menu, alongside "Delete list"). Showing the "Default" label on the overview list is in scope (FR-012),
  but offering the action there — e.g. a swipe action on the overview rows — is an intentionally separate,
  later decision and out of scope here.
- On the overview list the label is the same "Default" wording the detail screen uses, shown on the row
  itself; rows keep their current order, so the default list is not moved to the top.
- Only one wishlist can be the default at a time, and the designation is a property of the set of
  wishlists as a whole, not of any single wishlist's own data — reassigning it is a change of "which one,"
  not a change to the wishlists' own attributes.
- Switching the default is purely forward-looking: it only changes where the heart button saves to next;
  it never retroactively moves, copies, or removes games already saved in any wishlist.
- The app continues to start every fresh install with one seeded wishlist, which begins as the default —
  consistent with current behavior where a starter wishlist always exists and cannot be deleted.
