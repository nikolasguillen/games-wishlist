# Feature Specification: Default Wishlist Selection

**Feature Branch**: `005-wishlist-default-selection`

**Created**: 2026-10-01

**Status**: Draft

**Input**: User description: "we need to improve the wishlist screen. We need to allow the user to pick it's default wishlist. The default wishlist must NOT be deletable. This means that there must always be at least one wishlist. The heart button in GameDetailActionPill.kt must add the game to the wishlist the user selected as default."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose a default wishlist (Priority: P1)

A user who has created several wishlists (e.g. "RPGs to Try", "Co-op Picks", "Finished") wants to tell
the app which one is their main, go-to wishlist, so that quick-save actions elsewhere in the app have an
obvious, predictable target instead of always landing in whichever list happened to be created first.

**Why this priority**: This is the capability the whole feature exists to deliver. Without it, there is no
way to express a preference, and every other requirement in this feature has nothing to attach to.

**Independent Test**: Can be fully tested by opening the wishlist management screen, selecting a
different wishlist as default, and confirming the screen now reflects that choice as the current default.

**Acceptance Scenarios**:

1. **Given** a user has two or more wishlists, **When** they view the wishlist management screen, **Then**
   exactly one wishlist is visibly marked as the default.
2. **Given** a user has two or more wishlists and one is marked default, **When** they choose a different
   wishlist to be the default, **Then** that wishlist becomes the default and the previous default loses
   the designation.
3. **Given** a user has only one wishlist, **When** they view the wishlist management screen, **Then**
   that single wishlist is shown as the default and no other choice is available.

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

**Independent Test**: Can be fully tested by attempting to delete the wishlist currently marked as
default and confirming the deletion is blocked with clear feedback, while deletion of any non-default
wishlist succeeds normally.

**Acceptance Scenarios**:

1. **Given** a wishlist is currently the default, **When** the user attempts to delete it, **Then** the
   deletion is blocked and the user is shown a clear explanation.
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
- How does the system behave if a deletion is attempted on the default wishlist through any entry point
  (not just the primary wishlist management screen)? It must be blocked consistently everywhere deletion
  can be triggered, not only on the main screen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The wishlist management screen MUST visibly indicate which single wishlist is currently the
  default.
- **FR-002**: Users MUST be able to designate any of their existing wishlists as the default from the
  wishlist management screen.
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

### Key Entities

- **Wishlist**: A user-created, named collection of games (existing entity). Gains a derived
  relationship to the default designation below; its own attributes (name, description, icon, cover
  image, game count) are unchanged by this feature.
- **Default designation**: A single, always-present pointer to exactly one of the user's wishlists. Moves
  between wishlists only when the user explicitly reassigns it; never absent, never on more than one
  wishlist at a time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can change their default wishlist in a single action from the wishlist management
  screen.
- **SC-002**: 100% of attempts to delete the current default wishlist are blocked, across every entry
  point that can trigger a deletion.
- **SC-003**: After any change of default, 100% of subsequent heart-button taps save to the newly chosen
  default wishlist, with no games moved or lost from their existing wishlists.
- **SC-004**: Across the lifetime of an install, the app never reaches a state with zero wishlists.

## Assumptions

- The wishlist management screen (where wishlists are listed and created today) is where the user picks
  the default, since that is the existing surface for managing wishlists as a set; no new top-level screen
  is introduced for this.
- Only one wishlist can be the default at a time, and the designation is a property of the set of
  wishlists as a whole, not of any single wishlist's own data — reassigning it is a change of "which one,"
  not a change to the wishlists' own attributes.
- Switching the default is purely forward-looking: it only changes where the heart button saves to next;
  it never retroactively moves, copies, or removes games already saved in any wishlist.
- The app continues to start every fresh install with one seeded wishlist, which begins as the default —
  consistent with current behavior where a starter wishlist always exists and cannot be deleted.
