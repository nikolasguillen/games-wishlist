# Quickstart: Verifying Two-Pane Search and Game Detail

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Contracts**: [navigation-contract.md](./contracts/navigation-contract.md)

## Prerequisites

- IGDB credentials in `local.properties` (search needs the network).
- Android emulators or devices:
  - **Tablet**: Pixel Tablet (1280×800dp in landscape, 800×1280dp in portrait).
  - **Foldable**: Pixel Fold, or the resizable emulator's foldable mode (unfolded about 841dp+ wide in landscape).
  - **Phone**: any phone under 840dp in both orientations, plus one at least 840dp wide in landscape (for example
    412×915dp) to check the large-phone decision in [research.md R3](./research.md#r3-the-840dp-threshold-and-where-it-is-read).
  - **Resizable**: freeform windows or split-screen on the tablet, to cross 840dp without rotating.
- iOS (Mac only): an iPad simulator.

## Build and tests

```powershell
.\gradlew.bat :core:navigation:testAndroidHostTest --console=plain -q   # route round-trip incl. fromSearch
.\gradlew.bat :shared:testAndroidHostTest --console=plain -q            # run/open/back rules, KoinGraphTest
.\gradlew.bat :core:ui:testAndroidHostTest --console=plain -q           # isLandscapeShape
.\gradlew.bat test                                                      # everything
.\gradlew.bat :androidApp:assembleDebug
```

On a Mac, also run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q` and the `xcodebuild` command from
`CLAUDE.md`.

## Scenarios

Each scenario lists the spec items it proves.

### S1. No empty pane until a game opens (US1-1, FR-002)
Tablet, landscape, open Search. **Expect** Search at full width next to the rail, with no divider, blank half or
placeholder. Rotate to portrait (800dp, below the threshold). **Expect** today's portrait layout.

### S2. Open a game beside the results (US1-2…5, FR-001, FR-003, FR-006, FR-007, FR-014)
Tablet, landscape. Search "zelda", apply a platform filter, scroll a few rows, tap a game. **Expect**:
- detail on the right, with a close (X) button at its top-left and no back button;
- left pane: same query, same filter chip, same scroll position, and still scrollable on its own;
- rail visible, with Search highlighted.

Tap the X. **Expect** the detail to close and Search to return to full width with the same results. Reopen a game and\nadd it to a list. **Expect** it to work as on a phone. Go offline and open another game. **Expect** the X on the error\npage too.

### S3. Replace, never stack (US2, FR-004, FR-005, FR-008, SC-001, SC-004)
From S2, tap five other results, then a similar game inside the detail. **Expect** the right pane to always show the
last game opened, with no blink of the left pane on any swap or on open and close
([R8](./research.md#r8-transitions)). Press system back **once**. **Expect** Search to return to full width with
the same query and results. There is no predictive back preview while two panes are showing (a known limit,
[R5](./research.md#r5-the-system-back-gesture-while-both-panes-are-composed)); the back itself must still work.

### S4. Back does not clear the search (R5)
In two-pane mode with a committed query, press system back. **Expect** the detail to close and the query and
results to stay. Press back again. **Expect** the query to clear (Search's own behavior, unchanged).

### S5. Rotate and resize across 840dp (US3, FR-009…011, SC-003)
1. Two panes open (query, filter, game X). Rotate the tablet to portrait. **Expect** X full-screen with a back
   button. Back leads to the same results and scroll position.
2. Open X again in portrait, rotate to landscape. **Expect** two panes with X on the right.
3. In portrait, open X, then a similar game Y, then Z (stacked). Rotate to landscape. **Expect** Z on the right.
   One back gives full-width Search.
4. Resize a freeform window from 700dp to 900dp to 700dp with a game open. **Expect** the same as 1 and 2, and no
   state lost.
5. Repeat 1 and 2 twenty times (SC-003). **Expect** no empty pane and no duplicated detail.

### S6. Nothing else changes (US4, FR-012, FR-013, SC-005, SC-006)
- Phone under 840dp, both orientations: open a game from Search, open a similar game, back, back. **Expect**
  today's behavior exactly.
- Tablet landscape: open a game from Radar, then from a wishlist. **Expect** full-screen detail with a back button
  and no rail.
- Tablet landscape on Search with a game open in two panes: trigger a release-reminder notification and tap it.
  **Expect** that game full-screen, covering both panes. Back returns to the two panes.
- From Radar's full-screen detail, tap a similar game. **Expect** it to stack full-screen (not two panes).

### S7. Both screens fit a half pane (US5, FR-015, FR-016, SC-007)
At about 841dp (foldable or freeform window) and at 1280dp, in two-pane mode, go through:
- **Search pane**: Discover feed, a results grid (expect 2 columns in a portrait-shaped pane, with no card narrower
  than in portrait on a phone), "see all", a genre browse, the empty, error and loading states, the filter and
  sort sheets, the list chooser (long press).
- **Detail pane**: loading, error (airplane mode), and success with hero, gallery, description, platforms, similar
  games and the action pill. Nothing clipped, overlapping or under the rail.
- **Insets**: the landscape phone of at least 840dp with its camera cutout on each side in turn. **Expect** the
  cutout padding only on the pane next to it, and no gap at the divider.
- Tap the search bar in the left pane. **Expect** the full-screen search overlay over both panes, with its landscape
  layout from spec 017. Submit. **Expect** the panes back with the new results on the left and the same game on the
  right.

### S8. Tab switch from two panes (R9)
In two-pane mode, tap Radar on the rail. **Expect** Radar at full width. Tap Search. **Expect** Search at full
width with the detail closed (today's pop-to-root behavior).

### S9. iOS (Mac only)
On an iPad simulator, landscape, repeat S2, S3 (with the edge-swipe back gesture), S5-1/2 (rotate) and S7.
