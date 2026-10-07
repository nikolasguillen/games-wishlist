---

description: "Task list for the welcome onboarding feature"
---

# Tasks: Welcome Onboarding

**Input**: Design documents from `/specs/009-welcome-onboarding/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-and-domain-contracts.md, quickstart.md

**Tests**: Included. The constitution (Principle V) requires new ViewModel, mapper, use-case and store logic to
get a test in its own module's `src/test` (JUnit4 + MockK + `kotlinx-coroutines-test`, mocking use cases, with
a `StandardTestDispatcher` and `Dispatchers.setMain`/`resetMain`). Test tasks sit before the code they cover.

**Organization**: Tasks are grouped by user story. US1 and US2 are both P1 and ship together as the MVP.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1–US4, mapping to the user stories in spec.md

## Path Conventions

Android multi-module. Sources live in `<module>/src/main/java/com/nikolasguillen/questlog/...`, and the paths below
abbreviate that prefix to `<module>/.../`. `FEATURE` means
`feature/onboarding/src/main/java/com/nikolasguillen/questlog/feature/onboarding`. `FTEST` means
`feature/onboarding/src/test/java/com/nikolasguillen/questlog/feature/onboarding`.

Verification is local: `./gradlew :<module>:compileDebugKotlin --console=plain -q` for a single module,
`./gradlew :app:assembleDebug` when DI or several modules are touched, `./gradlew test` for the suites. On Windows
use `.\gradlew.bat`.

**Commits**: one commit per task group, in `type(scope): subject` form per the root `CLAUDE.md`, ending with the
`Co-Authored-By` trailer that file requires. The scope for this module is `onboarding`, which T003 adds to the hook.

**Documentation and the constitution**: the constitution (Governance) requires an amendment to land in the same commit
as the change that motivates it, and `CLAUDE.md` rules must change with the thing they describe. So documentation edits
are folded into the task that causes them, not collected at the end:

- the module count (17 → 18) and `feature/onboarding` in the `CLAUDE.md` files and tech-debt: T004, in the module commit;
- `feature/onboarding` in Principle V's list of test source sets: T012, with the first ViewModel test;
- `core/ui` in that list, the `core/ui/CLAUDE.md` inventory and the tech-debt coverage entry: T033, T040 and T042, with
  the commits that create them.

The constitution edits form one PATCH amendment. The first one (T004) bumps `Version` to `1.0.2` and `Last Amended` to
`2026-10-07`; the later ones extend the same amendment and keep that version. The owner approved all of it on 2026-10-07.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new module and make it buildable.

- [X] T001 Create `feature/onboarding/build.gradle.kts` by copying `feature/search/build.gradle.kts` and changing only the `namespace` to `com.nikolasguillen.questlog.feature.onboarding`. Keep the dependencies exactly: `:core:common`, `:core:model`, `:core:domain`, `:core:ui`, `:core:navigation`, `:core:designsystem`. Do not add `:core:data`, `:core:network`, `:core:database`, `:core:ai` or `:feature:*`. Also create the empty source directories `feature/onboarding/src/main/java/com/nikolasguillen/questlog/feature/onboarding/` and `feature/onboarding/src/test/java/com/nikolasguillen/questlog/feature/onboarding/`.
- [X] T002 Add `include(":feature:onboarding")` to `settings.gradle.kts` after `include(":feature:settings")`, and `implementation(project(":feature:onboarding"))` to `app/build.gradle.kts` after the `:feature:settings` line.
- [X] T003 [P] Add `onboarding` to the `SCOPES` list in `.githooks/commit-msg` (line 6), so commits scoped `onboarding` are accepted.
- [X] T004 [P] Update the documentation for the new module, in the same commit as T001–T003 (constitution Governance):
  - root `CLAUDE.md`: "17 modules" becomes 18, and `onboarding` joins the `:feature:{…}` list in the module graph and in the sentence naming feature modules;
  - `feature/CLAUDE.md`: add `feature/onboarding` to the "Applies to" list;
  - `docs/tech-debt.md`: "all 17 module build files" becomes 18;
  - `.specify/memory/constitution.md`: Principle I's "17 modules" becomes 18; this opens the PATCH amendment, so set `Version` to `1.0.2` and `Last Amended` to `2026-10-07`.
  Keep the rules style: instructions, not a changelog.

**Checkpoint**: `./gradlew :feature:onboarding:compileDebugKotlin --console=plain -q` succeeds on the empty module, and `git grep -n "17 module"` finds nothing outside `specs/`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The persisted "completed" flag and the route. Every user story needs both.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 [P] Create `core/domain/.../core/domain/settings/OnboardingPreferenceStore.kt`: `interface OnboardingPreferenceStore { fun observeOnboardingCompleted(): Flow<Boolean>; suspend fun setOnboardingCompleted() }`. KDoc: "Contract only: `:core:domain` never imports `androidx.datastore`", emits `false` until first set, setter is idempotent. Mirror `AppearancePreferenceStore.kt`.
- [X] T006 [P] Create `core/domain/.../core/domain/usecase/settings/GetOnboardingCompletedUseCase.kt`: `class GetOnboardingCompletedUseCase @Inject constructor(private val onboardingPreferenceStore: OnboardingPreferenceStore)` with `operator fun invoke(): Flow<Boolean>`. Mirror `GetAppearanceModeUseCase.kt`.
- [X] T007 [P] Create `core/domain/.../core/domain/usecase/settings/CompleteOnboardingUseCase.kt`: same shape, `suspend operator fun invoke()` calling `setOnboardingCompleted()`.
- [X] T008 Create `core/data/.../core/data/settings/OnboardingPreferenceStoreImpl.kt` over the injected `DataStore<Preferences>` with `private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")`. Reading falls back to `false`. Setting writes `true`. Copy the KDoc approach of `AppearancePreferenceStoreImpl.kt` (the `DataStore` is injected so the class is unit-testable with a temp file). Depends on T005.
- [X] T009 Add `@Binds @Singleton abstract fun bindOnboardingPreferenceStore(onboardingPreferenceStoreImpl: OnboardingPreferenceStoreImpl): OnboardingPreferenceStore` to `core/data/.../core/data/di/DataModule.kt`, next to `bindWishlistViewModePreferenceStore`. Depends on T008.
- [X] T010 [P] Create `core/data/src/test/java/com/nikolasguillen/questlog/core/data/settings/OnboardingPreferenceStoreImplTest.kt`, modelled on `AppearancePreferenceStoreImplTest.kt` (temp-file `PreferenceDataStoreFactory`, KDoc header). Cases: reading before any write returns `false`; after `setOnboardingCompleted()` it returns `true`; a fresh store over the same file still returns `true`; calling `setOnboardingCompleted()` twice is harmless. Depends on T008.
- [X] T011 [P] Add `@Serializable data object OnboardingRoute : GameNavKey` to `core/navigation/.../core/navigation/Routes.kt`, after `ReleaseNotificationsRoute`.

**Checkpoint**: `./gradlew :core:data:testDebugUnitTest --console=plain -q` and `:app:compileDebugKotlin` pass.

---

## Phase 3: User Story 1 - Understand the app on first launch (Priority: P1) 🎯 MVP

**Goal**: A fresh install shows a paged tour (Welcome, Search & Discover, Lists, Radar) before anything else, with
a position indicator and back/next. Finishing lands on Search with no way back into the flow. A relaunch opens
straight on Search.

**Independent Test**: `adb shell pm clear com.nikolasguillen.questlog`, launch, page to the end, tap "Get started"
and land on Search. Relaunch and confirm no flow. Quickstart scenarios 1, 2, 8, 11, 13, 14.

### Tests for User Story 1 ⚠️

> Write these first and confirm they fail before implementing T021.

- [X] T012 [US1] Create `FTEST/OnboardingViewModelTest.kt` (KDoc header, `StandardTestDispatcher`, `Dispatchers.setMain`/`resetMain`, `mockk<CompleteOnboardingUseCase>(relaxed = true)`). Cases:
  - the content state starts `Loading`;
  - after `NotificationFactsResolved(requiresRuntimePermission = true, canDeliver = false)` it is `Ready` with exactly `[Welcome, Discover, Lists, Radar]` (the setup pages arrive in US3);
  - a second `NotificationFactsResolved` does not rebuild the list;
  - `FinishClicked` calls `CompleteOnboardingUseCase` once and emits `Finished`;
  - tapping `FinishClicked` twice emits `Finished` only once.
  - In the same commit, add `feature/onboarding` to the list of test source sets in Principle V of `.specify/memory/constitution.md` (the amendment started in T004; keep `Version` 1.0.2).

### Implementation for User Story 1

- [X] T013 [P] [US1] Create `FEATURE/model/OnboardingPage.kt`: `internal sealed interface OnboardingPage` with `data object Welcome`, `Discover`, `Lists`, `Radar`. The sealed file keeps its implementations together. `Platforms` and `Reminders` are added in US3.
- [X] T014 [P] [US1] Create `FEATURE/model/OnboardingInfoPageUiModel.kt`: `@Immutable internal data class OnboardingInfoPageUiModel(val headline: UiText, val body: UiText, val icon: ImageVector?)`. `UiText` is from `core/ui/model/UiText.kt`. A `null` icon means the page shows the controller animation instead (the Welcome page, research R10).
- [X] T015 [P] [US1] Create `FEATURE/model/OnboardingContentState.kt`: `@Immutable internal sealed interface OnboardingContentState` with `data object Loading` and `data class Ready(val pages: List<OnboardingPage>)`.
- [X] T016 [P] [US1] Create `FEATURE/model/OnboardingUiState.kt`: `@Immutable internal data class OnboardingUiState(val contentState: OnboardingContentState = OnboardingContentState.Loading)`.
- [X] T017 [P] [US1] Create `FEATURE/model/OnboardingUiEvent.kt`: `internal sealed interface OnboardingUiEvent` with `data class NotificationFactsResolved(val requiresRuntimePermission: Boolean, val canDeliver: Boolean)` and `data object FinishClicked`.
- [X] T018 [P] [US1] Create `FEATURE/model/OnboardingUiEffect.kt`: `internal sealed interface OnboardingUiEffect` with `data object Finished`.
- [X] T019 [P] [US1] Create `feature/onboarding/src/main/res/values/strings.xml` with: for each of the four pages a headline and a body of at most two sentences (FR-008) — what QuestLog is for; finding games with Search and the Discover suggestions; saving games into lists with a play status; following release dates in Radar with optional reminders. Also: `onboarding_next`, `onboarding_back`, `onboarding_get_started`, and `onboarding_page_position` (`Page %1$d of %2$d`, used as the indicator's content description).
- [X] T020 [US1] Create `FEATURE/mapper/OnboardingPageUiMapper.kt`: `internal fun buildOnboardingPages(requiresRuntimePermission: Boolean, canDeliver: Boolean): List<OnboardingPage>` returning the four info pages (both parameters are accepted now and used from US3, so that story only adds to this function), and `internal fun OnboardingPage.toInfoUiModel(): OnboardingInfoPageUiModel?` mapping each info page to its `UiText.StringResource` headline/body and a Material icon (`Search`, `Bookmarks` and `CalendarMonth` for Discover, Lists and Radar). Welcome maps to a `null` icon. Depends on T013, T014, T019.
- [X] T021 [US1] Create `FEATURE/OnboardingViewModel.kt`: `@HiltViewModel class OnboardingViewModel @Inject constructor(private val completeOnboardingUseCase: CompleteOnboardingUseCase)`.
  - It owns `_uiState: MutableStateFlow<OnboardingUiState>` exposed as `internal val uiState`, and `Channel<OnboardingUiEffect>(Channel.BUFFERED)` exposed through `receiveAsFlow()`.
  - `internal fun onEvent(event)` is an exhaustive `when`.
  - `NotificationFactsResolved` builds `Ready(buildOnboardingPages(...))` only the first time.
  - `FinishClicked` goes through a private `complete()`, which is guarded so it runs once: it calls the use case and then sends `Finished`.
  - Depends on T007, T016, T017, T018, T020. T012 must pass after this.
- [X] T022 [P] [US1] Create `FEATURE/components/OnboardingInfoPage.kt`: a stateless composable taking an `OnboardingInfoPageUiModel`.
  - It shows the illustration on top, sized with `fillMaxWidth(fraction)` + `aspectRatio(1f)` and framed with the existing `metallicBorder`/`rememberCoverBrush` modifiers (no `dp` literals), then the headline, then the body.
  - The illustration is the page's icon, or `ControllerLoadingAnimation` from `:core:ui` when the icon is `null` (the Welcome page). Read that component's parameters before using it, and keep it out of the loading-state meaning: it is purely decorative here, so give it no content description. It plays one pass (`repeat = false`) and settles on the full outline instead of circling forever, and it is drawn in `appColors.textOnSurface` like the icons, because `primary` is unreadable on the light background.
  - The column is vertically scrollable so a large font size never hides text (spec edge case).
  - The headline carries `semantics { heading() }`.
  - Add a private `@QuestLogPreviews` wrapped in `QuestLogTheme { }`.
- [X] T023 [P] [US1] Create `FEATURE/components/OnboardingBottomBar.kt`: Back (hidden on page 0), `CustomPagerIndicator(pagerState, …)` with a `contentDescription` from `onboarding_page_position`, and Next, which becomes "Get started" on the last page. Callbacks only (`onBack`, `onNext`, `onFinish`). Spacing from `MaterialTheme.spacing`, with a preview.
- [X] T024 [US1] Create `FEATURE/OnboardingScreen.kt` with a public `OnboardingScreen(viewModel, onFinish, modifier)` carrying `@Suppress("ParamsComparedByRef")`, and an `internal OnboardingContent(state, pagerState, onEvent, …)`.
  - The public part collects `uiState` with `collectAsStateWithLifecycle()`, collects effects inside `LaunchedEffect { lifecycle.repeatOnLifecycle(STARTED) { … } }` (`Finished` → `onFinish()`), and sends `NotificationFactsResolved(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU, permissionState.canDeliver)` once, where `permissionState = rememberNotificationPermissionState()` from `:core:ui`. Do not call `NotificationManagerCompat` directly: the helper already holds that check, and the module has no `androidx.core` dependency of its own.
  - `OnboardingContent` renders `LoadingPage` for `Loading`; for `Ready` it renders a `HorizontalPager` driven by `rememberPagerState { pages.size }` (swipe on), the info pages, and the bottom bar. Back/Next call `animateScrollToPage`.
  - `BackHandler(enabled = pagerState.currentPage > 0)` moves to the previous page.
  - Previews for `Loading` and `Ready`.
  - Depends on T021–T023.
- [X] T025 [US1] Add the `OnboardingRoute` branch to the `entryProvider` in `app/src/main/java/com/nikolasguillen/questlog/QuestLogNavDisplay.kt`: `OnboardingScreen(viewModel = hiltViewModel<OnboardingViewModel>(), onFinish = { if (backStack.size == 1) { backStack.add(SearchRoute); backStack.removeAt(0) } else { backStack.removeLastOrNull() } })`. Add the imports. Depends on T011, T024.
- [X] T026 [US1] Update `app/src/main/java/com/nikolasguillen/questlog/MainActivity.kt` (research R5, R6).
  - Inject `GetOnboardingCompletedUseCase`.
  - In `onCreate`, launch in `lifecycleScope` a `first()` read into a nullable `mutableStateOf<Boolean?>`.
  - Hold the first frame with an `OnPreDrawListener` on `android.R.id.content`, which returns `false` until the value is non-null, then removes itself.
  - Call `setContent` as before, but pass `startWithOnboarding = !completed` into `MainContent`.
  - In `MainContent`, use `rememberNavBackStack(if (startWithOnboarding) OnboardingRoute else SearchRoute as NavKey)`.
  - In the deep-link `LaunchedEffect`, when `backStack.firstOrNull() is OnboardingRoute`, call `onDeepLinkConsumed()` and return without navigating (spec edge case).
  - `MainContent`'s bottom bar already shows only on Search, Radar and Lists, so no change is needed there. Depends on T006, T009, T011.
- [X] T027 [US1] Run `./gradlew :feature:onboarding:testDebugUnitTest :app:assembleDebug --console=plain -q`, then walk quickstart scenarios 1, 2, 8, 11, 13, 14 on an emulator.

**Checkpoint**: US1 works on its own. A fresh install shows the four-page tour and lands on Search, and a relaunch skips it.

---

## Phase 4: User Story 2 - Skip the flow at any point (Priority: P1)

**Goal**: A visible "Skip" on every page ends the flow in one action, and counts as completing it.

**Independent Test**: Fresh install, tap Skip on page 1, land on Search, relaunch and see no flow. Quickstart scenario 3 (skip part).

### Tests for User Story 2 ⚠️

- [X] T028 [US2] Extend `FTEST/OnboardingViewModelTest.kt`: `SkipClicked` calls `CompleteOnboardingUseCase` once and emits `Finished`; `SkipClicked` followed by `FinishClicked` still emits a single `Finished`. Confirm the new cases fail before T029.

### Implementation for User Story 2

- [X] T029 [US2] Add `data object SkipClicked` to `FEATURE/model/OnboardingUiEvent.kt` and route it through the same guarded `complete()` in `FEATURE/OnboardingViewModel.kt`.
- [X] T030 [P] [US2] Add `onboarding_skip` to `feature/onboarding/src/main/res/values/strings.xml`.
- [X] T031 [US2] In `FEATURE/OnboardingScreen.kt`, add a "Skip" text button in the top end corner, visible on every page including the last (FR-004), sending `SkipClicked`. It sits inside the safe-drawing insets and stays reachable at large font sizes. Add a preview with it. Depends on T029, T030.
- [X] T032 [US2] Run `./gradlew :feature:onboarding:testDebugUnitTest --console=plain -q`, then walk quickstart scenario 3 (skip, relaunch).

**Checkpoint**: US1 and US2 both work. This is the shippable MVP.

---

## Phase 5: User Story 3 - Set up platforms and release reminders (Priority: P2)

**Goal**: The flow offers an owned-platforms step (the same full, searchable list as Settings, saved on every tap) and, on Android 13+ when notifications are off, a reminders page that explains why before asking, with a clear declined state.

**Independent Test**: Quickstart scenarios 3–7, 9, 10, 12, 16. Pick platforms, allow or decline notifications, finish, and confirm Settings reflects both.

### 5a. Share the platform picker (Settings must behave exactly as before)

- [X] T033 [US3] Add `testImplementation(libs.junit)` to `core/ui/build.gradle.kts`. This is `:core:ui`'s first test source set, so also create `core/ui/src/test/java/com/nikolasguillen/questlog/core/ui/mapper/`. In the same commit, add `core/ui` to the list of test source sets in Principle V of `.specify/memory/constitution.md`.
- [X] T034 [P] [US3] Move `feature/settings/.../model/PlatformUiModel.kt` to `core/ui/.../core/ui/model/PlatformPickerItemUiModel.kt` (use `git mv`), renaming the class to `PlatformPickerItemUiModel`. It becomes `public` and stays `@Immutable`, with the same four fields.
- [X] T035 [P] [US3] Move `feature/settings/.../model/OwnedPlatformsContentState.kt` to `core/ui/.../core/ui/model/PlatformPickerContentState.kt`, renamed `PlatformPickerContentState` (`Loading`, `Empty`, `NoSearchResults`, `Success(platforms: List<PlatformPickerItemUiModel>)`). It becomes `public`, `@Immutable` on the sealed interface.
- [X] T036 [P] [US3] Move `feature/settings/.../components/PlatformRow.kt` to `core/ui/.../core/ui/component/PlatformRow.kt` and make it `public`. It takes `PlatformPickerItemUiModel`. Keep its preview.
- [X] T037 [P] [US3] Move `feature/settings/.../components/PlatformSearchField.kt` to `core/ui/.../core/ui/component/PlatformSearchField.kt` and make it `public`. It should read its strings from `:core:ui`'s own `R` (imported bare, because it is the module's own).
- [X] T038 [US3] Move the strings `owned_platforms_empty`, `owned_platforms_no_results`, `owned_platforms_clear_search_action`, `owned_platforms_search_hint` and `owned_platforms_search_clear_content_description` from `feature/settings/src/main/res/values/strings.xml` to `core/ui/src/main/res/values/strings.xml`. Leave `owned_platforms_title`, `owned_platforms_no_filter` and the `owned_platforms_filtering` plural in Settings, since only the Settings screen's caption uses them.
- [X] T039 [US3] Move `feature/settings/.../mapper/OwnedPlatformsUiMapper.kt` to `core/ui/.../core/ui/mapper/PlatformPickerMapper.kt` (use `git mv`). Rename `toContentState` to `List<Platform>.toPlatformPickerContentState(selectedIds, query, pinnedIds)` and make it `public`. Keep `PLATFORM_ORDER`, `CURATED_RANK` and `matches` private with their KDoc, since the ranking explanation is the point of the file. `PlatformVisuals` is in the same module, so no new dependency. Depends on T034, T035.
- [X] T040 [US3] Create `core/ui/.../core/ui/component/PlatformPickerList.kt`: `@Composable fun PlatformPickerList(state: PlatformPickerContentState, onToggle: (Int) -> Unit, onClearQuery: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null)`.
  - `header` is optional. In `Success` it is emitted as the first item of the `LazyColumn`, so it scrolls away with the list. In the other states the caller draws it itself (see the onboarding platforms page). Settings passes none.
  - It holds the `when` that currently sits inside `OwnedPlatformsContent`: `LoadingPage`, `EmptyPage` with a retry action, `EmptyPage` with a "clear search" action, and the `LazyColumn` of `PlatformRow` keyed by id.
  - Add a preview per state.
  - In the same commit, add `PlatformRow`, `PlatformSearchField` and `PlatformPickerList` to the component inventory in `core/ui/CLAUDE.md`, in one line: the shared owned-platforms picker used by Settings and the welcome flow, driven by `PlatformPickerContentState` from `toPlatformPickerContentState`.
  - Depends on T035, T036, T038.
- [X] T041 [US3] Update `feature/settings`: `model/OwnedPlatformsUiState.kt` uses `PlatformPickerContentState`; `OwnedPlatformsViewModel.kt` calls `toPlatformPickerContentState`; `OwnedPlatformsScreen.kt` renders `PlatformSearchField` and `PlatformPickerList` from `:core:ui` and drops the inlined `when`; update the preview data to `PlatformPickerItemUiModel`; import cross-module resources as `CoreUiR`. Delete any moved file left behind. Depends on T037, T039, T040.
- [X] T042 [P] [US3] Create `core/ui/src/test/.../core/ui/mapper/PlatformPickerMapperTest.kt` (KDoc header, plain JUnit4) with the cases moved out of `OwnedPlatformsViewModelTest`, rewritten as direct calls of `toPlatformPickerContentState`:
  - selected ids mark `isSelected`;
  - `pinnedIds == null` gives `Loading`;
  - an empty catalogue gives `Empty`;
  - a name query and an abbreviation query both match;
  - no match gives `NoSearchResults`;
  - the entry-pinned platforms come first while the query is blank, in the same relative order;
  - a non-blank query drops the pinning and keeps the ranking;
  - curated platforms rank first, then newest generation, then name.
  - In the same commit, reword the test-coverage entry in `docs/tech-debt.md` so it no longer says `:core:ui` mappers have no tests (only `PlatformPickerMapper` has one now). Edit the sentence directly; do not annotate it as fixed.
  - Depends on T039 and T033. If `PlatformVisuals` needs Android classes on the JVM, set `testOptions { unitTests.isReturnDefaultValues = true }` in `core/ui/build.gradle.kts`.
- [X] T043 [US3] Trim `feature/settings/src/test/.../OwnedPlatformsViewModelTest.kt` to the wiring that remains in the ViewModel: stays `Loading` until the catalogue, the selection and the entry order have arrived; refreshes the catalogue on open; `OnRetrySync` syncs again; the state comes from the shared mapper. Remove the cases moved in T042. Leave the toggle cases for T051.
- [X] T044 [US3] Run `./gradlew :core:ui:testDebugUnitTest :feature:settings:testDebugUnitTest :app:compileDebugKotlin --console=plain -q`. Settings → Owned platforms must look and behave exactly as before.

### 5b. Race-free toggle write path

- [X] T045 [US3] In `core/database/.../core/database/dao/PlatformDao.kt` add `@Query("SELECT EXISTS(SELECT 1 FROM owned_platforms WHERE platformId = :platformId)") suspend fun isOwned(platformId: Int): Boolean`, `@Query("DELETE FROM owned_platforms WHERE platformId = :platformId") suspend fun deleteOwnedPlatform(platformId: Int)`, and a default-bodied `@Transaction suspend fun toggleOwnedPlatform(platformId: Int)` that deletes when `isOwned` and inserts `OwnedPlatformEntity(platformId)` otherwise. Follow the `GameDao.saveGame` pattern from `core/database/CLAUDE.md`. Do not touch any entity, and confirm `core/database/schemas/` shows no diff.
- [X] T046 [US3] In `core/domain/.../core/domain/repository/GameRepository.kt` replace `setOwnedPlatforms(platformIds: Set<Int>)` with `suspend fun toggleOwnedPlatform(platformId: Int)`. Rewrite the KDoc: adds the platform or removes it if already owned, and clears every cached generic Discover lane first so a cached lane can never be served for a selection it was not fetched under.
- [X] T047 [US3] In `core/data/.../repository/GameRepositoryImpl.kt` replace `setOwnedPlatforms` with `override suspend fun toggleOwnedPlatform(platformId: Int)`: `discoverCacheDao.clearAll()` first, then `platformDao.toggleOwnedPlatform(platformId)`. Keep the "order matters" KDoc. Depends on T045, T046.
- [X] T048 [US3] Remove `setOwnedPlatforms` and `clearOwnedPlatforms` from `PlatformDao.kt`, since nothing calls them after T047. Depends on T047.
- [X] T049 [US3] Create `core/domain/.../core/domain/usecase/discover/ToggleOwnedPlatformUseCase.kt` (`suspend operator fun invoke(platformId: Int)` calling `repository.toggleOwnedPlatform`) and delete `SetOwnedPlatformsUseCase.kt`. Depends on T046.
- [X] T050 [P] [US3] Replace `core/data/src/test/.../repository/GameRepositoryImplSetOwnedPlatformsTest.kt` with `GameRepositoryImplToggleOwnedPlatformTest.kt`, same setup, one case: `discoverCacheDao.clearAll()` is called before `platformDao.toggleOwnedPlatform(48)` (`coVerifyOrder`). Depends on T047.
- [X] T051 [US3] Update `feature/settings/.../OwnedPlatformsViewModel.kt`: inject `ToggleOwnedPlatformUseCase` instead of `SetOwnedPlatformsUseCase`, delete `selectionWriteLock` and the read-modify-write, and make `togglePlatform` a single `viewModelScope.launch { toggleOwnedPlatformUseCase(platformId) }`. Update the KDoc to say the write is atomic in the data layer. Then update `OwnedPlatformsViewModelTest.kt`: a tap calls the use case with that id once. Delete the old "two taps in quick succession both survive" case: the guarantee now lives in a Room transaction, and the project has no DAO tests (a known gap in `docs/tech-debt.md`), so quickstart scenario 16 covers it. Depends on T041, T049.
- [X] T052 [US3] Run `./gradlew :core:database:compileDebugKotlin :core:data:testDebugUnitTest :feature:settings:testDebugUnitTest --console=plain -q`, then `git status` to confirm no file under `core/database/schemas/` changed.

### 5c. Platforms step in the flow

- [X] T053 [US3] Extend `FTEST/OnboardingViewModelTest.kt` with mocks for `GetKnownPlatformsUseCase`, `GetSelectedPlatformIdsUseCase`, `ToggleOwnedPlatformUseCase` and `SyncPlatformCatalogUseCase`. Cases:
  - `buildOnboardingPages` ends with `Platforms` after the four info pages;
  - `PlatformToggled(id)` calls the toggle use case with that id;
  - the catalogue sync runs on init, and again on `RetryPlatformSync`;
  - `selectedPlatformCount` follows the stored selection;
  - the picker state is built from the shared mapper: the current picks appear first when the ViewModel is created with a stored selection (the replay case), and it stays `Loading` until the entry-time selection has been read.
- [X] T054 [P] [US3] Add `data object Platforms` to `FEATURE/model/OnboardingPage.kt`.
- [X] T055 [P] [US3] Add `val platformPicker: PlatformPickerContentState = PlatformPickerContentState.Loading` and `val selectedPlatformCount: Int = 0` to `FEATURE/model/OnboardingUiState.kt`.
- [X] T056 [P] [US3] Add `data class PlatformToggled(val platformId: Int)`, `data object ClearPlatformQuery` and `data object RetryPlatformSync` to `FEATURE/model/OnboardingUiEvent.kt`.
- [X] T057 [P] [US3] Add to `feature/onboarding/.../strings.xml` the platforms page headline and body (at most two sentences, saying the choice is optional and changeable in Settings), plus a caption pair mirroring Settings' wording: none selected means no platform filter, otherwise a plural "Filtering to %1$d platforms".
- [X] T058 [US3] Update `FEATURE/mapper/OnboardingPageUiMapper.kt` so `buildOnboardingPages` appends `Platforms` after the four info pages. Depends on T054.
- [X] T059 [US3] Update `FEATURE/OnboardingViewModel.kt`: inject the four platform use cases; own a `TextFieldState`; read the stored selection once in `init` as the pinned set (`null` until read); sync the catalogue in `init`; and fold `combine(getKnownPlatforms(), getSelectedPlatformIds(), snapshotFlow { query }.distinctUntilChanged(), pinnedIds)` into `_uiState` through `viewModelScope.launch { …collect { _uiState.update { … } } }` using `toPlatformPickerContentState`. Handle `PlatformToggled` (calls the use case), `ClearPlatformQuery` (clears the text) and `RetryPlatformSync`. Depends on T049, T053, T055, T056, T058.
- [X] T060 [US3] Create `FEATURE/components/OnboardingPlatformsPage.kt`: headline, body, the count caption, `PlatformSearchField(state = textFieldState, …)` and `PlatformPickerList(…)` from `:core:ui`, with `onToggle`, `onClearQuery` and `onRetry` sending the events from T056. Spacing comes from `MaterialTheme.spacing`. Add previews for Success and Empty.
  - The headline is pinned above the search field, which is pinned above the list. In `Success`, the body and count caption are passed as `PlatformPickerList`'s `header`, so they are the first items of the list and scroll away at large font scales.
  - In the other states (Loading, Empty, NoSearchResults) there is nothing long to scroll, so draw the same body and caption statically above the state's message. In every state, including the offline/empty one, the body saying the choice can be made later in Settings stays visible (FR-012).
  - The headline carries `semantics { heading() }` (FR-015).
  - Depends on T057.
- [X] T061 [US3] In `FEATURE/OnboardingScreen.kt`, render `OnboardingPlatformsPage` for `OnboardingPage.Platforms`, passing `viewModel.textFieldState`. Add `LocalFocusManager.clearFocus()` whenever `pagerState.currentPage` changes, so the keyboard never stays open on another page. The picker's vertical scrolling inside the horizontal pager must keep working. Depends on T059, T060.

### 5d. Reminders step in the flow

- [X] T062 [US3] Extend `FTEST/OnboardingViewModelTest.kt`. Cases:
  - `Reminders` is last in the list when `requiresRuntimePermission = true, canDeliver = false`;
  - it is absent when `requiresRuntimePermission = false`, and absent when `canDeliver = true` (FR-018);
  - `AllowNotificationsClicked` emits `RequestNotificationPermission`;
  - `NotificationPermissionResult(true)` gives `Granted`, and `(false)` gives `Declined`;
  - `NotNowClicked` gives `Declined` without emitting `RequestNotificationPermission`;
  - `Declined` followed by `PermissionStateChanged(canDeliver = true)` gives `Granted`, while `PermissionStateChanged(false)` changes nothing;
  - finishing from `Declined` still emits `Finished`.
- [X] T063 [P] [US3] Create `FEATURE/model/ReminderStepState.kt`: `@Immutable internal sealed interface ReminderStepState` with `data object Undecided`, `Granted`, `Declined`.
- [X] T064 [P] [US3] Add `data object Reminders` to `FEATURE/model/OnboardingPage.kt`. Run after T054.
- [X] T065 [P] [US3] Add `val reminderStep: ReminderStepState = ReminderStepState.Undecided` to `FEATURE/model/OnboardingUiState.kt`. Run after T055.
- [X] T066 [P] [US3] Add `AllowNotificationsClicked`, `NotNowClicked`, `data class NotificationPermissionResult(val granted: Boolean)` and `data class PermissionStateChanged(val canDeliver: Boolean)` to `FEATURE/model/OnboardingUiEvent.kt`. Run after T056.
- [X] T067 [P] [US3] Add `data object RequestNotificationPermission` to `FEATURE/model/OnboardingUiEffect.kt`.
- [X] T068 [P] [US3] Add an optional `onResult: (granted: Boolean) -> Unit = {}` parameter to `rememberNotificationPermissionState()` in `core/ui/.../core/ui/util/NotificationPermission.kt`. Invoke it at the end of the launcher callback, after `canDeliver` and `isPermanentlyDenied` are updated. Use `rememberUpdatedState` so a changing lambda never re-creates the launcher. Document it in the KDoc. The four existing callers must compile unchanged. In the same commit, if `core/ui/CLAUDE.md` lists the helper, mention the optional `onResult`.
- [X] T069 [US3] Update `buildOnboardingPages` in `FEATURE/mapper/OnboardingPageUiMapper.kt` to append `Reminders` last only when `requiresRuntimePermission && !canDeliver`. Depends on T058, T064.
- [X] T070 [US3] Update `FEATURE/OnboardingViewModel.kt`:
  - `AllowNotificationsClicked` sends `RequestNotificationPermission`;
  - `NotNowClicked` sets `Declined`;
  - `NotificationPermissionResult` sets `Granted` or `Declined`;
  - `PermissionStateChanged(true)` moves `Declined` to `Granted`, and nothing else changes.
  - Depends on T059, T062, T063, T065, T066, T067.
- [X] T071 [P] [US3] Add to `feature/onboarding/.../strings.xml` the three reminders states, each at most one headline and two sentences:
  - Undecided: explains that QuestLog can tell the user on the day a saved game comes out, that reminders are chosen per game, and that the system will ask for permission next. Actions: "Allow notifications" and "Not now".
  - Granted: confirms reminders are available.
  - Declined: says reminders are off and can be changed later from Settings.
  - Plus `onboarding_reminders_allow`, `onboarding_reminders_not_now`, and "Get started" reused.
- [X] T072 [US3] Create `FEATURE/components/OnboardingRemindersPage.kt` rendering the three `ReminderStepState`s from T071. `Undecided` shows the primary "Allow notifications" and the secondary "Not now". `Granted` and `Declined` show only text, because the bottom bar's "Get started" ends the flow. Callbacks only. The headline carries `semantics { heading() }` (FR-015). Add a preview per state. Depends on T063, T071.
- [X] T073 [US3] In `FEATURE/OnboardingScreen.kt`:
  - pass `onResult = { onEvent(NotificationPermissionResult(it)) }` to the `rememberNotificationPermissionState()` call that T024 introduced;
  - render `OnboardingRemindersPage` for `Reminders`;
  - on `RequestNotificationPermission`, call `permissionState.request()` unless `permissionState.isPermanentlyDenied`, in which case send `NotificationPermissionResult(false)` directly (research R8), and never open system settings from the flow;
  - send `PermissionStateChanged(permissionState.canDeliver)` from a `LaunchedEffect(permissionState.canDeliver)`.
  - Depends on T068, T070, T072.
- [X] T074 [US3] Run `./gradlew :feature:onboarding:testDebugUnitTest :app:assembleDebug --console=plain -q`, then walk quickstart scenarios 3–7, 9, 10, 12 and 16.

**Checkpoint**: US3 works. Platform picks persist as they are made, the reminders page behaves in all three states, and Settings' platform screen is unchanged to the user.

---

## Phase 6: User Story 4 - Revisit the tour later (Priority: P3)

**Goal**: A "Show welcome tour" row in Settings reopens the flow from the first page, and finishing or skipping returns to Settings.

**Independent Test**: Complete the flow, open Settings → App → "Show welcome tour", finish or skip, and land back on Settings. Quickstart scenario 7.

### Implementation for User Story 4

- [X] T075 [P] [US4] Add `settings_show_welcome_tour` and a short subtitle string to `feature/settings/src/main/res/values/strings.xml`.
- [X] T076 [US4] In `feature/settings/.../SettingsScreen.kt` add `onShowWelcomeTourClick: () -> Unit` to `SettingsScreen` and `SettingsContent`, and a `SettingsRow` with a suitable icon (e.g. `Icons.Default.School` or `Icons.Outlined.Explore`) at the top of the "App" group, before the translation-model row. Update every `SettingsContent(...)` preview call to pass `onShowWelcomeTourClick = {}`. Depends on T075.
- [X] T077 [US4] In the `SettingsRoute` branch of `app/.../QuestLogNavDisplay.kt` pass `onShowWelcomeTourClick = { if (backStack.lastOrNull() != OnboardingRoute) backStack.add(OnboardingRoute) }`. Depends on T076.
- [X] T078 [US4] Verify quickstart scenario 7 and the replay rules:
  - on replay, `backStack.size > 1`, so `onFinish` pops to Settings;
  - the platforms step shows the current picks first;
  - the reminders page is absent when notifications are already on;
  - system back on page 1 of a replay returns to Settings rather than closing the app.

**Checkpoint**: All four user stories work.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final verification. Documentation edits are not collected here: each was made in the task that causes it (see the header).

- [X] T079 Run `./gradlew test` and `./gradlew :app:assembleDebug`. Every suite must be green, `git status` must show no change under `core/database/schemas/`, and `grep -rn "SetOwnedPlatformsUseCase\|setOwnedPlatforms\|clearOwnedPlatforms" --include=*.kt .` must find nothing.
- [X] T080 Run the whole `quickstart.md` manual table on an API 33+ emulator, and scenario 9 on an API 29–32 emulator. Report any scenario that fails rather than marking this task done.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. It blocks every user story.
- **US1 (Phase 3)** and **US2 (Phase 4)**: depend on Foundational. US2 edits files US1 creates, so it follows US1.
- **US3 (Phase 5)**: depends on US1, because it adds pages to the existing screen and ViewModel. Sections 5a and 5b do not touch the onboarding module and may start right after Foundational, in parallel with US1 and US2. 5c needs 5a and 5b. 5d needs 5c.
- **US4 (Phase 6)**: depends on US1, because it needs `OnboardingRoute` to have a screen. It is otherwise independent of US3.
- **Polish (Phase 7)**: after everything else.

### Within Each Story

- Tests first (T012, T028, T053, T062). Confirm they fail, then implement.
- Models and strings before the mapper, the mapper before the ViewModel, the ViewModel before the screen, the screen before navigation.
- Several tasks edit the same file, so they run in task-number order and are not `[P]` with each other: `OnboardingViewModelTest.kt` (T012, T028, T053, T062), `OnboardingUiEvent.kt` (T017, T029, T056, T066), `OnboardingUiState.kt` (T016, T055, T065), `OnboardingPage.kt` (T013, T054, T064), `OnboardingViewModel.kt` (T021, T029, T059, T070), `OnboardingScreen.kt` (T024, T031, T061, T073), `OnboardingPageUiMapper.kt` (T020, T058, T069), `QuestLogNavDisplay.kt` (T025, T077) and `strings.xml` in `feature/onboarding` (T019, T030, T057, T071).

### Parallel Opportunities

- Phase 2: T005, T006, T007 and T011 together. T010 once T008 exists.
- US1: T013–T019 together (seven new files), then T022 and T023 together.
- US3: T034, T035, T036 and T037 together. T054–T057 together. T063–T068 mostly together, with the ordering notes above.
- 5a/5b (core and settings work) can run beside US1/US2 (onboarding module work) as a second stream.

### Parallel Example: User Story 1 models and strings

```text
T013 OnboardingPage.kt          T016 OnboardingUiState.kt
T014 OnboardingInfoPageUiModel  T017 OnboardingUiEvent.kt
T015 OnboardingContentState.kt  T018 OnboardingUiEffect.kt
T019 strings.xml
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 and Phase 2.
2. US1, then US2. They are both P1 and tiny together, since skipping is one more event and one button.
3. **Stop and validate**: a fresh install shows the tour, Skip and Get started both land on Search, and a relaunch shows nothing. This is shippable on its own, and it already gives a first-time user the explanation the spec calls "the feature".

### Incremental Delivery

1. MVP above.
2. US3 in four steps: 5a/5b first as a behaviour-preserving refactor of Settings, which is safe to merge on its own; then 5c (platforms); then 5d (reminders).
3. US4, which adds the Settings row.
4. Polish and the full quickstart.

### Notes

- Commit after each group. Suggested subjects (lowercase, imperative, scopes from the hook):
  - `build(onboarding): add the feature module` (also carries T005's docs and amendment)
  - `feat(data): persist the onboarding completed flag`
  - `feat(onboarding): add the first-launch tour`
  - `feat(onboarding): let the tour be skipped`
  - `refactor(ui): move the platform picker into core:ui`
  - `refactor(data): toggle owned platforms in one transaction`
  - `feat(onboarding): add the platforms step`
  - `feat(onboarding): ask for notification permission`
  - `feat(settings): add a show welcome tour row`
- Do not edit anything under `**/build/generated/**`, and keep display text out of models, mappers and composables.
- Leave the known deviations in `docs/tech-debt.md` alone, other than the module-count edit in T005 and the test-coverage edit in T042.
