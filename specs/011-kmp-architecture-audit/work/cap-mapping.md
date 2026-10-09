# R-CAP-01 — Platform source → contract mapping

Baseline `b982bf11`. All 48 git-tracked `.kt` files under `*/src/androidMain` (27) and `*/src/iosMain` (21), from
`git ls-files '*/src/androidMain/*.kt' '*/src/iosMain/*.kt'`. "Row" is the section of
`specs/010-kmp-migration/contracts/platform-contracts.md` (PC). Paths are relative to
`<module>/src/<set>/kotlin/com/nikolasguillen/questlog/`.

Status: `mapped` (one row found), `unmapped` (no row, a finding), `counterpart missing` (a finding). A "counterpart" of
`commonMain` means the other platform's implementation of that contract is a class in `commonMain` bound by that
platform's Koin module, as `core/data/CLAUDE.md` documents.

## Android (27)

| # | File | Maps to (PC row) | Counterpart | Status |
|---|---|---|---|---|
| 1 | `core/common` · `AppVersionProviderImpl.kt` | `AppVersionProvider` | iOS `AppVersionProviderImpl.kt` | mapped |
| 2 | `core/common` · `NetworkStatusProviderImpl.kt` | `NetworkStatusProvider` | iOS `NetworkStatusProviderImpl.kt` | mapped |
| 3 | `core/common` · `DateRendering.android.kt` | "`DateUtils` platform rendering" | iOS `DateRendering.ios.kt` | mapped |
| 4 | `core/common` · `di/CommonPlatformModule.android.kt` | binds #1, #2 (Rule: bound in the platform module) | iOS `CommonPlatformModule.ios.kt` | mapped |
| 5 | `core/data` · `di/DataPlatformModule.android.kt` | binds #6–#16 (Rule: bound in the platform module) | iOS `DataPlatformModule.ios.kt` | mapped |
| 6 | `core/data` · `local/WishlistCoverImageStorageImpl.kt` | `WishlistCoverImageStorage` | iOS `WishlistCoverImageStorageImpl.kt` | mapped |
| 7 | `core/data` · `mapper/TranslationMapper.kt` | Compat seam: "`TranslationPromptBuilder`, `TranslationArtifactSanitizer`, `TranslationMapper` (Android only)" | iOS `UnsupportedGameDescriptionTranslator` (`commonMain`) | mapped |
| 8 | `core/data` · `notification/ReleaseNotifierImpl.kt` | `ReleaseNotifier` | iOS `NoOpReleaseNotifier` (`commonMain`) | mapped |
| 9 | `core/data` · `repository/RepositoryErrorMapper.android.kt` | "Connectivity classification" | iOS `RepositoryErrorMapper.ios.kt` | mapped |
| 10 | `core/data` · `scheduler/ReleaseNotificationSchedulerImpl.kt` | `ReleaseNotificationScheduler` | iOS `NoOpReleaseNotificationScheduler` (`commonMain`) | mapped |
| 11 | `core/data` · `scheduler/ReleaseRefreshSchedulerImpl.kt` | `ReleaseRefreshScheduler` | iOS `InProcessReleaseRefreshScheduler` (`commonMain`) | mapped |
| 12 | `core/data` · `translation/GameDescriptionTranslatorImpl.kt` | `GameDescriptionTranslator` | iOS `UnsupportedGameDescriptionTranslator` (`commonMain`) | mapped |
| 13 | `core/data` · `translation/TranslationArtifactSanitizer.kt` | Compat seam (Android-only translator part) | n/a, documented | mapped |
| 14 | `core/data` · `translation/TranslationPromptBuilder.kt` | Compat seam (Android-only translator part) | n/a, documented | mapped |
| 15 | `core/data` · `worker/ReleaseDatesRefreshWorker.kt` | `ReleaseRefreshScheduler` (WorkManager job it enqueues) | iOS `InProcessReleaseRefreshScheduler` | mapped |
| 16 | `core/data` · `worker/ReleaseNotificationWorker.kt` | `ReleaseNotificationScheduler` / `ReleaseNotifier` (WorkManager job) | iOS no-ops | mapped |
| 17 | `core/database` · `di/DatabasePlatformModule.android.kt` | PC "Platform shell" does not list it; `core/database/CLAUDE.md` + `module-contracts.md` ("`*PlatformModule`") cover it: the Room builder is per platform | iOS `DatabasePlatformModule.ios.kt` | mapped (see note 1) |
| 18 | `core/designsystem` · `theme/SystemBarsAppearance.android.kt` | `SystemBarsAppearance` | iOS `SystemBarsAppearance.ios.kt` (no-op, documented) | mapped |
| 19 | `core/network` · `PlatformTransportFailure.android.kt` | "Connectivity classification" | iOS `PlatformTransportFailure.ios.kt` | mapped |
| 20 | `core/network` · `di/NetworkPlatformModule.android.kt` | binds `ElapsedRealtimeSource` and the OkHttp engine (`core/network/CLAUDE.md`) | iOS `NetworkPlatformModule.ios.kt` | mapped (see note 1) |
| 21 | `core/ui` · `component/FullScreenDialogProperties.android.kt` | Compat seam: `fullScreenDialogProperties()` | iOS `…ios.kt` | mapped |
| 22 | `core/ui` · `component/NotificationPermissionDeniedDialog.android.kt` | "Notification permission UI" | iOS `…ios.kt` (inert) | mapped |
| 23 | `core/ui` · `util/CoverImagePicker.android.kt` | `rememberCoverImagePicker` | iOS `CoverImagePicker.ios.kt` | mapped |
| 24 | `core/ui` · `util/NotificationPermission.android.kt` | "Notification permission UI" | iOS `NotificationPermission.ios.kt` (inert) | mapped |
| 25 | `core/ui` · `util/TextSharer.android.kt` | `rememberTextSharer` | iOS `TextSharer.ios.kt` | mapped |
| 26 | `feature/search` · `components/SearchBarScrollBehaviorCompat.android.kt` | Compat seam: `SearchBarScrollBehaviorCompat` | iOS `…ios.kt` | mapped |
| 27 | `shared` · `RunBlocking.android.kt` | Compat seam: `runBlockingCompat` | iOS `RunBlocking.ios.kt` | mapped |

## iOS (21)

| # | File | Maps to (PC row) | Counterpart | Status |
|---|---|---|---|---|
| 28 | `core/common` · `AppVersionProviderImpl.kt` | `AppVersionProvider` | Android #1 | mapped |
| 29 | `core/common` · `NetworkStatusProviderImpl.kt` | `NetworkStatusProvider` | Android #2 | mapped |
| 30 | `core/common` · `ApplicationSupport.kt` | Compat seam: `applicationSupportDirectory()` (iOS only) | n/a, documented | mapped |
| 31 | `core/common` · `DateRendering.ios.kt` | "`DateUtils` platform rendering" | Android #3 | mapped |
| 32 | `core/common` · `di/CommonPlatformModule.ios.kt` | platform module | Android #4 | mapped |
| 33 | `core/data` · `di/DataPlatformModule.ios.kt` | platform module | Android #5 | mapped |
| 34 | `core/data` · `local/WishlistCoverImageStorageImpl.kt` | `WishlistCoverImageStorage` | Android #6 | mapped |
| 35 | `core/data` · `repository/RepositoryErrorMapper.ios.kt` | "Connectivity classification" | Android #9 | mapped |
| 36 | `core/database` · `di/DatabasePlatformModule.ios.kt` | platform module | Android #17 | mapped (note 1) |
| 37 | `core/designsystem` · `theme/SystemBarsAppearance.ios.kt` | `SystemBarsAppearance` | Android #18 | mapped |
| 38 | `core/network` · `PlatformTransportFailure.ios.kt` | "Connectivity classification" | Android #19 | mapped |
| 39 | `core/network` · `di/NetworkPlatformModule.ios.kt` | platform module | Android #20 | mapped (note 1) |
| 40 | `core/ui` · `component/FullScreenDialogProperties.ios.kt` | Compat seam | Android #21 | mapped |
| 41 | `core/ui` · `component/NotificationPermissionDeniedDialog.ios.kt` | "Notification permission UI" | Android #22 | mapped |
| 42 | `core/ui` · `util/CoverImagePicker.ios.kt` | `rememberCoverImagePicker` | Android #23 | mapped |
| 43 | `core/ui` · `util/NotificationPermission.ios.kt` | "Notification permission UI" | Android #24 | mapped |
| 44 | `core/ui` · `util/TextSharer.ios.kt` | `rememberTextSharer` | Android #25 | mapped |
| 45 | `core/ui` · `util/TopViewController.kt` | Compat seam: `TopViewController` (iOS only) | n/a, documented | mapped |
| 46 | `feature/search` · `components/SearchBarScrollBehaviorCompat.ios.kt` | Compat seam | Android #26 | mapped |
| 47 | `shared` · `MainViewController.kt` | "Platform shell": iOS entry point | `:androidApp` `MainActivity` | mapped |
| 48 | `shared` · `RunBlocking.ios.kt` | Compat seam: `runBlockingCompat` | Android #27 | mapped |

## Result

48 of 48 files map to a row. 0 `unmapped`, 0 `counterpart missing`.

**Note 1.** The `DatabasePlatformModule` and `NetworkPlatformModule` files are not named in `platform-contracts.md`:
they are the Room builder and the HTTP engine / monotonic clock, bound per platform, and are described in
`core/database/CLAUDE.md`, `core/network/CLAUDE.md` and `module-contracts.md`. They fit the rule "a service with state
is an interface bound in the module's `*PlatformModule`" (R-CAP-03) and R-CAP-01 is met through those documents rather
than through `platform-contracts.md`. See C-CAP-1.

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-CAP-1 | `platform-contracts.md` does not list the database builder and the network engine/clock as platform capabilities | Rows 17, 20, 36 and 39 are bound per platform and documented in two `CLAUDE.md` files, but the file the root `CLAUDE.md` names as the authority ("Every file in `androidMain` or `iosMain` implements one of the capabilities in `specs/010-kmp-migration/contracts/platform-contracts.md`") has no row for them. The code is right and the contract list is incomplete. Severity low, follow-up (doc) |
