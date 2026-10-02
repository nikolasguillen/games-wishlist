# Bug Verification: Deleting a wishlist pops more than one screen

- **Slug**: double-nav-back
- **Tested**: 2026-10-02
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug no longer reproduces. On the emulator, deleting a list now lands on the Lists overview with the app
still open, both for a list with no games (which used to end on the Search tab) and for a list holding a
game (which used to close the app). The new unit tests pass and the full suite shows no regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction, list with no games (post-fix) | Emulator: Lists tab, open "No Games", "⋮", Delete list, Delete | pass | Landed on "My Wishlists" with the other two lists; app in the foreground. Before the fix: Search tab. |
| Reproduction, list with a game (post-fix) | Emulator: Lists tab, open "With Games" (holds Cindergate), "⋮", Delete list, Delete | pass | Landed on "My Wishlists"; same app process (pid 11731) before and after, so it did not close. Before the fix: Android home screen. |
| Data after the deletes | Pulled the app database over `adb` | pass | Both lists gone; the default list "Finished" and its game (Cindergate) untouched. |
| New and updated tests | `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q` | pass | `WishlistViewModelTest`: 9 tests, 0 failures. The two behavior tests failed before the fix (see `fix.md`). |
| Regression suite | `./gradlew test :app:assembleDebug --console=plain -q` | pass | Exit 0; 323 unit tests across all modules, 0 failures; the app assembles. |
| Lint / type-check | none configured | not-run | No detekt, ktlint or spotless in this project (`CLAUDE.md`). Kotlin compilation is covered by the two commands above. |

## Output Excerpts

```text
WishlistViewModelTest: 9 tests, 0 failures
unit tests across all modules: 323 run, 0 failures
```

```text
After deleting "No Games":    text='My Wishlists'  rows: Finished (Default), With Games    app pid 11731
After deleting "With Games":  text='My Wishlists'  rows: Finished (Default)                app pid 11731
Database after:               default list: Finished -> ['Cindergate']
```

## Residual Risks

- **The "before" behavior is from earlier in the session, not re-run on this build.** The pre-fix outcomes
  (Search tab, then home screen) were observed during the spec 005 quickstart run, with different lists
  and games than today's. The post-fix runs used the same two situations (no games, one game), so the
  comparison is fair, but it is not a same-build A/B.
- **The third-pop theory was never logged.** The assessment rated the "repeated `null` emission" cause as
  medium-high confidence. The fix addresses it directly, and the list-with-a-game case now passes, which is
  the strongest evidence available. Nothing in this run logged the individual emissions.
- **One emulator, one build.** Android API 37, debug build, one screen size. Navigation behavior is not
  expected to differ by device.
- **The other half of the "vanished" path is covered by a unit test only.** A list deleted from somewhere
  other than its own screen cannot happen in the app today, so there was nothing to exercise by hand. The
  unit test (`detail, null, null` gives one `NavigateBack`) covers it.
- **The unguarded `removeLastOrNull()` on every route is unchanged**, as the assessment advised. A stray
  extra `NavigateBack` from some future change could still close the app.
- **The refused-delete path was not exercised on the device.** The default list has no delete action, so
  it cannot be reached from the UI. It is covered by `a refused delete shows a snackbar and stays on the
  screen`, which still passes.

## Recommendation

Close the bug: verified on the device in both cases and by the automated suite. The fix is uncommitted
(it sits in `WishlistViewModel.kt` and `WishlistViewModelTest.kt`), so commit it next, for example as
`fix(wishlist): pop one screen when a list is deleted`. If you want certainty on the third-pop theory,
temporarily log the `null` emissions in the `onEach` while deleting a list with games.
