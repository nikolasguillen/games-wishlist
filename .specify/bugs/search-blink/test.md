# Bug Verification: Search header flashes darker than the background when scrolling back to the top

- **Slug**: search-blink
- **Tested**: 2026-10-13
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The owner tested the fix by hand and reported it OK: the header no longer flashes darker than the background when the
list returns to the top. The automated checks from `fix.md` pass.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | The owner ran the app manually, following the steps in `assessment.md` | pass | Reported as "test is ok". The breakdown by surface and theme was not recorded separately. |
| Module compile (Android) | `./gradlew :feature:search:compileAndroidMain` | pass | Run during the fix. |
| Module tests | `./gradlew :feature:search:testAndroidHostTest` | pass | Run during the fix. No test was added for this bug (see `fix.md`). |
| Module compile (iOS) | `./gradlew :feature:search:compileKotlinIosSimulatorArm64` | pass | Only the pre-existing `BackHandler` deprecation warning. |
| Lint / type-check | none configured | not-run | No detekt, ktlint or spotless in this project (`CLAUDE.md`). |

## Residual Risks

- **No automated regression guard.** Nothing in the test suite would catch a return to `Color.Transparent`. The comment
  at the animation is the only safeguard.
- **iOS not run on the simulator.** It was only compiled. The change is pure `commonMain` colour math, so no
  platform-specific difference is expected.
- **`StatusBarProtection`'s own gradient** still ends in `Color.Transparent` (see the follow-ups in `fix.md`).

## Recommendation

Close the bug and commit the fix.
