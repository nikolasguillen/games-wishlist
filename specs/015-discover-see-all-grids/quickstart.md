# Quickstart: See-All Grids for Discover Rows

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

How to check that the feature works, from the unit suites to a hands-on run on both platforms. The behaviour
being checked is defined in [spec.md](spec.md) and [data-model.md](data-model.md). This guide only says how
to observe it.

## Prerequisites

- `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` in `local.properties`.
- An Android emulator or device (API 29+). For the iOS steps you need a Mac with Xcode and an iOS simulator.
- TalkBack (Android) and VoiceOver (iOS) for the accessibility step.
- A library with at least three saved games and a recurring developer or genre, so the feed has personalised
  shelves. A fresh install shows only the generic shelves.

## 1. Unit suites

```bash
./gradlew :core:domain:testAndroidHostTest --console=plain -q    # SearchResultFacetsTest: unchanged, still green after the visibility change
./gradlew :feature:search:testAndroidHostTest --console=plain -q # DiscoverMapperTest (new) + SearchViewModelTest (new shelf cases, updated accessors)
./gradlew test                                                   # everything, before each commit
./gradlew :androidApp:assembleDebug                              # strings and shared UI change
```

Expected: all green, with no existing test deleted or weakened. `KoinGraphTest` is untouched, because no
constructor changes.

## 2. Static checks

```bash
# Only values/ exists, so the one new string needs no translation. Re-check if a values-xx/ appears.
ls feature/search/src/commonMain/composeResources/ core/ui/src/commonMain/composeResources/

# The new string is defined.
grep -n 'discover_see_all_shelf' feature/search/src/commonMain/composeResources/values/strings.xml

# No literal display text in the touched composables: expect no Text("...") or contentDescription = "...".
grep -nE 'Text\(\s*"|contentDescription\s*=\s*"' \
  feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt \
  feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchBars.kt \
  feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/SearchMainContent.kt

# No new dp literal in the shelf row (the existing 140.dp card width and the refresh prompt's literals predate this).
git diff develop -- feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt | grep -n '^+.*[0-9]\.dp'

# The cap is not applied in a composable.
grep -n 'take(' feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/components/DiscoverFeed.kt
```

Expected: one `values` directory in each module, the string present, and no matches from the last three
commands, except that the existing `Modifier.width(140.dp)` card width may show in the dp diff if the
`LazyRow` is moved within `DiscoverShelf`. That literal predates this feature and is not a new one. The
literal-text grep only catches the common forms, so review the diff of those three files as well.

## 3. Hands-on, Android

Install the debug build. Each row gives the action, then what you should see.

| # | Do | Expect |
|---|----|--------|
| 1 | Open Search. | Every shelf (personalised, "Most anticipated", "Popular this month") shows **at most six** cards. The hero card is unchanged above them. |
| 2 | Look at each title row. | A shelf with more than six games has an arrow at the end of its title row. A shelf with six or fewer has none, and tapping its title does nothing. |
| 3 | Note the first six cards of "Popular this month", then tap its title row. | No spinner. The feed gives way to a grid whose **first six cards are the same six, in the same order**, followed by the rest. The bar is empty and reads "Popular this month". X and the back arrow are visible. |
| 4 | In the grid, open a card, go back; save a card; long-press another. | The game opens and back returns to the grid. A snackbar confirms the save. The list selector opens. All the same as a text search. |
| 5 | Open Sort. | The first option reads **Top picks**, selected and descending, and the grid is in shelf order. Pick ascending: the order reverses. Pick Rating, then close the grid (X). Run a text search and open Sort: **Relevance**, with Rating still selected. |
| 6 | Open a shelf grid, open Filter. | Platforms, genres and game types from the shelf's games. No genre is missing. Apply a platform and a genre: the grid narrows to games with both. Apply a combination nothing matches: "no matches" with "Clear filters". |
| 7 | Scroll the feed down to "Popular this month", open it, then tap X. | The feed returns **at the position you left**. Repeat with the back arrow, then with the system back gesture. |
| 8 | Open a shelf grid, then type "zelda" and submit. | Text results with the default placeholder. "zelda" is in Recent searches and the shelf is **not**. Repeat: open a grid, open the bar, tap a genre chip. That genre browse replaces the grid. |
| 9 | Open two personalised shelves in turn. | Each grid shows its own games and its own title as the placeholder. |
| 10 | Turn on airplane mode, return to Search, open a shelf grid. | The grid opens exactly as online. Covers that were never downloaded show the usual placeholder. |
| 11 | With a grid open, rotate the device; then background the app and return. | The same grid, placeholder, sort and filters, with no flash of the feed. Close it: the feed is at its old position. |
| 12 | Tap a shelf's title row twice quickly. | One grid; nothing flickers. |
| 13 | With a stale feed (change a saved game's status so the refresh prompt shows), open a grid, then go back. | The prompt is not over the grid. It is still on the feed when you return. |
| 14 | TalkBack on: swipe to a shelf's title row with an arrow, then to one without. | First: **"See all: Popular this month, Button"**, announced once and not once for the title and again for the arrow. Second: just the title, as a heading-like text. Double-tap the first: the grid opens. Focus the bar: it announces the shelf name. |
| 15 | Set the device to an RTL language (e.g. Arabic) or enable "Force RTL layout direction". | The arrow sits at the row's end and points the other way. |
| 16 | Developer options → "Show layout bounds" on a title row with an arrow. | The row is at least 48dp tall. |

## 4. iOS (Mac only, unverified until run)

These steps can only be run on a Mac. Until they have been, they are recorded as **not run**, not as passed.

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

Run the app on the simulator and repeat steps 1–9 and 12–13.
- Step 10 cannot be run on a simulator without taking the Mac offline (see `docs/tech-debt.md`). The grid
  makes no request, so the step is low-risk, but record it as not run.
- Step 11: there is no rotation-driven recreation on iOS, so check that backgrounding and returning keeps the
  grid.
- Step 14 with VoiceOver: it should say "See all: …, button" once.
- Step 15 with the simulator's right-to-left pseudolanguage.
- Step 16: `minimumInteractiveComponentSize()` is new to this codebase, so confirm the row's height with the
  Xcode view debugger.
