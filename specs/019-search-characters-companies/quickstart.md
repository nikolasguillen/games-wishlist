# Quickstart: Validating Search by Character and Developer

## Prerequisites

- IGDB credentials in `local.properties` (`IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`).
- **Back up before installing**: the translation cache's key changes ([research.md R7](./research.md)), and with the
  database pinned to `version = 1`, a device that already has the app crashes at launch on the new build until its
  data is cleared (Settings, Apps, Storage, Clear data, or a reinstall). In the currently installed build, go to
  Settings, then Account & backup, then Back up now, and restore after clearing.
- macOS for the iOS steps. On Windows use `.\gradlew.bat`, and skip the `ios*` tasks and `xcodebuild`.

## Build and tests

```bash
./gradlew :core:network:testAndroidHostTest --console=plain -q    # new DTOs and service paths
./gradlew :core:data:testAndroidHostTest --console=plain -q       # queries, paging, mapping, translation key
./gradlew :core:domain:testAndroidHostTest --console=plain -q     # developer ranking, game ordering
./gradlew :core:navigation:testAndroidHostTest --console=plain -q # route round trips
./gradlew :feature:search:testAndroidHostTest --console=plain -q  # SearchViewModel types, EntityDetailViewModel
./gradlew :feature:game-detail:testAndroidHostTest --console=plain -q
./gradlew :shared:testAndroidHostTest --console=plain -q          # KoinGraphTest
./gradlew test                                                    # everything
./gradlew :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q          # macOS
```

Expected: everything green, and `core/database/schemas/…/1.json` regenerated and committed with the entity change.

## Scenarios (Android, then iOS on a Mac)

The cases in parentheses are the spec's user stories and acceptance scenarios: **US** is the user story and **AS** the
acceptance scenario.

| # | Steps | Expected |
|---|---|---|
| S1 | Open Search and tap the bar (US1 AS1). | Three chips under the bar, with Games checked. The hint reads "Search for a game…". Genre browse, recent searches and recently viewed are below. |
| S2 | Tap Characters, then Developers, then Games (US1 AS2–3). | Exactly one chip checked each time, and the hint follows it. The content below the chips does not change. |
| S3 | With Characters selected, type "kratos" (edge case: suggestions). | Matching recent searches and "See all results" show, but no "Games" suggestion rows and no loading rows. Switching to Games while typing brings game suggestions back. |
| S4 | Submit "kratos" with Characters selected (US2 AS1). | A grid of circular portraits with Kratos first. Young Kratos has the person placeholder (US2 AS2). The count reads "2 characters found", with no sort or filter buttons. |
| S5 | Tap Kratos (US2 AS3, US4). | A full-screen page with no rail: back arrow, header (portrait, "Kratos", a game-count chip equal to the number of cards in the grid), description card, then a "Games" grid, newest first. |
| S6 | On the page, tap translate (US4 AS2). Do it on a device language other than English, with the model ready. | Shimmer, then the translated text with the engine badge. "Show original" restores it. The game-detail translation still works on a game whose id equals a character's (for example game 2134). |
| S7 | Tap the heart on a game, then long-press another (US4 AS5). | A snackbar names the default wishlist, and the list selector opens and saves as in Search. Back on the results, the cards are unaffected. Open the game, go back twice, and the search query, Characters chip and scroll position are intact (US4 AS6). |
| S8 | Select Developers and submit "capcom" (US3 AS1, research R2). | Rounded logo tiles on light tiles in both themes, with Capcom first. The page shows a "Founded <year>" chip and about 509 games (the count on 2026-10-20), all of which load (scroll to the end). |
| S9 | Submit "naughty dog" as Developers and open it. | 31 games, none of them a port, bundle or edition. |
| S10 | With Characters results on screen, open the bar, pick Developers and do not submit (US1 AS5). | The grid still shows characters until submit. Submitting replaces it with developers. |
| S11 | With Characters selected, tap a genre chip (FR-022). | Games results for the genre, and the Games chip is checked when the bar is reopened. |
| S12 | With Developers selected, tap a recent search (US1 AS6). | It is searched as a developer. |
| S13 | Submit a nonsense query for each type (US2 AS4, US3 AS5). | "No characters found…" or "No developers found…", and the games message is unchanged. |
| S14 | Airplane mode and submit as Characters, then restore and retry (US5 AS1). | Error with retry, and retry searches characters again. |
| S15 | Open a developer page, cut the network while the games load, then restore and retry (US5 AS2). | Header and description stay, and the games area shows its own error and recovers on retry. |
| S16 | On a window at least 840dp wide, open a game from Search (two panes), then switch to Characters and open a character. | The page covers both panes, without the rail. Back returns to the two panes with the game still open. |
| S17 | TalkBack or VoiceOver over the chips and the cards (FR-020). | Chips announce their name and selected state. Cards announce "Character, Kratos" or "Developer, Capcom" as one button. |
| S18 | Kill and relaunch the app. | Games is selected again. |
