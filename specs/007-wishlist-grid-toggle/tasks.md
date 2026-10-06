---

description: "Task list for the Wishlist Grid View Toggle (007)"
---

# Tasks: Wishlist Grid View Toggle

**Input**: Design documents from `specs/007-wishlist-grid-toggle/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Included. The constitution (Principle V) requires a test for new ViewModel, mapper and
use-case logic, and plan.md lists them. Write each test before the code it covers; it must fail (or not
compile) first. JUnit4 + MockK + `kotlinx-coroutines-test`, `StandardTestDispatcher`,
`Dispatchers.setMain`/`resetMain`, `advanceUntilIdle()`. There is no CI, so "run" always means a local
Gradle command.

**Organization**: grouped by user story from the spec. Foundational work that several stories need is
in Phase 2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1–US5, matching the user stories in spec.md
- Commands use the Unix wrapper. On Windows use `.\gradlew.bat`.

## Path shorthand (all under the repo root)

| Alias | Directory |
|-------|-----------|
| `MODEL` | `core/model/src/main/java/com/nikolasguillen/questlog/core/model` |
| `DOMAIN` | `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain` |
| `DATA` | `core/data/src/main/java/com/nikolasguillen/questlog/core/data` |
| `DATA_TEST` | `core/data/src/test/java/com/nikolasguillen/questlog/core/data` |
| `COREUI` | `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui` |
| `SEARCH` | `feature/search/src/main/java/com/nikolasguillen/questlog/feature/search` |
| `WL` | `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist` |
| `WL_TEST` | `feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist` |

Rules that apply to every task: one `data class` / `class` / `enum` per file (a sealed hierarchy keeps its
direct implementations in its own file); `internal` by default inside a feature module; comments and KDoc
in English; spacing from `MaterialTheme.spacing`, never a new `dp` literal; user-visible text through
`strings.xml` / `UiText`; the wishlist module's own `R` imported bare, `:core:ui`'s as `CoreUiR`; every new
composable gets a `private` `@QuestLogPreviews` preview wrapped in `QuestLogTheme { }`.

---

## Phase 1: Setup

- [ ] T001 Confirm a green baseline before touching anything: run
  `./gradlew :core:data:testDebugUnitTest :feature:wishlist:testDebugUnitTest :feature:search:testDebugUnitTest --console=plain -q`
  on branch `007-wishlist-grid-toggle`. If anything already fails, stop and report it; do not fix it as part of this feature.

---

## Phase 2: Foundational (blocks every user story)

**Purpose**: the persisted view-mode preference (the toggle is built on it), the `VerticalGameCard`
contract change (the grid is built on it), and the strings every story uses.

- [ ] T002 [P] Create `MODEL/WishlistViewMode.kt`: `enum class WishlistViewMode(val id: Int)` with entries
  `LIST(0)` and `GRID(1)`, and `companion object { fun fromId(id: Int): WishlistViewMode }` that returns
  `LIST` for any unknown id (never throws). KDoc: the `id` is what is persisted, never the enum name; ids
  are stable, never renumber, only append. Model it on `MODEL/AppearanceMode.kt`. No Android or Compose import.
- [ ] T003 Create `DOMAIN/settings/WishlistViewModePreferenceStore.kt` (depends on T002): interface with
  `fun observeWishlistViewMode(): Flow<WishlistViewMode>` (emits `LIST` when nothing is persisted) and
  `suspend fun setWishlistViewMode(mode: WishlistViewMode)`. KDoc says `:core:domain` never imports
  `androidx.datastore`. See `contracts/wishlist-view-mode-store.md`; model it on
  `DOMAIN/settings/AppearancePreferenceStore.kt`.
- [ ] T004 [P] Create `DOMAIN/usecase/list/GetWishlistViewModeUseCase.kt` (depends on T003): plain class,
  `@Inject constructor(store: WishlistViewModePreferenceStore)`, `operator fun invoke(): Flow<WishlistViewMode>`
  delegating to `observeWishlistViewMode()`. No base type.
- [ ] T005 [P] Create `DOMAIN/usecase/list/SetWishlistViewModeUseCase.kt` (depends on T003): plain class,
  `@Inject constructor(store: ...)`, `suspend operator fun invoke(mode: WishlistViewMode)` delegating to
  `setWishlistViewMode(mode)`.
- [ ] T006 Create `DATA/settings/WishlistViewModePreferenceStoreImpl.kt` (depends on T003):
  `@Inject constructor(private val settingsDataStore: DataStore<Preferences>)` (the existing singleton
  `"settings"` store, no second DataStore file). Key: `intPreferencesKey("wishlist_view_mode")`. Read through
  `WishlistViewMode.fromId(prefs[KEY] ?: WishlistViewMode.LIST.id)`; write `mode.id`. Local data: bare `Flow` /
  `Unit`, never `AppResult`. Model it on `DATA/settings/AppearancePreferenceStoreImpl.kt`.
- [ ] T007 Edit `DATA/di/DataModule.kt` (depends on T006): add
  `@Binds @Singleton abstract fun bindWishlistViewModePreferenceStore(impl: WishlistViewModePreferenceStoreImpl): WishlistViewModePreferenceStore`
  next to `bindAppearancePreferenceStore`.
- [ ] T008 [P] Edit `COREUI/component/gamecard/VerticalGameCard.kt`. (a) Change `onSaveClick` to
  `(() -> Unit)? = null`; when it is `null`, do not draw `SaveToWishlistButton` at all. (b) Add a **required**
  `onLongClickLabel: String` parameter with no default, placed after `onClick` and before the optional
  parameters. Pass it to `GenericGameCardLayout(onLongClickLabel = ...)` and to
  `SaveToWishlistButton(longClickLabel = ...)`; delete the internal
  `stringResource(R.string.choose_list_content_description)` lookup and any import it leaves unused. Update the
  file's previews to pass a label. Do **not** touch `CompactGameCard.kt`, which hardcodes the same label and is
  out of scope.
- [ ] T009 Edit `SEARCH/components/SearchResultGrid.kt` (depends on T008): pass
  `onLongClickLabel = stringResource(CoreUiR.string.choose_list_content_description)` to `VerticalGameCard`,
  adding `import com.nikolasguillen.questlog.core.ui.R as CoreUiR` (the file already imports Search's own bare
  `R`). Search must announce the same long-press action as before. Confirm with
  `grep -rn "VerticalGameCard(" --include='*.kt' feature core app` that no other caller exists.
- [ ] T010 [P] Edit `feature/wishlist/src/main/res/values/strings.xml`: add `filter_all` = "All",
  `filtered_empty_message` = "No games match this filter", `show_as_grid_action` = "Show as grid",
  `show_as_list_action` = "Show as list", `list_view_state` = "List view", `grid_view_state` = "Grid view".
- [ ] T011 Checkpoint: run
  `./gradlew :core:model:compileDebugKotlin :core:domain:compileDebugKotlin :core:data:compileDebugKotlin :core:ui:compileDebugKotlin :feature:search:compileDebugKotlin :feature:wishlist:compileDebugKotlin --console=plain -q`
  and `./gradlew :feature:search:testDebugUnitTest --console=plain -q`. Both must pass.

**Checkpoint**: foundation ready; user stories can start.

---

## Phase 3: User Story 1 — Browse a wishlist as a grid of covers (P1) 🎯 MVP

**Goal**: a toggle on the wishlist detail screen switches between the existing rows and a two-column grid
of `VerticalGameCard`s, grouped under the same status headers. Tapping a card opens the game; long-pressing
it opens the existing remove confirmation.

**Independent Test**: quickstart Q2, Q4, Q5 and Q13. Open a wishlist with several games, switch to grid,
open a game and come back, long-press a card and confirm removal, switch back to list.

- [ ] T012 [US1] Edit `WL/model/WishlistUiState.kt`: add `val viewMode: WishlistViewMode = WishlistViewMode.LIST`.
- [ ] T013 [P] [US1] Edit `WL/model/WishlistUiEvent.kt`: add `data object OnViewModeToggled : WishlistUiEvent`.
- [ ] T014 [US1] Tests first, edit `WL_TEST/WishlistViewModelTest.kt` (depends on T012, T013). Add
  `getWishlistViewModeUseCase = mockk<GetWishlistViewModeUseCase>()` (default
  `every { invoke() } returns flowOf(WishlistViewMode.LIST)`) and
  `setWishlistViewModeUseCase = mockk<SetWishlistViewModeUseCase>(relaxed = true)`; pass both to
  `newViewModel()`, appended after `updateListUseCase` (matching T015's constructor order). Add cases: the
  state's `viewMode` reflects the stored mode (stub `GRID`); `OnViewModeToggled` from `LIST` calls
  `set(GRID)` and from `GRID` calls `set(LIST)` (`coVerify`). Extend the class KDoc. These fail until T015.
- [ ] T015 [US1] Edit `WL/WishlistViewModel.kt` (depends on T012, T013): add `getWishlistViewModeUseCase` and
  `setWishlistViewModeUseCase` constructor parameters after `updateListUseCase`. Keep the existing detail flow
  (`distinctUntilChanged` + `onEach` `NavigateBack`) unchanged, then replace its `.map { }` with
  `combine(detailFlow, getWishlistViewModeUseCase()) { detail, viewMode -> ... }` and put `viewMode` into
  `WishlistUiState` (both the `null` and non-null branches). Keep `.stateIn(viewModelScope,
  SharingStarted.WhileSubscribed(5000), WishlistUiState())`. Handle `OnViewModeToggled` in the exhaustive
  `onEvent`: `viewModelScope.launch { setWishlistViewModeUseCase(if (uiState.value.viewMode == LIST) GRID else LIST) }`.
  (US4 later adds the third `combine` source.)
- [ ] T016 [P] [US1] Create `WL/components/WishlistViewModeToggle.kt`: `internal fun WishlistViewModeToggle(viewMode:
  WishlistViewMode, onClick: () -> Unit, modifier: Modifier = Modifier)`: an `IconButton` showing the icon of
  the layout it switches **to**: `Icons.Outlined.GridView` while in `LIST`,
  `Icons.AutoMirrored.Outlined.ViewList` while in `GRID`. `contentDescription` is `show_as_grid_action` or
  `show_as_list_action` (the action); `Modifier.semantics { stateDescription = ... }` is `list_view_state` or
  `grid_view_state` (the current view). Icon tint matches the top bar's actions
  (`MaterialTheme.colorScheme.onSurface`). Previews for both modes.
- [ ] T017 [P] [US1] Create `WL/components/WishlistViewOptionsRow.kt`: `internal fun WishlistViewOptionsRow(viewMode:
  WishlistViewMode, onToggleClick: () -> Unit, modifier: Modifier = Modifier)`: a `Row` with
  `fillMaxWidth()`, horizontal padding `MaterialTheme.spacing.large`, vertically centred, and the toggle pushed
  to the end (`Arrangement.End` for now; US4 adds the chip strip on the left). Previews.
- [ ] T018 [US1] Rewrite `WL/components/WishlistGamesList.kt` (depends on T016, T017): replace `LazyColumn` with
  `LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState, horizontalArrangement =
  Arrangement.spacedBy(MaterialTheme.spacing.large), modifier = modifier.fillMaxSize())`. Do **not** set a
  `verticalArrangement` (it would add gaps between list-view rows; give grid cards `padding(bottom =
  MaterialTheme.spacing.large)` instead). New signature adds `viewMode: WishlistViewMode`, `gridState:
  LazyGridState` and `viewOptionsRow: @Composable () -> Unit`, and keeps `sections`, `revealedGameId`,
  `onRevealedGameIdChange`, `onGameClick`, `onGameRemoveClick`, `header`. Items (every non-card item uses
  `span = { GridItemSpan(maxLineSpan) }`): `"list_header"`, `"view_options"`, then per section
  `"header_${status}"` (`StatusSectionHeader`, unchanged), the games, `"spacer_${status}"` (unchanged).
  In `LIST`, each game is a full-span `SwipeToRevealRow { WishlistGameRow }` exactly as today (key
  `"game_${status}_${id}"`). In `GRID`, each game is a single-cell `VerticalGameCard(game, onClick =
  { onGameClick(game.id) }, onLongClickLabel = stringResource(R.string.remove_game_action), onLongClick =
  { onGameRemoveClick(game) }, onSaveClick = null)`, using `itemsIndexed` **per section** with the cell padding
  copied from `SEARCH/components/SearchResultGrid.kt`: even index gets `start = spacing.large`, odd index gets
  `end = spacing.large`, plus `animateItem(fadeOutSpec = null)`. Use distinct `contentType`s for
  header, options row, list game, grid game and spacer. Update the existing preview to pass the new
  parameters and add a `GRID` preview.
- [ ] T019 [US1] Edit `WL/WishlistScreen.kt` (depends on T015, T018): in `WishlistContent` add
  `val gridState = rememberLazyGridState()` (hoisted so spec 008 can observe it later), pass `viewMode =
  state.viewMode`, `gridState`, and `viewOptionsRow = { WishlistViewOptionsRow(viewMode = state.viewMode,
  onToggleClick = { onEvent(WishlistUiEvent.OnViewModeToggled) }) }` to `WishlistGamesList`. Add
  `LaunchedEffect(state.viewMode) { revealedGameId = null }` so a revealed swipe row is dismissed on a mode
  change (nothing is removed). Leave the `Loading` and `Empty` branches untouched. Add `viewMode` to the
  existing preview state and add a grid-mode `Success` preview.
- [ ] T020 [US1] Verify: `./gradlew :feature:wishlist:compileDebugKotlin :feature:wishlist:testDebugUnitTest
  --console=plain -q` (including the T014 cases), then run quickstart Q2, Q4, Q5, Q13 and the Search regression
  Q16 on a device or emulator. Fix anything that fails before moving on.

**Checkpoint**: grid view works end to end. This is a shippable MVP (the filter chips, the persistence test
and polish follow).

---

## Phase 4: User Story 2 — The toggle only appears when there is something to lay out (P1)

**Goal**: no toggle while loading or when the list is empty; it appears with the first game and disappears
with the last, without losing the stored view choice.

**Independent Test**: quickstart Q11 and Q12.

- [ ] T021 [US2] Tests, edit `WL_TEST/WishlistViewModelTest.kt`: add cases that (a) before the detail loads,
  `contentState` is `Loading` and `viewMode` is `LIST`; (b) a detail with no games yields
  `WishlistContentState.Empty`; (c) with the stored mode stubbed as `GRID`, a list that goes from one game to
  none emits `Success` then `Empty` while `viewMode` stays `GRID` (FR-012: the stored choice is not reset). Use
  a `MutableSharedFlow<WishlistDetail?>` for the detail like the existing "list that vanishes" test.
- [ ] T022 [US2] Review `WL/WishlistScreen.kt`: confirm `WishlistViewOptionsRow` is only reachable through the
  `Success` branch's `WishlistGamesList` (never in `Loading` or `Empty`). Fix it if T019 put it elsewhere.
  No new code is expected here.
- [ ] T023 [US2] Verify: run `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q`, then quickstart
  Q11 and Q12.

**Checkpoint**: US1 and US2 both hold.

---

## Phase 5: User Story 3 — Games stay grouped by status in grid view (P2)

**Goal**: in grid view each status keeps its header and count; no orphan headers, and odd-sized sections
look right. The rendering was built in T018; this phase pins down the grouping rules and checks the odd cases.

**Independent Test**: the new mapper test, plus quickstart Q2 and Q3.

- [ ] T024 [US3] Create `WL_TEST/mapper/WishlistUiMapperTest.kt` (package
  `com.nikolasguillen.questlog.feature.wishlist.mapper`; KDoc header describing what it covers). Cover
  `List<Game>.toWishlistSectionUiModel()`: sections come in the order PLAYING, BOUGHT, WANT_TO_BUY, COMPLETED,
  DROPPED, then "No status" last; a status with no games produces no section; games of one status produce
  exactly one section with no empty companions; each section's `games.size` matches; a list whose games all
  share one status produces a single section. Build `Game` instances the way `WishlistViewModelTest` does.
- [ ] T025 [US3] Edit `WL/components/WishlistGamesList.kt` (after T018): add a `GRID` preview with one section of
  three games (odd count) followed by a section of one game, so the lone-card and
  header-on-a-new-line cases are visible in the preview.
- [ ] T026 [US3] Verify: `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q`, then quickstart Q2
  and Q3 on a list with an odd-sized status.

**Checkpoint**: US1–US3 hold.

---

## Phase 6: User Story 4 — Filter the wishlist by status (P2)

**Goal**: status chips on the left of the toggle row narrow the list to one status in either view. A filter
whose status has run out of games stays on and shows a centered "no games match" message.

**Independent Test**: quickstart Q6, Q7, Q8, Q8b, Q8c, Q9, Q14, Q15.

- [ ] T027 [P] [US4] Create `WL/model/WishlistStatusFilter.kt`: `@Immutable internal sealed interface
  WishlistStatusFilter` with `data object All` and `data class Only(val status: GameStatus?)` in the same file
  (sealed-hierarchy exception). KDoc: `Only(null)` is the "No status" section, which is why a nullable status
  alone could not also mean "All".
- [ ] T028 [P] [US4] Create `WL/model/WishlistFilterChipUiModel.kt`: `@Immutable internal data class
  WishlistFilterChipUiModel(val filter: WishlistStatusFilter, val label: UiText, val isSelected: Boolean)`.
- [ ] T029 [P] [US4] Edit `WL/model/WishlistContentState.kt`: add `data object FilteredEmpty :
  WishlistContentState`. Update the KDoc: `Empty` means the **list** has no games; `FilteredEmpty` means it has
  games but none match the active filter; `Success.sections` is the filtered, non-empty list.
- [ ] T030 [US4] Edit `WL/model/WishlistUiState.kt` (after T012): add `val filterChips:
  List<WishlistFilterChipUiModel> = emptyList()`.
- [ ] T031 [P] [US4] Edit `WL/model/WishlistUiEvent.kt` (after T013): add `data class
  OnStatusFilterSelected(val filter: WishlistStatusFilter) : WishlistUiEvent`.
- [ ] T032 [US4] Tests first, edit `WL_TEST/mapper/WishlistUiMapperTest.kt` (after T024). Against the signatures
  in T034: **chips**: no chips for 0 sections under **any** filter (`All` and `Only(s)`: the whole row is
  hidden for an empty list); no chips for 1 section under `All`; with two or more sections the list is `All` +
  one chip per section in section order, labelled with the section's `label`, exactly one selected (`All` by
  default); under `Only(PLAYING)` with a single remaining section the chips are still returned; a filtered
  status with no games keeps its chip, selected, in its correct ordered slot with the right label (including
  `Only(null)` → "No status", last). **filteredBy**: `All` returns every section; `Only(s)` returns only the
  section whose `status == s`; `Only(null)` returns only the "No status" section; `Only(s)` with no matching
  section returns an empty list. Compare enum values, never labels.
- [ ] T033 [US4] Edit `WL/mapper/WishlistUiMapper.kt` (depends on T027, T028; makes T032 pass): (a) extract the
  status-to-label logic out of `toWishlistSectionUiModel()` into `internal fun GameStatus?.toSectionLabel():
  UiText` (`status.toLabelUiText()`, or `UiText.StringResource(R.string.no_status_section_label)` for `null`) and
  make the section mapper use it; (b) add `internal fun List<WishlistSectionUiModel>.filteredBy(filter:
  WishlistStatusFilter): List<WishlistSectionUiModel>`; (c) add `internal fun
  List<WishlistSectionUiModel>.toFilterChips(filter: WishlistStatusFilter): List<WishlistFilterChipUiModel>`:
  returns `emptyList()` when the receiver is empty (whatever the filter) or when `size < 2 && filter == All`;
  otherwise `All` followed by one chip for each status
  in {statuses of the sections} ∪ {the filter's status, if `Only`}, ordered by `STATUS_ORDER` index with `null`
  last, labelled via `toSectionLabel()`, `isSelected = (chip.filter == filter)`. The `All` chip's label is
  `UiText.StringResource(R.string.filter_all)`.
- [ ] T034 [US4] Tests first, edit `WL_TEST/WishlistViewModelTest.kt` (after T014, T021). Stub the detail flow
  with games in at least three statuses and add cases: `OnStatusFilterSelected(Only(PLAYING))` narrows
  `contentState` to one section and marks that chip selected; the filter survives a mode toggle; the header's
  `gameCountText` still counts all games under a filter (FR-017); when the filtered status loses its last game
  while others remain, `contentState` becomes `FilteredEmpty`, the filter and its chip stay selected, and
  `Success` returns when a game regains that status; when the whole list becomes empty, `contentState` is
  `Empty`, `filterChips` is empty and the filter is back at `All` (a later detail with games shows `All` selected);
  selecting the already-selected chip changes nothing. Drive changes with a `MutableSharedFlow<WishlistDetail?>`.
- [ ] T035 [US4] Edit `WL/WishlistViewModel.kt` (depends on T029–T033; makes T034 pass): add
  `private val selectedFilter = MutableStateFlow<WishlistStatusFilter>(WishlistStatusFilter.All)` and extend
  the `combine` from T015 to three sources `(detailFlow, getWishlistViewModeUseCase(), selectedFilter)`. In the
  mapper step: `allSections = detail.games.toWishlistSectionUiModel()`, `visible = allSections.filteredBy(filter)`,
  `filterChips = allSections.toFilterChips(filter)`, and `contentState = when { allSections.isEmpty() -> Empty;
  visible.isEmpty() -> FilteredEmpty; else -> Success(visible) }`. In the detail flow's `onEach`, when
  `detail != null && detail.games.isEmpty()` set `selectedFilter.value = All` (the only automatic reset; the
  `NavigateBack`-on-null logic stays as is and runs before the combine). Handle `OnStatusFilterSelected` in the
  exhaustive `onEvent` by setting `selectedFilter.value = event.filter`.
- [ ] T036 [US4] Edit `WL/components/WishlistViewOptionsRow.kt` (after T017): add `chips:
  List<WishlistFilterChipUiModel>` and `onChipClick: (WishlistStatusFilter) -> Unit`. Layout: a `LazyRow` with
  `Modifier.weight(1f)` and `horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)` holding
  one `CustomFilterChip(label = chip.label.asString(), selected = chip.isSelected, onFilterClick = {
  onChipClick(chip.filter) })` per chip (stable key from the filter), then the toggle at the end. When `chips` is
  empty, render an empty weighted spacer instead so the toggle stays at the end and is never pushed off-screen.
  Update the previews (several chips, one selected; no chips).
- [ ] T037 [US4] Edit `WL/WishlistScreen.kt` (depends on T035, T036): pass `chips = state.filterChips` and
  `onChipClick = { onEvent(WishlistUiEvent.OnStatusFilterSelected(it)) }` to the row. Add the
  `WishlistContentState.FilteredEmpty` branch (keeps `when` exhaustive), mirroring the `Empty` branch: `Column
  (modifier = contentModifier) { header(); the same WishlistViewOptionsRow; EmptyPage(message =
  stringResource(R.string.filtered_empty_message), icon = Icons.Outlined.FilterAltOff, modifier =
  Modifier.weight(1f)) }`. To avoid building the row twice, extract it into a local `val viewOptionsRow:
  @Composable () -> Unit` next to `header`. Add a `FilteredEmpty` preview and a `Success` preview with chips.
- [ ] T038 [US4] Verify: `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q`, then quickstart Q6,
  Q7, Q8, Q8b, Q8c, Q9, Q14 and Q15 (TalkBack on chips and toggle).

**Checkpoint**: US1–US4 hold.

---

## Phase 7: User Story 5 — The chosen view is remembered (P3)

**Goal**: the list/grid choice survives leaving the screen, opening other wishlists, rotation and app
relaunch. The store and its wiring were built in Phase 2 because the toggle depends on them; this phase
proves the persistence guarantees.

**Independent Test**: the store test, plus quickstart Q10 and Q1.

- [ ] T039 [P] [US5] Create `DATA_TEST/settings/WishlistViewModePreferenceStoreImplTest.kt`, mirroring
  `DATA_TEST/settings/AppearancePreferenceStoreImplTest.kt` (temp-file-backed `DataStore<Preferences>`, no
  Robolectric, KDoc header). Cases: a fresh store emits `LIST`; `setWishlistViewMode(GRID)` makes the next
  emission `GRID`; a second store instance over the same file reads back `GRID` (the relaunch case); an unknown
  stored id (write `99` under `intPreferencesKey("wishlist_view_mode")` directly) reads as `LIST`. Run with
  `./gradlew :core:data:testDebugUnitTest --console=plain -q`.
- [ ] T040 [US5] Verify on a device: quickstart Q10 (open another wishlist, force-stop and relaunch, rotate) and
  Q1 (fresh install or cleared data opens in list view).

**Checkpoint**: all five stories hold.

---

## Phase 8: Polish & cross-cutting

- [ ] T041 [P] Edit `core/ui/CLAUDE.md`: in the `component/gamecard/` paragraph, document `VerticalGameCard`'s
  required `onLongClickLabel` (the caller states what a long-press does; there is no default) and that a `null`
  `onSaveClick` hides the heart (used by the wishlist grid, where a heart would be misleading). Write it as an
  instruction, not a changelog.
- [ ] T042 Review the whole diff against the constitution and the module `CLAUDE.md` files: one type per file;
  `internal` visibility in `feature/wishlist`; no `dp` literal that has a `MaterialTheme.spacing` token; no
  hardcoded display text; `collectAsStateWithLifecycle()` untouched; no unused imports; KDoc in English; no new
  module dependency (`git diff -- '*.gradle.kts'` must be empty).
- [ ] T043 Run `./gradlew :app:assembleDebug --console=plain -q` (the change spans modules and adds a Hilt
  binding).
- [ ] T044 Run `./gradlew test --console=plain -q`. Everything must pass, including the pre-existing suites.
- [ ] T045 Walk the whole [quickstart.md](quickstart.md) once more on a device, Q1–Q17, including Q16 (Search
  unchanged) and Q17 (300+ games in grid scrolls without visible stutter).

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (T001)** → no dependencies.
- **Phase 2 (T002–T011)** → after T001; **blocks all stories**.
- **US1 (T012–T020)** → after Phase 2. It is the MVP.
- **US2 (T021–T023)** → after US1 (it asserts behaviour of the toggle row and ViewModel built there).
- **US3 (T024–T026)** → T024 can start any time after Phase 2; T025 needs T018.
- **US4 (T027–T038)** → after US1 (extends the ViewModel, the row, the screen and the state).
- **US5 (T039–T040)** → T039 only needs T006, so it can run alongside US1; T040 needs the finished app.
- **Polish (T041–T045)** → after the stories you want to ship. T041 only needs T008.

### Within-phase ordering

- Phase 2: T002 → T003 → {T004, T005, T006} → T007; T008 → T009; T010 independent; T011 last.
- US1: {T012, T013} → T014 (tests) → T015; {T016, T017} → T018 → T019 → T020.
- US4: {T027, T028, T029} → {T030, T031}; T032 (tests) → T033; T034 (tests) → T035; T036 → T037 → T038.

### Files edited by more than one story (do not parallelise across these)

`WL/WishlistViewModel.kt` (T015, T035), `WL/WishlistScreen.kt` (T019, T022, T037),
`WL/components/WishlistGamesList.kt` (T018, T025), `WL/components/WishlistViewOptionsRow.kt` (T017, T036),
`WL/model/WishlistUiState.kt` (T012, T030), `WL/model/WishlistUiEvent.kt` (T013, T031),
`WL_TEST/WishlistViewModelTest.kt` (T014, T021, T034), `WL_TEST/mapper/WishlistUiMapperTest.kt` (T024, T032).

---

## Parallel Opportunities

```text
# Phase 2, after T001:
T002 (model enum)          T008 (VerticalGameCard)          T010 (strings)
   └─▶ T003 ─▶ T004 ∥ T005 ∥ T006 ─▶ T007           T008 ─▶ T009

# US1, once Phase 2 is done:
T012 ∥ T013  ─▶  T014 (tests) ─▶ T015            T016 ∥ T017 ─▶ T018 ─▶ T019 ─▶ T020

# US4 models, then the two test-first chains:
T027 ∥ T028 ∥ T029 ─▶ T030 ∥ T031      T032 ─▶ T033      T034 ─▶ T035

# Alongside US1 (different module, only needs T006):
T039 (store test)
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 and Phase 2, ending with the T011 compile checkpoint.
2. US1 (T012–T020). **Stop and validate**: toggle between list and grid, open a game, long-press to remove.
3. This already ships the grid. Demo it before continuing.

### Incremental delivery

1. MVP (US1) → then US2 (toggle only with games) → US3 (grouping pinned down) → US4 (filter chips) → US5 (persistence proof).
2. Each story ends with a verify task. Do not start the next story on a red build.
3. Commit after each story (or each logical group). Commit messages follow `type(scope): subject`,
   for example `feat(wishlist): add grid view toggle`, `feat(data): persist the wishlist view mode`,
   `refactor(ui): require a long-press label on VerticalGameCard`.

### Notes

- Spec 008 (back-to-top) depends on the single `LazyGridState` hoisted in T019. Do not move it into the list
  component.
- `CompactGameCard` still hardcodes the "choose list" long-press label. It is out of scope for this feature;
  mention it to the owner rather than fixing it here.
- If a task's instructions and the source disagree, the source wins: re-read the file and adapt, then note it.
