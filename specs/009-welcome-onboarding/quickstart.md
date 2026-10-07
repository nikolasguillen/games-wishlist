# Quickstart: validating Welcome Onboarding

## Automated checks

```bash
./gradlew :app:assembleDebug                                          # new module + DI wiring
./gradlew :feature:onboarding:testDebugUnitTest --console=plain -q    # OnboardingViewModelTest
./gradlew :core:data:testDebugUnitTest --console=plain -q             # store + repository toggle
./gradlew :core:ui:testDebugUnitTest --console=plain -q               # PlatformPickerMapperTest
./gradlew :feature:settings:testDebugUnitTest --console=plain -q      # OwnedPlatformsViewModelTest after the move
./gradlew test                                                        # every suite stays green
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`.

The new tests cover the following. The ViewModel tests use the same setup as the other feature tests
(JUnit4, MockK and `StandardTestDispatcher`).

- **`OnboardingPreferenceStoreImplTest`**:
  - The store reads `false` before any write.
  - It reads `true` after `setOnboardingCompleted()`.
  - The value survives a fresh store over the same file.
  - Writing twice is harmless.
- **`GameRepositoryImplToggleOwnedPlatformTest`** (replaces the `SetOwnedPlatforms` test): the Discover
  cache is cleared before `platformDao.toggleOwnedPlatform(id)`.
- **`PlatformPickerMapperTest`** (cases moved from `OwnedPlatformsViewModelTest`):
  - curated platforms come first, then by generation, then by name;
  - the pinned set comes first only when the query is blank;
  - search matches on name and abbreviation;
  - `null` pins give `Loading`, an empty catalogue gives `Empty`, and no matches give `NoSearchResults`.
- **`OwnedPlatformsViewModelTest`** (trimmed): a tap calls `ToggleOwnedPlatformUseCase` with that id; the
  catalogue sync runs on init and on retry; the state is built from the shared mapper.
- **`OnboardingViewModelTest`**:
  - The platforms wiring matches the trimmed Settings test above: a tap calls the toggle use case, the sync
    runs on init and on retry, and the selected count follows the stored selection.
  - The page list leaves out `Reminders` when `requiresRuntimePermission = false`, and also when
    `canDeliver = true`.
  - The page list includes `Reminders` last otherwise.
  - A second `NotificationFactsResolved` does not rebuild the list.
  - `AllowNotificationsClicked` sends `RequestNotificationPermission`.
  - `NotificationPermissionResult(true)` leads to `Granted`, and `(false)` leads to `Declined`.
  - `NotNowClicked` leads to `Declined`.
  - `Declined` followed by `PermissionStateChanged(true)` leads to `Granted`.
  - `SkipClicked` and `FinishClicked` each call `CompleteOnboardingUseCase` once, then send `Finished`.
    Repeat taps send only one `Finished`.

## Manual run on a device or emulator

Reset to a first launch with `adb shell pm clear com.nikolasguillen.questlog`.

Use an API 33+ emulator for the reminders steps. Use an API 29–32 emulator for scenario 9.

| # | Steps | Expected |
|---|-------|----------|
| 1 | Cold launch after `pm clear` | The flow appears straight away. There is no flash of Search and no bottom bar. |
| 2 | Swipe or tap Next through every page | The position indicator moves. System back on page 2+ goes to the previous page. |
| 3 | On the platforms page, pick 2 platforms, then Skip | Search opens. Settings → Owned platforms shows both. Relaunch: no flow. |
| 4 | `pm clear`, go to the reminders page, tap "Allow notifications", grant | Confirmation, then "Get started" → Search. Settings shows no "Notifications are off" row. |
| 5 | `pm clear`, go to the reminders page, tap "Allow", deny | The "Reminders are off, change it in Settings" text appears and "Get started" works. |
| 6 | `pm clear`, go to the reminders page, tap "Not now" | Same as 5, and no system dialog is ever shown. |
| 7 | After 5, deny again to make it permanent, then Settings → "Show welcome tour" → reminders page → "Allow" | It goes straight to the declined text and no settings screen opens. Finishing returns to Settings. |
| 8 | Finish the flow, then press back on Search | The app closes; it does not return into the flow. |
| 9 | API 29–32 emulator, `pm clear`, go through the flow | There is no reminders page. The platforms page is last and shows "Get started". |
| 10 | Airplane mode, `pm clear`, go through the flow | The platforms page shows its empty state with retry and a Settings hint. The flow still completes. |
| 11 | Rotate on page 3, and on the platforms page with a search query typed | The same page, query and picks remain. |
| 12a | On page 4, swipe the app away from recents and relaunch | The flow starts again from page 1 (nothing was completed), and picks made before are still selected on the platforms page. |
| 12b | On page 4, background the app, run `adb shell am kill com.nikolasguillen.questlog`, then reopen it from recents | The system restores the same page (4) and the same picks, as it does after rotation. |
| 13 | Switch the system to dark mode, then light mode; set the largest font size and display size | Every page is readable, with text scrolling if needed, and all controls stay reachable. On the platforms page the headline, body and caption scroll away with the list, the search field stays pinned, and the list is usable. |
| 14 | TalkBack on | Each page's headline, body and buttons are announced. |
| 15 | Time a read-through of the full flow | Under 60 seconds (SC-001). |
| 16 | Settings → Owned platforms: tap three platforms as fast as possible, then untap one | Exactly two remain selected, in both the list and the Settings summary. The picker looks, orders and searches exactly as before the move. |
| 17 | `pm clear`, then `adb shell am start -a android.intent.action.VIEW -d questlog://game/1942 com.nikolasguillen.questlog` | The flow is shown first. Finishing it lands on Search, not on the game, because the link is ignored while the flow is the root. |
| 18 | Inspection: open `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml` | Both are still the unmodified samples, with no `<exclude>` for the `settings` DataStore, so a restored backup brings the completed flag back and the flow is not shown. No emulator run needed. |
