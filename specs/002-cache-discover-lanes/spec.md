# Feature Specification: Cache the Discover feed's generic lanes

**Feature Branch**: `002-cache-discover-lanes`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Cache the Discover feed's two generic lanes (\"Most anticipated\" and \"Popular this month\") so reopening Discover does not repeat the network round-trip. Today GameRepositoryImpl.fetchPopularityRankedGames hits IGDB fresh on every call — a /popularity_primitives rank plus a /games hydrate, per lane — and nothing is persisted; GetDiscoverFeedUseCase's own comment already notes this is \"a screen the user opens constantly\", so the repeated cost is real, not hypothetical. Do not persist these results into the existing `games` table. That table already doubles as a cache with ownership flags (isWishlisted, lastViewedAt) for the user's own games, and mixing in catalogue browsing results would make \"the user's own games\" ambiguous — this was an explicit decision recorded in docs/roadmap.md. Use a separate cache entity holding the ordered id list plus a fetchedAt timestamp instead. Scope is the two generic lanes only. The personalized \"recommended\" shelves already have their own staleness signal and are out of scope here."

## Clarifications

### Session 2026-09-25

- Q: Should the cached "Most anticipated" data include the editorial hero pick as the first element of the same ordered list, so that reopening Discover from cache restores the hero too without a network call? → A: Yes — the hero is the first element of the cached "Most anticipated" ordered list; there is no separate cache entry for it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reopening Discover shows the same lanes instantly (Priority: P1)

A user browses Discover, backs out to another tab or screen, and comes back within the same session. The
"Most anticipated" and "Popular this month" lanes appear immediately, without a visible loading state,
showing the same games and order as before.

**Why this priority**: This is the entire point of the feature — Discover is, per the existing use case
documentation, "a screen the user opens constantly," and every reopen today re-runs two network calls per
lane for content that has not changed. Removing that wait on the common path is the whole value being
delivered.

**Independent Test**: Open Discover, let both generic lanes load, navigate away, then navigate back. Can be
verified by confirming the lanes render without a loading indicator and without a new network request, and
that the games and their order match the prior load exactly.

**Acceptance Scenarios**:

1. **Given** a user has opened Discover once and both generic lanes loaded successfully, **When** they
   leave and reopen Discover a minute later, **Then** both lanes display the same games in the same order
   as before, with no loading state shown for those two lanes.
2. **Given** a cached lane exists, **When** Discover reopens, **Then** the personalized shelves and the
   staleness prompt for recommendations continue to behave exactly as they do today — this feature changes
   nothing about how those are loaded or refreshed.
3. **Given** the "Most anticipated" lane is served from cache, **When** Discover reopens, **Then** the
   editorial hero pick also appears immediately with no network call, since the hero is the first element
   of that same cached lane rather than a separately fetched or cached item.

---

### User Story 2 - Cached lanes refresh once they go stale (Priority: P2)

A user reopens Discover after enough time has passed that the cached lane content is no longer considered
current. The app fetches fresh data for that lane instead of showing the stale cached copy indefinitely.

**Why this priority**: A cache that never refreshes would eventually show a "Most anticipated" or "Popular
this month" lane that no longer reflects reality (games released, popularity shifted). This is what keeps
the feature from trading a real cost (network calls) for a worse one (permanently stale content).

**Independent Test**: Force the cached lane's age past the staleness threshold (e.g. by manipulating the
stored timestamp in a test), reopen Discover, and confirm a fresh network fetch occurs and the lane updates.

**Acceptance Scenarios**:

1. **Given** a cached lane was fetched long enough ago to be considered stale, **When** the user opens
   Discover, **Then** the app fetches that lane fresh from the network and replaces the cached copy with
   the new result.
2. **Given** a cached lane is still within its freshness window, **When** the user opens Discover,
   **Then** no network call is made for that lane and the cached copy is shown as-is.

---

### User Story 3 - Platform selection changes are respected even when cached (Priority: P2)

A user changes their platform filter in Settings, then opens Discover. The generic lanes reflect the new
platform selection rather than showing a cached result computed for the old selection.

**Why this priority**: The existing use case already re-emits the feed whenever the platform selection
changes, specifically so the picker doesn't look broken until a restart. A cache that ignored the platform
selection would reintroduce that exact bug for the two lanes being cached.

**Independent Test**: Load Discover with one platform selection, change the platform filter in Settings,
return to Discover, and confirm the lanes reflect the new filter rather than the old cached result.

**Acceptance Scenarios**:

1. **Given** a cached lane exists for platform selection A, **When** the user changes their platform
   selection to B and opens Discover, **Then** the lane is fetched fresh for selection B rather than
   showing the cache built for A.
2. **Given** a cached lane exists for the current platform selection, **When** the user opens Discover
   without changing the platform selection, **Then** the cached copy is used.

---

### Edge Cases

- What happens when a cached lane's games include one the user has since wishlisted, or one that no longer
  exists / has since become unavailable from the catalogue? The lane should still render using the cached
  ordering; ownership flags and per-game details are resolved the same way the rest of the app already
  resolves them for any catalogue game, not duplicated into the cache.
- What happens when the network fetch to refresh a stale lane fails (no connectivity, IGDB error)? The
  user should not lose the lane entirely — the previous cached copy (even though stale) should remain
  visible rather than the lane going blank or erroring out, consistent with how a "screen opened
  constantly" should degrade gracefully.
- What happens on the very first Discover open, with no cache yet for either lane? Behavior is unchanged
  from today: both lanes fetch fresh from the network.
- What happens when the platform selection is cleared back to "no filter"? This is treated as its own
  distinct selection, the same as any other change — a cache built under a specific filter is not reused
  for "no filter" and vice versa.
- What happens if the two generic lanes are cached at different ages (e.g. one refreshed moments ago, the
  other stale)? Each lane's freshness is evaluated independently; one can serve from cache while the other
  triggers a fresh fetch in the same Discover load.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST persist the result of each generic lane ("Most anticipated" and "Popular
  this month") after a successful fetch, including the ordered list of game identifiers and the time the
  fetch completed. For "Most anticipated", this list MUST include the editorial hero pick as its first
  element — the hero is not a separate fetch or a separate cache entry, it is simply the first item of
  this same ordered list, exactly as it is derived today.
- **FR-002**: The system MUST persist a cached lane's result separately from the platform selection it was
  fetched under, such that a cached result is only reused when the current platform selection matches the
  one it was fetched with.
- **FR-003**: When a user opens Discover and a valid (non-stale, matching platform selection) cached
  result exists for a generic lane, the system MUST display that cached result without issuing a new
  network request for that lane.
- **FR-004**: When a user opens Discover and no valid cached result exists for a generic lane (absent,
  stale, or fetched under a different platform selection), the system MUST fetch that lane fresh from the
  network, display the result, and persist it as the new cached result.
- **FR-005**: The system MUST treat each generic lane's cached freshness independently — one lane may be
  served from cache while the other is refreshed in the same Discover load.
- **FR-006**: The system MUST NOT persist generic-lane results into the existing games/ownership storage;
  the cached lane data MUST be held separately from any table that also tracks the user's own
  wishlisted/owned games.
- **FR-007**: The system MUST leave the personalized "recommended" shelves and their existing staleness
  signal untouched — no caching, storage, or freshness logic introduced by this feature applies to those
  shelves.
- **FR-008**: When a network refresh of a stale generic lane fails, the system MUST continue showing the
  previously cached result for that lane rather than an empty or error state, as long a previously cached
  result exists.
- **FR-009**: The system MUST apply a fixed staleness window after which a cached generic lane is
  considered expired and eligible for a fresh fetch on the next Discover open.

### Key Entities

- **Generic Lane Cache**: Represents the last successful fetch of one generic Discover lane ("Most
  anticipated" or "Popular this month") for one platform selection. Holds which lane it is, the platform
  selection it was fetched under, the ordered list of game identifiers returned by that fetch, and the
  timestamp the fetch completed. Distinct per lane and per platform selection — it does not carry any
  ownership or user-relationship data (that remains the responsibility of the existing games storage). For
  the "Most anticipated" lane, the editorial hero pick is not a distinct entity — it is the first game
  identifier in this same ordered list.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Reopening Discover with a valid cache for both generic lanes shows both lanes' final content
  with no visible loading state and no network activity for those lanes.
- **SC-002**: The number of network round-trips issued for the two generic lanes, counted across repeated
  Discover opens within a single freshness window, drops to one round-trip pair per lane per window
  instead of one pair per open.
- **SC-003**: A user who changes their platform filter always sees generic lanes that match the new filter
  on the very next Discover open — never a cached result computed for a prior filter.
- **SC-004**: A temporary network failure while refreshing a stale lane never results in an empty or
  errored generic lane as long as a prior cached result exists for the current platform selection.

## Assumptions

- "Reopening Discover" means returning to the Discover screen within the same app process/session as well
  as across a fresh app launch — the cache is expected to survive at least a process restart, since it is
  persisted (not held only in memory), matching how the existing `games` table cache already behaves.
- A single fixed staleness window applies to both generic lanes; the exact duration is an implementation
  tuning detail left to planning, not a product decision this spec needs to fix. A window on the order of
  hours (long enough to avoid re-fetching within a session, short enough that "Popular this month" stays
  meaningful) is a reasonable default.
- The platform selection used to key the cache is the same set of platform IDs already used to scope these
  two lanes today (from Settings); no new selection concept is introduced.
- This feature does not change what data the two lanes fetch or how they are ranked — only whether/when
  the fetch happens. The existing ranking, filtering, and hydration behavior of the two lanes is preserved
  exactly.
- Clearing the app's data/cache (e.g. via OS settings) is out of scope for defining special behavior; it is
  expected to behave like any other persisted cache being wiped, falling back to User Story 1's "no cache
  yet" path.
