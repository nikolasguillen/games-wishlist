# Backlog

Unrefined points: bugs, ideas, chores and improvements that are not scheduled. This is the inbox before
`docs/tech-debt.md` (deviations from `CLAUDE.md`) and `docs/roadmap.md` (features with an agreed shape).

**For AI agents:** these are notes, not tasks. Do not start on an item unless the user asked for it.

**When an item is done or dropped, delete it.** No "fixed in" note; `git log` is the history.

## Bugs

### Release notification card hidden for games saved only in a non-default list
- **Added:** 2026-11-03
- **Where:** `feature/game-detail/.../mapper/GameDetailUiMapper.kt:188` (`Game.isSaved`, feeds `isNotificationAvailable`), shown in `components/GameDetailSuccessContent.kt:165`
- **Platform:** all
- **Steps:** save a game to a list that is not the default wishlist, with no status or priority, then open its detail
- **Expected / Actual:** expected: the notification card appears for a game saved in any list; actual: it appears only if the game is in the default wishlist or has a status or priority
- **Hypothesis:** the `Game` model carries no flag for non-default list membership, so the gate cannot see it (unverified; the mapper's KDoc says the same). The eligibility check in `SyncReleaseNotificationsUseCase` already covers every list

### Mouse wheel does not scroll in Discover (and maybe other screens)
- **Added:** 2026-11-03
- **Where:** the Discover feed in `feature/search`; other screens may be affected and are not identified
- **Platform:** Desktop
- **Expected / Actual:** `specs/020-desktop-app/spec.md:74` requires list-like screens to scroll with the wheel or trackpad; Discover does not
- **Hypothesis:** Discover shelves scroll sideways, and a wheel only moves the vertical axis (unverified)

## Features

### Upcoming releases widget
- **Added:** 2026-11-03
- **Why:** see the next releases from the home screen without opening the app
- **Scope:** an Android widget listing the next upcoming saved games, fed by the Radar release data; iOS is out of scope for now; no widget code exists yet
- **Where:** new Android code; data from the `:core:domain` radar use cases
- **Notes:** a new module or dependency edge must be called out first

### How Long To Beat data on game detail
- **Added:** 2026-11-03
- **Why:** show how long a game takes to finish, next to the rest of the detail info
- **Scope:** a time-to-beat figure on game detail, from IGDB's own time-to-beat data; no second data source
- **Where:** `:core:network` (`IgdbApiService`), `:core:data` (`GameMapper`), `feature/game-detail`
- **Notes:** IGDB's endpoint and field names are not checked against the current API docs

### Taste statistics in Settings (most liked genres, developers)
- **Added:** 2026-11-03
- **Why:** let the user see what the app infers about their taste
- **Scope:** a read-only stats view in Settings listing top genres and developers; "liked" still needs a definition
- **Where:** `feature/settings`; builds on `TasteProfile` and `TasteProfileBuilder` in `:core:domain`
- **Notes:** `TasteProfile` is keyed by IDs, so names must be resolved in the UI layer. Developer counts are split across sub-studios (see the multi-studio entry in `docs/tech-debt.md`). `docs/roadmap.md` says a Settings row needs a backend first; a read-only view over `TasteProfile` may qualify

### Per-list default game state: one list per game, or status tied to the list
- **Added:** 2026-11-03
- **Why:** each list should be able to define a "default state" that a game gets when added to it
- **Scope:** a design decision first, no code yet. Option A: a game can be in only one list. Option B: a game's status belongs to the list membership, not the game. Pros and cons are still to be discussed
- **Where:** `core/database` (`GameListCrossRef`, `DefaultWishlistEntity`, `games.status`), `ToggleWishlistUseCase`, `ListSelectorSheet`, `feature/lists`
- **Notes:** today `status` and `priority` live on the game itself, so a game in two lists shares them. Status is also read by the wishlist sections, by `TasteProfileBuilder` weights and by the "saved" query in `GameDao`. The cloud backup stores lists, so a schema change touches backup and restore

### Sign-in step in the welcome flow (first launch only, flow no longer skippable there)
- **Added:** 2026-11-07
- **Why:** let a new user sign in during first launch, so the cloud backup is available from the start instead of having to be found later in Settings → Account & backup
- **Scope:**
  - A new last page in the welcome flow (after `Platforms` and `Reminders`) with the existing sign-in providers.
  - On first launch the flow can no longer be skipped: the "Skip" button goes away on every page, which changes today's behaviour (`OnboardingScreen.kt:165` shows it on every page, citing FR-004, "a way out from anywhere").
  - When the flow is replayed from Settings ("Show welcome tour"), the sign-in page is left out and Skip stays available.
- **Where:** `feature/onboarding` (`OnboardingPage` is the closed set of pages, `buildOnboardingPages` in `mapper/OnboardingPageUiMapper.kt` decides which pages a launch shows, `OnboardingViewModel` handles `SkipClicked`, `OnboardingScreen.kt`); `core/navigation/Routes.kt` (`OnboardingRoute`); `shared/.../QuestLogNavDisplay.kt` (the `OnboardingRoute` entry and the Settings `onShowWelcomeTourClick`)
- **Notes:**
  - *Replay is not detectable today:* `OnboardingRoute` is a `data object`, so first launch and replay open the same route. The only difference is in `QuestLogNavDisplay.kt`'s `onFinish`, which checks `backStack.size == 1`. The page list and the Skip button both depend on it, so the feature needs an explicit signal, e.g. a route parameter (`OnboardingRoute` would stop being a `data object`, which affects `RoutesSerializationTest` and `GameNavSavedStateConfiguration`) or a ViewModel argument.
  - *Reuse:* `SignInUseCase`, `GetSignInProviderUseCase` and `ObserveAccountUseCase` are already in `:core:domain`, so `feature/onboarding` needs no new dependency edge. `ProviderSignInButton` lives in `feature/settings`, which onboarding must not depend on, so it would have to move to `:core:ui`. `AccountBackupViewModel` is the reference for sign-in errors and in-progress states.
  - *Not skippable means a blocking sign-in:* the last page then has to be completed by signing in, so a failed or cancelled sign-in, being offline, or a platform whose provider is unavailable must not trap the user. Decide what the exit looks like (e.g. a retry with an explanation) before building. Also decide what happens when the account already has cloud data (restore prompt?).
  - *Platforms:* the providers differ per platform (Google on Android and desktop, Apple on iOS), and the Apple half has never run on a device (see `docs/tech-debt.md`).
  - *Already signed in:* the owner says a signed-in user should never reach the first-launch flow, so no special case is planned.
  - *Spec:* dropping Skip reverses a deliberate requirement (FR-004), so this needs its own spec.

## Tech debt

## Improvements

### Responsive layouts from the Claude Design mockups (desktop and tablets)
- **Added:** 2026-11-03
- **Where:** `feature/search/SearchScreen.kt` (expanded search first), then the other screens; `desktopApp`; the two-pane scene in `shared/.../SearchDetailScene.kt`
- **Platform:** large windows: desktop and tablets
- **Now → Better:** the expanded search layout is broken on large screens, and the desktop app mirrors the mobile layout; desired: layouts from the mockups, chosen by the space a screen is given, not by device type
- **Why:** one set of layouts serves desktop and tablets
- **Notes:** the broken expanded search is the first screen to fix. `specs/020-desktop-app/spec.md:18` says the goal was parity, not a redesign, so this needs its own spec. The mockups live in the owner's Claude chat, not the repo; where to keep them is to be decided. `specs/017-search-landscape-layout` and `specs/018-two-pane-search-detail` already cover Search on wide windows

### Haptic feedback on the like button in game detail
- **Added:** 2026-11-03
- **Where:** `feature/game-detail` (the favourite action fed by `GameDetailSuccessContent.kt:88`); the existing haptic is in `core/ui/.../gamecard/SaveToWishlistButton.kt`
- **Now → Better:** cards give `HapticFeedbackType.Confirm` on tap; the detail screen's toggle has no haptic call
- **Why:** consistent tactile confirmation

### Higher-resolution hero images on desktop
- **Added:** 2026-11-03
- **Where:** `core/data/.../mapper/GameMapper.kt` (`toIgdbImageUrl`, default `t_720p`), shown by `GameDetailHeroHeader` and `ImageGalleryPager`
- **Platform:** Desktop
- **Now → Better:** artwork and screenshots are requested at 720p; larger desktop windows likely look soft (unverified)
- **Why:** sharper hero on big windows without wasting bandwidth on phones

### Split wishlist games into released and upcoming
- **Added:** 2026-11-03
- **Where:** `feature/wishlist/.../mapper/WishlistUiMapper.kt` (`toWishlistSectionUiModel`), `StatusSectionHeader`
- **Now → Better:** a list is grouped by status only; desired: separate released from not-yet-released games
- **Why:** see at a glance what can be played now
- **Notes:** the design has open catches that are still to be discussed; how it fits with the status grouping is undecided

### Anchor the profile button at the bottom of the navigation rail on desktop and tablets
- **Added:** 2026-11-04
- **Where:** `shared/.../QuestLogNavigationSuite.kt` (the rail); `core/ui/.../component/MainScreenHeader.kt` (draws `ProfileIconButton` in the header's trailing slot); `shared/.../QuestLogNavDisplay.kt` (passes `onProfileClick` to Search, Radar and Lists)
- **Platform:** Desktop and tablets
- **Now → Better:** the profile button, which opens Settings, sits in the header of every top-level screen; desired: where the rail is shown on a large window it is anchored at the bottom of the rail and no longer appears in the header at all
- **Why:** a fixed position is visually easier to reach
- **Notes:** `MainScreenHeaderDefaults.Height` and the header's profile slot exist so top-level screens line up, so removing the button from the header on a wide window changes that slot too. Undecided: a phone in landscape also gets the rail, but its header is already crowded, so whether it follows is to be decided. Unverified: the rail comes from `NavigationSuiteScaffold`, whose items may not offer a bottom-anchored slot, so this could need a custom rail layout. Related to "Responsive layouts from the Claude Design mockups (desktop and tablets)"

### Show more cards per Discover row on wide windows (10 to 12 instead of 6)
- **Added:** 2026-11-04
- **Where:** `feature/search/.../mapper/DiscoverMapper.kt` (`DISCOVER_SHELF_ROW_SIZE = 6`, applied by `toDiscoverContentState` through `take` and `hasMore`); `DiscoverMapperTest.kt` asserts the value; the wide rows are `WideDiscoverFeed.kt` (spec `specs/021-discover-wide-layout`)
- **Platform:** Desktop and tablets (windows of at least 840dp)
- **Now → Better:** every Discover row is capped at 6 games, so on a wide window the row ends well before the edge; desired: a wide window shows more, around 10 to 12
- **Why:** the width is there to be used
- **Notes:** the cap is applied in the mapper, which both layouts share and which only sees the games, not the window, so a larger wide-only row means either passing the width class to the mapper or keeping more games in the state and trimming them in the UI; the compact feed must stay at 6. `specs/021-discover-wide-layout/spec.md` deliberately keeps rows at 6 (layout only, no state change), so this needs its own spec. "See all" and its grid would need to keep starting from the row's games. Unverified: how many games each lane actually holds, since the feed fetches the whole lane but its size was not checked

### Refetch a cached game detail when IGDB has newer data
- **Added:** 2026-11-05
- **Where:** `core/data/.../repository/GameRepositoryImpl.kt:628` (`refreshGameDetail`, the `detailsFetchedAt != null` branch), `GameEntity.detailsFetchedAt`
- **Platform:** all (seen on one device, expected everywhere: the code is shared)
- **Now → Better:** once a game's detail has been fetched, `refreshGameDetail` never calls the network for it again, so a game seen earlier keeps its old cover, artworks and screenshots. Discover has a 6 h TTL (`DISCOVER_LANE_CACHE_TTL`), so it showed the new image of The Elder Scrolls 6 while game detail showed the old one. Desired: when a cached game is opened, compare IGDB's `updated_at` with the saved one; if IGDB's is newer, clear the cached detail entry so that the next time the user opens the game it is fetched again.
- **Why:** the two screens disagree about the same game, and a cached detail never heals.
- **Notes:**
  - *Check:* a one-field query (`fields updated_at; where id = X;`), not a hash. IGDB's field name is not checked against the current API docs.
  - *What's missing:* `updated_at` is neither requested nor stored today. Storing it needs a new column on `GameEntity` (Room migration) and probably a field in the backup mapper, since backup stores `detailsFetchedAt`.
  - *How to clear:* resetting `detailsFetchedAt` to null is the likely way, since `refreshGameDetail` already treats a null as "catalogue result, fetch again". The check runs on open and the refetch on the next open, so the stale detail shows once more first.
  - *Open question:* whether the check runs on every open or at most once per some interval.
