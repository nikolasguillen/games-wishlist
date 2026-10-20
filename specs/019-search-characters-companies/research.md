# Phase 0 Research: Search by Character and Developer

All findings about IGDB below were checked against the live API on 2026-10-20 with the project's own credentials
(read-only queries). Counts are what IGDB returned that day.

## R1. One lookup per search, chosen by the search type

**Decision**: Each submitted search sends exactly one request, to the endpoint of the selected type:

| Type | Endpoint | Query shape |
|---|---|---|
| Games | `/games` | unchanged (`search "q"; … limit 500;`) |
| Characters | `/characters` | `search "q"; fields name, mug_shot.url; limit 100;` |
| Developers | `/companies` | `fields name, logo.url, developed; where name ~ *"q"* & developed != null; limit 500;` |

**Rationale**:

- IGDB has no single query that serves all three. `search` is a per-endpoint clause. On `/companies` it returned
  `[]` for "nintendo", while `where name ~ *"nintendo"*` returned the companies, so developers are matched by
  name-contains. On `/characters`, `search` works and ranks by relevance ("kratos" → Kratos, Young Kratos).
- `/search`, the "everything" endpoint, returns only a name and a reference id with no images. It surfaced characters
  ("kratos") but never a company ("nintendo", "naughty dog"), so it cannot fill the new cards.
- `/multiquery` batches up to ten queries in one HTTP request, but a `search` clause inside it returned `[]`. Only
  `where` matches worked. Since the owner chose a type selector (clarification 2026-10-20), there is nothing to batch.
- `developed != null` keeps the Developers type to companies credited with developing at least one game. It drops
  pure publishers and catalogue debris ("Nintendo bro385" with one game survives, "Dr. Cruzlink"-style empty
  entries do not), which is what the chip's name promises.

**Alternatives considered**: three requests per search, rejected by the owner. `/multiquery`, rejected because
`search` does not work inside it, and name-contains for games would change the ranking of the existing Games search.

## R2. Ranking developer results

**Decision**: `SearchDevelopersUseCase` ranks the repository's matches on the client. Exact name match (ignoring case)
comes first, then names starting with the query, then the rest. Within a group, more developed games ranks higher, and
the name breaks ties. The result is capped at `DEVELOPER_RESULTS_LIMIT = 100`. Characters keep IGDB's relevance order
from `search` and are capped by the query's `limit 100`.

**Rationale**: a name-contains match has no relevance order. With `limit 10`, "capcom" did not even return Capcom
itself (id 37). The 500-row pool and the client ranking put the studio the user meant first. The number of developed
games (`developed`, an id array) is the only popularity signal a company has. The ranking is business logic, so it
lives in a use case that `:core:domain`'s JVM tests can reach, the same place as `SearchResultFacets`.

**Alternatives considered**: separate exact, prefix and contains requests, which triple the requests for a rare gain.
Sorting on the server is not possible, since apicalypse has no relevance sort for `~`.

**Known limit**: a query matching more than 500 companies ("studio" matches 7,019) ranks only the first 500 IGDB
returns, in id order, which favours older and better-known companies. This is acceptable for a name search.

## R3. Images on the new cards and in the header

**Decision**:

- Character portraits use `mug_shot.url` rewritten to `t_cover_big` (264×374), cropped into a circle.
- Developer logos use `logo.url` rewritten to `t_logo_med` (284×160, fit), drawn with `ContentScale.Fit` inside a
  rounded-square tile. The tile's background is a new `AppColors` slot, `logoTileContainerColor`, which is light in
  both themes.
- The existing `String.toIgdbImageUrl(size)` in `GameMapper.kt` becomes `internal` so the new mappers share it.
- With no image, the circle or tile shows a neutral icon (`Icons.Outlined.Person`, `Icons.Outlined.Business`) on
  `surfaceContainerHighest`, sized as a fraction of the shape, so no new `dp` literal is needed.

**Rationale**: both size names were checked (HTTP 200). IGDB logos are opaque JPEGs that mostly sit on white, so a
light tile matches them in both themes. A logo on a dark tile would show a white box. The constitution requires colors
to come from `MaterialTheme.appColors`, hence the new slot rather than a `Color.White` literal.

**Alternatives considered**: PNG logos (`.png` extension) on a theme-colored tile. A transparent black logo disappears
on the dark theme.

## R4. Where the detail page lives

**Decision**: in `:feature:search`, next to `SearchScreen`: `EntityDetailScreen.kt` and `EntityDetailViewModel.kt` at
the package root, with their models in `model/` and components in `components/`. `:feature:settings` already hosts
four screens this way.

**Rationale**: the page is reached only from Search. It needs the search grid's column rules
(`resultGridColumnCount`, `SearchResultGridCells`), the list-selector state and mapper (`ListSelectorState`,
`toSelectorItem`), the save snackbar strings, and the new portrait and logo images. All of these are already in
`:feature:search`, and only the translation UI has to move (R8). No new module and no new dependency edge.

**Alternatives considered**:

- A new `:feature:entity-detail` module would need the grid rules, the selector state and mapper, and the snackbar
  strings moved to `:core:ui` as well as the translation UI. It would also add a module plus a `:shared` →
  module edge. It is cleaner if the page ever gets a second entry point. This is listed as a decision for the owner.
- Hosting the page in `:feature:game-detail` would avoid moving the translation UI. It would instead move the grid
  rules, the selector and the strings out of Search, and a game-detail module showing a studio is misleading.

## R5. Loading the games of a character or developer

**Decision**:

- **Character**: the profile request returns the character's `games` (an id array). The games are then fetched with
  `where id = (ids) & game_type != (noisy) & version_parent = null`.
- **Developer**: the games are fetched with `where involved_companies.company = id &
  involved_companies.developer = true & game_type != (noisy) & version_parent = null`. This request runs in parallel
  with the profile request.
- Both go through one repository helper that pages `sort id asc; limit 500; offset n;` until a short page, with a
  safety cap of `CREDITED_GAMES_MAX_PAGES = 10` (5,000 games).
- The use case orders the full list newest first, with undated games last.
- The game-count chip is the size of that list.

**Rationale**:

- **Paging is needed.** After the same filters Search applies, some studios still exceed IGDB's 500 cap: Capcom has 509
  games and Konami 871. Naughty Dog has 31.
- **The filters change the count a lot.** Before filtering, Naughty Dog has 72 games, Capcom 919 and Konami 1,219.
  The chip must therefore count what the grid shows, not the raw `developed` array.
- **Load everything, then sort.** Without a sort or filter control, an all-at-once list is simplest. It is one
  request for almost every entity and two for the largest studios. The grid is lazy, so rendering cost is unaffected.
  Paging by `id` keeps the order stable across pages; paging by release date would not, because ties could
  duplicate or skip a game between pages. The newest-first order is applied once everything has arrived.
- **The developer filter works per row.** Conditions on one nested array apply to the same element. Sony returns 8
  games (developed) instead of 394 (published), and Square Enix 160 instead of 1,148. So the server returns only
  developed games, matching the spec's "the games it created".

**Spec impact**: FR-014 and Story 4 scenario 8 said the grid "loads progressively". The plan delivers every game, with
the header and description readable first. The spec was reworded to match (see the plan's "Spec updates"), and its
"catalogue's default order" assumption became "newest first, undated last".

**Alternatives considered**:

- Infinite scroll with `offset`, which needs a separate `/games/count` request for the chip, plus paging state in the
  view model.
- Reading the `X-Count` header (IGDB sends it) would give a total with each page, but `IgdbApiService` returns bare
  lists and would need a page type.
- Neither is worth it while the largest catalogue fits in two requests.

## R6. Character data in IGDB is sparse

**Finding, not a decision**: characters link to few games. Kratos links to 6, Mario to 2 and Link to 1. Games cannot
be queried by character (`games.characters` is not a field), so a character's page can only show what the
character's own `games` array lists. The feature works as specified, but character pages will often be short. This is
the data, not a defect to fix. It is worth knowing before judging the feature on a few searches.

## R7. Translating a character's or developer's description

**Decision**: generalize the translator's key from a game id to a subject:

- `GameDescriptionTranslator.translate(gameId: Int, description)` becomes
  `translate(subject: TranslationSubject, description)`.
- `TranslationSubject` is a sealed interface in `core/domain/translation/` with `Game(id)`, `Character(id)` and
  `Developer(id)`.
- `TranslatedDescriptionEntity` gains a `subjectType` column and renames `gameId` to `subjectId`. The primary key
  becomes `(subjectType, subjectId, languageTag)`. `translateWithCache` takes the subject.
- `TranslateGameDescriptionUseCase` becomes `TranslateDescriptionUseCase(subject, description)`. Its only caller is
  `GameDetailViewModel`.
- The interface keeps its name: it is referenced across `:core:data`, the platform contracts, Settings and the
  documentation, and renaming it adds nothing functional.

**Rationale**: the Room cache is keyed by `gameId`. Character 2134 (Kratos) and game 2134 would share a row. The
`sourceHash` check prevents a wrong translation from being shown, but the two would keep overwriting each other. An
explicit subject is the honest key.

**Consequence the owner should know**: this is a Room entity change. By the accepted policy (database pinned to
`version = 1`, `docs/tech-debt.md`), the exported schema is regenerated and local data on development devices is wiped.
Backing up through Settings before installing this build avoids losing lists.

**Alternatives considered**:

- Namespacing ids, such as negative ids for characters. That is a hack that silently breaks if IGDB ids change sign
  semantics, and still needs a third namespace.
- No cache for character and developer translations. That avoids the schema change but re-translates on every visit.
  It is listed as the owner's fallback.
- A content-addressed key (hash only). A 32-bit `hashCode` collision would return another text's translation.

## R8. Sharing the translation UI

**Decision**: move the description card and the badge family from `:feature:game-detail` to `:core:ui` unchanged in
behavior:

| From `:feature:game-detail` | To `:core:ui` |
|---|---|
| `components/GameDescriptionCard.kt` | `component/DescriptionCard.kt` (renamed, now `public`) |
| `components/TranslationBadge.kt`, `GeminiNanoBadge.kt`, `AppleIntelligenceBadge.kt` | `component/translation/` |
| `components/TranslationEngineBadge.kt` (`expect`) with its `android`/`ios` `actual`s | `component/translation/` (`commonMain`, `androidMain`, `iosMain`) |
| `model/DescriptionTranslationState.kt` | `model/DescriptionTranslationState.kt` |
| strings `description_translate_action`, `description_show_original`, `description_retry_translation_action`, `description_translating`, `description_translation_info_action`, `powered_by_*` (4), and drawable `ic_gemini` | `:core:ui` `composeResources` |

`GameDetailScreen` then imports them with the `CoreUiRes` alias rule. The view-model translation logic stays in each
view model (about 35 lines, a small state machine).

**Rationale**: the description card moves to the shared layer now that a second feature shows it (Principle IV).
`specs/010-kmp-migration/contracts/platform-contracts.md` lists `TranslationEngineBadge` under `:feature:game-detail`,
so that row's module changes to `:core:ui` in the same commit.

**Alternatives considered**: a second copy in `:feature:search`, which would drift (Principle IV), or hosting the page
in `:feature:game-detail` (R4).

## R9. The detail header and the shapes

**Decision**: extract the container of `WishlistDetailHeader` into `:core:ui` as `component/DetailHeader.kt`. It is a
tinted row with a `cover` slot, a title with configurable `titleMaxLines`, an optional description, and a `chips` row
slot. `WishlistDetailHeader` becomes a thin caller with no visual change. Its `96.dp × 128.dp` cover and `40.dp`
placeholder icon literals move into `DetailHeaderDefaults`.

The entity page passes, as the cover:

- a circular portrait for a character (`DetailHeaderDefaults.CoverWidth` square, `CircleShape`);
- a rounded-square logo tile for a developer (same square, `MaterialTheme.shapes.large`).

Its chips are the game count (`CustomInfoChip`), plus "Founded <year>" for a developer.
Long names wrap to two lines.

**Rationale**: the owner asked for a design like the wishlist detail screen. Sharing the container keeps both headers
identical in padding, color and typography, and the moved literals add no new `dp` (the `WishlistFormSheet` tech-debt
entry exists because of literals like these).

**Alternatives considered**: an `EntityDetailHeader` copy in `:feature:search`. It is quicker but drifts visually
(Principle IV).

## R10. Search state: selected type and results type

**Decision**:

- `SearchUiState` gains `selectedSearchType: SearchType` (the chip, default `GAMES`) and `resultsType: SearchType` (what
  the content area holds or is loading). They differ only while the user has picked a chip and not yet submitted.
- `performSearch` stamps `resultsType` and branches to the matching use case. History records the text for every
  type.
- Retry re-runs with `resultsType`.
- A genre browse or a Discover shelf sets both types to `GAMES` (FR-022 and the matching edge case).
- `SearchContentState` gains `EntityResults(items: List<EntityResultUiModel>)`. Its card shape comes from
  `resultsType`.
- The empty message is chosen by `resultsType`.
- The suggestions collector skips the remote game lookup unless `selectedSearchType == GAMES`. History suggestions
  still run. `OnSearchTypeSelected` re-emits the reset trigger, so switching type while typing clears or fetches the
  game suggestions straight away.
- The chip row sits at the top of the expanded overlay's content, above the existing `when` (suggestions, home or
  initial message), with a divider under it, as in `mockups/search-type-chips.png`. That keeps it visible both before
  and while typing.

**Rationale**: picking a chip never starts a search (FR-005), so the grid can legitimately show one type while the chip
shows another. A single field would make the empty message, the card shape and retry follow the chip instead of what
is on screen. Gating remote suggestions saves a request per keystroke that the user would never see.

**Alternatives considered**: making `SearchContentState.Empty` and `Loading` carry the type. That changes two `data
object`s that the existing tests and previews pattern-match.

## R11. The chip row's chips

**Decision**: `CustomFilterChip` gains an optional `leadingIcon: (@Composable () -> Unit)? = null`. The row uses
`CustomChipRow`. Each chip shows its type icon (`Icons.Outlined.SportsEsports`, `Person`, `Business`), and the selected
chip shows `Icons.Default.Check` in its place, as in the mockup.

**Rationale**: `core/ui/CLAUDE.md` says chip strips use `CustomChipRow` and the app's chips are `CustomFilterChip`. The
missing piece is the leading icon, a backwards-compatible parameter.

## R12. Navigation, view model and Koin

**Decision**:

- **Routes**: two routes in `Routes.kt`, `CharacterDetailRoute(characterId: Int)` and
  `DeveloperDetailRoute(developerId: Int)`, each with a `subclass(...)` line and a round trip in
  `RoutesSerializationTest`.
- **View model**: one `EntityDetailViewModel(subject: EntityDetailSubject, …)`. `EntityDetailSubject` is a public
  sealed interface in `:feature:search` with `Character(id)` and `Developer(id)`. The two `entryProvider` branches
  build it and pass it through `parametersOf(...)`. `KoinGraphTest` adds `EntityDetailSubject::class` to `extraTypes`.
- **Wiring**: `SearchScreen` gains `onCharacterClick: (Int) -> Unit` and `onDeveloperClick: (Int) -> Unit`. Both are
  plain guarded pushes.
- **Games from the page**: the page's `onGameClick` pushes `GameDetailRoute(gameId)` (not `fromSearch`), so it opens
  full screen.
- **Two-pane and rail**: unchanged. `searchDetailRun` only spans `fromSearch` details sitting directly on
  `SearchRoute`, so a detail page pushed over a run covers it, and back pops one entry, back to the two panes. The rail
  is hidden on the page, because it is not a top-level route, the same as the wishlist detail.

**Rationale**: two routes keep `Routes.kt` free of a new enum. A kind enum in `:core:navigation` would need either a
serializable type there or a `:core:navigation` → `:core:model` edge, and `:core:navigation` depends on neither today.
One view model avoids duplicating the translation, save, selector and loading logic.

**Alternatives considered**: two view models with an `Int` argument each (no `KoinGraphTest` change, but about 250
duplicated lines), or one route with a kind (see above).

## R13. Founding year and country

**Decision**:

- **Founding year**: `start_date` (Unix seconds) is turned into a year in UTC with `kotlinx-datetime` in
  `:core:data`'s mapper (`DeveloperProfile.foundedYear: Int?`). The chip reads "Founded %1$d".
- **Country: dropped by the owner.** IGDB's `country` is an ISO 3166-1 numeric code, and showing a localized name
  needs a 249-entry table plus an `expect`/`actual` for the locale display name. That is a separate change if it is
  ever wanted.

## R14. A profile IGDB no longer has

**Decision**: a profile request that returns no row becomes `AppResult.failure(RepositoryError.Unknown())`, built in
the repository, and the page shows the existing `ErrorPage` with retry.

**Rationale**: `RepositoryError` has no not-found case. Adding one means a new `toUiText()` string for a case that only
happens if IGDB deletes an entity between the search and the tap. The generic error is honest enough.

## R15. Quotes in a query

**Finding**: the existing game queries interpolate the user's text unescaped (`search "$query"`), so a `"` in the query
breaks the apicalypse and comes back as an HTTP 400, shown as the generic error. The new queries use the same
interpolation and inherit the same behavior. Escaping all queries is a separate fix across existing code. It is not
done silently here, and it is a candidate for `docs/tech-debt.md`.
