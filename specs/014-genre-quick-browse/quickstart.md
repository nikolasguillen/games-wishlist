# Quickstart: Genre Quick-Browse in Search

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

How to check that the feature works, from the unit suites to a hands-on run on both platforms. The
behaviour being checked is defined in [contracts/](contracts/) and [data-model.md](data-model.md); this
guide only says how to observe it.

## Prerequisites

- `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` in `local.properties`.
- An Android emulator or device (API 29+). For the iOS steps: a Mac with Xcode and an iOS simulator.
- TalkBack (Android) and VoiceOver (iOS) available for the accessibility step.

## 1. Check the IGDB data before writing the strings and constants

Do this once, before `KnownGenre` and the thresholds are committed. Use an app access token obtained with
your own credentials (the same `client_credentials` call `IgdbAuthService` makes).

1. **Genre ids**: `POST https://api.igdb.com/v4/genres` with body `fields id, name; limit 50;`. Every
   `id`/`name` pair must match the `KnownGenre` table in [data-model.md](data-model.md). A genre IGDB has
   and the table lacks is not a blocker (it falls back to IGDB's name), but add it if one turns up.
2. **Thresholds**: for each default genre (12, 5, 31, 15, 32) and two niche ones (30 Pinball, 26 Quiz),
   `POST /v4/games/count` with the `where` clause from [contracts/genre-browse.md](contracts/genre-browse.md).
   Expected: the five defaults return a full grid's worth (dozens or more). The niche genres may return few
   or none; that case is accepted (spec Edge Cases). If a **default** genre comes back thin, widen
   `GENRE_BROWSE_RELEASE_WINDOW_YEARS` or lower `GENRE_BROWSE_MIN_RATING_COUNT` and write the reason in the
   constant's comment.

## 2. Unit suites

```bash
./gradlew :core:domain:testAndroidHostTest --console=plain -q    # GetBrowseGenresUseCaseTest, BrowseGenreUseCaseTest, GetTasteProfileUseCaseTest (unchanged, still green)
./gradlew :core:data:testAndroidHostTest --console=plain -q      # GameRepositoryImplGenreBrowseTest
./gradlew :core:ui:testAndroidHostTest --console=plain -q        # GenreMapperTest
./gradlew :feature:search:testAndroidHostTest --console=plain -q # SearchViewModelTest (new browse cases + existing)
./gradlew :shared:testAndroidHostTest --console=plain -q         # KoinGraphTest: the two new use cases are bound
./gradlew test                                                   # everything, before each commit
./gradlew :androidApp:assembleDebug                              # DI wiring spans modules
```

Expected: all green, with no existing test deleted or weakened.

On a Mac, also: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q`.

## 3. Hands-on, Android

Install the debug build. Each line gives the action, then what you should see.

| # | Do | Expect |
|---|----|--------|
| 1 | Fresh install (or clear data), open Search, tap the bar. | "Browse by genre" above the empty-history message: **RPG, Shooter, Adventure, Strategy, Indie**. |
| 2 | Type "ze". | The genre section disappears; suggestions take over. Delete the text and it comes back. |
| 3 | Tap **RPG**. | The overlay closes, a spinner shows, then a grid of released RPGs from the last two years, most rated first. The bar is empty and reads "genre: RPG". X and the back arrow are both visible. |
| 4 | Save a card; long-press another. | Snackbar "Added … to Wishlist"; the list selector opens. Same as a text search. |
| 5 | Open Sort. | The first option reads **Top picks**, selected and descending. Pick Rating, then close. Clear the browse (X). Open Sort again on a text search: **Relevance**, with Rating still selected. |
| 6 | Browse **Shooter**, open Filter. | "Shooter" is not in the genre group. Select another genre and a platform, apply: the grid narrows to games that have all of them. |
| 7 | Tap X. | Discover feed, default placeholder, no X, no arrow. Repeat with the back arrow, then with the system back gesture. |
| 8 | Browse a genre, then type "zelda" and submit. | Text results; the placeholder is back to default; "zelda" is in Recent searches and the genre browse is **not**. |
| 9 | Tap **RPG** and, within a second, reopen the bar and tap **Indie**. | Only Indie results ever appear; the placeholder reads "genre: Indie". |
| 10 | Turn on airplane mode, browse a genre. | Error page. Turn airplane mode off and tap Retry: the **same genre** loads. |
| 11 | During a browse, rotate the device; then background the app and come back. | The same genre, results, placeholder, sort and filters; no flash of the Discover feed. |
| 12 | Save a few games of other genres (e.g. two Puzzle games marked Playing), reopen the bar. | Puzzle leads the row, followed by the defaults that are not already present; five chips, no duplicates. |
| 13 | Mark a Puzzle game Dropped so the genre's weight goes negative. | Puzzle no longer leads on the strength of that game. |
| 14 | TalkBack on: focus a genre chip; then focus the bar during a browse. | "RPG, Button", and never "selected". The bar announces "genre: RPG". |

## 4. Hands-on, iOS (Mac only)

Build with the `xcodebuild` command in the root `CLAUDE.md`, run it on the simulator and repeat steps 1–12.
Step 10 cannot be run on a simulator (see `docs/tech-debt.md`); the Darwin error mapping is unit-tested.
Step 11: there is no rotation-driven recreation on iOS, so check that backgrounding and returning keeps the
browse. Step 14 with VoiceOver.

Any step not run on a platform is written down as not run, not assumed to pass.
