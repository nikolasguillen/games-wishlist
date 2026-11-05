# Quickstart: validating the Search Dropdown on Wide Windows

How to prove the feature works end to end. The design is in [plan.md](./plan.md), the composables and the keyboard
table are in [contracts/ui-contract.md](./contracts/ui-contract.md), and the item order and highlight rules are in
[data-model.md](./data-model.md).

Commands use the Unix wrapper (macOS). On Windows use `.\gradlew.bat` with the same tasks. The iOS steps are
macOS-only.

## 1. Build and tests

```bash
./gradlew :feature:search:compileAndroidMain --console=plain -q   # fast check of the module
./gradlew :feature:search:testAndroidHostTest --console=plain -q  # SearchDropdownItemsTest, SearchDropdownNavigationTest + existing
./gradlew :androidApp:assembleDebug                               # the dispatch spans the screen
./gradlew test                                                    # every JVM suite stays green
./gradlew :desktopApp:run                                         # the desktop app, for sections 3-6
```

iPad (macOS only):

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

**Expected**: everything compiles, and every suite passes with no existing test edited.

## 2. Compact is unchanged (SC-006)

On a phone, or any window narrower than 840dp:

| # | Do | Expect |
|---|---|---|
| 2.1 | Tap the search bar | The full-screen overlay, as before: type chips, genres, recent searches, recent games |
| 2.2 | Type a few letters | Recent matches, games, "See all results for …", as before |
| 2.3 | Long-press a recent search, confirm; tap "Clear all", cancel | The same two dialogs, over the overlay, as before |
| 2.4 | Rotate with a dialog open | The dialog is still open |
| 2.5 | Back gesture with the overlay open | The overlay closes, the text stays |

## 3. The wide panel (US1)

Desktop window at about 1280×800, with some history (search a few games, open two or three):

| # | Do | Expect |
|---|---|---|
| 3.1 | Click the search bar | A panel opens directly under the bar, separated by a small gap, exactly as wide as the bar and always as tall as Material allows (half the window), whatever the content. The rail and the page stay visible and undimmed around it |
| 3.2 | Look at it with an empty query | From the top: type chips, "Browse by genre" chips, "Recent searches" with "Clear all", "Recently viewed" cards. The type chips are one row, start-aligned; the genre and recent-search chips wrap, nothing scrolls sideways |
| 3.3 | Pick "Characters" | The chip is selected, the panel stays open, the placeholder changes as in the overlay |
| 3.4 | Type "zel" | Type chips on top, then matching recent searches, shimmer rows, then up to four games and "See all results for "zel"" |
| 3.5 | Click a game suggestion | The game opens (beside the results when two panes are shown), the panel closes |
| 3.6 | Click "See all results" or a recent search | The search is committed, the results appear, the panel closes |
| 3.7 | Click a genre | The genre browse appears, the panel closes |
| 3.8 | Clear all history, then click the bar | Type chips, genres and the first-time message |
| 3.9 | Shrink the window height to about 500dp with a full history | The panel stops at about half the window and scrolls inside itself; the page does not move |
| 3.10 | Open and close the panel over Discover, results, and a character grid | The page behind never scrolls, reflows or reloads (SC-008) |
| 3.11 | With the same history, type the same query once on this window and once on a window under 840dp | The same chips, recent matches, games and "See all" row, in the same order (SC-007) |

## 4. Keyboard (US2)

Panel open, nothing highlighted. Keep the mouse still.

| # | Do | Expect |
|---|---|---|
| 4.1 | Press Down repeatedly | The highlight walks every item in reading order (chips, genres, "Clear all", recent searches, recent games) and stops at the last |
| 4.2 | Press Up back to the top, once more | The highlight leaves the first chip; the caret is where it was, the text unchanged |
| 4.3 | Type "ma", Down to a game, Enter. Time it from the first keystroke | The game opens, in under 5 seconds (SC-002) |
| 4.4 | Type "ma", Enter with nothing highlighted | The search is committed for "ma" |
| 4.5 | Highlight a row, then type, Left/Right, Backspace, Cmd/Ctrl+A, C, V | All go to the text field; suggestions update; a highlight whose row disappears clears |
| 4.6 | Down to a chip, Enter | The type changes, the panel stays open |
| 4.7 | Highlight a recent search, Shift+Delete | The removal dialog; confirm: the row is gone, the panel is still open |
| 4.8 | Highlight a recent game, Shift+Delete | The game leaves "Recently viewed" |
| 4.9 | Highlight an item low in a long panel | It is scrolled into view |
| 4.10 | Tab to the bar from elsewhere (panel closed) | The bar shows focus and does not open; Down or typing opens it |
| 4.11 | With the mouse, click a type chip, then a recent game's remove button (if one is shown), then press Down, Up and Escape and type a letter | The highlight moves, Escape closes the panel and the letter goes into the text field: keyboard focus never left the field. Tab does not move into the panel's chips, rows or cards |

## 5. Mouse and closing (US2, US3)

| # | Do | Expect |
|---|---|---|
| 5.1 | Move the pointer over rows and chips | The same highlight mark as the keyboard's follows the pointer |
| 5.2 | Highlight with the keyboard while the pointer rests on another row | The keyboard's highlight stays until the pointer moves |
| 5.3 | Type "ember", press Escape | Panel closes, "ember" stays in the bar; click the bar: it reopens on "ember" |
| 5.4 | Reopen, click a game card on the page beside the panel | Panel closes, "ember" stays, **the card does not open** (FR-022) |
| 5.5 | Reopen, click a rail item | Panel closes; the tab does not change on that click |
| 5.6 | Reopen, press Tab | Panel closes, "ember" stays, focus moves on from the bar (or stays on the bar: note which) |
| 5.7 | Reopen, click inside the text to move the caret | Panel stays open |
| 5.8 | Reopen with an empty query, click "Clear all", then click inside the dialog | The dialog works and the panel stays open behind it; cancel returns to the panel |
| 5.9 | Type "ember", press Escape (panel closes), then press Escape again | Record what happens: a second Escape does what it does today, and today's back handling clears a committed search when the bar is collapsed with text. If it clears the typed text, note it |

## 6. Crossing 840dp (US4)

| # | Do | Expect |
|---|---|---|
| 6.1 | At 1000dp, type "ember" with the panel open; narrow the window to 700dp | The full-screen overlay with "ember", the caret where it was, the same type |
| 6.2 | Widen back to 1000dp | The dropdown with "ember" and its suggestions |
| 6.3 | Open the removal dialog, cross 840dp both ways | The dialog is still open each time |
| 6.4 | Exactly 840dp | The dropdown |
| 6.5 | Any crossing | No search is committed or cleared; Discover or the results behind do not reload |

## 7. Other wide windows

| Where | Check |
|---|---|
| Resizable Android emulator ≥ 840dp (touch) | Sections 3 and 5 with taps; a tap outside does not click through; the keyboard shows and hides cleanly on 6.1/6.2 |
| Android emulator with a hardware keyboard | Section 4 |
| iPad simulator (landscape) | Sections 3, 5 and 6; section 4 with the simulator's hardware keyboard |
| Two panes (a game open beside Search) | The panel is as wide as the bar and does not reach over the detail pane; a click on the detail pane only closes the panel |

## 8. Accessibility

| Where | Check |
|---|---|
| macOS VoiceOver on the desktop app | Moving the highlight announces the item's label; the field says suggestions are available |
| TalkBack on the Android tablet | Same |

If an announcement or a platform behaviour above (5.4, 5.5, 5.6, the live region) does not hold on a platform,
record exactly what was seen in `docs/tech-debt.md`, as the Discover wide layout did, rather than marking it done.

## Record

Note the date, the platforms checked, and any row that failed or could not be run, in the task that closes the
verification.
