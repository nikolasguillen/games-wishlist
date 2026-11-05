# Research: Search Dropdown on Wide Windows

Phase 0 of [plan.md](./plan.md). Each entry records a decision, why it was made, and what was rejected. The
Material 3 facts were read from the sources of the two artifacts this project builds against: JetBrains
`org.jetbrains.compose.material3:material3:1.12.0-alpha03` (what `commonMain` compiles against) and Jetpack
`androidx.compose.material3:material3-android:1.5.0-beta01` (what Android runs).

## R1. One dispatch point for all of Search

**Decision**: The Search screen reads `isWideWindow` in exactly one place, `SearchScreenContent`, and chooses
between two whole-screen composables, `CompactSearchLayout` and `WideSearchLayout`. The Discover switch that spec 021
put inside `SearchMainContent` moves up into that choice: `SearchMainContent` loses its `isWideWindow` parameter and
takes the Discover feed as a slot, which each layout fills with its own feed. The state and callbacks both layouts
need (the search bar state, the list and grid states, the scroll behaviour, the commit/browse/open lambdas, the back
handler and the history dialog state) stay in `SearchScreenContent`, above the choice, so they survive a swap.

**Rationale**:
- The owner's standing rule (feature `CLAUDE.md`, and the feedback recorded on 2026-11-03) is to decide the size
  class once, at the top, and dispatch to whole-screen composables, never to scatter size conditionals.
- Results and filters will get wide variants next (the side filter panel, mockup page 4). That panel changes the
  arrangement of the Scaffold's content, not just one slot in it, so a wide layout that owns its own Scaffold is
  where that work lands with no second refactor.
- With the dropdown, Search would otherwise read the flag in two places (the top bar and the Discover content), and
  in a third once filters arrive. Moving 021's switch now costs one slot parameter.

**Alternatives considered**:
- *One dispatch per area* (top bar, content, filters each choose for themselves): fewer moves today, but every new
  wide area adds another `if (isWideWindow)` deeper in the tree, which is what the owner asked to avoid.
- *One Scaffold with slots chosen at the top*: works today, but the side filter panel would split the Scaffold
  anyway; whole-screen layouts now avoid doing the same refactor twice.

**Cost**: the Scaffold call (about 25 lines) exists twice. The pieces inside it are shared composables
(`SearchTopBar`, `SearchMainContent`, the sheets and the list selector), so the duplication is the arrangement
only, which is exactly the part that will diverge.

## R2. The dropdown is Material 3's `ExpandedDockedSearchBarWithGap`, without a scrim

**Decision**: The wide panel is `ExpandedDockedSearchBarWithGap`, driven by the same `SearchBarState` and the same
input field as the compact overlay, with `dropdownScrimColor = Color.Unspecified`, the pill shape the collapsed bar
already uses as `shape`, a gap from `MaterialTheme.spacing`, and
`PopupProperties(focusable = true, clippingEnabled = false, dismissOnClickOutside = <no history dialog open>)`.

**Facts verified in the sources**:
- Both artifacts declare the same signature (`state`, `inputField`, `modifier`, `shape`, `dropdownShape`,
  `dropdownGapSize`, `dropdownScrimColor`, `colors`, `tonalElevation`, `shadowElevation`, `properties`, `content`),
  so `commonMain` can call it with no `expect`/`actual` (unlike `SearchBarScrollBehavior`, which needed
  `SearchBarScrollBehaviorCompat`). JetBrains marks it `@ExperimentalMaterial3ExpressiveApi`, so the call site opts in;
  Jetpack 1.5.0-beta01 has it as stable.
- It renders in a `Popup` positioned at the collapsed bar's top-left (`state.collapsedBounds`), and its width is the
  collapsed bar's width (`collapsedBounds.width`, falling back to 360dp). The input field is capped at 720dp
  (`SearchBarMaxWidth`), so the panel is exactly as wide as the bar, which is FR-002.
- Its height fits the content between 240dp and half of the window's height, and the content area scrolls beyond
  that (FR-003). The 240dp is a hard minimum of the component (`DockedExpandedTableMinHeight`), so a sparse panel
  (typing with no suggestions: the chips and "See all") has blank space under its last row. The owner accepted this on
  2026-11-04 rather than build a custom panel; FR-003 states the minimum.
- It re-renders the input field inside the popup, as `ExpandedFullScreenSearchBar` does inside its dialog, and
  requests focus on it the first time it opens. The text and the caret live in the ViewModel's `TextFieldState`, so
  both copies show the same text and caret.
- With a scrim, the popup covers the whole window, navigation rail included, with a clickable dim layer. Without
  one, the popup covers only the bar and the panel.

**Rationale**: It is the component Material 3 recommends for medium and large windows, it already shares the state
machine the compact overlay uses (so the swap across 840dp is a recomposition, not a migration), and the gap variant
matches the mockup's separate pill and card. No scrim, because the spec says the panel does not cover the rail or
the window.

**Alternatives considered**:
- *`ExpandedDockedSearchBar`* (no gap): the field and the results share one container. It is not experimental, but
  it does not match the mockup's separate card. The two calls take the same arguments apart from the gap and the
  scrim, so switching later is a one-line change.
- *A custom overlay inside the Search screen* (no popup): full control over focus, so Tab could leave the panel
  naturally. But it re-implements positioning, elevation, the expand animation, the height cap, Escape and the
  outside click, and drifts from the `SearchBarState` machine both presentations share. Rejected as more code and more
  risk for one behaviour that R3 gets another way.

## R3. Keyboard model: a virtual highlight, focus stays in the text field

**Decision**: Keyboard focus never leaves the text field while the panel is open. The panel keeps a *highlight*:
which item, if any, Enter will activate.
- **Down / Up** move the highlight one item through every actionable item of the panel in reading order (the type
  chips, the genres, "Clear all", the recent searches, the recently viewed games; or, while typing, the type chips,
  the matching recent searches, the game suggestions and the "see all results" row). They stop at the last item. Up
  from the first item clears the highlight, which puts the user back in the text field. Down with nothing
  highlightable does nothing.
- **Enter** activates the highlighted item exactly as a click does. With nothing highlighted it is left to the field,
  whose search action commits the typed query as today.
- **Escape** closes the panel.
- **Tab / Shift+Tab** close the panel, keeping the text, and let focus move on from the bar.
- **Shift+Delete** on a highlighted recent search asks to remove it (the existing dialog); on a highlighted recently
  viewed game it removes it, as the card's remove button does. On anything else it is left to the field.
- **Left, Right, Home, End, typing and the usual shortcuts** are never intercepted: they edit the text (FR-019).

**Keeping focus in the field** (owner decision, 2026-11-04, from the analysis): Compose requests focus on a mouse press for any `clickable` (foundation 1.12.1, `Clickable.kt`, `requestFocusWhenInMouseInputMode`), and every chip, genre, row and card of the panel is one. Left alone, a click on a type chip would put keyboard focus on the chip, and the key handler below (which is on the field) would stop seeing Down, Up, Enter and Escape, and typing would no longer reach the field. So the panel items are made non-focusable (`focusProperties { canFocus = false }` on the panel root, plus the nested remove button of a recent game card if it needs it). They stay reachable by the arrow keys through the highlight, by the mouse, and by assistive tools through the live region. The alternative of handing focus back to the field after every click was rejected: it leaves a moment where focus is on the item, and Tab would still enter the popup.

The handler is an `onPreviewKeyEvent` on the input field, passed in through the field's modifier. A modifier passed
to `SearchBarDefaults.InputField` sits *outside* Material's own `onPreviewKeyEvent`, so it sees Down first and
consumes it. Material's own handler would move real focus into the list (`focusManager.moveFocus(Down)`), which
would stop typing from reaching the field.

**Rationale**: This is the combobox pattern (the address bar of a browser). It is the only model that satisfies
FR-015 (Up from the first row returns to the field with its caret intact) and FR-019 (typing keeps working while a
row is highlighted) at the same time. Real focus on a row would send keystrokes to the row.

**Consequence for the spec**: the spec asked for both "rows reachable through Tab" (US2-8) and "Tab moving focus
away closes the panel" (US3-3), which contradict each other. With a virtual highlight, the arrow keys reach every
item and Tab leaves, so US2-8 was corrected to "reachable with the arrow keys". Shift+Delete was added to FR-016 so
that keyboard-only users can remove a recent item, as "keyboard use is first-class" asks.

**Alternatives considered**: *Real focus with arrow-key traversal* (Material's default): rejected for FR-019 above.
*Up/Down between rows, Left/Right within a chip row*: fewer presses across the eight chips, but Left and Right are
caret movement in the field, which FR-019 protects. The chip rows are short (three types, five genres), so a linear
order costs at most eight presses.

## R4. What opens the panel

**Decision**: On a wide window the panel opens on a click or tap on the bar, on typing into the focused bar, and on
Down while the bar has keyboard focus. Tabbing onto the bar shows its focus but does not open it. To let typing open
it, the wide layout passes `readOnlyWhenCollapsed = false` to the shared input field. The compact overlay keeps the
field read-only while collapsed, so a touch on the pill still goes straight to the full-screen overlay.

**Rationale**: This is how Material's `InputField` already behaves when the input is not touch (it expands on focus
only in touch mode) and what the combobox pattern recommends. It also avoids a loop: after Escape or Tab closes the
popup, focus can land back on the collapsed field, and an open-on-focus rule would reopen the panel at once. The
owner's brief ("Focusing the search bar opens a panel") meant clicking into the bar. FR-001 was reworded to list the
three triggers. Flipping to open-on-keyboard-focus later is one `onFocusChanged` on the wide field.

## R5. Closing, and what an outside click does

**Decision**:
- **Escape**: the key handler collapses the bar. The popup's own back handling would also do it, so either path
  works.
- **Outside click or tap**: the focusable popup's `onDismissRequest` collapses the bar. A focusable popup consumes
  the click that dismisses it (Android's `PopupWindow` and Compose Multiplatform's focusable layers both work this
  way), so the click does not also activate what is under it (FR-022). A click on the navigation rail or on the
  detail pane closes the panel and does nothing else; a second click navigates. This is verified per platform in the
  quickstart.
- **Tab**: see R3. The panel collapses first. Focus then moves on from the bar where the platform allows it; at worst
  it stays on the collapsed bar, and the next Tab moves on.
- **What "the bar loses focus" means** (owner decision, 2026-11-04): only Tab and Shift+Tab. The app window losing
  focus (switching to another application) leaves the panel as it is, and FR-021 says so.
- **While a history dialog is open**, `dismissOnClickOutside` is off, so a click inside the dialog cannot count as a
  click outside the popup and close the panel behind it (US3-6).
- Collapsing never touches `TextFieldState` (FR-023). The app window losing focus (switching apps) leaves the panel
  as it is.

## R6. The history dialogs survive the swap across 840dp

**Decision**: `queryToRemove` and `showClearHistoryDialog` move from inside `ExpandedSearchBar` up to
`SearchScreenContent`, held in one `SearchHistoryDialogState` and still saved across rotation. Each presentation receives it and renders the same
`RemoveHistoryItemDialog` and `ClearHistoryDialog` from inside itself: the compact overlay where it does today, the
wide dropdown from inside its popup content.

**Rationale**: FR-027 requires an open confirmation to stay open when the window crosses the boundary. State held
inside one presentation dies with it. Rendering stays where it is today for the compact overlay, so its stacking
over the full-screen dialog is unchanged. This follows the feature rule that a confirmation emitting one event is
saveable composable state.

## R7. Where the highlight lives

**Decision**: The highlight is composable state in `WideSearchLayout` (a nullable item key), cleared whenever the bar
collapses and reconciled whenever the item list changes. The decisions it needs are pure functions, tested on the
JVM: the reading-order item list (`searchDropdownItems`), the moves (`highlightNext`, `highlightPrevious`) and the reconciliation
(`reconcileHighlight`, which clears a highlight whose item no longer exists).

**Rationale**: The highlight is interaction state that only exists while the panel is open, like a scroll position.
The ViewModel does not know when the bar collapses (`SearchBarState` is UI state), so holding it there would need
two-way syncing for nothing. Keeping the rules pure follows constitution III and spec 021's precedent of pure,
tested layout rules applied by the composable. The highlight is keyed by item, not by index, so a list that changes
under it never moves it silently to a different item (spec edge case).

**Alternative considered**: highlight in `SearchUiState` with move events. Rejected as above, and because it adds
ViewModel state for a presentation-only concern.

## R8. What the panel shows, and how

**Decision**: The panel reuses the overlay's own pieces, in the overlay's order (clarification of 2026-11-04):
- **Before typing**: the search-type chips (`SearchTypeChipRow`), the genre quick-browse (`GenreBrowseSection`), the
  recent searches with "Clear all" (`RecentSearchesSection`, moved out of `ExpandedSearchHome.kt`), the recently
  viewed games (`RecentGameCard`s), and the same `InitialMessage` when there is no history.
- **While typing**: the type chips, then the overlay's suggestion rows (`HistorySuggestionRow`, `GameSuggestionRow`,
  `LoadingSuggestionRow`, `SeeAllResultsRow`) under the same section headers. This is the mockup's "Recents / Games /
  see all" layout, which the overlay already has.
- Every chip and card row in the panel **wraps** instead of scrolling sideways (the existing `wrap` precedent of
  `RecentSearchesSection`). Every item is then always composed and can be scrolled into view, and nothing needs a
  horizontal mouse wheel.
- The panel's content is one vertically scrolling `Column`, not a lazy list. It holds at most 3 types, 5 genres,
  10 recent searches and 10 recent games, or 3 types, 5 history suggestions, 4 games and one row.

**Rationale**: Same content as the overlay (FR-007, FR-008, SC-007), from the same composables, so the two cannot
drift. Wrapping fits a fixed-width panel and keeps keyboard scrolling simple.

## R9. The highlight mark, hover, scrolling into view and announcements

**Decision**: One modifier, `Modifier.searchPanelItem(...)`, applied to every actionable item:
- draws the highlight mark (a tinted container and an outline in the item's shape, from theme colors), the same for
  keyboard and hover (FR-017);
- reports pointer `Enter` and `Move` as hover, which sets the highlight. A pointer resting still never steals it
  back from the keyboard, because only real movement reports (spec edge case);
- brings the item into view when it becomes highlighted (`BringIntoViewRequester`; FR-020).

The reused chips do not take a modifier (`CustomFilterChip` has none), so the reusing sections gain an optional
per-item decoration (`chipModifier` / `itemModifier`, default `Modifier`) applied to a wrapping `Box`. The compact
overlay passes nothing and is unchanged. A recent game card owns its size only when no modifier is passed, so the
mark goes on a wrapping `Box`, not on the card.

**Announcements**: an invisible polite live region in the panel carries the highlighted item's label (type, genre,
query, game name, "Clear all", "See all results for …", all existing strings). `selected` semantics are *not* used
for the highlight, because the type chips already use `selected` for the chosen type. Desktop and iPad screen-reader
support for live regions is unverified; if it does not announce, that is recorded in `docs/tech-debt.md` as the
Discover wide layout's open checks were.

## R10. Crossing 840dp with the panel open

**Decision**: Nothing special beyond R1 and R6. Both presentations read the same `SearchBarState`, which is
`Expanded`, so the one entering composition opens and focuses its own copy of the input field. The text and the caret
come from the ViewModel's `TextFieldState`, the type from `SearchUiState`, and the dialog flags from R6. Only the
highlight resets (R7), which the spec does not protect. No event is sent, so nothing is committed or cleared
(FR-027). On Android the software keyboard may hide and show once while the dialog swaps for the popup, which is
acceptable.

## R11. Verification

**Decision**: JVM tests in `feature/search/src/androidHostTest` for the pure rules (`SearchDropdownItemsTest`,
`SearchDropdownNavigationTest`). The existing suites stay green untouched: no ViewModel change, so
`SearchViewModelTest` is not edited. Presentation, keyboard, mouse and focus are checked by hand on the desktop app,
a resizable Android emulator and, since this Mac can, an iPad simulator ([quickstart.md](./quickstart.md)). The
project has no Compose UI test harness, and adding one is out of scope.
