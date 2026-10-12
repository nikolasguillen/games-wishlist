# Bug Assessment: Search header flashes darker than the background when scrolling back to the top

- **Slug**: search-blink
- **Created**: 2026-10-13
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: low

## Report (verbatim or summarized)

> when scrolling back up the search results list, the search bar header background instead of going from dark to background color it also flashes of a darker color for an instant

## Symptom

When the results list is scrolled back to the very top, the Search header (and the status-bar protection strip above it)
fades from the "scrolled" grey back to the screen background, but partway through it briefly turns **darker than both the
grey it started from and the background it ends on**. Expected: a smooth fade from the scrolled grey straight to the
background with nothing darker in between.

## Reproduction

1. Switch the app to the light theme, open the Search tab, and either commit a query (or a genre browse) so the results
   grid fills the screen, or stay on the Discover feed.
2. Scroll down far enough for the header to pick up its scrolled container colour.
3. Scroll back up until the grid reaches its first item (`firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0`).
4. Watch the header while it fades back: for about one spring duration it dips darker than the background before it settles.

The owner confirmed that the flash shows **only in the light theme** (`#E4E4E4` grey on an off-white background), and
that it shows on **the Discover feed as well as the results grid**, as predicted, since both share the same
`backgroundColor`. In the dark theme the same math gives only a dip from `#121212` to about `#0D0D0D`, too small to notice
on `#282828`. The fix below covers both themes anyway.

Why it is only noticed **when scrolling up**: the bar uses `enterAlwaysSearchBarScrollBehavior`. When scrolling down, the
bar slides off-screen while the colour animates toward grey, so the bad midpoint is mostly hidden. When the list comes back
to the top, the bar is fully on-screen while `isScrolled` turns false, so the whole transition to transparent can be seen.

## Suspected Code Paths

- `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt:213-218`: the
  `animateColorAsState` whose target switches between `MaterialTheme.appColors.searchBarScrolledContainerColor` and
  **`Color.Transparent`**. This is the root cause.
- `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt:236` and `:272`:
  the animated colour is passed both to `SearchTopBar` (→ `CollapsedSearchBar` → `MainScreenHeader`'s `Surface`) and to
  `StatusBarProtection`, so both surfaces flash together.
- `core/ui/src/commonMain/kotlin/com/nikolasguillen/questlog/core/ui/component/MainScreenHeader.kt:58`: the
  `Surface(color = containerColor)` that paints the intermediate colour. This is where the flash is drawn, not where it
  comes from.
- `core/designsystem/src/commonMain/kotlin/com/nikolasguillen/questlog/core/designsystem/theme/QuestLogTheme.kt:85,112`:
  the scrolled colours (`NeutralMediumGrey` `#282828` dark, `NeutralSoftGrey` `#E4E4E4` light). Context only, nothing to
  change there.

## Root Cause Hypothesis

`Color.Transparent` is `Color(0x00000000)`, which is **transparent black** and not "the current colour with zero alpha".
`animateColorAsState` animates through `Color.VectorConverter`, which converts both ends to Oklab and interpolates
alpha, L, a and b **independently, without premultiplying**. So while alpha falls from 1 to 0, lightness falls toward 0 at
the same rate. At the midpoint the header is roughly a 50%-alpha near-black, drawn over the background.

Worked through for the light theme: at t = 0.5 the colour is about `#585858` at 50% alpha. Composited over the off-white
background that gives about `#A6A6A6`, much darker than either end (`#E4E4E4` and the background). For the dark theme,
t ≈ 0.5–0.75 composites to about `#0D0D0D`, slightly below the `#121212` background.

That's the "flash of a darker colour for an instant" in the report. The spring (`StiffnessMediumLow`, no bounce) does not
overshoot, so the animation spec isn't the cause. **Confidence: high.**

## Proposed Remediation

**Preferred**: in `SearchScreen.kt`, change the non-scrolled target from `Color.Transparent` to the scrolled colour with
zero alpha:

```kotlin
val scrolledColor = MaterialTheme.appColors.searchBarScrolledContainerColor
targetValue = if (isScrolled) scrolledColor else scrolledColor.copy(alpha = 0f)
```

Both ends then share the same L/a/b, and only alpha changes. The header fades out cleanly over whatever is behind it, and
`StatusBarProtection` gets the fix for free because it receives the same value. The resting colour is still fully
transparent, so nothing changes visually at either end of the animation. That matters if any content ever draws behind
the header.

**Alternatives**:
- Animate to opaque `MaterialTheme.colorScheme.background` instead. This also removes the dip, since both ends are opaque
  and the path never passes through black. The trade-off: the resting header becomes an opaque slab rather than
  transparent, which changes behaviour if any content draws under the bar, and makes the status-bar gradient end on a
  solid colour. Not recommended.
- Drop `animateColorAsState` and animate a `Float` alpha instead (`animateFloatAsState`), applying it as
  `scrolledColor.copy(alpha = fraction)`. This gives the same result as the preferred fix with more code. Only worth it if
  something else needs that fraction.

**Files likely to change**:
- `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt`

**Tests to add or update**:
- The project has no Compose UI test harness (no `createComposeRule` / `runComposeUiTest` anywhere), and a colour
  animation is a poor fit for the existing ViewModel JVM tests. Verification is **manual**: repeat the reproduction in
  both light and dark themes on Android, and on the iOS simulator when a Mac is available, and confirm the header fades
  without dipping below the background.
- Optional: if the target selection is pulled into a small `internal` helper (for example
  `topBarBackgroundTarget(isScrolled, scrolledColor)`), an `androidHostTest` could check that the non-scrolled target keeps
  the scrolled colour's RGB with `alpha == 0f`, which guards against a regression back to `Color.Transparent`. This only
  makes sense if the helper reads naturally. Don't extract it just to have something to test.

## Risks & Considerations

- Very low risk: a one-line change in a single composable. It touches no state, no ViewModel, no shared component API,
  and no module boundary.
- `StatusBarProtection` (`core/ui/.../StatusBarProtection.kt:26-34`) builds its own vertical gradient ending in
  `Color.Transparent`, so it has the same transparent-black interpolation **across space** (a faint darker band at the
  bottom of the strip). It's a separate, cosmetic issue in a shared `:core:ui` component used by other screens. Mention it
  but keep it out of this fix unless the owner asks.
- Shared `commonMain` code, so the change ships to iOS as well. iOS verification needs a Mac.

## Open Questions

None. The owner confirmed it happens in the light theme only, on both the results grid and the Discover feed (2026-10-13).
Manual verification after the fix should focus on the light theme, on both surfaces, and also check that nothing in the
dark theme gets worse.
