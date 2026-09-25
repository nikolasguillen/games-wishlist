# Feature Specification: Unannounced upcoming games in the anticipated lane

**Feature Branch**: `spec_kit_adoption`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Include unannounced upcoming games in the Discover feed's anticipated lane. Today that lane filters on a release date later than now, which silently drops every game whose release date IGDB has not recorded — so genuinely anticipated but undated titles never appear. Include a game whose release date is unknown because it has not been announced yet, and keep excluding one whose date was simply never recorded historically; the date-precision signal IGDB exposes is what tells the two apart."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Undated but anticipated games reach the shelf (Priority: P1)

A user opens Discover to find out what is coming. Several of the most talked-about unreleased games in
the catalogue have been announced but have no release date yet — only a year, a quarter, or nothing more
than "to be announced". Today none of them can appear in the "Most anticipated" shelf, no matter how much
anticipation they carry, because the shelf only admits games with a known future date. After this change
they appear alongside the dated ones, ordered by the same anticipation ranking.

**Why this priority**: This is the entire point of the feature. The shelf claims to show what is most
anticipated and currently cannot, because the games with the loudest anticipation are frequently the ones
with no date attached. Without this story there is nothing to ship.

**Independent Test**: Populate the catalogue pool with a mix of dated-future and announced-but-undated
titles, open Discover, and confirm the undated ones appear in the shelf in anticipation order. Delivers
the full user-visible value on its own.

**Acceptance Scenarios**:

1. **Given** a game that is announced, highly anticipated, and has no recorded release date, **When** the
   user opens the Discover feed, **Then** that game appears in the "Most anticipated" shelf.
2. **Given** a game that is announced with only a year or a quarter known, **When** the user opens the
   Discover feed, **Then** that game appears in the "Most anticipated" shelf.
3. **Given** a mixed set of dated-future and undated anticipated games, **When** the shelf is rendered,
   **Then** the existing anticipation ranking determines the order and undated games are not grouped
   separately, pushed to the end, or given any special position.

---

### User Story 2 - Games whose date was lost to history stay out (Priority: P2)

The catalogue contains old titles that shipped long ago but whose release date was never recorded. These
are indistinguishable from unannounced upcoming games by the absence of a date alone. A user browsing
"Most anticipated" must never see a decade-old title presented as something to look forward to.

**Why this priority**: This is the guard that makes User Story 1 safe to ship. Relaxing the date filter
without it turns the shelf into a mix of genuine upcoming titles and catalogue debris, which is worse
than the current behaviour. It is P2 only because it has no value on its own — it exists to bound P1.

**Independent Test**: Place a game with no date and no evidence of being announced into the pool, open
Discover, and confirm it is absent from the shelf while the announced-but-undated games from User Story 1
are present.

**Acceptance Scenarios**:

1. **Given** a game with no release date and no indication that a date is still pending, **When** the
   user opens the Discover feed, **Then** that game does not appear in the "Most anticipated" shelf.
2. **Given** a game that already released in the past, **When** the user opens the Discover feed,
   **Then** that game does not appear in the "Most anticipated" shelf regardless of how its date is
   recorded.

---

### User Story 3 - Undated games render correctly wherever they land (Priority: P3)

An undated game admitted to the shelf is shown on the same card as every other game. The card has a slot
for a release date, and the feed promotes the top-ranked anticipated game into a large editorial hero at
the top of Discover — so an undated game can occupy either.

**Why this priority**: Presentation, not admission. The shelf already has a fallback for an unknown date,
so this is expected to need no work; it is listed so it gets verified rather than assumed.

**Independent Test**: Force an undated game into the top-ranked position, open Discover, and inspect both
the hero and a shelf card for missing, empty, or placeholder-looking date text.

**Acceptance Scenarios**:

1. **Given** an undated game in the shelf, **When** its card is rendered, **Then** the date slot shows
   the existing unknown-date wording rather than being blank or showing a fallback timestamp.
2. **Given** an undated game ranked first among anticipated games, **When** the feed is rendered,
   **Then** the hero renders completely, with no empty region where a date would be.

---

### Edge Cases

- **Every anticipated result is undated.** The shelf is entirely undated games. It must still render as a
  normal shelf, and the hero must still be chosen, rather than falling back to the empty-feed placeholder.
- **An undated game is later given a real date.** On the next feed fetch it must appear exactly once, with
  its date shown — not duplicated across the two admission paths and not dropped by both.
- **A game has a date in the past but is still marked as pending.** Contradictory data. It must be treated
  as released and excluded, so a stale marker cannot resurrect an old title into the shelf.
- **No anticipation signal has any precision information at all.** The shelf must degrade to its current
  behaviour — dated future games only — rather than emptying out.
- **The user has a platform filter active.** Undated games must be subject to exactly the same platform
  filtering as dated ones; the relaxed date rule must not become a way to bypass it.
- **An undated game carries no cover art.** It is excluded on the same terms as any other game, since the
  shelf is a grid of covers.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The anticipated shelf MUST admit games that have no recorded release date but are still
  awaiting one.
- **FR-002**: The anticipated shelf MUST continue to admit games with a recorded release date in the
  future, with no change to which of them qualify.
- **FR-003**: The anticipated shelf MUST exclude games with no recorded release date when there is no
  indication that a date is still pending.
- **FR-004**: The anticipated shelf MUST exclude games whose recorded release date has already passed.
- **FR-005**: The system MUST distinguish "no date because it has not been announced" from "no date
  because it was never recorded" using the release-date precision information the catalogue exposes, and
  MUST NOT infer the difference from the game's age, rating, popularity, or name.
- **FR-006**: Undated games admitted to the shelf MUST be ranked by the same anticipation signal as dated
  games, interleaved with them rather than segregated.
- **FR-007**: The "popular" shelf of already-released games MUST behave exactly as it does today — same
  admissions, same exclusions, same order.
- **FR-008**: The platform filter, the exclusion of noisy game types, the requirement that a game have
  cover art, and the size of the candidate pool MUST apply to undated games on identical terms.
- **FR-009**: The order in which the feed presents anticipated games MUST remain the anticipation ranking,
  including after the pool is hydrated with full game details.
- **FR-010**: The structure of the feed handed to the Discover screen, and the domain models the screen
  consumes, MUST NOT change; this feature changes which games occupy the existing anticipated slot only.
- **FR-011**: A game MUST appear at most once in the anticipated shelf, regardless of how its date is
  recorded.
- **FR-012**: When no undated game qualifies, the shelf MUST contain exactly what it contains today.
- **FR-013**: The editorial hero MUST continue to be the top-ranked anticipated game, whether or not it
  has a release date. The hero selection MUST NOT skip, demote, or prefer a game on the basis of whether
  its date is known.

### Key Entities

- **Anticipated game**: An unreleased catalogue entry eligible for the anticipated shelf. Carries an
  anticipation score used for ranking, optional cover art, optional platform associations, and a release
  date that is either known, known only approximately, or explicitly still pending.
- **Release date precision**: How exactly a game's release date is known — an exact day, a month, a
  quarter, a year, or explicitly to-be-announced. The distinction between "explicitly pending" and
  "absent entirely" is the signal this feature depends on.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user browsing the anticipated shelf sees announced-but-undated titles that were
  previously impossible to surface there.
- **SC-002**: No already-released game appears in the anticipated shelf, measured by inspecting every
  entry the shelf produces across a representative set of platform-filter states.
- **SC-003**: The shelf's ordering for games that qualify today is unchanged, verified by comparing the
  sequence of those games before and after the change.
- **SC-004**: The popular shelf produces an identical result before and after the change for the same
  inputs.
- **SC-005**: Every undated game shown in the feed displays the existing unknown-date wording, with no
  blank date slot and no placeholder date.
- **SC-006**: The Discover feed continues to load and render in a single pass with no additional user-
  visible wait introduced by the wider admission rule.

## Assumptions

- The catalogue distinguishes "release date still to be announced" from "no release-date record at all".
  This is the load-bearing assumption of the whole feature: the two cases are what FR-005 separates, and
  if the catalogue turns out not to mark pending dates reliably, User Story 2's guard cannot be built as
  specified and the feature must be reconsidered rather than shipped with a weaker guard.
- The anticipation ranking already used by the shelf applies to undated games as meaningfully as it does
  to dated ones, so no separate ranking treatment is needed.
- The existing unknown-date wording shown elsewhere in the app is appropriate for this shelf; no new
  wording is introduced.
- The candidate pool size stays as it is. Admitting more games may mean the pool fills with different
  titles than before, and that is accepted rather than compensated for by enlarging it.
- No new user-facing setting governs this behaviour; the wider admission rule applies to everyone.
- The hero slot continues to be filled from the top of the anticipated ranking, with no preference for
  or against undated games (see FR-013). Decided 2026-09-25: the ranking decides the hero, and the hero
  falls back to the existing unknown-date wording like any other card.
