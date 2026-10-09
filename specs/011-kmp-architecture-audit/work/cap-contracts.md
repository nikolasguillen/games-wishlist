# R-CAP — Contract completeness

Baseline `b982bf11`. Complements `cap-mapping.md` (file → contract). Here: contract → implementations, `expect` →
`actual`, signatures, and the capability exception register against the iOS code.

## Contract → implementations

Interfaces and signatures were read from the code and compared to `platform-contracts.md`; all match.

| Contract (module) | Android | iOS | Bound in |
|---|---|---|---|
| `ReleaseRefreshScheduler` (`:core:domain`) | `ReleaseRefreshSchedulerImpl` (WorkManager) | `InProcessReleaseRefreshScheduler` (`commonMain`, `Clock.System`) | `dataPlatformModule` `actual`s |
| `ReleaseNotificationScheduler` (`:core:domain`) | `ReleaseNotificationSchedulerImpl` | `NoOpReleaseNotificationScheduler` (`commonMain`) | same |
| `ReleaseNotifier` (`:core:domain`) | `ReleaseNotifierImpl` | `NoOpReleaseNotifier` (`commonMain`) | same |
| `GameDescriptionTranslator` (`:core:domain`) | `GameDescriptionTranslatorImpl` (+ `GeminiNanoClient` from `:core:ai`) | `UnsupportedGameDescriptionTranslator` (`commonMain`): `modelStatus() = UNSUPPORTED`, `translate() = null`, `downloadModel() = flowOf(Failed)` | same |
| `ReleaseRemindersAvailability` (`:core:domain`) | `StaticReleaseRemindersAvailability(true)` | `StaticReleaseRemindersAvailability(false)` | same |
| `WishlistCoverImageStorage` (`:core:data`) | `WishlistCoverImageStorageImpl` | `WishlistCoverImageStorageImpl` | same (`factoryOf … bind<>()` on both) |
| `AppVersionProvider`, `NetworkStatusProvider` (`:core:common`) | `…Impl` | `…Impl` | `commonPlatformModule` `actual`s |
| `rememberCoverImagePicker`, `rememberTextSharer`, notification-permission UI, `fullScreenDialogProperties` (`:core:ui`) | `actual`s in `androidMain` | `actual`s in `iosMain` (permission ones inert) | n/a (composables) |
| `SystemBarsAppearance` (`:core:designsystem`) | `actual` (`WindowCompat`) | `actual` no-op | n/a |
| Date rendering (`:core:common`) | `java.time` | `NSDateFormatter` | n/a |
| Connectivity classification (`:core:data`, `:core:network`) | `UnknownHost/Connect/Socket…`; network `null` | the network module's two types; `NSError` translation in `:core:network` | n/a |

## `expect` → `actual`

19 `expect` declarations in `commonMain`. 18 of them have a hand-written `actual` in `androidMain` **and** in
`iosMain` (17 distinct names; `renderLocalDate` has two overloads, each with its own `actual` on both platforms). The
nineteenth, `expect object QuestLogDatabaseConstructor`, has Room-generated `actual`s on every target, as
`core/database/CLAUDE.md` states (the Android `.class` and `.dex` are in `core/database/build`). 0 `expect` is missing
an `actual`.

Shape: 5 are Koin platform modules (4 `val`s) or Room's constructor (1 `object`); 14 are single functions,
composables or extension properties. There is no `expect class`, so R-CAP-03 holds: services with state are
interfaces in `commonMain`.

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-CAP-02 | **pass** | Every contract above is an interface (or a single `expect` function) in `commonMain` with one implementation per platform |
| R-CAP-03 | **pass** | No `expect class`; the stateful services are interfaces bound in `*PlatformModule` `actual`s |
| R-CAP-04 | **pass** | Per-name check on both platforms: 17 names hand-written (18 declarations) + 1 Room-generated |
| R-CAP-05 | **pass** | No `Context`, `Uri`, `NSURL`, `UIViewController`, `Bundle`, `Activity`, `NSError` or `Intent` in any `commonMain` file that contains an `expect` or one of the contract interfaces. The only non-primitive types are Compose ones (`DialogProperties`, `SearchBarScrollBehavior`, `Modifier`) and `kotlinx-datetime`'s `LocalDate` |
| R-CAP-06 | **pass** | 0 files or classes named `Fake*`/`Stub*`/`Mock*` in any `commonMain`; doubles live in `androidHostTest` |
| R-CAP-07 | **pass** | `core/domain/src` has `commonMain` and `androidHostTest` only |
| R-CAP-08 | **pass** | `RELEASE_DATES_REFRESH_INTERVAL = 24.hours` in `scheduler/ReleaseRefreshInterval.kt`; used by `ReleaseRefreshSchedulerImpl` (Android, `inWholeHours`) and `InProcessReleaseRefreshScheduler` (iOS, `inWholeMilliseconds`) |
| R-CAP-09 | **pass** | Register vs code, below |
| R-CAP-10 | **pass** | `RepositoryErrorMapper.ios.kt`: `this is IgdbConnectivityException` / `this is IgdbTimeoutException`; the Darwin `NSError` translation is `PlatformTransportFailure.ios.kt` in `:core:network` |
| R-CAP-11 | **pass** (one wording point) | Background refresh, notifications and translation each sit behind a `:core:domain` contract. Splash is not behind a contract: it is an Android-shell concern (`MainActivity` + `RootViewModel`), listed under "Platform shell (not contracts)" in `platform-contracts.md`. See C-CAP-2 |

## Capability exception register vs code

| Register row | Code evidence | Match |
|---|---|---|
| Release reminders absent on iOS | `ReleaseRemindersAvailability` = `false` on iOS; injected by `SettingsViewModel`, `GameDetailViewModel`, `OnboardingViewModel`, `RadarViewModel` (the four consumers the contract names). iOS permission actuals are inert and documented as never read | yes |
| Refresh while the app is closed: none on iOS | `InProcessReleaseRefreshScheduler`; `MainViewController` calls `schedulePeriodicRefresh()` once per launch (guarded by `KoinPlatform.getKoinOrNull() == null`) | yes |
| On-device translation absent on iOS | `UnsupportedGameDescriptionTranslator` returns `UNSUPPORTED` | yes |
| Status-bar icon colour | `SystemBarsAppearance.ios.kt` is a no-op | yes |
| Deep link not registered on iOS | `MainViewController` passes `pendingDeepLinkGameId = null` | yes |
| Display corner radius fixed on iOS | `displayCornerRadius = 0.dp` in `MainViewController` | yes (the "Platform shell" table says "fixed default") |

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-CAP-2 | The constitution lists the splash screen among the capabilities that "MUST be reached through a contract owned by shared code", but `platform-contracts.md` deliberately classes it as platform shell | `.specify/memory/constitution.md` ("Kotlin Multiplatform", last bullet) vs `specs/010-kmp-migration/contracts/platform-contracts.md` "Platform shell (not contracts)". The code follows the contracts file (no splash contract exists). Per the precedence rule the source and `CLAUDE.md` win and the constitution is corrected. Severity low, follow-up (doc) |
