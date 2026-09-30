# Tasks: Light Appearance & Theme Preference

**Input**: Design documents from `/specs/004-light-dark-theme/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/domain-ports.md](./contracts/domain-ports.md),
[contracts/ui-contracts.md](./contracts/ui-contracts.md), [quickstart.md](./quickstart.md)

**Tests**: Not TDD — tests are added alongside the code they cover (per the constitution's Principle V:
"New ViewModel, mapper, use-case or error-mapping logic gets a test"), not written to fail first. Trivial
pass-through use cases (`GetAppearanceModeUseCase`, `SetAppearanceModeUseCase`) get no dedicated test,
matching the existing `GetReleaseNotificationGameIdsUseCase` precedent (no test file for it either).

**Organization**: Tasks are grouped by user story. Because all three `AppearanceMode` values
(`LIGHT`/`DARK`/`SYSTEM`) are a single enum offered together in one 3-way selector from the start (there is
no way to ship "just 2 of 3 segmented-button options"), nearly all of the actual plumbing lands in
**Phase 2: Foundational** — the same shape `specs/003-release-notifications/tasks.md` used for its two
Android port implementations. The per-user-story phases that follow are almost entirely about *proving*
what Foundational already built: User Story 1's phase is where the real remaining work is (the hardcoded
dark-only color audit), while User Story 2 and User Story 3 are functionally complete the moment Foundational
lands and their phases are the corresponding quickstart verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3) — omitted for Setup/Foundational/Polish
- File paths below are full, real repo-relative paths — copy them directly, nothing is abbreviated.

---

## Phase 1: Setup

**Purpose**: Gradle dependency changes needed before any of the new code compiles.

- [ ] T001 [P] In `core/data/build.gradle.kts`, add `implementation(libs.androidx.datastore.preferences)`
  to the `dependencies { }` block — needed by `AppearancePreferenceStoreImpl` (Phase 2). The artifact is
  already in `gradle/libs.versions.toml` (`androidx-datastore-preferences`), just unused until now.
- [ ] T002 [P] In `core/designsystem/build.gradle.kts`, add `implementation(libs.androidx.core.ktx)` to
  the `dependencies { }` block — needed by `QuestLogTheme`'s system-bar `SideEffect` (Phase 2) for
  `WindowCompat`/`WindowInsetsControllerCompat` (research.md §5).
- [ ] T003 [P] In `app/build.gradle.kts`, remove the now-dead
  `implementation(libs.androidx.datastore.preferences)` line — `:app` never touches DataStore directly,
  it goes through `:core:domain` use cases (research.md §1, plan.md's "deliberate additions" list).

**Checkpoint**: Gradle sync succeeds with all three module dependency changes.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The persisted preference, the domain port, the light color tokens, and the UI wiring every
user story needs. Unlike a typical feature where Foundational is invisible until a story phase lights it
up, this phase alone already delivers Light/Dark/Follow-System end to end — the story phases after it are
about hardening (US1) and verification (US2, US3), not new mechanism.

**⚠️ CRITICAL**: Complete this phase before starting any user story phase.

### Domain layer

- [ ] T004 [P] Create `core/model/src/main/java/com/nikolasguillen/questlog/core/model/AppearanceMode.kt` exactly per
  [data-model.md](./data-model.md):
  ```kotlin
  package com.nikolasguillen.questlog.core.model

  enum class AppearanceMode(val id: Int) {
      SYSTEM(0),
      LIGHT(1),
      DARK(2);

      companion object {
          fun fromId(id: Int): AppearanceMode = entries.find { it.id == id } ?: SYSTEM
      }
  }
  ```
  Give it the same KDoc discipline as `core/model/.../GameStatus.kt` (`@property id` note: "Stable
  identifier written to DataStore. Never renumber existing entries", `fromId` note: "Falls back to
  [SYSTEM] for an unknown [id] rather than throwing").

- [ ] T005 Create `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/settings/AppearancePreferenceStore.kt`
  per [contracts/domain-ports.md](./contracts/domain-ports.md):
  ```kotlin
  package com.nikolasguillen.questlog.core.domain.settings

  import com.nikolasguillen.questlog.core.model.AppearanceMode
  import kotlinx.coroutines.flow.Flow

  interface AppearancePreferenceStore {
      fun observeAppearanceMode(): Flow<AppearanceMode>
      suspend fun setAppearanceMode(mode: AppearanceMode)
  }
  ```
  Contract only — `:core:domain` never imports `androidx.datastore`. Depends on T004.

- [ ] T006 [P] Create
  `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/usecase/settings/GetAppearanceModeUseCase.kt`:
  ```kotlin
  class GetAppearanceModeUseCase @Inject constructor(
      private val appearancePreferenceStore: AppearancePreferenceStore
  ) {
      operator fun invoke(): Flow<AppearanceMode> = appearancePreferenceStore.observeAppearanceMode()
  }
  ```
  Depends on T005.

- [ ] T007 [P] Create
  `core/domain/src/main/java/com/nikolasguillen/questlog/core/domain/usecase/settings/SetAppearanceModeUseCase.kt`:
  ```kotlin
  class SetAppearanceModeUseCase @Inject constructor(
      private val appearancePreferenceStore: AppearancePreferenceStore
  ) {
      suspend operator fun invoke(mode: AppearanceMode) = appearancePreferenceStore.setAppearanceMode(mode)
  }
  ```
  Depends on T005.

### Data layer

- [ ] T008 Create `core/data/src/main/java/com/nikolasguillen/questlog/core/data/settings/AppearancePreferenceStoreImpl.kt`:
  ```kotlin
  package com.nikolasguillen.questlog.core.data.settings

  import android.content.Context
  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.edit
  import androidx.datastore.preferences.core.intPreferencesKey
  import androidx.datastore.preferences.preferencesDataStore
  import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
  import com.nikolasguillen.questlog.core.model.AppearanceMode
  import dagger.hilt.android.qualifiers.ApplicationContext
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.map
  import javax.inject.Inject

  private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
  private val APPEARANCE_MODE_KEY = intPreferencesKey("appearance_mode")

  class AppearancePreferenceStoreImpl @Inject constructor(
      @ApplicationContext private val context: Context
  ) : AppearancePreferenceStore {

      override fun observeAppearanceMode(): Flow<AppearanceMode> =
          context.settingsDataStore.data.map { prefs ->
              AppearanceMode.fromId(prefs[APPEARANCE_MODE_KEY] ?: AppearanceMode.SYSTEM.id)
          }

      override suspend fun setAppearanceMode(mode: AppearanceMode) {
          context.settingsDataStore.edit { prefs -> prefs[APPEARANCE_MODE_KEY] = mode.id }
      }
  }
  ```
  Matches [data-model.md](./data-model.md)'s persistence shape exactly: one file, one
  `intPreferencesKey("appearance_mode")`, absent key resolves to `SYSTEM`. Depends on T001, T005.

- [ ] T009 [P] Create
  `core/data/src/test/java/com/nikolasguillen/questlog/core/data/settings/AppearancePreferenceStoreImplTest.kt`.
  Back a real `DataStore<Preferences>` with a JUnit4 `TemporaryFolder` rule (the standard way to
  unit-test DataStore without a device — no mocking of DataStore itself). Cover:
  - reading before any write returns `AppearanceMode.SYSTEM`
  - `setAppearanceMode(LIGHT)` then `observeAppearanceMode()` emits `LIGHT`
  - writing, then reconstructing a fresh `AppearancePreferenceStoreImpl` over the same file, still reads
    the persisted value back (proves FR-009/SC-003's restart-survival at the persistence layer)

  Give the class the same KDoc-header discipline as other test classes in this module
  (`RepositoryErrorMapperTest.kt`). Depends on T008.

- [ ] T010 In `core/data/src/main/java/com/nikolasguillen/questlog/core/data/di/DataModule.kt`, add the binding
  (alongside the existing `bindGameRepository`, `bindReleaseNotificationScheduler`, etc.):
  ```kotlin
  @Binds
  @Singleton
  abstract fun bindAppearancePreferenceStore(
      appearancePreferenceStoreImpl: AppearancePreferenceStoreImpl
  ): AppearancePreferenceStore
  ```
  Depends on T008.

### Light color tokens

- [ ] T011 [P] In `core/designsystem/src/main/java/com/nikolasguillen/questlog/core/designsystem/theme/Color.kt`, add:
  - A `*Light` counterpart for every `*Dark` constant currently feeding `darkColorScheme(...)` in
    `QuestLogTheme.kt`: `PrimaryLight`, `OnPrimaryLight`, `PrimaryContainerLight`, `OnPrimaryContainerLight`,
    `SecondaryLight`, `OnSecondaryLight`, `SecondaryContainerLight`, `OnSecondaryContainerLight`,
    `TertiaryLight`, `OnTertiaryLight`, `TertiaryContainerLight`, `OnTertiaryContainerLight`, `ErrorLight`,
    `OnErrorLight`, `ErrorContainerLight`, `OnErrorContainerLight`, `BackgroundLight`, `OnBackgroundLight`,
    `SurfaceLight`, `OnSurfaceLight`, `OutlineLight`, `OutlineVariantLight`.
  - 2-3 new light neutral tokens for `AppColors`' light instance (T012 in `QuestLogTheme.kt`) — e.g. an
    off-white for the app background and one or two pale greys for elevated surfaces/search-bar/nav-bar
    containers. Reuse the *existing* `NeutralBlack`/`NeutralDarkGrey`/`NeutralMediumGrey` as the "on-light"
    text/icon colors wherever they sit on one of these new light backgrounds — they're already dark enough
    to contrast well, no new dark tokens needed.
  - Reuse `Gold`/`GoldDeep`/`GoldMuted`/`GoldBright` as-is for `PrimaryLight`/`PrimaryContainerLight`/etc.
    per FR-004 ("same hue") — do not invent a second gold ramp.

  **Constraint (FR-010 / SC-002 / data-model.md validation rules)**: every text-or-icon color paired with
  the surface/container color it sits on in the light scheme MUST be verified at or above WCAG 2.1 AA —
  4.5:1 for normal text, 3:1 for large text and icons — with a contrast-ratio tool before the values are
  final. Where `Gold` alone doesn't clear 4.5:1 as small text on a light background, use it as a
  container/accent fill with a dark "on" color instead (mirroring `OnPrimaryDark = Color.Black` in the
  existing dark scheme), per research.md §11 — do not weaken the ratio to keep gold-on-white body text.

- [ ] T012 In `core/designsystem/src/main/java/com/nikolasguillen/questlog/core/designsystem/theme/QuestLogTheme.kt`:
  - Change the signature to `fun QuestLogTheme(darkTheme: Boolean = true, content: @Composable () -> Unit)`.
  - Wrap the existing `darkColorScheme(...)` call in `if (darkTheme) { darkColorScheme(...) } else {
    lightColorScheme(...) }`, building the `else` branch from the T011 `*Light` tokens, field-for-field
    matching the existing dark branch's mapping (e.g. `primary = PrimaryLight`, `background =
    BackgroundLight`, etc.).
  - Likewise branch the existing `appColors = AppColors(...)` literal between the current values and a new
    light `AppColors(...)` literal — same 17 fields, using T011's light tokens.
  - Add, inside the `Surface(color = MaterialTheme.appColors.appBackground) { content() }` block (or
    immediately around it): a `SideEffect` that reads `LocalView.current`, walks its `context` up to an
    `Activity` (a small local `tailrec fun Context.findActivity(): Activity?` helper, returning `null` if
    none is found rather than throwing — some preview/host environments have no real `Activity`), and when
    non-null calls `WindowCompat.getInsetsController(activity.window, view).apply { isAppearanceLightStatusBars
    = !darkTheme; isAppearanceLightNavigationBars = !darkTheme }`.

  Depends on T002, T011.

- [ ] T013 [P] In `core/designsystem/CLAUDE.md`, delete the bullet *"Dark theme only. `QuestLogTheme.kt`
  exposes a single `darkColorScheme`. There is no light scheme and no dynamic color. Do not assume
  light-mode support or add `isSystemInDarkTheme()` branches."* — it is now false. Per the root
  `CLAUDE.md`'s instruction to delete a stale rule in the same commit as the change that makes it stale,
  not annotate it as historical.

### `core/ui` label mapping

- [ ] T014 [P] Add three strings to `core/ui/src/main/res/values/strings.xml`, next to the existing
  `status_*` block for consistency: `appearance_light` ("Light"), `appearance_dark` ("Dark"),
  `appearance_system` ("Follow System").

- [ ] T015 Create
  `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/mapper/AppearanceModeMapper.kt`:
  ```kotlin
  package com.nikolasguillen.questlog.core.ui.mapper

  import com.nikolasguillen.questlog.core.model.AppearanceMode
  import com.nikolasguillen.questlog.core.ui.R
  import com.nikolasguillen.questlog.core.ui.model.UiText

  fun AppearanceMode.toLabelUiText(): UiText {
      val resId = when (this) {
          AppearanceMode.LIGHT -> R.string.appearance_light
          AppearanceMode.DARK -> R.string.appearance_dark
          AppearanceMode.SYSTEM -> R.string.appearance_system
      }
      return UiText.StringResource(resId)
  }
  ```
  Same shape as `GameStatus.toLabelUiText()` in `GameUiMapper.kt`. Depends on T004, T014.

### `feature/settings` wiring

- [ ] T016 [P] In `feature/settings/src/main/java/com/nikolasguillen/questlog/feature/settings/model/SettingsUiState.kt`, add the field
  `val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM` to the `data class SettingsUiState`, with a
  `@property appearanceMode` KDoc line next to the existing ones: "The user's current Appearance selection,
  reflected by the segmented control in the App group. Starts `SYSTEM`, matching the persisted default."
  Import `com.nikolasguillen.questlog.core.model.AppearanceMode`. Depends on T004.

- [ ] T017 [P] In `feature/settings/src/main/java/com/nikolasguillen/questlog/feature/settings/model/SettingsUiEvent.kt`, add to the `sealed interface SettingsUiEvent`:
  ```kotlin
  /** The user picked a different Appearance option from the segmented control. */
  data class AppearanceModeChanged(val mode: AppearanceMode) : SettingsUiEvent
  ```
  Depends on T004.

- [ ] T018 In `feature/settings/src/main/java/com/nikolasguillen/questlog/feature/settings/SettingsViewModel.kt`:
  - Inject `private val getAppearanceModeUseCase: GetAppearanceModeUseCase` and
    `private val setAppearanceModeUseCase: SetAppearanceModeUseCase` in the constructor.
  - Add `observeAppearanceMode()` to `init { }` (alongside `observeOwnedPlatforms()`,
    `loadTranslationModelStatus()`, `observeReleaseNotificationCount()`), implemented the same way
    `observeOwnedPlatforms()` is — collected for the ViewModel's whole life:
    ```kotlin
    private fun observeAppearanceMode() {
        viewModelScope.launch {
            getAppearanceModeUseCase().collect { mode ->
                _uiState.update { it.copy(appearanceMode = mode) }
            }
        }
    }
    ```
  - Add a branch to `onEvent`'s `when`:
    ```kotlin
    is SettingsUiEvent.AppearanceModeChanged ->
        viewModelScope.launch { setAppearanceModeUseCase(event.mode) }
    ```
  Depends on T006, T007, T016, T017.

- [ ] T019 [P] Add the string `settings_group_appearance` ("Appearance") to
  `feature/settings/src/main/res/values/strings.xml`, next to the existing `settings_group_*` entries.

- [ ] T020 In `feature/settings/src/main/java/com/nikolasguillen/questlog/feature/settings/SettingsScreen.kt`'s `SettingsContent`, add a new `SettingsGroup` as the
  **first** child of the outer `Column` (before `settings_group_game_profile`), so the appearance control
  reads as the top-level personalization setting rather than being buried under app-data rows:
  ```kotlin
  SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
      CustomSegmentedButton(
          options = AppearanceMode.entries,
          selectedIndex = AppearanceMode.entries.indexOf(state.appearanceMode),
          onOptionSelected = { index ->
              onEvent(SettingsUiEvent.AppearanceModeChanged(AppearanceMode.entries[index]))
          },
          label = { Text(it.toLabelUiText().asString()) },
          modifier = Modifier.fillMaxWidth()
      )
  }
  ```
  Import `com.nikolasguillen.questlog.core.model.AppearanceMode`,
  `com.nikolasguillen.questlog.core.ui.component.CustomSegmentedButton`, and
  `com.nikolasguillen.questlog.core.ui.mapper.toLabelUiText`. Add a matching `SettingsUiState(appearanceMode
  = ...)` value to at least one of the existing `@Preview` functions so the new group renders in previews.
  Depends on T015, T018, T019.

- [ ] T021 [P] In `feature/settings/src/test/java/com/nikolasguillen/questlog/feature/settings/SettingsViewModelTest.kt`, add test
  cases (mock `GetAppearanceModeUseCase`/`SetAppearanceModeUseCase` the same way the existing MockK setup
  mocks the other use cases — never the repository):
  - `uiState.appearanceMode` reflects whatever `GetAppearanceModeUseCase` emits.
  - Dispatching `SettingsUiEvent.AppearanceModeChanged(AppearanceMode.LIGHT)` calls
    `setAppearanceModeUseCase(AppearanceMode.LIGHT)` exactly once.
  Depends on T018.

### `:app` wiring

- [ ] T022 In `app/src/main/java/com/nikolasguillen/questlog/MainActivity.kt`:
  - Add the field `@Inject lateinit var getAppearanceModeUseCase: GetAppearanceModeUseCase` to the
    `@AndroidEntryPoint class MainActivity`.
  - Inside `setContent { }`, before the existing `QuestLogTheme { ... }` call, add:
    ```kotlin
    val appearanceMode by getAppearanceModeUseCase()
        .collectAsStateWithLifecycle(initialValue = AppearanceMode.SYSTEM)
    val darkTheme = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    ```
  - Change `QuestLogTheme { MainContent(...) }` to `QuestLogTheme(darkTheme = darkTheme) {
    MainContent(...) }`.
  - Leave the existing `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))`
    call in `onCreate` untouched — it configures edge-to-edge layout, not the reactive icon color, which
    now lives inside `QuestLogTheme` (T012).

  Per [contracts/ui-contracts.md](./contracts/ui-contracts.md) — no new `ViewModel`. Depends on T006, T012.

- [ ] T023 [P] In `docs/roadmap.md`'s "Settings holds only what has a backend" section, delete the clause
  *"; appearance has nothing to switch, because `:core:designsystem` is dark-only by design"* (keep the
  rest of that sentence about the genre picker intact) — it's no longer true once this feature ships.

**Checkpoint**: Build the full app (`./gradlew :app:assembleDebug`). Light/Dark/Follow System are all
selectable from Settings, persist across restarts, and resolve correctly everywhere `QuestLogTheme` is the
root — the feature is functionally complete. What's left is hardening (US1) and verification (US2, US3).

---

## Phase 3: User Story 1 - Manually choose Light or Dark appearance (Priority: P1) 🎯 MVP

**Goal**: A user can pick Light or Dark from Settings and every screen renders correctly in that appearance,
with the brand color unchanged and no leftover dark-only elements.

**Independent Test**: Open Settings, select "Light", browse every top-level screen (Search, Wishlist,
Lists, Radar, a game detail page, Settings itself) and confirm light surfaces, unchanged brand/accent hue,
legible system bar icons, and no unreadable or visually-broken content anywhere; select "Dark" and confirm
no regression from today's look.

The selector, the persistence, and the color-scheme switch itself are already built in Phase 2. What
remains is the audit research.md §10 flagged: 22 files use a hardcoded `Color(0x...)`/`Color.Black`/
`Color.White` outside the theme layer, and each one needs a per-file judgment call — genuinely
theme-independent by design (keep as-is) or a dark-only assumption (route through
`MaterialTheme.colorScheme` / `MaterialTheme.appColors` instead). This is what Acceptance Scenario 3
("no leftover dark-only elements") and FR-010 actually require to be *true*, not just wired.

### Hardcoded-color audit (research.md §10)

For each task below: read the file, find every `Color(0x...)`, `Color.Black`, and `Color.White` literal
in it, and for each one decide — and record the reasoning in the diff/commit, not just in your head:
- **Keep hardcoded** when the color is intentionally theme-independent (an image scrim for text-over-cover
  legibility, a full-bleed viewer's black chrome, a brand-fixed effect like the metallic brushes) — these
  are expected to look the same in Light and Dark, the same way a photo viewer's black surround doesn't
  follow a light-themed host app.
- **Route through the theme** when the color assumes the current dark background/text (e.g. a literal
  `Color.White` used as body text color, or a dark literal used as a card background) — replace it with
  the matching `MaterialTheme.colorScheme.*` or `MaterialTheme.appColors.*` token so it adapts.

- [ ] T024 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/util/MetallicEffects.kt`
- [ ] T025 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/util/ColorUtils.kt`
- [ ] T026 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/util/PlatformVisuals.kt`
- [ ] T027 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/util/modifiers/MetallicModifiers.kt`
- [ ] T028 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/util/modifiers/VisualModifiers.kt`
- [ ] T029 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/PlatformTile.kt`
- [ ] T030 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/FullScreenImageViewer.kt`
- [ ] T031 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/CustomFilterChip.kt`
- [ ] T032 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/RatingBadge.kt`
- [ ] T033 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/ImageGalleryPager.kt`
- [ ] T034 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/ImmersiveDetailLayout.kt`
- [ ] T035 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/gamecard/VerticalGameCard.kt`
- [ ] T036 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/gamecard/CompactGameCard.kt`
- [ ] T037 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/gamecard/SaveToWishlistButton.kt`
- [ ] T038 [P] [US1] Audit `core/ui/src/main/java/com/nikolasguillen/questlog/core/ui/component/gamecard/RecentGameCard.kt`
- [ ] T039 [P] [US1] Audit `feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/components/GameDetailInfoSection.kt`
- [ ] T040 [P] [US1] Audit `feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/components/GameReleaseInfoCard.kt`
- [ ] T041 [P] [US1] Audit `feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/model/GameDetailUiModel.kt`
- [ ] T042 [P] [US1] Audit `feature/game-detail/src/main/java/com/nikolasguillen/questlog/feature/gamedetail/components/GameDetailActionPill.kt`
- [ ] T043 [P] [US1] Audit `feature/radar/src/main/java/com/nikolasguillen/questlog/feature/radar/RadarScreen.kt`
- [ ] T044 [P] [US1] Audit `feature/radar/src/main/java/com/nikolasguillen/questlog/feature/radar/model/RadarEntryUiModel.kt`
- [ ] T045 [P] [US1] Audit `feature/search/src/main/java/com/nikolasguillen/questlog/feature/search/components/DiscoverHero.kt`

### Verification

- [ ] T046 [US1] Manually run [quickstart.md](./quickstart.md) Scenario 1 end to end on a device/emulator:
  Light selection, brand-hue check, system-bar legibility, Dark selection with no regression, and the
  force-close/reopen check. Also cover two of spec.md's Edge Cases not otherwise exercised: tap Light, Dark,
  Light in quick succession and confirm no glitches or stale appearance (rapid-switching edge case); and
  open a dialog or expand the search bar, then switch appearance, confirming it stays open/expanded and
  otherwise unaffected (in-progress-content edge case). Time the Settings → tap-option → visible-change
  round trip once and confirm it's well under SC-001's 1-second/3-tap budget. Depends on T024-T045 and
  Phase 2.

**Checkpoint**: User Story 1 is fully functional and independently testable — this is the MVP.

---

## Phase 4: User Story 2 - Follow the device's system appearance (Priority: P2)

**Goal**: With "Follow System" selected, the app matches the device's light/dark setting and updates live
if that setting changes while the app is open.

**Independent Test**: Select "Follow System", set the device to light mode and confirm the app shows Light,
then switch the device to dark mode without touching the app and confirm it switches to Dark on its own.

The mechanism is already built in Phase 2 — `MainActivity`'s `when` (T022) resolves `AppearanceMode.SYSTEM`
via `isSystemInDarkTheme()` in the same pass as `LIGHT`/`DARK`, and Android's default behavior (no
`configChanges` declared for `MainActivity` in `AndroidManifest.xml`) recreates the activity on a `uiMode`
change, re-evaluating `isSystemInDarkTheme()` fresh. The "system dark mode unsupported" edge case needs no
code either — `isSystemInDarkTheme()` already defaults to `false` (Light) absent a signal (research.md §7).
This phase is verification, not new implementation.

- [ ] T047 [US2] Manually run [quickstart.md](./quickstart.md) Scenario 2 end to end on a device/emulator:
  confirm the app matches the device's system setting on open in both directions, and updates live within
  roughly a second when the system setting changes while the app is in the foreground. Also cover spec.md's
  backgrounded-change edge case directly: with the app backgrounded (not closed) and "Follow System"
  selected, flip the device's system-wide setting, then bring the app back to the foreground and confirm it
  now matches — distinct from the foreground-live case above, since this path goes through the activity's
  next resume rather than a live recomposition while visible. Depends on Phase 2.

**Checkpoint**: User Story 2 confirmed working — combined with User Story 1, both manual and automatic
appearance selection are independently functional.

---

## Phase 5: User Story 3 - Appearance choice is remembered (Priority: P3)

**Goal**: Once set, the Appearance selection survives app restarts and device reboots without the user
having to pick it again.

**Independent Test**: Select an appearance, force-close the app, reopen it, and confirm both the rendered
appearance and the Settings selector still show the same choice.

Persistence is delivered by the DataStore-backed store built in Phase 2 (T008), already covered by an
automated round-trip test (T009) proving a value survives a fresh `AppearancePreferenceStoreImpl` instance
over the same file — the same guarantee a process restart gives. This phase is the device-level
confirmation of that guarantee, plus the fresh-install default.

- [ ] T048 [US3] Manually run [quickstart.md](./quickstart.md) Scenario 3 end to end: on a clean install (or
  after clearing app data), confirm Settings shows "Follow System" selected already; then select "Light" (or
  "Dark"), force-close and reopen, and confirm both the rendered appearance and the Settings selector still
  show that choice. Depends on Phase 2.

**Checkpoint**: All three user stories are independently verified. The feature is complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification that nothing else broke.

- [ ] T049 Run `./gradlew :app:assembleDebug` (spans modules, touches DI wiring) and `./gradlew test`.
  Confirm every existing suite stays green — `core/data`, `core/domain`, `feature/search`, `feature/radar`,
  `feature/lists`, `feature/game-detail`, `feature/settings`, and `app` — per plan.md's Constitution Check
  (Principle V) and [quickstart.md](./quickstart.md)'s "Build and unit tests" section.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — three independent Gradle file edits, can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001, T002). Internally: `AppearanceMode` (T004) → domain
  port (T005) → use cases (T006, T007) → data impl (T008) → DI binding (T010); color tokens (T011,
  needs T002) → `QuestLogTheme` (T012) → `:app` wiring (T022, needs T006 too); `core:ui` strings (T014) →
  mapper (T015, needs T004); `feature/settings` state/event (T016, T017, both need T004) → ViewModel (T018,
  needs T006, T007) → screen (T020, needs T015, T018, T019) → tests (T021, needs T018). BLOCKS all user
  story phases.
- **User Story phases (Phase 3-5)**: All depend on Foundational (Phase 2) completing. They do not depend on
  each other — US1's audit tasks, US2's verification, and US3's verification can happen in any order or in
  parallel once Phase 2 is done.
- **Polish (Phase 6)**: Depends on all of Phase 2-5 being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on Foundational. The 22 audit tasks (T024-T045) have no dependencies
  on each other.
- **User Story 2 (P2)**: Depends only on Foundational — functionally delivered by it, this phase verifies.
- **User Story 3 (P3)**: Depends only on Foundational — functionally delivered by it (persistence-wise),
  this phase verifies the device-level experience.

### Parallel Opportunities

- T001, T002, T003 (Setup) — three different Gradle files.
- T006, T007 (the two use cases) — different files, both depend only on T005.
- T011, T013 (color tokens, CLAUDE.md doc edit) — independent of each other, though T013 only makes sense
  to commit alongside/after T012 actually lands.
- T016, T017 (UiState field, UiEvent case) — different files.
- All 22 of T024-T045 (the hardcoded-color audit) — 22 independent files, the single biggest parallelization
  opportunity in this feature.

---

## Parallel Example: Phase 2 kickoff

```bash
# After T001/T002 land, these four can start together:
Task: "Create AppearanceMode.kt in core/model"                          # T004
Task: "Add *Light color tokens to Color.kt in core/designsystem"        # T011
Task: "Add appearance_* strings to core/ui strings.xml"                 # T014
# (T005 waits on T004; T012 waits on T002+T011)
```

## Parallel Example: User Story 1 audit

```bash
# All 22 are independent files — launch as many as your team/tooling can run at once:
Task: "Audit core/ui/util/MetallicEffects.kt"        # T024
Task: "Audit core/ui/util/ColorUtils.kt"             # T025
Task: "Audit core/ui/component/RatingBadge.kt"       # T032
Task: "Audit feature/radar/RadarScreen.kt"           # T043
# ...and the remaining 18
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational — this alone already makes Light/Dark/Follow-System selectable and
   persistent app-wide, just not yet audited for leftover dark-only spots
3. Complete Phase 3: User Story 1 (the audit + verification)
4. **STOP and VALIDATE**: run quickstart.md Scenario 1
5. This is the MVP — Light and Dark are both fully correct even before Follow System is separately verified

### Incremental Delivery

1. Setup + Foundational → the whole mechanism exists, functionally complete but unaudited
2. User Story 1 → audited, verified, MVP-ready
3. User Story 2 → verified (no new code)
4. User Story 3 → verified (no new code)
5. Polish → full build + test confirmation

### Notes

- [P] tasks = different files, no dependencies
- This feature's shape is unusual for the template: because Foundational already delivers all
  user-visible behavior, "incremental delivery" here means incremental *verification*, not incremental
  *capability* — there is no way to ship Follow System later than Light/Dark without literally deleting a
  `when` branch, which would just be extra work. Documented in research.md/plan.md rather than treated as
  a process violation.
- Commit after each task or logical group, per the project's normal workflow (no CI/lint gate exists —
  `./gradlew :app:assembleDebug` / `./gradlew test` are the verification, per plan.md).
