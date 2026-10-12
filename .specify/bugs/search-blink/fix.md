# Bug Fix: Search header flashes darker than the background when scrolling back to the top

- **Slug**: search-blink
- **Fixed**: 2026-10-13
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

The Search header's background now fades to the scrolled container colour at zero alpha instead of to
`Color.Transparent` (transparent black). Only alpha animates, so the header no longer dips darker than the background
partway through the fade. This applies to both the results grid and the Discover feed, and to the status-bar
protection strip, which receives the same colour.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `feature/search/src/commonMain/kotlin/com/nikolasguillen/questlog/feature/search/SearchScreen.kt` | modified | The non-scrolled target of the `topBarBackground` animation is now `searchBarScrolledContainerColor.copy(alpha = 0f)`, with a comment explaining why. Removed the `androidx.compose.ui.graphics.Color` import, which was no longer used. |

## Diff Highlights (optional)

```kotlin
// Fades to the scrolled color at zero alpha rather than Color.Transparent: that is transparent *black*,
// and the color animation interpolates lightness alongside alpha, so the header would dip darker than
// the background midway through the fade.
val scrolledContainerColor = MaterialTheme.appColors.searchBarScrolledContainerColor
val backgroundColor by animateColorAsState(
    targetValue = if (isScrolled) scrolledContainerColor
    else scrolledContainerColor.copy(alpha = 0f),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "topBarBackground"
)
```

## Tests Added or Updated

- None. As the assessment notes, the project has no Compose UI test setup, and the colour target is a single inline
  expression. The optional helper extraction was skipped, because pulling it out only to test a constant would not
  read naturally.

## Local Verification

- Commands run:
  - `./gradlew :feature:search:compileAndroidMain` → passed
  - `./gradlew :feature:search:testAndroidHostTest` → passed
  - `./gradlew :feature:search:compileKotlinIosSimulatorArm64` → passed. The only warning is the pre-existing
    `BackHandler` deprecation at `SearchScreen.kt:204`, which this change did not touch.
- `:androidApp:assembleDebug` was not run: the change is confined to one composable in one module and does not touch
  DI wiring.
- Manual checks: **not done yet.** In the **light theme**, scroll the results grid and the Discover feed back to the top
  and confirm the header fades from grey to the background without a darker flash. Then check that the dark theme looks
  unchanged.

## Deviations from Assessment

None.

## Follow-ups

- `StatusBarProtection` (`core/ui/.../StatusBarProtection.kt:26-34`) still ends its own vertical gradient in
  `Color.Transparent`, which can leave a faint darker band at the bottom of the strip in the light theme. It's a
  separate cosmetic issue in a shared `:core:ui` component, so it was left out of scope, as the assessment agreed.
