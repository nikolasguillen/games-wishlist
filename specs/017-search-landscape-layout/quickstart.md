# Quickstart: Search Landscape Layout

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

How to prove the feature works. The geometry rules are in [data-model.md](data-model.md), and the reasons
for them are in [research.md](research.md).

## 1. Prerequisites

- `local.properties` with `IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` (search needs the network).
- An Android emulator with rotation enabled (Pixel 7 class, API 35+). For the cutout rows, turn on
  **Developer options → Display cutout → Punch hole** (or "Tall cutout").
- For §4 only: a Mac with Xcode and an iPhone simulator that has a notch or Dynamic Island.

## 2. Automated checks

```bash
./gradlew :feature:search:testAndroidHostTest --console=plain -q   # SearchResultGridCellsTest + the existing suite
./gradlew :shared:compileAndroidMain --console=plain -q            # root layout, rail, destinations
./gradlew test                                                     # every JVM suite, KoinGraphTest included
./gradlew :androidApp:assembleDebug                                # the change spans :core:ui, :core:designsystem, :shared, :feature:search
```

On Windows, use `.\gradlew.bat` with the same tasks.

Expected: all green, with no existing test changed or removed.

## 3. Hands-on matrix (Android)

Install the debug build and run every row. Rotate with the emulator's rotate buttons, which cover both
directions.

| # | Setup | Action | Expected | Spec |
|---|---|---|---|---|
| 1 | Search, portrait | Look | Bottom bar, 2-column grid and hero exactly as before the feature | SC-007, FR-002 |
| 2 | Search, portrait | Rotate to landscape | Vertical rail at the start edge with Search / Radar / Lists centered and Search selected. No bottom bar, and no empty strip where it was | US1-1, FR-001, FR-005 |
| 3 | Landscape | Tap Radar, then Lists, then Search | Each opens in one tap and the matching item is selected | US1-2, SC-004 |
| 4 | Landscape, Lists | Rotate to portrait | Bottom bar returns with Lists selected | US1-3 |
| 5 | Landscape, Search | Open a game, then back | The rail slides away on the detail screen and is back on return | US1-4, US1-5, FR-004 |
| 6 | Landscape | Search "zelda" | ≥ 3 cards per row (4 on a Pixel 7), all the same width and similar to their portrait size; even gaps; the last incomplete row starts at the left like the others | US2-1/2/3, FR-007–009, SC-001/002 |
| 7 | Row 6 + a platform filter | Look at the chips row | Full width above the grid, scrolling to the screen edge under its fades | US2-4, FR-010 |
| 8 | Landscape, Discover | Tap a shelf's "see all", then a genre from the expanded bar | Both grids use the same multi-column layout | US2-5, FR-011 |
| 9 | Row 6 | Tap a card; long-press a card | Opens the game; opens the list selector | US2-6 |
| 10 | Landscape, Discover feed | Look, then scroll to the end | The hero is shorter than the screen and the next shelf's title is visible without scrolling; every shelf is reachable | US3-1 |
| 11 | Landscape | Tap the search bar | The overlay covers the whole screen, rail included; history and genres all reachable | US3-2, R6 |
| 12 | Landscape | Search nonsense; turn on airplane mode and search; search while loading | Empty, error and loading are centered to the right of the rail, with message and actions visible | US3-3 |
| 13 | Landscape, results | Open Filters, then Sort | All options reachable (scroll if needed), apply works, dismiss works. Repeat at the largest font size | US3-4, FR-012, R10 |
| 14 | Punch-hole cutout, landscape | Look at the rail | No icon or label under the cutout | US3-5, FR-006 |
| 15 | Punch-hole cutout, **rotate the other way** | Look at the grid's last column and the profile button | Neither is under the cutout; the rail is still clear | SC-006, R8 |
| 16 | Results scrolled down, query typed, Filters open | Rotate both ways | Query, results, scroll position (same first game) and the open sheet all survive | US3-6, FR-013, SC-005 |
| 17 | Expanded search bar with the keyboard up | Rotate | Bar still expanded with the same text | Edge cases, R9 |
| 18 | Navigate to a game and rotate during the rail's slide | Return | The rail or bar ends in the right place, with no leftover gap | Edge cases |
| 19 | Developer options → Force RTL layout direction, landscape | Look | The rail is on the right (start) edge, the grid mirrors, and nothing is clipped | Edge cases |
| 20 | Split screen, the app in a tall narrow half | Look | Portrait layout: bottom bar, 2 columns | FR-014, edge cases |
| 21 | Landscape, Search | Look at the content's top-start corner next to the rail | The corner rounding is not noticeable | R11 |
| 22 | Landscape | Visit Radar and Lists | Rail and full height; their headers keep the profile button clear of the cutout | Assumptions, R8 |

## 4. iOS (Mac only)

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

Run rows 1–6, 10, 11, 14–16 and 19 on a notched simulator, rotating with ⌘← / ⌘→. The rail must clear the
notch or Dynamic Island in both directions, and the grid's last column must clear the opposite safe area.

**Status until this is run**: not verified on iOS.
