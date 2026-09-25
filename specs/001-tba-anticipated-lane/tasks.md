---

description: "Task list for the TBA anticipated-lane feature"
---

# Tasks: Unannounced upcoming games in the anticipated lane

**Input**: Design documents from `/specs/001-tba-anticipated-lane/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/upcoming-lane-release-filter.md

**Tests**: Included. Not because the spec asked for them, but because Constitution Principle V requires new
`:core:data` logic to get a test in its own module's `src/test` — and because `:core:data` already has the
suite and the idioms (`GameRepositoryImplPopularGamesTest`, JUnit4 + MockK, `RequestBody.asText()` query
assertions) to extend.

**Organization**: Grouped by user story. Read the scale note below before planning around these phases.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

## Scale note — read this first

This feature is two files. Be suspicious of any plan that makes it look bigger:

- **All production code is one method.** `fetchPopularityRankedGames` in `GameRepositoryImpl.kt` — one
  branch of one `if`, plus one constant.
- **US2 has no production code of its own.** The exclusion it asks for is a property of the same
  `where` clause US1 adds; the spec says as much ("it has no value on its own — it exists to bound P1").
  Its tasks are assertions, not implementation.
- **US3 has no production code at all.** `research.md` confirmed the unknown-date fallback and the hero
  selection already behave correctly. It is a verification task.
- **Nothing is parallelizable.** Every test task edits the same file
  (`GameRepositoryImplPopularGamesTest.kt`) and every implementation task edits the same file
  (`GameRepositoryImpl.kt`). No task carries `[P]`, and that is correct rather than an oversight.

---

## Phase 1: Setup

**Purpose**: Confirm the external dependency the whole design rests on before writing code against it.

- [ ] T001 Run the IGDB signal check in `specs/001-tba-anticipated-lane/quickstart.md` (section 1) and confirm it returns exactly one result, id `81249` (The Elder Scrolls VI). If id `28029` (Half-Life 3) also returns, or neither returns, STOP: IGDB has changed how it represents pending dates, and `specs/001-tba-anticipated-lane/contracts/upcoming-lane-release-filter.md` must be re-derived before any code is written.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Pin the behaviour that must *not* change, before editing the method that both lanes share.

**⚠️ CRITICAL**: T002 must be written and passing before T004 touches `fetchPopularityRankedGames`. It is
the only thing that will catch an accidental change to the popular lane, which FR-007 and SC-004 forbid.

- [ ] T002 Add a characterization test in `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplPopularGamesTest.kt` that captures the hydrate `RequestBody` from `repository.getPopularGames(emptySet())` and asserts the query contains `first_release_date != null & first_release_date <=` and does NOT contain `release_dates.date_format`. Follow the existing `slot<RequestBody>()` + `asText().contains(...)` idiom already used in that file.

**Checkpoint**: The popular lane is pinned. The shared method can now be edited safely.

---

## Phase 3: User Story 1 - Undated but anticipated games reach the shelf (Priority: P1) 🎯 MVP

**Goal**: Admit games with no `first_release_date` that carry at least one `release_dates` row with IGDB's
`date_format = 7` ("to be announced") into the anticipated lane, ranked by the same anticipation signal as
dated games.

**Independent Test**: The upcoming lane's hydrate query contains the new OR-fragment; the popular lane's
does not. End-to-end, an undated high-hype title (The Elder Scrolls VI) appears in "Most anticipated".

### Tests for User Story 1

> Write T003 first and watch it fail — it asserts a query fragment that does not exist yet.

- [ ] T003 [US1] Add a test in `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplPopularGamesTest.kt` asserting that the hydrate query from `repository.getUpcomingGames(emptySet())` contains `first_release_date = null & release_dates.date_format = 7`, and that the fragment is wrapped so the `|` cannot bind across the surrounding `&` clauses (assert the query contains `(first_release_date > ` and that a `)` closes the release filter before ` & ` rejoins the platform/id clauses). Expect this to FAIL until T005.

### Implementation for User Story 1

- [ ] T004 [US1] Add `private const val IGDB_DATE_FORMAT_TBD = 7` to `core/data/src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt`, alongside the existing `POPULARITY_TYPE_WANT_TO_PLAY` / `POPULARITY_TYPE_PLAYING` constants (around lines 54-63) and matching their KDoc style. The KDoc must state that this is IGDB's `release_dates.date_format` scalar for "to be announced" and that it is the same value `DatePrecision.TBD` maps from in `GameMapper.fromIgdbDateFormat`, so the two never drift.
- [ ] T005 [US1] In `fetchPopularityRankedGames` in `core/data/src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt` (the `releaseFilter` assignment, currently around line 297), replace the `upcomingOnly == true` branch with `"(first_release_date > $nowSeconds | (first_release_date = null & release_dates.date_format = $IGDB_DATE_FORMAT_TBD))"`. Copy the shape from `specs/001-tba-anticipated-lane/contracts/upcoming-lane-release-filter.md` exactly. **The outer parentheses are required** — without them the `|` binds across the id list, type exclusion and platform filter that the fragment is `&`-joined with. Leave the `else` branch (the popular lane) byte-for-byte unchanged.
- [ ] T006 [US1] Add a comment above the `releaseFilter` assignment in `core/data/src/main/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImpl.kt` explaining *why* the TBD branch is gated on `first_release_date = null` — matching the "explains why, not what" comment style the module's `CLAUDE.md` points at `WishlistCoverImageStorage.kt` for. The reason to capture: the gate is what makes an already-released game with a pending port structurally unable to reach the shelf.

**Checkpoint**: T003 now passes, T002 still passes. US1 is complete and independently verifiable.

---

## Phase 4: User Story 2 - Games whose date was lost to history stay out (Priority: P2)

**Goal**: Games with no date and no pending-date signal (23,757 of them in the live catalogue, per
`research.md`) stay excluded from the anticipated lane.

**Independent Test**: The query's TBD branch is gated on `first_release_date = null`, so a game with no
`release_dates` row cannot satisfy either branch.

**No production code.** This story is delivered by T005's `where` clause. The task below asserts the
property; if it fails, the fix is in T005, not in new code.

- [ ] T007 [US2] Add a test in `core/data/src/test/java/com/nikolasguillen/questlog/core/data/repository/GameRepositoryImplPopularGamesTest.kt` asserting the TBD branch is *gated*: the upcoming query must contain the exact substring `first_release_date = null & release_dates.date_format`, proving the `date_format` clause can never be satisfied on its own by a game that has a (past) `first_release_date`. Give the test a name that states the rule, e.g. `the TBD branch only applies to games with no first_release_date at all`.

**Checkpoint**: The guard that bounds US1 is pinned by a test.

---

## Phase 5: User Story 3 - Undated games render correctly wherever they land (Priority: P3)

**Goal**: An undated game's card — including the editorial hero — shows the existing unknown-date wording
rather than a blank slot or a fabricated date.

**Independent Test**: quickstart.md section 3, visually.

**No production code, and deliberately no automated test.** `research.md` confirmed
`GameUiMapper.toGameItem()` already falls back to `R.string.unknown_release_date` when `Game.releaseDate`
is null (`core/ui/mapper/GameUiMapper.kt:125-127`), and `DiscoverMapper.kt:19` already picks the hero with
`upcoming.firstOrNull()` with no date filtering. Per `data-model.md`, every game this feature admits has
`Game.releaseDate == null` by construction, so both paths are already exercised.

An automated test would mean creating a `src/test` source set for `:core:ui`, which has none.
`docs/tech-debt.md` lists `:core:ui` mappers as a known coverage gap, and Constitution Principle V says a
known gap is "not a blocker: do not plan a coverage campaign unless coverage is the feature being asked
for." It isn't. So this is verified by eye, once.

- [ ] T008 [US3] Build and run the app (`./gradlew :app:assembleDebug`, or from Android Studio), open the Search tab, and follow `specs/001-tba-anticipated-lane/quickstart.md` section 3: confirm an undated title appears in "Most anticipated", that its card shows the unknown-date wording rather than a blank or fabricated date, and that the hero renders completely if an undated game lands there. On Windows use `.\gradlew.bat`.

**Checkpoint**: All three stories verified.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T009 Delete the resolved `**TBA release dates.**` bullet from `docs/roadmap.md` (lines 26-32, under "Open items in the shipped Discover feed"). That file's own header states it "describes what is **not** built yet; the moment a phase ships, delete it from here", and root `CLAUDE.md` repeats the rule: do not rewrite the entry into a note saying it was fixed. Delete the bullet outright in the same commit as the code.
- [ ] T010 Run `./gradlew :core:data:testDebugUnitTest --console=plain -q` and confirm the whole `:core:data` suite is green, including the pre-existing `GameRepositoryImplPopularGamesTest` cases and `GameRepositoryImplDeveloperGamesTest` (which also asserts on query strings and must be unaffected).
- [ ] T011 Run `./gradlew test` and confirm every other module's JVM suite is still green. Per Constitution Principle V, `:app:assembleDebug` is not required here — this change touches one module and no DI wiring — but T008 will have built the app anyway.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies. T001 gates everything — if the IGDB signal is gone, the design is
  invalid and no other task should start.
- **Phase 2 (Foundational)**: Depends on T001. BLOCKS T005 specifically.
- **Phase 3 (US1)**: Depends on Phase 2. This is the only phase containing production code.
- **Phase 4 (US2)**: Depends on T005 (the code it asserts against).
- **Phase 5 (US3)**: Depends on T005 (needs the wider admission rule live to have an undated game to look
  at).
- **Phase 6 (Polish)**: Depends on all of the above.

### Within User Story 1

T003 (failing test) → T004 (constant) → T005 (filter edit) → T006 (comment). T004 before T005 only because
T005 references the constant.

### Parallel Opportunities

**None.** Two files, and every task in a given phase touches one of them. Marking anything `[P]` here would
invite two agents to collide on `GameRepositoryImplPopularGamesTest.kt`. Run T001 → T011 in order.

---

## Implementation Strategy

### MVP

Phases 1-3 (T001-T006) are the MVP and the whole user-visible value: undated anticipated games reach the
shelf. Stop there and validate if you want to ship incrementally.

US2 (T007) adds no behaviour — it pins the guard that is already implicit in T005. US3 (T008) adds no
behaviour either. Both are worth doing in the same sitting; neither is worth a separate release.

### Suggested commit shape

One commit for the code and its tests (T002-T007), one for the roadmap deletion (T009) — or fold T009 into
the code commit, since root `CLAUDE.md` asks for the doc change "in the same commit" as the change that
resolves it. The latter is more faithful to the rule.

---

## Notes

- Every task names its exact file path; line numbers are given as "around line N" because T004 shifts them.
- The two constants already in `GameRepositoryImpl.kt` (`POPULARITY_TYPE_WANT_TO_PLAY`,
  `POPULARITY_POOL_LIMIT_UPCOMING`) are the style guide for T004's KDoc — read them first.
- Do not add `release_dates.date_format` to the query's `fields` list. It is filtered on, never read. The
  contract document says why, and `research.md` confirms IGDB allows filtering on an unselected relation
  field.
