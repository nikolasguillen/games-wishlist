# Research: Light Appearance & Theme Preference

**Feature**: `004-light-dark-theme` | **Date**: 2026-09-30

Each entry: Decision → Rationale → Alternatives considered.

## 1. Persistence mechanism for the Appearance preference

**Decision**: Jetpack DataStore Preferences, one `intPreferencesKey`, implemented in `:core:data`.

**Rationale**: `androidx.datastore:datastore-preferences:1.2.1` is already declared in
`app/build.gradle.kts` (`libs.androidx.datastore.preferences`) but has zero usages anywhere in the
codebase — a pre-staged, unused dependency. The preference is a single scalar value with no relations, the
exact shape DataStore Preferences is for. It moves to `:core:data`, where it is actually read/written
through the new port (below); `:app` no longer needs the dependency directly, since `:app`'s only future
touchpoint is a `ViewModel` call through the domain layer, not DataStore itself.

**Alternatives considered**:
- **Room table**: rejected — a single enum value has no relations and doesn't need SQL; Room is already
  used for actual game data, and adding a one-row settings table would be a heavier tool for a lighter job.
- **Raw `SharedPreferences`**: rejected — no existing usage in the codebase, and DataStore is the modern,
  coroutine/`Flow`-native replacement already staged as a dependency.

## 2. Domain boundary shape

**Decision**: A new port interface `AppearancePreferenceStore` in `core/domain/settings/`, implemented as
`AppearancePreferenceStoreImpl` in `core/data/settings/`, bound with `@Binds @Singleton` in the existing
`core/data/di/DataModule.kt` — the same shape `ReleaseNotificationScheduler` / `ReleaseNotifier` already
use for a platform capability that isn't game data.

**Rationale**: `core/domain/repository/` currently holds exactly one interface, `GameRepository`, and
`core/data/CLAUDE.md` is explicit that adding a second repository needs the owner's agreement. An
appearance preference isn't game data by any reading of `GameRepository`'s existing mandate (search,
history, detail, wishlist, lists, Discover, the platform catalogue, Radar's dates), so folding it in there
would stretch that interface past its stated scope. The notification feature already established the
alternative shape for exactly this situation — a narrow port, not a repository — so this feature reuses it
rather than inventing a third pattern. **Flagged explicitly here** (rather than silently added) because the
"second repository" rule exists for a reason and this is the kind of addition it's meant to catch; a port
is judged not to trigger it, but the call is worth a second pair of eyes.

**Alternatives considered**:
- **Extend `GameRepository`**: rejected — pulls an unrelated concern into the one interface the constitution
  explicitly scopes to game data.
- **`feature/settings` reads DataStore directly**: rejected outright — `feature/*` modules are constitutionally
  barred from depending on `:core:data`.

## 3. Where Light vs. Dark is resolved

**Decision**: The boolean `darkTheme` is computed once, at the single production call site
(`MainActivity`'s `setContent`), by combining the persisted `AppearanceMode` with Compose's
`isSystemInDarkTheme()`. `AppearanceMode` is read via `GetAppearanceModeUseCase`, **field-injected directly
into `MainActivity`** (it's already `@AndroidEntryPoint`) and collected with `collectAsStateWithLifecycle()`
— no dedicated `ViewModel`. `QuestLogTheme` itself takes `darkTheme: Boolean` as a plain parameter and stays
ignorant of where that value came from.

**Rationale**: `core/designsystem/CLAUDE.md` currently states *"Do not assume light-mode support or add
`isSystemInDarkTheme()` branches"* inside the theme file — this plan changes the first half of that rule but
keeps the spirit of the second: `QuestLogTheme` stays a pure function of an explicit flag, not a composable
that reaches into system state itself. That keeps every `@Preview` call site (which doesn't have a real
`Activity`/system config to read) fully in control of which variant it renders.

No `ViewModel` sits in between. `GetAppearanceModeUseCase()` is a zero-logic `Flow` pass-through — no
branching, no combined sources — so the two things a `ViewModel` would normally buy here (surviving a
configuration change, and being the only way to reach Hilt) don't apply: a cheap DataStore re-read has no
in-flight state worth preserving across rotation, and Hilt supports field injection into `@AndroidEntryPoint`
Activities directly, which is the only injection style available to an `Activity` regardless (constructor
injection isn't an option for framework components either way). An earlier draft of this plan introduced an
`AppThemeViewModel` for this, reasoning it matched the codebase's ViewModel-per-screen convention; on review
that convention is scoped to *screens* (`feature/CLAUDE.md`), not the app-shell composition root, and the
"VM" bought nothing beyond what `collectAsStateWithLifecycle()` already does directly. Dropped in favor of
the simpler shape.

**Alternatives considered**:
- **Read `isSystemInDarkTheme()` inside `QuestLogTheme`**: rejected — collapses the "Light"/"Dark" explicit
  choices and "Follow System" into the same code path, and previews would no longer be able to force a
  specific variant.
- **A dedicated `AppThemeViewModel`**: considered and rejected (see above) — adds a file, a `hiltViewModel()`
  call, and `:app`'s "first ViewModel" as a thing to explain, for a `Flow.stateIn(...)` that
  `collectAsStateWithLifecycle()` already provides for free at the collection site.

## 4. `QuestLogTheme` signature change

**Decision**: `fun QuestLogTheme(darkTheme: Boolean = true, content: @Composable () -> Unit)`.

**Rationale**: `QuestLogTheme(` is called from dozens of `@Preview` functions across every module.
Defaulting the new parameter to `true` (today's only behavior) means none of those call sites need to
change to keep compiling and rendering exactly as before; only `MainActivity` passes the resolved value
explicitly. Individual previews can opt into `darkTheme = false` later where a light-mode preview adds
value, but that is not required for every existing preview as part of this feature.

**Alternatives considered**:
- **Require every call site to pass `darkTheme` explicitly**: rejected — churns ~15+ preview files for no
  behavioral gain within this feature's scope.

## 5. System status bar / navigation bar icon color

**Decision**: Move the icon-color decision out of `MainActivity.onCreate`'s one-time `enableEdgeToEdge(...)`
call and into a `SideEffect` inside `QuestLogTheme`, using `WindowCompat.getInsetsController(window,
view).isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars`, keyed off the same `darkTheme`
parameter. `enableEdgeToEdge()` itself still runs once in `onCreate` (it sets up edge-to-edge layout, not
per-frame icon color) but no longer hardcodes a style. Requires adding `androidx.core:core-ktx`
(`libs.androidx.core.ktx`, already in the version catalog) as a new library dependency of
`:core:designsystem`.

**Rationale**: `MainActivity.kt:49-51` currently calls `enableEdgeToEdge(statusBarStyle =
SystemBarStyle.dark(TRANSPARENT))` exactly once, in `onCreate`, before `setContent`. That's fine for a
permanently-dark app, but FR-007/FR-008/FR-012 require the icons to update **without a restart** both when
the user flips the in-app selector and when "Follow System" tracks a live system change. A `SideEffect`
inside `QuestLogTheme` re-runs on every recomposition where `darkTheme` changes, covering both cases with
one mechanism. Placing it in `QuestLogTheme` (rather than duplicating it at each future call site) keeps it
co-located with the color scheme it has to stay in sync with, and doesn't add a new composable — designsystem's
"no composables beyond `QuestLogTheme`" rule still holds, since this lives inside the existing one.

**Alternatives considered**:
- **Leave the one-time `enableEdgeToEdge` call as the only mechanism**: rejected — fails FR-007/FR-012
  outright, since `onCreate` runs once per process, not once per appearance change.
- **Put the `SideEffect` in `MainActivity`/`MainContent` instead of `QuestLogTheme`**: viable, but rejected
  in favor of `QuestLogTheme` owning it, since every future consumer of the theme gets correct system-bar
  behavior for free rather than having to remember to wire it up again.

## 6. `AppearanceMode` default and persistence encoding

**Decision**: `core/model` enum `AppearanceMode(val id: Int) { SYSTEM(0), LIGHT(1), DARK(2) }`, with a
`fromId(id: Int)` companion falling back to `SYSTEM` for an unrecognized id — the same shape as
`GameStatus.fromId()`. `SYSTEM` is both the enum's fallback and the app's shipped default (per the spec's
Clarifications: the app is pre-release, so every install — there being no existing-user population —
starts on `SYSTEM`).

**Rationale**: Persisting the stable `id`, not the enum name, matches the existing convention
(`GameStatus`, presumably others) and survives a future rename of the Kotlin constant without a migration.

**Alternatives considered**: None materially different — this is the codebase's one established pattern for
a persisted enum.

## 7. "System dark mode unsupported" fallback

**Decision**: No special-case code. `isSystemInDarkTheme()` already returns `false` (i.e., resolves to
Light) on any device/configuration where no `UI_MODE_NIGHT_YES` signal is present.

**Rationale**: This is exactly the fallback the spec's edge cases ask for, and it's Compose's existing
default behavior — nothing to build.

## 8. Appearance selector UI

**Decision**: Reuse `core/ui/component/CustomSegmentedButton` inline in a new "Appearance" `SettingsGroup`
on the existing Settings screen. No new route, no new sub-screen.

**Rationale**: `AppColors` already declares `segmentedButtonSelectedColor` / `segmentedButtonSelectedContentColor`
token slots, currently provided by `QuestLogTheme` but not consumed by any composable yet — `grep` for
`segmentedButtonSelectedColor` usage outside the theme file returns only `CustomSegmentedButton.kt` itself.
Three short, mutually exclusive, always-visible options (Light / Dark / Follow System) are exactly what
`CustomSegmentedButton`'s single-choice row is for, and it already visually indicates the current selection
(FR-011) without extra code. This also means no new `NavKey` in `core/navigation/Routes.kt` and no new
`entryProvider` branch in `:app` — the smallest structure that satisfies FR-001/FR-011.

**Alternatives considered**:
- **A dialog via `CustomAlertDialog`**: rejected — heavier interaction for a 3-way toggle that's meant to be
  glanceable and instantly reversible, and hides the current selection behind a tap instead of showing it
  inline.
- **A management sub-screen** (the `OwnedPlatformsScreen` / `ReleaseNotificationsScreen` shape): rejected —
  that shape earns its keep for an arbitrary-length list; three fixed options don't need it.

## 9. Label text for the three options

**Decision**: `AppearanceMode.toLabelUiText(): UiText` as a new top-level extension in
`core/ui/mapper/AppearanceModeMapper.kt`, with the three strings added to `core/ui`'s own `strings.xml`.

**Rationale**: Matches the existing `GameStatus.toLabelUiText()` (`core/ui/mapper/GameUiMapper.kt`) and
`WishlistIcon.toDrawableRes()` (`core/ui/mapper/WishlistIconMapper.kt`) precedent — a `core:model` enum's
UI label is mapped in `core:ui`, not inline in the feature, and not hardcoded per the root `CLAUDE.md`'s
"never hardcode display text" rule.

## 10. Existing hardcoded colors outside the theme layer

**Decision**: Not resolved here — flagged as a required audit, to be enumerated file-by-file in `tasks.md`.

**Finding**: `Color(0x...)` literals outside `core/designsystem/theme/` appear in 9 files (`MetallicEffects.kt`,
`ColorUtils.kt`, `PlatformVisuals.kt`, `PlatformTile.kt`, `GameDetailInfoSection.kt`,
`GameReleaseInfoCard.kt`, `GameDetailUiModel.kt`, `RadarScreen.kt`, `RadarEntryUiModel.kt`).
`Color.Black`/`Color.White` literals outside the theme layer appear in 14 files, including
`MetallicModifiers.kt`, `VisualModifiers.kt`, `FullScreenImageViewer.kt`, `CustomFilterChip.kt`,
`RatingBadge.kt`, `ImageGalleryPager.kt`, `ImmersiveDetailLayout.kt`, the `gamecard/` family, and others.

**Rationale for deferring**: Not every hit is a bug. Several are theme-independent by design — a full-bleed
image viewer's black chrome, or a `Color.Black.copy(alpha = ...)` scrim behind text overlaid on a game
cover, are expected to stay dark regardless of the app's own appearance, the same way a photo viewer in a
light-themed app usually keeps a black surround. Others may genuinely assume the current dark background
and need to route through `MaterialTheme.colorScheme` / `MaterialTheme.appColors` instead. Sorting one from
the other requires reading each file's actual usage, not a blanket rule — that work belongs in `tasks.md`,
one task per file/decision, not invented here.

## 11. Deriving light-mode color tokens from the existing Gold brand color

**Decision**: The exact hex values for the Light `ColorScheme` and `AppColors` are chosen during
implementation and verified against WCAG 2.1 AA (4.5:1 normal text / 3:1 large text & icons) with a
contrast-ratio check before merging — not hardcoded in this plan. The Light variant reuses `Gold` at the
same hue (per FR-004) primarily as a container/accent fill with dark content on top, mirroring the existing
dark scheme's own `OnPrimaryDark = Color.Black` pairing, rather than as small body text directly on a light
surface (a pairing that hue alone can't guarantee passes AA).

**Rationale**: Contrast-safe color picking is content work best done with the actual token file open and a
checker running, not guessed at in a planning document. What the plan fixes is the *constraint*
(FR-004 + FR-010 together: same hue, but AA-safe placement) and the *verification method*; the tokens
themselves are a `tasks.md` line item.
