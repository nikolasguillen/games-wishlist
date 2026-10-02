# Bug Fix: Deleting a wishlist pops more than one screen

- **Slug**: double-nav-back
- **Fixed**: 2026-10-02
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Deleting a list now leaves the screen through one path only: the list disappearing from storage. The
delete call no longer sends `NavigateBack` itself, and repeated "list is gone" reports collapse into a
single `NavigateBack`. One delete now pops exactly one screen.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModel.kt` | modified | `deleteList()` keeps only the failure snackbar. The `uiState` pipeline gains `distinctUntilChanged { old, new -> old == null && new == null }` before the existing `onEach`. |
| `feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModelTest.kt` | modified | One test renamed and inverted, two added, one helper added. |

## Diff Highlights (optional)

```kotlin
internal val uiState: StateFlow<WishlistUiState> = getWishlistDetailUseCase(listId)
    // Deleting a list that holds games invalidates several queries, so the flow can report it gone more
    // than once. Only the first report may navigate; every other emission has to get through.
    .distinctUntilChanged { old, new -> old == null && new == null }
    .onEach { detail ->
        if (detail == null) _uiEffect.send(WishlistUiEffect.NavigateBack)
    }
```

```kotlin
private fun deleteList() {
    viewModelScope.launch {
        if (!deleteListUseCase(listId)) {          // success: nothing to send, the list vanishing navigates
            _uiEffect.send(ShowSnackbar(UiText.StringResource(R.string.unable_to_delete_wishlist)))
        }
    }
}
```

## Tests Added or Updated

- `WishlistViewModelTest::a successful delete does not navigate by itself` (replaces `a successful delete
  navigates back`): a successful delete emits no effect of its own. This pins down the first of the two
  sources.
- `WishlistViewModelTest::a list that vanishes navigates back once, however often it is reported gone`:
  a detail flow of `detail, null, null` produces exactly one `NavigateBack`. This pins down the repeated
  `null` that closed the app.
- `WishlistViewModelTest::updates to the list keep reaching the state after it was reported gone once or never`:
  a guard against the obvious wrong fix. A `distinctUntilChangedBy { it == null }` on the whole flow would
  have dropped every non-null update after the first one. This test fails if someone writes that.
- Unchanged and still passing: `a refused delete shows a snackbar and stays on the screen`.

Red then green: before the change, the first two failed and the guard passed. After it, all 9 pass.

## Local Verification

- `./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q` → 9 tests, 0 failures (was 2 failing
  before the fix).
- `./gradlew test :app:assembleDebug --console=plain -q` → exit 0; 323 unit tests across all modules,
  0 failures.
- `grep -rn NavigateBack` over `feature/` and `app/` → one producer left
  (`WishlistViewModel.kt:48`), one consumer (`WishlistScreen.kt:62`).
- Manual checks: none yet. The on-device check is left to `/speckit-bug-test`, which should delete a list
  with no games and a list with games and confirm both land on the Lists overview with the app still open.

## Deviations from Assessment

- **The assessment's example `distinctUntilChangedBy { it == null }` was not used.** Applied to the whole
  flow it keys every non-null emission as "unchanged", so after the first update it would silently drop
  later ones (a game added or removed, the default moving). I used
  `distinctUntilChanged { old, new -> old == null && new == null }`, which treats only two consecutive
  `null`s as duplicates. The assessment called its version "for example", so the preferred approach (collapse
  repeated `null`s, drop the delete's own `NavigateBack`) is unchanged.
- No other deviation. The change stayed within the two files the assessment listed. The optional nav-layer
  guard on `onBackClick` was not added, as the assessment advised.

## Follow-ups

- Run `/speckit-bug-test slug=double-nav-back` to confirm on a device: delete a list with no games and
  one with games.
- The third pop came from repeated `null` emissions, which the assessment rated medium-high confidence.
  If the app still closes after the fix, that theory was wrong; log the `null` emissions in the `onEach`.
- The unguarded `onBackClick = { backStack.removeLastOrNull() }` on every route is still there. It is a
  separate, app-wide question and was left alone on purpose.
- The fix is uncommitted, together with your own `GameReleaseInfoCard.kt` formatting cleanup.
