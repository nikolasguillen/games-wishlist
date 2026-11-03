# Quickstart: validating Discover Wide Layout

**Feature**: `021-discover-wide-layout` | **Date**: 2026-11-03

How to prove the feature works end to end. The commands are for Windows (PowerShell, batch wrapper). On macOS, use
`./gradlew` instead of `.\gradlew.bat`. What each scenario checks is in [spec.md](./spec.md). The shapes are in
[contracts/ui-contract.md](./contracts/ui-contract.md) and [data-model.md](./data-model.md).

## Prerequisites

- `local.properties` with `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET`, so the feed loads real data.
- A library with a few saved games, so at least one personalised shelf appears ("More from …" or
  "Because you like …").
- For Android: a resizable emulator (the *Resizable (Experimental)* device, or any tablet AVD), or a tablet.

## 1. Automated checks (must all pass)

```powershell
.\gradlew.bat :feature:search:compileAndroidMain --console=plain -q   # the feature compiles
.\gradlew.bat :feature:search:testAndroidHostTest --console=plain -q  # layout rules + mapper
.\gradlew.bat test                                                    # every JVM suite, KoinGraphTest included
.\gradlew.bat :androidApp:assembleDebug                               # the app, since :shared changes
```

Expected new coverage:
- **`WideDiscoverLayoutTest`**:
  - about 1080dp of content gives 6 columns; narrower widths give fewer, never below 1;
  - "See all" shows when `hasMore`, or when the games outnumber the columns, and never otherwise;
  - the hero height is ≥ 1.3 × 240dp at 800dp tall, stays within [280, 560], and is capped by 0.8 × the width.
- **Unchanged and still green**: `SearchViewModelTest` and `DiscoverMapperTest` (no ViewModel, mapper or state
  change), `SearchResultGridCellsTest`.

## 2. Desktop, the main wide target

```powershell
.\gradlew.bat :desktopApp:run
```

Open Search with the field empty.

| Step | Expected |
|---|---|
| Window at about 1280 × 800 | Full-width banner, clearly taller than the phone hero, with the kicker, title, the same `developer · release date` meta line as the phone hero, and two buttons inside it. The footnote sits under it. Each card shows cover, title, release year (or TBA) and the heart, like the phone's shelf card. The first shelf's title and the top of its cards are visible without scrolling. |
| Count the cards in a full shelf at about 1080dp of content | Six, all whole. |
| Click "Add to wishlist" | The game is saved and the label becomes "In wishlist". A second click removes it. |
| Press and hold "Add to wishlist" | The list chooser opens. |
| Click "View details" | The game opens beside the feed (two panes), and the feed is still on the left. |
| Click the banner's image | Nothing happens. |
| Click a card / its heart / hold its heart | Opens the game / saves or removes it / opens the list chooser. |
| Click "See all" on any shelf | The see-all grid opens with that shelf's games, the same as the phone's arrow. |
| Narrow the window step by step down to 840dp | Fewer cards per row, never a cut-off card. Rows that now hide games show "See all". |
| Scroll to the second shelf, then drag the width across 840dp both ways, several times | The layout swaps at 840. The second shelf is still at the top each time, with no spinner and no refetch. |
| Tab through the screen | Focus reaches the search bar, both banner buttons, each "See all" and each card, in screen order. Enter activates them. |
| Mouse wheel over a shelf row | The feed scrolls vertically. If it now works, delete the backlog item "Mouse wheel does not scroll in Discover"; if it does not, leave it. |
| A stale feed (change a saved game's status, come back) | The refresh prompt appears over the wide layout, works, and keeps its state across a width swap. |
| Switch the app between light and dark | Banner and card text stay legible on every cover. |

## 3. Android

```powershell
.\gradlew.bat :androidApp:installDebug
```

| Step | Expected |
|---|---|
| Phone, portrait and landscape | Discover is exactly as before: short hero with the heart, square cards, sideways-scrolling shelves, arrows. |
| Tablet or resizable emulator ≥ 840dp | The wide layout, as on desktop. |
| Resize or rotate across 840dp while scrolled | Same shelf at the top, no reload. |
| Two panes, a game open, at an 840dp window | The feed in the narrow left pane: buttons below the title, about two cards per row, nothing cut off. |
| Loading (first launch, slow network), error (airplane mode, then retry) | The same loading and error pages in both layouts. |

## 4. iOS

Not verifiable on Windows. The code is `commonMain` only, so no iOS-specific work is expected. When a Mac is at hand,
run the iOS app on an iPad simulator, check sections 2 and 3's wide steps, and record that it was done. Until then,
say it was not checked rather than assuming it.
