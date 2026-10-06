# Quickstart: Edit Wishlist

How to prove the feature works. Contracts: [data-and-domain](./contracts/data-and-domain.md),
[wishlist-form-sheet-ui](./contracts/wishlist-form-sheet-ui.md),
[wishlist-screen-ui](./contracts/wishlist-screen-ui.md).

## Build and tests

macOS / Linux (on Windows use `.\gradlew.bat` with the same arguments):

```bash
./gradlew :core:ui:compileDebugKotlin --console=plain -q
./gradlew :core:data:testDebugUnitTest --console=plain -q        # GameRepositoryImplUpdateListTest
./gradlew :feature:wishlist:testDebugUnitTest --console=plain -q # WishlistViewModelTest
./gradlew :feature:lists:testDebugUnitTest --console=plain -q    # unchanged, must stay green
./gradlew :app:assembleDebug                                     # spans modules + Hilt wiring
./gradlew test                                                   # every suite green
```

There is no CI, lint or formatter gate. These commands are the verification.

## Manual scenarios (debug build on a device or emulator)

| # | Steps | Expected |
|---|-------|----------|
| 1 | Lists tab → open a **non-default** list. | The top bar shows ✏️, ✓ and 🗑. |
| 2 | Tap ✏️. | The sheet is titled "Edit Wishlist" and the button says "Save". Name, description, icon and cover are pre-filled, and the stored cover image renders (it is a file path, not a URI). |
| 3 | Change the name, then tap Save. | The sheet closes and the header shows the new name right away. No snackbar. |
| 4 | Back to the Lists tab. | The row shows the new name, the same game count and the same Default badge state. |
| 5 | Open the **default** list. | Only ✏️ is in the top bar, and the "Default" badge is present. |
| 6 | Edit the default list's name and icon → Save. | It is updated and still shows "Default". From a game's detail, the heart still saves to this list. |
| 7 | Open the edit form, type a new name, then tap Cancel. Reopen it. | The original name is shown and the list is unchanged. |
| 8 | Clear the name field. | Save is disabled. |
| 9 | Remove the cover → Save. | The header falls back to the icon. |
| 10 | Pick a new cover → Save. | The new image is shown in the header and on the overview row. |
| 11 | Enter `"  Spaced  "` as the name → Save. | Stored and shown as `Spaced`. |
| 12 | Open edit, type something, rotate the device. | The sheet is still open with the typed text. |
| 13 | Clear a description → Save. | The header no longer shows a description line. |
| 14 | Add a game to the list from game detail. | The list picker shows the edited name. |
| 15 | Lists tab → **+ Create New Wishlist**. | The same form opens, titled "New Wishlist" with a "Create" button, and is empty. Creation works as before. |

## Optional storage check (scenario 10)

```bash
adb shell run-as com.nikolasguillen.questlog ls files/wishlist_covers
```

After replacing or removing a cover, the old file is gone and at most one file per list remains.
