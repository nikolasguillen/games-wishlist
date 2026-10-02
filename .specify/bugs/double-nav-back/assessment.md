# Bug Assessment: Deleting a wishlist pops more than one screen

- **Slug**: double-nav-back
- **Created**: 2026-10-02
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> deleting a list pops two screens. The delete sends "navigate back" itself, and the "list vanished" check
> sends it again.

The report names the cause as well as the symptom. Both claims are confirmed below. The reproduction on the
emulator shows the damage is larger than "two screens": with a list that holds games, the app closes.

## Symptom

Confirming "Delete list" on a wishlist's detail screen navigates back more than once. Expected: one step
back, onto the Lists overview the user came from.

## Reproduction

Reproduced on an emulator (API 37) on 2026-10-02, while running the spec 005 quickstart.

1. Open the Lists tab, so the back stack is `[Search, Lists]`, then open a non-default list, so it is
   `[Search, Lists, Wishlist]`.
2. Choose "⋮" then "Delete list", and confirm.
3. **List with no games**: the app lands on the Search tab, which is two pops.
4. **List with at least one game**: the app leaves to the Android home screen, which is three pops
   (the stack is emptied).

Not observed: deleting a list from anywhere other than its own detail screen. There is no such path today,
because this is the only place lists are deleted.

## Suspected Code Paths

- `feature/wishlist/.../WishlistViewModel.kt:41-43`: `uiState` pipeline. `.onEach { detail -> if (detail == null)
  _uiEffect.send(NavigateBack) }` fires every time the observed list is `null`, which is *every*
  emission after the list is gone. This is the "list vanished" check.
- `feature/wishlist/.../WishlistViewModel.kt:84-87`: `deleteList()` sends `NavigateBack` itself when the use
  case returns `true`. This is the second source.
- `feature/wishlist/.../WishlistScreen.kt:62`: the effect collector calls `onBackClick()` once per
  `NavigateBack`. All queued effects are consumed in one loop before the screen recomposes away, so each
  one pops.
- `app/.../QuestLogNavDisplay.kt:194`: `onBackClick = { backStack.removeLastOrNull() }` is an unguarded pop.
  It will remove the last remaining entry, which is what closes the app.
- `core/domain/.../GetWishlistDetailUseCase.kt`: `combine(observeListById, getGamesByList,
  observeDefaultListId)`. Each upstream re-emits independently, so `null` can reach `onEach` more than once.
- `core/database/.../ListDao.kt:57-61`: `deleteListWithGameRefs` deletes the cross-ref rows and then the list in
  one transaction. When the list has games, both `observeListById` and `getGamesByList` are invalidated;
  when it has none, no cross-ref row changes, so only `observeListById` re-emits.
- `app/.../MainActivity.kt:118-125`: switching tab resets the stack to `[Search, <tab>]`, which is why the
  list-with-no-games case ends on Search.

## Root Cause Hypothesis

One user action, "delete this list", is reported to navigation by two independent producers, and one of
them can repeat:

1. `deleteList()` sends `NavigateBack` on success (1 pop).
2. The detail flow then emits `null` because the row is gone, and `onEach` sends `NavigateBack` (1 pop per
   `null` emission).

A list with no games produces one `null`, which is 2 pops and ends on Search. A list with games produces
two `null` emissions, because the games query is invalidated as well, which is 3 pops. The stack
`[Search, Lists, Wishlist]` is emptied and the app closes. This matches both outcomes seen on the
emulator.

The `onEach` check was written for a list deleted "from another screen", but no other screen deletes
lists. So today its only trigger is this screen's own delete.

This is **long-standing, not a regression of spec 005**: both `NavigateBack` sends are on lines 44 and 74
of the file at `b52c5323`, before the feature work.

**Confidence**: high that two producers exist and that both fire (confirmed in the code and by the
emulator outcome of a 2-pop landing on Search). Medium-high that the third pop comes from a repeated
`null` emission. That explains the app closing, but I did not log the emissions to confirm it.

## Proposed Remediation

**Preferred**: make the database the single source of truth for leaving the screen, which is also what
the file's own comment ("Single source of truth: reactively observes local storage") describes.

- In `deleteList()`, stop sending `NavigateBack` on success. Keep the failure snackbar. Once the delete
  has gone through, the detail flow emits `null` and navigation follows.
- Make the "vanished" signal fire once: collapse repeated `null`s before `onEach`, for example with
  `distinctUntilChangedBy { it == null }`, so a list that disappears produces exactly one `NavigateBack`
  however many upstream flows re-emit.

Result: deleting any list pops exactly one screen, and the "deleted from elsewhere" case keeps working.

**Alternatives**:
- *Keep the delete's own `NavigateBack`, remove the `onEach` one.* Fewer moving parts, but it drops the
  safety net for a list removed from elsewhere. It also leaves the screen briefly rendering the default
  empty state between the delete and the pop.
- *Guard the pop at the nav layer only:* `onBackClick = { if (backStack.size > 1) backStack.removeLastOrNull() }`.
  It stops the app closing, but the user would still land on Search instead of Lists, so it hides the
  symptom. Worth considering as extra hardening, but not as the fix. It would also make this one route
  inconsistent with the other route lambdas, which pop unguarded.

**Files likely to change**:
- `feature/wishlist/src/main/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModel.kt`
- `feature/wishlist/src/test/java/com/nikolasguillen/questlog/feature/wishlist/WishlistViewModelTest.kt`

**Tests to add or update**:
- Update `a successful delete navigates back` in `WishlistViewModelTest`. It currently asserts that a
  successful delete itself emits `NavigateBack`. It should assert that the delete emits *no* effect on its
  own.
- Add: a detail flow that emits a list and then `null` twice results in exactly one `NavigateBack`.
- Keep `a refused delete shows a snackbar and stays on the screen` as it is.
- Manual, on the emulator: delete a list with no games, then one with games. Both should land on the Lists
  overview, with the app still open.

## Risks & Considerations

- **Ordering.** Deleting succeeds before the `null` arrives, so there is a short window where the screen
  shows the old list. It is the same window that exists today, and no worse.
- **A delete that succeeds but isn't observed.** The navigation now depends on the screen collecting the
  detail flow. It always does while visible, because `uiState` is collected with
  `collectAsStateWithLifecycle`. If the screen is stopped mid-delete, the effect stays buffered and is
  delivered when it resumes.
- **No data or API impact.** UI-layer only. The delete use case, the database and the foreign-key
  protection of the default list are untouched.
- **Do not widen the scope.** The unguarded `removeLastOrNull()` on every route is a separate, app-wide
  question. This fix should not change it.

## Open Questions

- None blocking. If you want certainty about the third pop, log the `null` emissions in `onEach` while
  deleting a list with games before changing anything.
