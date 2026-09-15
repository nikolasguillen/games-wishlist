# Roadmap

Planned features, in the order they are meant to be built, plus the decisions already taken so they are
not re-litigated in a later session. This file describes **what is not built yet**; the moment a phase
ships, delete it from here — `git log` is the history.

Written 2026-08-20.

## The shape that was decided

Two ideas were on the table: a `discover` tab (hyped/upcoming games filtered by the user's taste) and a
`calendar` tab (release dates of saved games). They overlap because they order the same catalogue on two
different axes — relevance and time. The resolution:

- **Discover is not a tab.** It replaces the zero-query state of the Search screen, which today is a dead
  `EmptyPage` placeholder. Search becomes "explore": no query → taste-based feed, query → results.
- **Radar is a tab.** A single chronological timeline that merges the user's saved upcoming games with
  taste-matched suggestions, distinguished visually rather than by living on separate screens.
- **No month-grid calendar.** IGDB release dates carry a precision flag — a large share of interesting
  upcoming titles are only `Q4 2026`, `2027` or `TBD` and cannot be placed on a calendar cell. The
  timeline uses buckets (This week / This month / Next 3 months / Later / TBA), which accommodate partial
  dates natively. Revisit only if the bucket list proves insufficient in practice.
- **Notifications are the payoff of Radar**, not an optional extra. A timeline the user must open is worth
  far less than a reminder that arrives on release day.

Bottom bar goes from 2 tabs to 3: Search · Radar · Lists.

- **Settings is not a tab.** A `SettingsRoute` (the "My platforms" picker lives here first) is reached via
  a profile icon in the top-right corner, present on every top-level screen — Search and Lists today,
  Radar when it exists — and absent from stacked screens (detail, wishlist, settings itself, and anything
  else pushed on the backstack). A fourth bottom-bar tab was considered and
  rejected: Search/Radar/Lists are peer content destinations, Settings is a utility action, not a peer of
  the same kind. The icon itself is one shared composable in `:core:ui` so Search, Lists and (later) Radar
  call the same implementation instead of duplicating it.

## What the taste profile already gives you

`core/domain/usecase/discover/` is built and tested: `GetTasteProfileUseCase` emits a `TasteProfile`
(`:core:model`) of normalised genre and developer weights, and `GetSelectedPlatformIdsUseCase` /
`GetSelectedPlatformsUseCase` / `SetOwnedPlatformsUseCase` / `GetKnownPlatformsUseCase` own the platform
filter, which is the user's explicit selection and nothing else. `TasteProfile.isEmpty`
is the cold-start signal. The "My platforms" picker is built end to end (`OwnedPlatformsRoute`, reached
from the Settings hub) and `SyncPlatformCatalogUseCase` fills the local `platforms` table from IGDB's
`/platforms` endpoint, so the picker offers the whole catalogue instead of only what the user's saved
games happen to cover. `GetDiscoverFeedUseCase` reads the selection and narrows every Discover shelf with
it, so the setting is live. It also reads `GetTasteProfileUseCase` to build the personalised shelves, so
every use case in `usecase/discover/` now has a consumer.

**The feed observes the selection**: `GetDiscoverFeedUseCase` returns a `Flow` keyed on the picked
platforms and re-fetches when they change, so the shelves follow the setting without waiting for a new
process. The taste profile is *not* refetched the same way — it is derived from the saved games, which
change on every status, priority or list edit, and refetching several calls on each of those costs far
more than the shelves are worth. But the feed does not simply go stale and say nothing: the use case also
tracks the genres the personalised shelves are built from (up to two, strongest first — see below), which
is the only part of the profile it actually reads, and re-derives that cheaply on every library change.
Once it no longer matches the ordered genre list a loaded feed was built against, `DiscoverFeed.
hasStaleRecommendations` flips, `SearchViewModel` surfaces it as `DiscoverContentState.Content.isStale`,
and `DiscoverFeed` (the composable) renders a sticky "Refresh suggestions" prompt pinned to the top of the
list. Tapping it is what actually re-fetches, via an explicit `refresh: Flow<Unit>` the use case also
accepts — the network call stays opt-in, the staleness signal is free.

`SearchViewModel` collects that flow for its whole life and gates the *display* instead of cancelling
the collection. The race it guards against — a committed search and a slower feed fetch both landing in
`contentState` — is a write conflict, not a fetch conflict, so a boolean is enough. Cancelling, the
previous fix, also blinded the feed to platform changes made mid-search and forced a re-fetch on clear.

**Settings holds only what has a backend.** The screen groups its rows by theme, and the only groups
that exist are the ones with data behind them. Notifications belong to Phase 3; a genre picker was
considered and dropped, since the taste profile infers genres from saved games and the user does not
edit them by hand; appearance has nothing to switch, because `:core:designsystem` is dark-only by
design. Do not add a row before the thing it configures exists.

## Phase 1 — Discover feed in Search

Replaces `SearchContentState.Initial` (today `InitialSearchPlaceholder` in
`feature/search/components/SearchPlaceholders.kt`). The search bar overlay keeps owning recent searches and
recently viewed games — the feed lives in the body, behind it.

- Cold start (`TasteProfile.isEmpty`) degrades to generic popular/upcoming. Build that path first — it is
  the first shippable milestone and it needs none of the ranking.
- Coarse filtering server-side in apicalypse (`where genres = (...) & platforms = (...)`), fine ranking
  locally. Personalisation is not expressible in apicalypse, and `limit` caps at 500 — fetch a candidate
  pool, rank in a mapper/use case.
- Every row states its reason ("Because you saved Baldur's Gate 3", "Your genre: RPG"). A recommendation
  the user cannot explain reads as a bug.
- New API surface on `IgdbApiService`: same `@POST("games")` endpoint, different apicalypse bodies.
  Worth checking `popularity_primitives` for real hype signal instead of the raw `hypes` field.

**Personalisation is its own shelves, not a reshuffle of the generic two.** The generic lanes are the
global popularity top-N; reordering them by taste only reorders what was already globally popular, which
is not what the user's profile says. So the taste profile buys its own query (`where genres = (...)`) per
genre it leans towards, each its own shelf, titled with the reason. Reasons sit on the shelf rather than
on each card: a 140dp cover has no room for a sentence, and every game in a shelf is there for the same
reason anyway.

Deliberate limits of the shelves as built, each one a place to extend rather than a bug:

- **Up to two shelves — a recurring developer first when the library earns one, then the strongest
  positive genres.** Every extra shelf is another network call on a screen the user opens constantly, so
  the cap (`MAX_RECOMMENDED_SHELVES` in `GetDiscoverFeedUseCase`) is a cost decision, not a belief that a
  third shelf would not also be worth showing. A developer only takes the lead slot at
  `MIN_DEVELOPER_SAVED_GAMES` (2) saved games from it with a positive weight — one game is a coincidence,
  not a pattern — which is what `TasteSignal.count` in `TasteProfile` exists to tell apart from a
  normalised weight alone.
- **A game that qualifies for two shelves is kept once**, in the stronger shelf only — pruning runs in
  rank order, each shelf excluding what every stronger one already kept, on top of saved games and the
  generic shelves. Recommending the same game twice for two different reasons in one feed reads as a bug,
  not as extra confidence.
- **Each shelf disappears rather than degrading, independently of the others**: below a minimum sample
  size, with no positive signal left for it, on a failed fetch, or when pruning leaves too few entries. A
  half-empty personalised row next to two full generic ones reads as a loading bug — losing one of two
  personalised shelves to this does not take the other down with it.

**No rating-count floor — rank by confidence instead.** IGDB can sort by raw score but not by a score
weighted for how many people voted, so `sort total_rating desc` leads with whatever scores 100 across
three votes. Excluding thinly-rated games is the wrong fix: it drops every niche and newly released
title in the genre, which is what the shelf exists to surface, and does nothing about a mediocre game
that clears the floor. So the query keeps only a token floor and the use case re-ranks the whole pool by
a Bayesian average against a neutral prior. That is why the pool is far wider than the shelf: a narrow
one would already be filled by the games the ranking is meant to demote. The developer shelf drops even
that token floor — it exists to surface a followed studio's unreleased titles too, which have no ratings
at all yet, and orders them ahead of the rest by hype instead.

`RATING_CONFIDENCE_THRESHOLD` and `NEUTRAL_RATING` in `GetDiscoverFeedUseCase` are the knob — raise the
prior and thin gems climb, lower it and the shelf fills with established titles. **Both are reasoned
guesses about IGDB's rating distribution that have never been checked against a real pool.** Validate
them against live data before treating the shelf's quality as settled.

**An empty platform selection means no platform filter** — the feed omits the `where platforms = (...)`
clause entirely rather than substituting something. An earlier design filled the gap with the platforms
carried by the user's saved games. That was removed: it filtered the feed on a rule the user could neither
see nor explain, which is the opposite of the "every row states its reason" line above, and it did nothing
for the brand-new user it was meant to help, whose library is empty too. Do not reintroduce a fallback
here, and do not seed the selection behind the user's back either — the picker is one tap from every
top-level screen and holds the whole IGDB catalogue.

The filter lands on the `/games` hydrate call, never on `/popularity_primitives`, which returns a game id
and a score and has no platform field. The pool is therefore ranked before it can be filtered, so a
narrow selection thins an already-truncated list — that is what the widened pool limit in
`GameRepositoryImpl` pays for. Any further filter added to a lane inherits the same problem.

**Cache**: do not dump discovered games into the `games` table unqualified. That table already doubles as
a cache with ownership flags (`isWishlisted`, `lastViewedAt`); mixing in feed results makes "the user's own
games" ambiguous. Use a separate entity holding the ordered id list plus a `fetchedAt` stamp.

**TBA release dates — open problem.** The two lanes split on the release window (`first_release_date > now`
for "Most anticipated", `<= now` for "Popular this month"), which drops games with a null
`first_release_date`. Plenty of genuinely anticipated upcoming titles are still TBA, so they silently miss
the anticipated shelf. Not just relaxing the filter: a bare `| first_release_date = null` also lets in old
games whose date was never recorded, so we cannot tell "unannounced upcoming" from "date lost to history"
without the date-precision flag — the same flag the Radar timeline depends on (see the Phase 2 note on
`release_dates`). Decide this together with that work rather than bolting a heuristic on here.

### Debounced remote suggestions stay

They were considered for removal as redundant with the results grid. They are not: the grid answers "show
me everything matching X", the suggestions answer "I already know the game, take me to it" — a tap goes
straight to the detail screen. In a wishlist app search is predominantly known-item, and the Discover feed
makes it more so, since browsing moves to the feed. Removing them would also leave the expanded search bar
overlay holding three strings of history while covering the feed the user was browsing.

The overlay itself is built. The suggestion cap is 4, so `sort hypes desc` is aggressive — an obscure
title can be squeezed out by hyped ones sharing a substring. If that becomes annoying, sort by name-match
quality rather than raising the cap.

Note the two paths do not use the same matching: suggestions use `where name ~ *"query"*` sorted by hypes,
the full search uses IGDB's `search "query"` full-text relevance with a different `game_type` filter. A
suggestion is therefore a shortcut, not a preview of the grid. That is defensible for an autocomplete —
substring-on-title is less noisy than IGDB full-text — and it is why the "see all results" row commits the
query to the grid.

## Phase 2 — `:feature:radar`, saved games only

- New module (copy `feature/search/build.gradle.kts`, register in `settings.gradle.kts`), `RadarRoute` in
  `core/navigation/Routes.kt`, tab wired in `:app`.
- Chronological bucket timeline over the user's saved games.
- **The release dates are not there yet.** `GamePlatformCrossRef.releaseDate` is only populated by the
  detail fetch (`GameRepositoryImpl.kt:125` requests `release_dates.date`; the search query at line 52 does
  not). A refresh job for saved games' dates is a prerequisite, not a detail.
- Query `@POST("release_dates")`, not `games`: it returns one row per game×platform×region, sortable and
  filterable by date directly, and carries the date-precision field the buckets depend on.
- Dates slip constantly. Without periodic refresh the timeline lies, which is worse than not having one.

## Phase 3 — Release notifications

Opt-in per game ("Notify me"), driven by the same refreshed dates.

## Phase 4 — Suggestions lane in Radar

Fold the taste profile into the timeline: saved games get the visual accent, suggestions sit in a minor
tone alongside them. Only after phases 2 and 3 are real.

## AI description translation

**Independent of Phases 1–4** — it shares no code with Discover or Radar and can be built whenever. Added
2026-09-15.

Translate the IGDB `summary` rendered by `GameDescriptionCard` into the device language, entirely
on-device, through ML Kit's GenAI **Prompt API** backed by Gemini Nano in AICore. The point of using an
LLM rather than a phrase-based translator is context: it can tell a game mechanic from its literal
meaning, and leave titles and studio names alone.

### The decisions, so they are not re-litigated

- **Gemini Nano only. No second engine.** ML Kit's classic Translation API was weighed and dropped: it
  downloads a ~30 MB NMT model per language pair — the per-app download this feature exists to avoid —
  and it translates sentence by sentence with no notion of the domain. Gemini Nano is a *system* model
  shared by every app on the device, so the app ships no model at all. The seam for adding a fallback
  later is `GameDescriptionTranslatorImpl` in `:core:data`: a second client class in `:core:ai` plus a
  branch there. **Do not build that abstraction before the second engine exists.**
- **Only `FeatureStatus.AVAILABLE` counts as supported.** `DOWNLOADABLE` and `DOWNLOADING` are treated as
  unsupported and **`download()` is never called**. The feature is meant to appear only where the system
  model is already present, so the app never spends the user's bandwidth on it.
- **Off by default, switched on in Settings.** Once on, translation runs automatically when a game detail
  opens. There is no per-game "translate" button.
- **Where it cannot work, the Settings row is absent, not greyed out** — no Gemini Nano, or a device
  already in English. A switch that can never move is worse than no switch at all.
- **The card shows a loading placeholder while translating** and falls back to the original English
  silently on any failure. A failed translation is not an error state: it is just the original text.
- **Translations are cached in Room**, keyed by game id + language tag + a hash of the source text, so an
  IGDB summary that gets edited is re-translated instead of served stale.
- **The toggle lives in DataStore**, which this feature introduces. `androidx.datastore-preferences` is
  already in the version catalog and used by nothing. Room was considered — it is where
  `OwnedPlatformEntity` keeps the platform filter — and rejected for this: the destructive migration
  wipes the database on every schema change, which would silently reset the user's choice, and a
  one-row table is the wrong shape for a boolean.
- **A new `:core:ai` module** holds the ML Kit SDK, isolating it exactly as `:core:network` isolates
  Retrofit. `core/data/CLAUDE.md` calls `:core:data` the module a KMP move would want in `commonMain`;
  putting a Play-Services-backed, Android-only SDK in it would undo that.

**KMP**: ML Kit GenAI has no multiplatform counterpart. What survives a migration is the
`GameDescriptionTranslator` port in `:core:domain`; `:core:ai` is replaced wholesale. Same shape as the
`ReleaseRefreshScheduler` decision below.

### Two API facts to verify against the artifact before writing code

The published snippets for `com.google.mlkit:genai-prompt:1.0.0-beta4` (Beta) show `Generation.getClient()`,
`checkStatus()`, `generateContent(prompt)` and `generateContentStream(prompt)`, with a documented input
ceiling of ~4000 tokens (≈3000 English words) — IGDB summaries sit far below it. Two things the docs do
not pin down and that change the code:

1. **Whether `checkStatus()` / `generateContent()` are `suspend` or return a `Task`.** If they return a
   `Task`, `:core:ai` also needs `kotlinx-coroutines-play-services` for `.await()`.
2. **The exact shape of the non-streaming response.** The streaming sample reads
   `chunk.candidates[0].text`; confirm whether `generateContent` returns the same type or a plain `String`.

Resolve both by looking at the dependency's own sources in the Gradle cache after Step 1, not by guessing.

### Step 1 — `:core:ai`, the Gemini Nano client

Commit: `feat(ai): add the :core:ai module wrapping ML Kit's Gemini Nano client`

- `gradle/libs.versions.toml`: `mlkitGenaiPrompt = "1.0.0-beta4"` and
  `mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenaiPrompt" }`.
- `settings.gradle.kts`: `include(":core:ai")`.
- `core/ai/build.gradle.kts`: copy `core/data/build.gradle.kts` (namespace
  `com.example.gameswishlist.core.ai`, `compileSdk = 37`, `minSdk = 29`, Java 11, Hilt + KSP). Depends on
  nothing but `libs.mlkit.genai.prompt`, Hilt and coroutines — **not** on `:core:model` or `:core:domain`.
- `core/ai/src/main/java/.../core/ai/GeminiNanoClient.kt` — the module's only class:
  - `@Singleton class GeminiNanoClient @Inject constructor()`.
  - Holds one lazily-created client (`Generation.getClient()`); it lives for the process, so nothing
    closes it.
  - `suspend fun isModelReady(): Boolean` — `true` only for `FeatureStatus.AVAILABLE`. Everything else,
    exception included, is `false`. A KDoc line must say why `DOWNLOADABLE` is `false`, or someone will
    "fix" it later.
  - `suspend fun generate(prompt: String): String?` — returns the model's text, or `null` on any failure.
    It rethrows `CancellationException` before swallowing anything, same rule as
    `RepositoryErrorMapper` in `:core:data`.
- Root `CLAUDE.md`: the module graph goes from 15 modules to 16 — update the count, the diagram and the
  `:core:{...}` list. Add one line to the dependency rules: `:core:ai` is reachable from `:core:data`
  only, and feature modules must never see it.

Verify: `./gradlew :core:ai:compileDebugKotlin --console=plain -q`. The module has no consumer yet; that
is expected at this step.

### Step 2 — the translation cache

Commit: `feat(database): cache translated game descriptions`

- `core/database/.../entity/TranslatedDescriptionEntity.kt`:
  ```kotlin
  @Entity(tableName = "translated_descriptions", primaryKeys = ["gameId", "languageTag"])
  data class TranslatedDescriptionEntity(
      val gameId: Int,
      val languageTag: String,
      val sourceHash: Int,
      val translatedText: String
  )
  ```
  A child table of `games`, like `GameArtworkEntity` — never a cross-ref. `sourceHash` is
  `description.hashCode()`: it is a staleness check, not a security boundary, so a cheap hash is enough.
  KDoc must explain that it exists because IGDB edits summaries in place.
- `core/database/.../dao/TranslationDao.kt` — its own DAO rather than more methods on the already large
  `GameDao`, following `PlatformDao`/`SearchHistoryDao`:
  - `suspend fun getTranslation(gameId: Int, languageTag: String): TranslatedDescriptionEntity?`
  - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTranslation(entity: ...)`
- Register the entity and `abstract fun translationDao(): TranslationDao` in `GamesWishlistDatabase`.
  **Do not bump `version`** — `core/database/CLAUDE.md` is explicit, the app is unpublished and
  `.fallbackToDestructiveMigration(true)` handles it.
- Regenerate `core/database/schemas/1.json` (a build of the module does it) and commit it in this same
  commit.
- `core/database/CLAUDE.md`: add `TranslatedDescriptionEntity`("translated_descriptions") to the entity
  naming list.

`saveGame` is deliberately **not** touched: translations are derived data with their own staleness rule,
not part of the IGDB payload that `saveGame` deletes and re-inserts.

Verify: `./gradlew :core:database:compileDebugKotlin --console=plain -q`.

### Step 3 — the preference, and DataStore's first appearance

Commit: `feat(data): store the description-translation preference in DataStore`

- `core/data/build.gradle.kts`: `implementation(libs.androidx.datastore.preferences)`.
- `core/data/src/main/java/.../core/data/local/UserPreferencesDataSource.kt` — next to the existing
  `WishlistCoverImageStorage.kt`:
  - A file-private `private val Context.userPreferences by preferencesDataStore(name = "user_preferences")`.
  - `@Singleton class UserPreferencesDataSource @Inject constructor(@ApplicationContext context)`.
  - `val isDescriptionTranslationEnabled: Flow<Boolean>` — reading DataStore genuinely throws
    `IOException` on a corrupt file, so `.catch { emptyPreferences() }` before `.map { it[KEY] ?: false }`
    is a real boundary, not defensive noise. **Default `false`.**
  - `suspend fun setDescriptionTranslationEnabled(enabled: Boolean)`.
  - No Hilt module: the `@ApplicationContext` constructor injection is enough.
- `core/domain/.../repository/GameRepository.kt` — two methods, on the existing repository. `:core:data`'s
  CLAUDE.md forbids a second repository without discussion, and `getOwnedPlatformIds`/`setOwnedPlatforms`
  are the precedent for a preference living here:
  - `fun isDescriptionTranslationEnabled(): Flow<Boolean>`
  - `suspend fun setDescriptionTranslationEnabled(enabled: Boolean)`
  Both return bare `Flow`/`Unit`, not `AppResult` — nothing here touches the network.
- `GameRepositoryImpl`: inject `UserPreferencesDataSource`, delegate both methods to it.
- Two use cases in a new `core/domain/usecase/translation/` package:
  `ObserveDescriptionTranslationEnabledUseCase`, `SetDescriptionTranslationEnabledUseCase`. Plain classes
  with `operator fun invoke(...)`, as everywhere else.

Verify: `./gradlew :core:data:compileDebugKotlin --console=plain -q`.

### Step 4 — the port, the implementation, the prompt

Commit: `feat(domain): translate game descriptions on device with Gemini Nano`

- `core/domain/.../translation/GameDescriptionTranslator.kt` — the port, and the only interface this
  feature introduces:
  ```kotlin
  interface GameDescriptionTranslator {
      suspend fun isSupported(): Boolean
      suspend fun translate(gameId: Int, description: String): String?
  }
  ```
  - `isSupported()` answers one question — "can this device translate descriptions for this user?" — and
    is `true` only when Gemini Nano is `AVAILABLE` **and** the device language is not English. Both
    Settings and Game Detail gate on this single call.
  - `translate` returns `String?`, **not** `AppResult`. The UI never surfaces a failure — `null` means
    "show the original", which is the whole error contract. Inventing a `RepositoryError` case nobody
    reads would be worse. Put that reasoning in the KDoc.
  - The target language is resolved by the implementation, so `:core:domain` stays free of `Locale`.
- `core/data/.../translation/GameDescriptionTranslatorImpl.kt`, bound `@Binds @Singleton` in the existing
  `di/DataModule.kt`. Constructor: `GeminiNanoClient`, `TranslationDao`. Order of work in `translate`:
  1. Bail out (`null`) if the description is blank or longer than `MAX_TRANSLATABLE_CHARS` (8000 — well
     inside the ~4000-token ceiling, and no IGDB summary comes close).
  2. Read the cache for `gameId` + language tag; return the hit when its `sourceHash` still matches
     `description.hashCode()`.
  3. Otherwise prompt Gemini Nano, and on a non-blank result persist it and return it.
  - The prompt instructs the model to translate from English into the device language, to leave game
    titles, character names, studios and platform names untranslated, to preserve paragraph structure,
    to add nothing, and to reply with the translation alone. Build the language name with
    `Locale.getDefault().getDisplayLanguage(Locale.ENGLISH)` so the prompt itself stays in English.
  - **Watch for a preamble** ("Here is the translation:") during manual testing on a real device. Do not
    pre-emptively write a stripper for it — fix it in the prompt first, and only add post-processing if
    the prompt cannot.
- `core/domain/usecase/translation/TranslateGameDescriptionUseCase.kt` and
  `IsDescriptionTranslationSupportedUseCase.kt`, both delegating to the port.
- Tests, `core/data/src/test/.../GameDescriptionTranslatorImplTest.kt`, MockK over `GeminiNanoClient` and
  `TranslationDao`: cache hit returns without prompting; stale `sourceHash` re-prompts; a successful
  translation is persisted; a `null` from the client yields `null` and writes nothing; a blank or
  oversized description never reaches the client.
- Root `CLAUDE.md`, "Where things live": a row for `GameDescriptionTranslator` → `core/domain/translation/`.

Verify: `./gradlew :core:data:testDebugUnitTest --console=plain -q`.

### Step 5 — the Settings toggle

Commit: `feat(settings): toggle on-device description translation`

- `feature/settings/components/SettingsRow.kt`: add `checked: Boolean? = null`. When it is non-null the
  row renders a `Switch` in place of the chevron and `onClick` flips it. This follows the file's existing
  shape — `trailingText` and `onClick` are already optional trailing modes — rather than adding a second
  row component. If the `when` over three trailing modes turns ugly, split it then, not now.
- `feature/settings/model/SettingsUiEvent.kt` — **new**: Settings currently has no events at all, so this
  introduces the `sealed interface` + single `onEvent` shape the other features use.
  One case: `data class SetDescriptionTranslation(val enabled: Boolean)`.
- `SettingsUiState`: `isTranslationSupported: Boolean = false` and `isTranslationEnabled: Boolean = false`.
  Starting at `false` means the row appears a beat after the screen does, once the async support check
  lands — acceptable, and better than showing a row that then vanishes.
- `SettingsViewModel`: inject the three use cases, `combine` the enabled flow into the existing
  `getSelectedPlatformsUseCase` pipeline, run `IsDescriptionTranslationSupportedUseCase` once in `init`,
  and add `internal fun onEvent(event: SettingsUiEvent)`.
- `SettingsScreen`: thread `onEvent` through to `SettingsContent`, and render the row inside the existing
  `settings_group_app` group, above "About" — it configures the app, not the game profile. Guard it with
  `if (state.isTranslationSupported)`. Add a preview with the row visible and keep one without it.
- `feature/settings/res/values/strings.xml`: `settings_translate_descriptions` and a subtitle explaining
  that it runs on-device. English, as every other string in this repo.
- `feature/settings/src/test/.../SettingsViewModelTest.kt` — **new file**, this ViewModel has no tests
  today: row hidden when unsupported, toggle reflects the stored value, the event writes through.

Verify: `./gradlew :feature:settings:compileDebugKotlin :feature:settings:testDebugUnitTest --console=plain -q`.

### Step 6 — the translated description on screen

Commit: `feat(game-detail): show the translated description`

- `feature/game-detail/model/DescriptionTranslationState.kt` — a `@Immutable internal sealed interface`
  with its implementations in the same file, as the root `CLAUDE.md` exception allows:
  `Off` (translation disabled, unsupported, or failed — show the original), `InProgress`,
  `Ready(val text: String)`. `String`, not `UiText`: the text can only ever come from the data source,
  which is the test the root `CLAUDE.md` sets.
- `GameDetailUiState`: add `descriptionTranslation: DescriptionTranslationState = Off`, as a sibling of
  `contentState`. It is an independent async area with its own lifecycle, so it does not belong inside
  `GameDetailUiModel` — the mapper would have to rebuild it on every unrelated edit.
- `GameDetailViewModel`:
  - Inject the three translation use cases; add a `MutableStateFlow<DescriptionTranslationState>` and
    fold it into the existing `combine` (a fourth flow — still within `combine`'s typed overloads).
  - Drive it from a collector on `currentGameFlow` with
    `.distinctUntilChangedBy { it?.description }`. This is the part that is easy to get wrong:
    `currentGameFlow` re-emits on every notes, status and priority edit, and without that operator every
    keystroke in the notes field would re-run inference.
  - For each new non-blank description: if the toggle is off or the device is unsupported, emit `Off`;
    otherwise emit `InProgress`, call `TranslateGameDescriptionUseCase`, then emit `Ready` or, on `null`,
    `Off`.
- `GameDescriptionCard`: take `translation: DescriptionTranslationState` alongside the existing
  `description: String`. Render the loading placeholder for `InProgress`, the translated text plus a small
  "translated on-device" label for `Ready`, and today's behaviour for `Off`. The expand/collapse state and
  the overflow detection must keep working across a text swap — `hasOverflow` is `rememberSaveable` and
  is recomputed by `onTextLayout`, so check it on a long description that becomes short once translated.
  Look in `core/ui/component/` before writing any new placeholder: `LoadingPage` is full-screen and wrong
  here, so a shimmer or a few `Text` skeleton lines inside the card is likelier to be right.
- Update the call in `GameDetailInfoSection.kt:45` and give the card a `@Preview` per state.
- `feature/game-detail/res/values/strings.xml`: the "translated on-device" label. Disclosing that text is
  machine-translated is not optional — the user is reading a description that is not what IGDB wrote.
- `GameDetailViewModelTest`: toggle off → never calls the translator; on and supported → `InProgress` then
  `Ready`; translator returns `null` → `Off`; a notes edit does not re-trigger inference.

Verify: `./gradlew :app:assembleDebug` — this step spans modules and touches DI wiring.

### Step 7 — verification on hardware, before calling it done

Not a commit. Everything above can compile and pass while the feature does nothing useful, because the
only thing that proves it works is a device where AICore actually has Gemini Nano: a Pixel 8 or newer, a
Galaxy S24 or newer, set to a non-English language. Check that the row appears only there, that the
translation is genuinely better than a phrase-based one on a description full of game jargon, that the
second open of the same game is instant from cache, and that an emulator without AICore simply shows no
row and no English text replaced. **If no such device is available, say so rather than reporting the
feature as working.**

## Decisions that would be expensive to reverse

Per the KMP section in the root `CLAUDE.md`:

- **Use `kotlinx-datetime`, not `java.time`.** A timeline feature spreads date math everywhere; rewriting
  it after a KMP move is exactly the work the project is trying to avoid.
- **WorkManager is Android-only.** Put the scheduling contract in `:core:domain` (e.g.
  `ReleaseRefreshScheduler`) with the implementation in `:core:data`, so a KMP move replaces only the impl.
- Room's destructive migration is deliberate while the app is unpublished — new entities for these phases
  wipe the device, and that is fine. See `docs/tech-debt.md`.
