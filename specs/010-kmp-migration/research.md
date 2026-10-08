# Research: Multiplatform Migration

All decisions below were taken against `develop` at `fa1a7978` (the spec branched from there). Library
versions were checked against public release notes on 2026-10-08. Where a version is still to be pinned,
the task that introduces the library pins it from the source named here, and does not guess.

The Technical Context in `plan.md` has no open `NEEDS CLARIFICATION` items. This file records how each
was resolved.

## Inventory that drives the plan

Measured on `develop` (main sources only, `build/` excluded):

| Module | `.kt` files | Platform-specific code that has to move or change |
|---|---|---|
| `:core:model` | 31 | none |
| `:core:common` | 5 | `DateUtils` (`java.time`, `Locale`), `NetworkStatusProvider` (`ConnectivityManager`), `AppVersionProvider` (`PackageManager`), Hilt |
| `:core:domain` | 62 | `java.time.LocalDate` in `GetDiscoverFeedUseCase`, `javax.inject` on 46 files |
| `:core:network` | 19 | Retrofit, Moshi (12 `@JsonClass` DTOs), OkHttp interceptor, `SystemClock`, `BuildConfig` credentials |
| `:core:database` | 40 | `Room.databaseBuilder(Context)`, `SupportSQLiteDatabase` seed callback, Android string resources for the seed |
| `:core:data` | 19 | DataStore via `Context`, WorkManager (2 workers, 2 schedulers), notifications, `ImageDecoder`/`Bitmap`/`Uri` cover storage, `java.net` exceptions in `RepositoryErrorMapper`, ML Kit via `:core:ai` |
| `:core:ai` | 3 | ML Kit GenAI. **Stays Android-only.** |
| `:core:designsystem` | 7 | `WindowCompat` and `Activity` for status-bar icon colours |
| `:core:ui` | 61 | 20 files read `R.*`, `HtmlCompat`, photo picker, notification permission, `UiText.asString(Context)` |
| `:core:navigation` | 1 | Navigation 3 runtime (`NavKey`) |
| `feature/*` (7) | 140 | `R.*` in 52 files, `LocalContext` for snackbar text (5 screens), share `Intent` (game detail), `BackHandler` (2), `shadowglow` (1) |
| `:app` | 4 | Activity, splash, edge-to-edge, `RoundedCorner`, Hilt entry points, `QuestLogNavDisplay`, `QuestLogBottomBar` |

Other facts:

- **Tests**: 48 test files; 31 use MockK, and 4 in `:core:data` touch Android types.
- **DAOs**: already `suspend` or `Flow`, as Room KMP requires.
- **Material 3**: no Expressive APIs are used, only `ExperimentalMaterial3Api`.
- **Strings**: there is exactly one locale (`values/`, English), about 385 lines of `strings.xml` across 11 modules.
- **Fonts**: none bundled. `Type.kt` uses `FontFamily.Default`.

## R1 — Toolchain and Compose Multiplatform version

**Decision**:

- **Kotlin**: bump 2.4.10 → **2.4.20**.
- **Compose Multiplatform**: Gradle plugin **1.12.1**. This is the pairing in JetBrains' current Compose compiler guide; CMP 1.12.0 shipped in August 2026.
- **Multiplatform AndroidX artifacts**: take the JetBrains-published versions listed in CMP 1.12.1's "Based on Jetpack" release table:
  - `org.jetbrains.androidx.navigation3:navigation3-ui`;
  - `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` and `lifecycle-viewmodel-navigation3`;
  - `org.jetbrains.compose.material3:material3`.
- **Gradle**: AGP 9.4.1 and Gradle 9.7.1 stay.

**Rationale**: CMP releases are tied to a Kotlin version through the Compose compiler. 2.4.20 is the current tooling release, so moving to it is a patch bump with no language change. The JetBrains coordinates are what make lifecycle, Navigation 3 and Material 3 resolve for iOS. On the Android target they redirect to Google's `androidx` artifacts, so Android keeps running the Jetpack code it runs today.

**Risk to track**:

- **Material 3 is still alpha** on the multiplatform side: `material3:1.12.0-alpha03`, based on Jetpack Material3 1.5.0-alpha22.
- **What Android resolves**: Gradle conflict resolution should keep Android on `androidx.compose.material3:1.5.0-beta01`, which the project pins today. The task that adds CMP must confirm this with `:app:dependencies`.
- **iOS gets the alpha build.** The app uses no Expressive APIs, so the gap between alpha22 and beta01 should not matter, but the iOS walkthrough is where it would show.

**Alternatives considered**:

- *Stay on Kotlin 2.4.10*: rejected, because the CMP plugin matching it is not the documented pair.
- *CMP 1.13.0-alpha*: rejected. It is a pre-release, and the project should not stack two alphas.

## R2 — Module shape under AGP 9

**Decision**:

- **Library modules**: all 16 except `:core:ai` become KMP libraries using `org.jetbrains.kotlin.multiplatform` + **`com.android.kotlin.multiplatform.library`**. Android settings move into `kotlin { android { … } }`.
- **`:app` stays** a `com.android.application` module, but it shrinks to the Android entry point:
  - `QuestLogApp` (`Application`, Koin start, WorkManager factory);
  - `MainActivity` (splash, edge-to-edge, `setContent { QuestLogRoot(...) }`);
  - manifest, launcher icons and splash theme;
  - the release signing config.
- **A new KMP module, `:shared`**, becomes the cross-platform app root. It holds:
  - the root composable (`QuestLogRoot`) with the theme resolution, `Scaffold` and bottom bar;
  - `QuestLogNavDisplay` with the single `entryProvider`;
  - the Koin graph assembly;
  - the iOS entry point (`MainViewController()`), exported as the static framework `QuestLogShared`.
- **A new Xcode project, `iosApp/`**, sits at the repo root and links that framework.
- **`:core:ai` stays** a plain `com.android.library`. `:core:data` depends on it from `androidMain` only.

**Rationale**:

- **AGP 9 forbids it**: a module cannot apply `kotlin.multiplatform` together with `com.android.application`, and the opt-out flags are deprecated in 9.4.1 and removed in AGP 10. The navigation entry point and scaffold therefore cannot stay in `:app` if iOS is to reach them.
- **Navigation stays in one place.** Moving it into `:shared` keeps "one module owns navigation, features own none". It also matches the KMP template layout, which pairs an Android app module with a shared Compose module and `iosApp/`.

**Alternatives considered**:

- *Keep the scaffold in `:app` and write a second, iOS-only nav graph*: rejected. It duplicates the `entryProvider`, and SC-007 (one edit per rule) would fail.
- *Make `:app` itself KMP using the deprecated opt-outs*: rejected, because it stops building on AGP 10.
- *Name the new module `:composeApp` and rename `:app` to `:androidApp`* (the wizard layout): rejected. Renaming `:app` touches the `applicationId` history, run configurations and every doc reference for no behaviour gain.

**Constitution impact**: Principle I says "`:app` is the only module that knows about navigation". After the move, `:shared` owns navigation and `:app` only hosts it. This is a wording amendment (see Complexity Tracking in `plan.md`).

## R3 — Convention plugins (`build-logic`)

**Decision**: add an included build, `build-logic/`, with four convention plugins:

- **`questlog.kmp.library`**:
  - applies KMP and the Android KMP library plugin;
  - adds the `android`, `iosArm64` and `iosSimulatorArm64` targets;
  - sets `compileSdk = 37`, `minSdk = 29` and JVM target 11;
  - enables `withHostTest {}`.
- **`questlog.kmp.compose`**: adds the Compose compiler, the CMP plugin and Compose resources, with `androidResources { enable = true }`.
- **`questlog.kmp.feature`**: applies both of the above plus the six allowed `:core:*` dependencies and the Koin ViewModel artifacts.
- **`questlog.android.application`**: used by `:app` only.

**Rationale**:

- **Every module needs the same 30 lines.** Each of the 17 KMP modules needs identical target, source-set, host-test and iOS configuration. Copy-pasting it is the duplication `docs/tech-debt.md` already lists ("No convention plugins"), and that file defers fixing it *because* "a multiplatform move would rewrite the build logic anyway".
- **One edit instead of 17.** A target change (for example adding `iosX64`, or dropping a deprecated one) becomes a single edit.

**Alternatives considered**:

- *Keep repeating configuration by hand*: rejected. 17 copies of KMP configuration are hard to keep consistent and easy to get subtly wrong.
- *`buildSrc`*: rejected. Any change to it invalidates the whole build's configuration cache, while an included build does not.

**Constitution impact**: Additional Constraints / Platform says "Every `build.gradle.kts` repeats its configuration by hand", and the root `CLAUDE.md` says "There is no `build-logic`". The owner approved the change on 2026-10-08. Both texts are amended in the first Phase C commit, the one that adds `build-logic/`. See Complexity Tracking.

## R4 — Dependency injection: Hilt → Koin

**Decision**: **Koin 4.2.2** (current stable), using the classic DSL (`singleOf`, `factoryOf`, `viewModelOf`, `viewModel { (id: Long) -> … }`). Artifacts:

- `koin-core`;
- `koin-compose` and `koin-compose-viewmodel`;
- `koin-androidx-workmanager` (Android only).

Ownership of the modules:

- **Core modules**: each one that provides bindings exposes **one** Koin `Module` from its existing `di/` package. `:core:common`, `:core:domain`, `:core:network`, `:core:database` and `:core:data` get `commonMain` modules, plus small `androidMain`/`iosMain` modules where an implementation is per platform.
- **`:shared`** assembles them, and registers every feature ViewModel in a single `di/ViewModelModule.kt`. So the "feature modules own no per-feature DI module" rule survives.
- **Assisted injection**: the two `@AssistedInject` ViewModels (`GameDetailViewModel`, `WishlistViewModel`) become `viewModel { (id) -> … }` and are fetched with `koinViewModel { parametersOf(route.id) }`.
- **Graph check**: Koin's `verify()` runs in a `:shared` host test, so a missing binding fails `./gradlew` rather than the app at runtime.

**Rationale**:

- **Hilt is JVM/Android-only and cannot stay.** `javax.inject.Inject` is not available in `commonMain`.
- **Koin is the counterpart already named** in `CLAUDE.md`.
- **Its integrations cover the app**: first-party Compose, ViewModel, Navigation 3 and WorkManager support, on every target this project needs.
- **The classic DSL needs no KSP.** After the migration, KSP remains only for Room.

**Alternatives considered**:

- *Metro* (compile-time, multiplatform, Dagger-like): it keeps Hilt's compile-time safety. Rejected because the documented lean is Koin, and Metro's WorkManager and Navigation 3 integrations are not first-party. This was a hard-to-reverse choice, so it was put to the owner, who **chose Koin on 2026-10-08**.
- *Koin Annotations (KSP)*: rejected. KSP would have to run per target in every module, for little gain over the DSL plus `verify()`.
- *kotlin-inject*: rejected, because it has a smaller ecosystem and no first-party ViewModel or Navigation 3 support.

**Sequencing**: the swap happens **while every module is still Android-only** (Phase B in `plan.md`), in two commits:

1. Koin modules are added next to Hilt and covered by `verify()`, with Hilt still in charge.
2. ViewModel retrieval, the workers and the `Application` switch to Koin, and Hilt is removed.

Both commits leave Android green.

## R5 — Networking: Retrofit + Moshi + OkHttp → Ktor + kotlinx.serialization

**Decision**:

- **Client**: Ktor client 3.x, with the `OkHttp` engine in `androidMain` and the `Darwin` engine in `iosMain`. The version is pinned at task time to the latest 3.x stable (3.6.0 at the time of writing). It needs at least 3.4.1, which fixed the Flow-invariant regression in 3.4.0.
- **Plugins**: `ContentNegotiation` with `kotlinx.serialization` JSON (`ignoreUnknownKeys = true`), and `Logging` (debug builds).
- **DTOs**: the 12 Moshi DTOs swap `@JsonClass`/`@Json(name=)` for `@Serializable`/`@SerialName`.
- **Service**: `IgdbApiService` keeps its contract (methods return bare `List<T>`, throw on failure), implemented over `HttpClient`.
- **Errors**: `IgdbHttpErrorInterceptor` becomes a Ktor `HttpResponseValidator` that throws the same `IgdbHttpException` for every non-2xx response. Ktor's own `ResponseException` never leaves `:core:network` (Principle II).
- **Clock**: `ElapsedRealtimeSource` (`SystemClock`) becomes `TimeSource.Monotonic`, behind the same small seam `IgdbAuthManagerTest` already fakes.

**Error mapping (`RepositoryErrorMapper`)**: the `java.net` checks become a per-platform `Throwable.isConnectivityFailure()`:

- **Android**: the existing `UnknownHostException`/`ConnectException`/`SocketException`/`SocketTimeoutException` checks, since the OkHttp engine still throws them.
- **iOS**: `DarwinHttpRequestException` whose `NSError` is in `NSURLErrorDomain` with a connectivity code.
- **Common**: `HttpRequestTimeoutException` and `kotlinx.io.IOException` are handled in common code. `CancellationException` is still rethrown first.

**Credentials**: the gmazzo `com.github.gmazzo.buildconfig` Gradle plugin generates an `internal object IgdbCredentials` into `:core:network`'s `commonMain`, under `build/generated/`, reading `local.properties` exactly as today. The KMP library plugin has no `BuildConfig`. Nothing is committed (FR-013).

**Alternatives considered**:

- *Keep Retrofit on Android and write Ktor only for iOS*: rejected. That means two clients and two DTO sets, and SC-007 fails.
- *BuildKonfig plugin*: equivalent, but less maintained.
- *A hand-written Gradle task*: works, but is more code to own than a one-line plugin.

## R6 — Persistence: Room on the driver API, DataStore with explicit paths

**Room decision**: stay on **Room 2.8.5**, which already supports KMP.

- **Database class**:
  - the database moves to `commonMain` with `@ConstructedBy(QuestLogDatabaseConstructor::class)` and an `expect object … : RoomDatabaseConstructor`;
  - the builder uses `.setDriver(BundledSQLiteDriver())` and `.setQueryCoroutineContext(Dispatchers.IO)`.
- **KSP and schemas**:
  - KSP runs per target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`);
  - the `androidx.room` Gradle plugin replaces the `ksp { arg("room.schemaLocation") }` line, keeping `core/database/schemas/`.
- **Database file path** comes from a per-platform builder factory:
  - **Android**: `context.getDatabasePath("<current name>")`. The file name is unchanged, so existing installs keep their data.
  - **iOS**: the app's Application Support directory.
- **Seed callback**: `onCreate(SupportSQLiteDatabase)` becomes `onCreate(SQLiteConnection)` with `connection.execSQL(...)`.
- **Seed text**: the default list's name and description come from a `DefaultWishlistSeed` value injected by Koin (data model). They are resolved from the shared string resources at the app root, instead of from `Context.getString` inside `:core:database`.
- **Schema rules**:
  - `version = 1` and `fallbackToDestructiveMigration(true)` stay (constitution, Persistence);
  - no entity changes, so the exported schema should not change; if Room's output does change, it is committed in the same commit.

**Rationale**: `.setDriver(...)` is exactly what `core/database/CLAUDE.md` reserved for this migration. The bundled SQLite reads the existing database file format, so Android data survives the switch. If it did not, the destructive fallback is the documented, accepted outcome while the app is unpublished.

**DataStore decision**:

- **Library**: `datastore-preferences-core` 1.2.1, which is already multiplatform, created with `PreferenceDataStoreFactory.createWithPath(...)`.
- **Path**:
  - **Android**: computed so it resolves to the **same file** `preferencesDataStore("…")` uses today (`files/datastore/<name>.preferences_pb`). Otherwise the onboarding-seen flag and the appearance choice would reset.
  - **iOS**: Application Support.
- **Instances**: one `DataStore` instance per file, held as a Koin `single`.

**Verification items for the tasks**:

- `GameDao`'s `@Transaction` functions on `abstract class` DAOs compile for iOS. Room 2.8.4 fixed codegen for such functions, and the KMP docs show the `open suspend` form.
- The KMP library plugin's `withHostTest` can run the 4 `:core:data` tests that touch Android types. If not, they move to the `:app` host tests unchanged.

## R7 — Dates and number formatting

**Decision**:

- **Dates**: `java.time` disappears from shared code. `DateUtils`, `GetDiscoverFeedUseCase` and `GameUiMapper` use `kotlinx-datetime` (0.8.0, already in the catalog and already the roadmap's rule).
- **`DateUtils` keeps its public API** (`formatUnixTimestamp(seconds, pattern)`, `formatIsoDate(...)`, `parseIsoDate(...)`, …). Its 11 call sites in the mappers stay unchanged.
- **Localized rendering** needs the device locale's month and weekday names, which have no common API. It goes through one `internal expect fun` in `:core:common`:
  - the inputs are either a Unicode date pattern or a `DateStyle` enum that replaces `java.time`'s `FormatStyle`;
  - **Android**: `DateTimeFormatter.ofPattern(pattern, Locale.getDefault())` / `ofLocalizedDate(style)`. This is exactly today's output, so the existing mapper tests pin it.
  - **iOS**: `NSDateFormatter`, with `dateFormat = pattern` (or `dateStyle`) and `NSLocale.currentLocale`.
- **Fixed English month abbreviations** (`"MMM"`, `Locale.ENGLISH` in `GameUiMapper`) and the data-side ISO `"yyyy-MM-dd"` in `GameMapper` are not locale-dependent. They use the `kotlinx-datetime` format DSL (`MonthNames.ENGLISH_ABBREVIATED`, `LocalDate.Formats.ISO`) in common code with no `expect`.

**Why `expect`/`actual` here and an interface elsewhere**: this is a stateless leaf function called from pure mapper functions. Injecting a formatter would thread a new parameter through every mapper and its tests for no benefit. The capabilities in R9 have lifecycles, state or test doubles, and those get interfaces.
- **Compact numbers**: `String.format(Locale.US, "%.1fM")` in `GameDetailUiMapper` becomes a small common one-decimal formatter, with a host test pinning the existing outputs.
- **Capitalizing**: `titlecase(Locale.getDefault())` in `RadarUiMapper` becomes the common `titlecase()`.

**Rationale**: FR-009 requires the same game to show on the same day on both platforms. A single shared date pipeline does that by construction:

- `Instant` → `LocalDate` in the device time zone, via `TimeZone.currentSystemDefault()`;
- the UTC-based year-only placeholder check keeps using `TimeZone.UTC`, as `ZoneOffset.UTC` does today.

Only the final localized rendering is per platform.

## R8 — Shared UI: Compose Multiplatform and resources

**Decision**:

- **UI modules**: `:core:designsystem`, `:core:ui` and all seven feature modules move to `commonMain` with Compose Multiplatform.
- **Resources**: Android `res/values/strings.xml`, `res/drawable` and plurals move to **Compose Multiplatform resources** (`src/commonMain/composeResources/`). Vector XML drawables and `placeholder.png` are supported as-is.
- **`UiText`** keeps its shape and role. It wraps `StringResource`/`PluralStringResource` instead of `@StringRes Int`, and gains:
  - a `@Composable asString()`;
  - a `suspend asString()` built on `getString`. This replaces `asString(context)` in the five screens that use `LocalContext` to resolve snackbar text inside a `LaunchedEffect`.
- **`:core:ui` resources** are public: `compose.resources { publicResClass = true; packageOfResClass = "com.nikolasguillen.questlog.core.ui.resources" }`. The cross-module alias rule carries over unchanged in spirit: `import …core.ui.resources.Res as CoreUiRes`. A module's own `Res` is imported bare.
- **Android-only strings stay Android resources**: the release-notification channel and notification texts in `:core:data` live in `:core:data`'s `androidMain/res`, because reminders are Android-only (FR-008).

Library replacements:

| Today | After | Notes |
|---|---|---|
| Coil 2 (`io.coil-kt:coil-compose`) | Coil 3 (`io.coil-kt.coil3:coil-compose` + `coil-network-ktor3`) | Pin the latest 3.x compatible with CMP 1.12. `File` models become path strings |
| `HtmlCompat` + spans | `AnnotatedString.fromHtml` (Compose UI text, common) | Covers the bold/underline spans `HtmlUtils` builds today |
| `androidx.activity.compose.BackHandler` | CMP's common back handler (`BackHandler` from `ui-backhandler` or `NavigationBackHandler`, whichever CMP 1.12 marks stable) | 2 call sites |
| `me.trishiraj:shadowglow` | Compose `Modifier.dropShadow(...)` | 1 call site (`GameDetailActionPill`); compare visually against the baseline |
| `material-icons-core` / `-extended` (BOM) | `org.jetbrains.compose.material:material-icons-*:1.7.3` | Frozen upstream but published for iOS; replacing icons is out of scope |
| Haze 2.0.0-rc02 | unchanged (already multiplatform) | |
| `isSystemInDarkTheme()` in `MainActivity` | moves with the theme resolution into `:shared`'s root composable | Still resolved exactly once |

**Rationale**: FR-004 and FR-005 require one design system and one set of strings on both platforms. Compose Multiplatform resources are the only mechanism CMP renders on iOS. `UiText` already isolates every resource read, which is what makes this a mechanical swap.

**Alternatives considered**:

- *moko-resources*: rejected. It is a third-party plugin, the CMP resources are first-party now, and the Android build gains nothing.
- *Keep `strings.xml` for Android and generate iOS strings from it*: rejected, because that means two pipelines.

## R9 — Platform capabilities and the iOS exceptions (FR-007, FR-008)

**Decision**: every capability that differs by platform is reached through a contract owned by shared code (see `contracts/platform-contracts.md`):

| Capability | Contract (module) | Android | iOS (first release) |
|---|---|---|---|
| Release-date refresh | `ReleaseRefreshScheduler` (`:core:domain`, exists) | WorkManager (unchanged) | In-process: runs `RefreshReleaseDatesUseCase` in an app-scoped coroutine. `schedulePeriodicRefresh()`, called at launch, runs it only when the last successful run is older than the Android interval (24h, timestamp kept in the settings DataStore). `scheduleImmediateRefresh()` runs it right away |
| Release reminders | `ReleaseNotificationScheduler`, `ReleaseNotifier` (exist) + **new** `ReleaseRemindersAvailability` | WorkManager + notifications (unchanged); availability `true` | No-op implementations; availability `false` |
| Notification permission UI | `NotificationPermission` (`:core:ui`, moves to `androidMain` behind an `expect` composable) | unchanged | Never shown (availability `false`) |
| On-device translation | `GameDescriptionTranslator` (`:core:domain`, exists) | ML Kit via `:core:ai` (unchanged) | Implementation that reports `TranslationModelStatus.UNSUPPORTED` |
| Localized date rendering | `DateUtils` (`:core:common`, exists) over an `internal expect fun` (R7) | `DateTimeFormatter` | `NSDateFormatter` |
| App version | `AppVersionProvider` (`:core:common`, exists) | `PackageManager` | `NSBundle` `CFBundleShortVersionString` |
| Network status | `NetworkStatusProvider` (`:core:common`, exists) | `ConnectivityManager` | `NWPathMonitor` |
| Share text | **new** `TextSharer` (`:core:ui`, composable-scoped) | `ACTION_SEND` chooser | `UIActivityViewController` |
| Pick a cover image | **new** `rememberCoverImagePicker` (`:core:ui`) | Photo picker (unchanged behaviour) | `PHPickerViewController` |
| Store a cover image | `WishlistCoverImageStorage` (`:core:data`, exists) | `ImageDecoder` downscale, app files dir | `UIImage` downscale, Application Support |
| Status-bar icon colour | **new** `SystemBarsAppearance` (`:core:designsystem`, `expect` composable) | `WindowCompat` (unchanged) | No-op in the first slice; verified in the iOS walkthrough |
| Splash screen | none (platform shell) | `core-splashscreen` in `:app` | Launch screen in `iosApp/` |
| Display corner radius for the bottom bar | parameter of `QuestLogRoot` | `RoundedCorner` read in `MainActivity` | Fixed default from `iosApp` |

**Hiding entry points (FR-008)**:

- **Translation needs no new code**: `UNSUPPORTED` already hides the Settings row (`SettingsViewModel` maps it to `TranslationModelRowState.Hidden`), and Game detail only offers translation when the status is `READY`.
- **Reminders need one new contract**, `ReleaseRemindersAvailability`. ViewModels read it, not composables (Principle III), to drop:
  - the reminders rows in Settings;
  - the per-game reminder control and banner in Game detail;
  - the reminders page in Onboarding;
  - the permission prompt.

  Each affected ViewModel gets a host test for the "unavailable" branch.

**Release-date refresh is not part of the reminders deferral.** Radar has no foreground refresh today: on
Android, saved games' release dates are kept current only by the WorkManager job. A no-op on iOS would
leave Radar's dates stale, and FR-009 would fail in practice. The in-process iOS implementation above
keeps Radar correct while the app is in use, without background execution (`BGTaskScheduler`). The only
part of the refresh deferred with reminders is the run that happens while the app is closed.

**Out of scope here**: implementing reminders and translation for iOS. Both are recorded in `docs/roadmap.md` (FR-016).

## R10 — Navigation 3 across platforms

**Decision**:

- **Routes**: they stay in `:core:navigation`, now in `commonMain`, on JetBrains' Navigation 3 artifacts.
- **Serialization config**: non-Android back stacks need an explicit polymorphic serializer configuration for `NavKey`. A `SavedStateConfiguration` registering the `GameNavKey` sealed subclasses lives **next to `Routes.kt`**, since it is the same closed set. `:shared` passes it to `rememberNavBackStack`.
- **ViewModel scoping**: `rememberViewModelStoreNavEntryDecorator` keeps ViewModels scoped per entry, so Koin's `koinViewModel()` inside an entry gets the entry's store, not the root's.

**Rationale**: Android serializes `NavKey`s reflectively, but iOS cannot, and omitting the configuration crashes on the first back-stack save on iOS.

## R11 — Tests

**Decision**:

- **Existing tests move unchanged** to each KMP module's `androidHostTest` source set: JUnit4, MockK and `kotlinx-coroutines-test`. Their content is not rewritten (FR-011, FR-012).
- **Added tests** cover:
  - the new common code paths: the date pipeline, the number formatter, the reminders-unavailable branches and `RepositoryErrorMapper`'s connectivity mapping;
  - the Koin graph via `verify()`.

  They are written in `androidHostTest` in the same style, unless they need no mocks. In that case they go in `commonTest` with `kotlin.test`, so they also run on iOS.
- **The test command changes.** Host tests run as `testAndroidHostTest`, not `testDebugUnitTest`. The first KMP-conversion task must confirm whether the root `./gradlew test` aggregate still reaches them (otherwise the command is `./gradlew allTests` or an aggregate task), and update the commands in `CLAUDE.md` and the constitution in the same commit.

**Rationale**: MockK has no Kotlin/Native support. Rewriting 31 suites onto hand-written fakes would be a coverage campaign, which the constitution explicitly excludes ("do not plan a coverage campaign unless coverage is the feature"). It would also put the "no test weakened" guarantee at risk. The JVM run on the Android target exercises the same `commonMain` code.

## R12 — iOS app shell

**Decision**:

- **Project**: `iosApp/` is a SwiftUI Xcode project with a single `ContentView` that hosts `MainViewControllerKt.MainViewController()` through `UIViewControllerRepresentable`, ignoring safe areas so the shared scaffold handles insets.
- **Framework**: `:shared` builds a **static** framework named `QuestLogShared` for `iosArm64` and `iosSimulatorArm64`. It is integrated through Kotlin's direct Xcode integration (the `embedAndSignAppleFrameworkForXcode` build phase). No CocoaPods and no SwiftPM export.
- **Deployment target**: iOS **16.0**. `PHPickerViewController` needs 14+, and 16 drops old devices without affecting any current iPhone.
- **Koin**: started from `MainViewController()` (idempotent), with the iOS platform modules.
- **iOS targets**: no `iosX64`. Intel simulators are not used, and Kotlin has demoted the x86_64 Apple targets.

**Platform note**: iOS builds require macOS with Xcode. On Windows, `kotlin.native.ignoreDisabledTargets=true` lets the Android build and host tests run as today.

## R13 — What stays in `:app` and is not ported

**Stays in `:app`** as Android-only shell code:

- splash screen;
- the `RoundedCorner` probe;
- edge-to-edge setup;
- `Application` and the WorkManager configuration;
- launcher icons and the release signing config. The debug-key signing is a tech-debt item and is not touched.

**Not touched**: the `adaptive`, `adaptive-layout` and `adaptive-navigation3` dependencies in `:app` are unused by current sources. This feature neither ports them to `:shared` nor deletes them; mention, don't fix.

## R14 — Order of work (each phase leaves Android releasable)

The phases in `plan.md` follow from three constraints:

1. **Every commit keeps Android green** (FR-011, constitution).
2. **Library swaps happen while the code is still Android-only**, so each swap is verified on the platform that already works before iOS adds a second unknown.
3. **Leaf modules convert before the modules that depend on them**:
   - non-UI: model → common → domain → network → database → data;
   - UI: designsystem → ui → features → `:shared`.

**Alternatives considered**:

- *Convert all modules to KMP first and swap libraries inside `commonMain` afterwards*: rejected. It would produce long stretches where `commonMain` can't compile for iOS, and the build could not be paused cleanly.
- *Big-bang branch*: rejected by User Story 3.
