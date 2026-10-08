---

description: "Task list for the Kotlin Multiplatform migration"
---

# Tasks: Multiplatform Migration

**Input**: Design documents from `/specs/010-kmp-migration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Requested by the constitution (Principle V) and by the plan: existing suites move unchanged, and every
new logic path gets a JUnit4 + MockK + `kotlinx-coroutines-test` test in its own module. Characterization tests
are written and passing **before** the code they pin is rewritten. New ViewModel and use-case tests follow
Principle V: a `StandardTestDispatcher` with `Dispatchers.setMain`/`resetMain`, and mocks of the use cases
rather than of the repository.

**Organization**: This is a migration, so the stories are not independent slices: US1 (Android on shared code)
must finish before US4 (iOS implementations of the contracts), which must finish before US2 (the iOS app).
Phases therefore run in dependency order, and `[USn]` marks the story each task serves. US3 ("migrate in
verifiable steps") is an invariant on every commit, plus the governance work that keeps the rules truthful.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1–US4, from spec.md. Setup, Foundational and Polish tasks carry no story label
- Every task names exact paths. `QL` stands for `com/nikolasguillen/questlog`

## Path Conventions

- Before a module is converted: `<module>/src/main/java/QL/...`, tests in `<module>/src/test/java/QL/...`
- After conversion: `<module>/src/commonMain/kotlin/QL/...`, `src/androidMain/kotlin/QL/...`,
  `src/iosMain/kotlin/QL/...`, tests in `src/androidHostTest/kotlin/QL/...`
- Modules keep their Kotlin package names; only the source-set directory changes
- Moves use `git mv` and are separate commits from content changes whenever the change is more than an import

## Mapping to the plan's phase letters

`plan.md`, `research.md` and `quickstart.md` use the letters A–G; this file uses Phases 1–7.

| Plan phase | Tasks phase |
|---|---|
| A — Toolchain | Phase 1 |
| B — Platform-neutral code and library swaps | Phase 2 (sections B1–B7) |
| C — Non-UI modules to KMP | Phase 3 (sections C0, C1) |
| D — UI modules to Compose Multiplatform | Phase 3 (section D) |
| E — `:shared` app root | Phase 3 (section E) |
| F — iOS | Phase 4 (iOS implementations) and Phase 5 (iOS app) |
| G — Documentation sweep | Phase 6, after the same-commit amendments in Phases 2–3 |
| Final verification | Phase 7 |

## Checks that gate every commit (FR-011, SC-005)

```bash
./gradlew :app:assembleDebug
./gradlew test                      # replaced by the settled aggregate once T060 is done
```

The test count must stay at or above the baseline recorded in T001. A commit that breaks either
check is not a commit; fix it before moving on. Commit subjects follow `type(scope): subject`; the scope list
is `SCOPES` in `.githooks/commit-msg`, and T053 extends it.

---

## Phase 1: Setup (Toolchain and baseline)

**Purpose**: Record what "unchanged" means, then move the toolchain without touching modules

- [X] T001 Create `specs/010-kmp-migration/baseline.md`. On the branch as it stands, run `./gradlew :app:assembleDebug` and `./gradlew test`, and record the date, the commit hash, the number of test classes and the total number of tests that ran (sum the per-module reports under `*/build/reports/tests/testDebugUnitTest/index.html`). This number is SC-001's floor
- [ ] T002 Manual: install the debug build on a device or emulator and capture a light and a dark screenshot of every screen in `quickstart.md` § Parity checklist. Store them outside the repo and list their location in `specs/010-kmp-migration/baseline.md`
- [ ] T003 Manual: on a device, still on the pre-migration build, create two lists with games and statuses, a custom list cover from the photo picker, set the appearance to dark, finish onboarding, and enable one release reminder. Do not uninstall: this is the device for the Android update-path check (`quickstart.md`)
- [X] T004 Bump Kotlin to `2.4.20` in `gradle/libs.versions.toml` (`kotlin` and `jetbrainsKotlinPluginSerialization`). If the build rejects KSP `2.3.12`, bump `googleDevtoolsKsp` to the version listed for Kotlin 2.4.20 in the KSP releases. Run the gate checks. Commit: `build(deps): bump kotlin to 2.4.20`
- [X] T005 Add the catalog entries for everything later phases introduce, in `gradle/libs.versions.toml`, **without using them yet** (existing aliases stay as they are). Pin each version from the source named here; do not guess: Compose Multiplatform plugin `1.12.1` (`org.jetbrains.compose`); `org.jetbrains.kotlin.multiplatform`; `com.android.kotlin.multiplatform.library` (version `agp`); Koin `4.2.2` (`koin-core`, `koin-android`, `koin-androidx-workmanager`, `koin-compose`, `koin-compose-viewmodel`, `koin-test`, `koin-test-junit4`); Ktor client latest 3.x stable from https://github.com/ktorio/ktor/releases (`ktor-client-core`, `-okhttp`, `-darwin`, `-content-negotiation`, `-logging`, `ktor-serialization-kotlinx-json`); `kotlinx-serialization-json`; `kotlinx-io-core`; Coil 3 latest stable that supports CMP 1.12 (`coil-compose`, `coil-network-ktor3`); `androidx.room` Gradle plugin `2.8.5` and `androidx.sqlite:sqlite-bundled`; `androidx.datastore:datastore-preferences-core` `1.2.1`; JetBrains Navigation 3 and lifecycle artifacts at the versions in the "Based on Jetpack" table of the CMP 1.12.1 release notes (https://github.com/JetBrains/compose-multiplatform/releases); `org.jetbrains.compose.material:material-icons-core`/`-extended` `1.7.3`; `com.github.gmazzo.buildconfig`. Run the gate checks (unused entries are harmless). Commit: `build(deps): add multiplatform entries to the version catalog`

**Checkpoint**: Android builds on Kotlin 2.4.20 and the baseline is on record

---

## Phase 2: Foundational (Make Android code platform-neutral and swap libraries, still Android-only)

**Purpose**: Every library swap happens here, while the code is still Android-only, so each swap is verified on
the one platform that already works (research R14). Nothing is converted to KMP yet.

**⚠️ CRITICAL**: No module may be converted to KMP (Phase 3) until this phase's checkpoint passes

**Documentation rule**: each library swap amends, in the same commit, every instruction file and constitution rule it makes false (the root and directory `CLAUDE.md` files and `.specify/memory/constitution.md`), as the constitution's Governance and the root `CLAUDE.md` require. Phase 6 only sweeps what is left.

### B1 — Platform-neutral dates, numbers and text (research R7, R8)

- [X] T006 [P] Write `core/common/src/test/java/QL/core/common/DateUtilsTest.kt` pinning today's `DateUtils` behaviour: `formatUnixTimestamp` with the patterns `"yyyy"`, `"yyyy-MM-dd"`, `"MMM yyyy"`, `"EEE"`, `"MMM d"` and with the default `MEDIUM` style; `formatIsoDate` (valid, empty, null, malformed input returns the input); `parseIsoDate`; `getYearFromIsoDate`; `timestampToLocalDate`; `isoDateToEpochSeconds`; `isYearOnlyPlaceholder` for both overloads (a 31 December date is a placeholder, any other date is not). Use `Locale.setDefault(Locale.ENGLISH)` and `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` in `@Before`, and add one case at a UTC−8 and one at a UTC+9 default zone around midnight to pin the day boundary (FR-009). Add `testImplementation(libs.junit)` to `core/common/build.gradle.kts`. The test must pass against the current code first
- [X] T007 [P] Add characterization cases to `feature/game-detail/src/test/java/QL/feature/gamedetail/mapper/GameDetailUiMapperTest.kt` for `formatLargeNumber` (`GameDetailUiMapper.kt`, reached through the public mapper path that prints counts; if no public path reaches it, make the function `internal`): `0`, `999`, `1_000`, `1_049`, `1_050`, `999_949`, `999_950`, `999_999`, `1_000_000`, `2_340_000`. Expected values are whatever the current `String.format(Locale.US, "%.1fK"/"%.1fM", …)` prints, including `999_999` → `1000.0K`. They must pass against the current code first
- [X] T008 Add `kotlinx-datetime` to `core/common/build.gradle.kts`. Create `core/common/src/main/java/QL/core/common/DateStyle.kt` (`enum class DateStyle { SHORT, MEDIUM, LONG, FULL }`). Rewrite `core/common/src/main/java/QL/core/common/DateUtils.kt` so that parsing, `Instant` → `LocalDate`, `timestampToLocalDate`, `isoDateToEpochSeconds` and `isYearOnlyPlaceholder` use `kotlinx.datetime` (`TimeZone.currentSystemDefault()` for local dates, `TimeZone.UTC` for the placeholder check, as `ZoneOffset.UTC` is used today). Remove the `Locale` parameters (no caller passes one) and replace `FormatStyle` with `DateStyle`. Isolate the only locale-dependent step in a new `core/common/src/main/java/QL/core/common/DateRendering.kt` with `internal fun renderLocalDate(date: LocalDate, pattern: String): String` and `internal fun renderLocalDate(date: LocalDate, style: DateStyle): String`, implemented with `java.time` (this file becomes an `expect`/`actual` pair in T058). Function names and the 11 call sites in `core/data/.../mapper/GameMapper.kt`, `feature/settings/.../ReleaseNotificationsUiMapper.kt`, `feature/game-detail/.../GameDetailUiMapper.kt` and `feature/radar/.../RadarUiMapper.kt` stay unchanged. T006 passes unmodified (a failing case means a behaviour change: fix the code, not the test)
- [X] T009 In `core/domain/src/main/java/QL/core/domain/usecase/discover/GetDiscoverFeedUseCase.kt` replace `java.time.LocalDate` and `LocalDate.now()` (line ~265, `date.isAfter(LocalDate.now())`) with `kotlinx.datetime.LocalDate` and today's date from `kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())` (confirm the `Clock` package for kotlinx-datetime 0.8.0 in its changelog). `GetDiscoverFeedUseCaseTest` must pass without changes
- [X] T010 In `core/ui/src/main/java/QL/core/ui/mapper/GameUiMapper.kt` (line ~95) replace `DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)` with the `kotlinx-datetime` `MonthNames.ENGLISH_ABBREVIATED` format (add `kotlinx-datetime` to `core/ui/build.gradle.kts`). First add `core/ui/src/test/java/QL/core/ui/mapper/GameUiMapperTest.kt` pinning `Game.toGameItem()` for `releaseDate = "2024-03-01"`, `"2024-03-02"`, `"2024-03-03"`, `"2024-03-04"`, `"2024-03-11"`, `"2024-03-22"` (ordinal suffixes st/nd/rd/th and the teens) and `null`/`"2024"`/malformed input, and run it against the current code, then make the change
- [X] T011 [P] In `feature/game-detail/src/main/java/QL/feature/gamedetail/mapper/GameDetailUiMapper.kt` replace `String.format(Locale.US, …)` in `formatLargeNumber` with integer arithmetic that rounds half-up to one decimal, and delete the `java.util.Locale` import. T007 must pass without changes
- [X] T012 [P] In `feature/radar/src/main/java/QL/feature/radar/mapper/RadarUiMapper.kt` replace `replaceFirstChar { it.titlecase(Locale.getDefault()) }` (line ~101) with `replaceFirstChar { it.titlecase() }` and delete the `Locale` import
- [X] T013 Replace `HtmlCompat` in `core/ui/src/main/java/QL/core/ui/util/HtmlUtils.kt` with a small parser for `<b>`, `<i>` and `<u>` (nesting allowed, unknown tags and unmatched tags kept as literal text). Remove the `android.graphics.Typeface`, `android.text.*` and `androidx.core.text.HtmlCompat` imports and `Spanned.toAnnotatedString()`. Test first, in `core/ui/src/test/java/QL/core/ui/util/HtmlUtilsTest.kt`: `"Remove <b>Zelda</b> now"` yields the text `Remove Zelda now` with one bold span over `Zelda`; nested bold+italic; underline; a stray `<` stays literal; plain text yields no spans. `annotatedStringResource(id: Int, …)` keeps its signature for now (its only caller is `feature/search/.../components/SearchBars.kt`)

**Commit boundary**: `refactor(common): make date, number and html handling platform-neutral` (split into one commit per module if it reads better; each must pass the gate checks)

### B2 — Capability seams (contracts/platform-contracts.md), still bound by Hilt

*These serve User Story 4 but are blocking prerequisites, so they carry no story label.*

- [X] T014 [P] In `core/common/src/main/java/QL/core/common/` turn `AppVersionProvider.kt` and `NetworkStatusProvider.kt` into interfaces with the same members (`val versionName: String`, `val isUnmeteredNetworkAvailable: Boolean`), and move today's bodies unchanged into `AppVersionProviderImpl.kt` and `NetworkStatusProviderImpl.kt` (one class per file). Bind them with `@Binds` in a new Hilt `core/common/src/main/java/QL/core/common/di/CommonModule.kt` (replaced in T029). `SettingsViewModelTest` keeps compiling, since MockK mocks interfaces
- [X] T015 [P] In `core/data/src/main/java/QL/core/data/local/` turn `WishlistCoverImageStorage.kt` into an interface (`suspend fun persist(source: String): String?`, `suspend fun delete(path: String)`) and move the current class body unchanged into `WishlistCoverImageStorageImpl.kt`. Bind it in `core/data/src/main/java/QL/core/data/di/DataModule.kt`. Update `GameRepositoryImpl` and any test that constructs it to use the interface
- [X] T016 Create `core/domain/src/main/java/QL/core/domain/notification/ReleaseRemindersAvailability.kt` (`interface ReleaseRemindersAvailability { val isAvailable: Boolean }`) and `core/data/src/main/java/QL/core/data/notification/StaticReleaseRemindersAvailability.kt` (`class StaticReleaseRemindersAvailability(override val isAvailable: Boolean)`). Bind it to `true` in `DataModule.kt`
- [X] T017 [P] Gate `SettingsViewModel` (`feature/settings/src/main/java/QL/feature/settings/SettingsViewModel.kt`): inject `ReleaseRemindersAvailability`; when `isAvailable` is `false` the UiState hides the release-reminders row and the notification-permission row (`model/SettingsUiState.kt`, `model/NotificationPermissionRowState.kt`). Add the constructor argument (a mock returning `true`) to the existing `SettingsViewModelTest.kt` set-up without touching any assertion, and add a new test for the `false` branch
- [X] T018 [P] Gate `GameDetailViewModel` (`feature/game-detail/src/main/java/QL/feature/gamedetail/GameDetailViewModel.kt`): when `isAvailable` is `false`, the per-game reminder control and the notifications banner are absent from the UiState and `GameDetailUiEffect` never asks for the permission. Same test rules as above, in `GameDetailViewModelTest.kt`
- [X] T019 [P] Gate `OnboardingViewModel` (`feature/onboarding/src/main/java/QL/feature/onboarding/OnboardingViewModel.kt`): when `isAvailable` is `false` the reminders page is not in the page list and the flow ends after the platforms step. Same test rules, in `OnboardingViewModelTest.kt`
- [X] T020 [P] Gate `RadarViewModel` (`feature/radar/src/main/java/QL/feature/radar/RadarViewModel.kt`): when `isAvailable` is `false` it never emits the permission-request effect (`model/RadarUiEffect.kt`). Same test rules, in `RadarViewModelTest.kt`

**Commit boundary**: `refactor(data): extract platform seams behind interfaces` then `feat(settings): hide reminder controls when reminders are unavailable` (and likewise per feature)

### B3 — Hilt → Koin (research R4). Two steps: Koin beside Hilt, then one atomic cutover

- [X] T021 Add `koin-core` to the `build.gradle.kts` of `core/common`, `core/domain`, `core/network`, `core/database` and `core/data`; add `koin-android`, `koin-androidx-workmanager`, `koin-compose`, `koin-compose-viewmodel`, `koin-test` and `koin-test-junit4` to `app/build.gradle.kts` (the last two as `testImplementation`) and `koin-androidx-workmanager` to `core/data/build.gradle.kts`
- [X] T022 [P] Create `core/common/src/main/java/QL/core/common/di/CommonKoin.kt` (`val commonModule = module { … }`) binding `AppVersionProviderImpl` and `NetworkStatusProviderImpl` to their interfaces as singletons (`androidContext()` supplies the `Context`)
- [X] T023 [P] Create `core/domain/src/main/java/QL/core/domain/di/DomainKoin.kt` (`val domainModule`) registering as `factoryOf(::X)` every class under `core/domain/src/main` that today has an `@Inject constructor` (enumerate with `grep -rl "@Inject constructor" core/domain/src/main`: the use cases under `usecase/**` and `radar/`, plus `GameReleaseResolver`, `ReleaseBucketResolver`, `ReleaseDateResolver` and any other injected helper)
- [X] T024 [P] Create `core/network/src/main/java/QL/core/network/di/NetworkKoin.kt` (`val networkModule`) reproducing `di/NetworkModule.kt` exactly (Moshi, `ElapsedRealtimeSource`, logging and auth interceptors, `OkHttpClient`, both Retrofit instances, `IgdbAuthManager`, `IgdbApiService`, `IgdbAuthService`). The Hilt `Lazy<IgdbAuthManager>` becomes `inject<IgdbAuthManager>()` / `get()` at call time. This module is replaced by the Ktor client in T040
- [X] T025 [P] Create `core/database/src/main/java/QL/core/database/di/DatabaseKoin.kt` (`val databaseModule`) building `QuestLogDatabase` exactly like `DatabaseModule.provideDatabase` (`androidContext()`, same name, same callback, same destructive fallback), plus the seven DAOs as `single { get<QuestLogDatabase>().xDao() }`
- [X] T026 [P] Create `core/data/src/main/java/QL/core/data/di/DataKoin.kt` (`val dataModule`): `GameRepository` → `GameRepositoryImpl`, the three preference stores, `GameDescriptionTranslator`, `ReleaseRefreshScheduler`, `ReleaseNotificationScheduler`, `ReleaseNotifier`, `WishlistCoverImageStorage`, `ReleaseRemindersAvailability` (`StaticReleaseRemindersAvailability(true)`), the app-scoped `CoroutineScope(SupervisorJob() + Dispatchers.Default)` and the settings `DataStore<Preferences>`. Register the two workers with `workerOf(::ReleaseDatesRefreshWorker)` / `workerOf(::ReleaseNotificationWorker)` following https://insert-koin.io/docs/reference/koin-android/workmanager. Also register `GeminiNanoClient` from `:core:ai`
- [X] T027 Create `app/src/main/java/QL/di/ViewModelKoin.kt` (`val viewModelModule`): `viewModelOf(::X)` for `SearchViewModel`, `RadarViewModel`, `ListsViewModel`, `SettingsViewModel`, `OwnedPlatformsViewModel`, `ReleaseNotificationsViewModel`, `OnboardingViewModel`; and `viewModel { (gameId: Int) -> GameDetailViewModel(gameId, …get()) }`, `viewModel { (listId: Long) -> WishlistViewModel(listId, …get()) }` for the two assisted ones (read both constructors for the parameter order). It moves to `:shared` in T085
- [X] T028 Write `app/src/test/java/QL/di/KoinGraphTest.kt`: `listOf(commonModule, domainModule, networkModule, databaseModule, dataModule, viewModelModule).verify(extraTypes = …)` with `extraTypes` for `Context`, `WorkerParameters`, `Int` and `Long` as the verifier reports. It must pass while Hilt is still in charge, proving every dependency is declared **before** the cutover

**Commit boundary**: `build(deps): add koin modules beside hilt`

- [X] T029 **Cutover, one commit with the five tasks below.** In `app/src/main/java/QL/QuestLogApp.kt` drop `@HiltAndroidApp`, `@Inject` and `HiltWorkerFactory`: `startKoin { androidContext(this@QuestLogApp); workManagerFactory(); modules(…all six…) }`, keep `Configuration.Provider` with on-demand WorkManager initialization as the Koin docs describe, and read `ReleaseRefreshScheduler` through `by inject()`. In `MainActivity.kt` drop `@AndroidEntryPoint` and `@Inject`, use `by inject()` for the two use cases
- [X] T030 (cutover) In `app/src/main/java/QL/QuestLogNavDisplay.kt` replace every `hiltViewModel()` with `koinViewModel()` and the two assisted calls with `koinViewModel { parametersOf(route.gameId) }` / `koinViewModel { parametersOf(route.listId) }`; keep the `rememberViewModelStoreNavEntryDecorator` so ViewModels stay scoped per entry
- [X] T031 (cutover) Delete every Hilt annotation and import (`@Inject`, `@Singleton`, `@HiltViewModel`, `@AssistedInject`, `@AssistedFactory`, `@Assisted`, `@ApplicationContext`, `@Module`, `@InstallIn`, `@Binds`, `@Provides`, `javax.inject.*`, `dagger.*`) from `core/common`, `core/domain` (46 files), `core/network`, `core/database`, `core/data`, `core/ai` and all seven `feature/*` ViewModels; delete the three Hilt modules (`core/data/.../di/DataModule.kt`, `core/network/.../di/NetworkModule.kt`, `core/database/.../di/DatabaseModule.kt`) and `core/common/.../di/CommonModule.kt`. Verify with `grep -rnE "dagger|javax\.inject|androidx\.hilt" --include=*.kt core feature app` printing nothing
- [X] T032 (cutover) Remove `alias(libs.plugins.hilt)`, `ksp(libs.hilt.compiler)`, `implementation(libs.hilt.*)`, `libs.hiltNavCompose`, `libs.androidx.hilt.work`, `ksp(libs.androidx.hilt.compiler)` from every `build.gradle.kts` (all 18 modules), and the `hilt` plugin line from the root `build.gradle.kts`. Keep `google-devtools-ksp` only where Room or Moshi codegen still uses it
- [X] T033 (cutover) Remove the Hilt entries (`hilt`, `hilt-android`, `hilt-compiler`, `hiltNavCompose`, `androidx-hilt-work`, `androidx-hilt-compiler` and their version refs) from `gradle/libs.versions.toml`. Run the gate checks, `KoinGraphTest`, and parity rows 1–10; check that both workers still run (`adb shell dumpsys jobscheduler | grep questlog`). **In the same commit** amend the rules this swap makes false: the root `CLAUDE.md` (stack lines 6–7, the `:core:ai` bullet that says "Hilt and coroutines", "the only modules without Hilt/KSP"); `feature/CLAUDE.md` (the `@HiltViewModel` / `@AssistedInject` / `hiltViewModel` patterns become `viewModelOf`, `viewModel { (id) -> … }` and `koinViewModel { parametersOf(…) }`, registered in `ViewModelKoin.kt`); `core/data/CLAUDE.md` (the `@HiltWorker` / `HiltWorkerFactory` section becomes `workerOf`); `core/network/CLAUDE.md` (the Hilt and `dagger.Lazy` notes); and the constitution (Principle I's `:core:ai` bullet, Additional Constraints / Platform and Injection), bumping its version per Governance. Commit: `refactor: replace hilt with koin`

### B4 — Retrofit + Moshi → Ktor + kotlinx.serialization (research R5)

- [X] T034 [P] Add `core/network/src/test/java/QL/core/network/IgdbDtoDecodingTest.kt` with a private `decode<T>(json: String)` helper and JSON fixtures for every DTO in `core/network/src/main/java/QL/core/network/model/` (12 `@JsonClass` files): a fully populated game, a game with every optional field absent, `null` values, unknown extra keys, and the auth response. Assert on the decoded fields. The helper uses Moshi now and is the **only** line that changes in T037; all assertions must pass before and after
- [X] T035 [P] Extend `core/data/src/test/java/QL/core/data/repository/RepositoryErrorMapperTest.kt` with cases for the Ktor timeout types that replace `SocketTimeoutException` in T040: `io.ktor.client.plugins.HttpRequestTimeoutException`, `io.ktor.client.network.sockets.ConnectTimeoutException` and `io.ktor.client.network.sockets.SocketTimeoutException` all map to `RepositoryError.RequestTimeout`; a `kotlinx.io.IOException` (not an `IgdbHttpException`) maps to `RepositoryError.Unknown`. These fail until T043
- [X] T036 Add the Ktor dependencies to `core/network/build.gradle.kts` (`ktor-client-core`, `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`, `kotlinx-serialization-json`, `kotlinx-io-core`); keep Retrofit and Moshi until the swap is done
- [X] T037 Convert the 12 DTOs in `core/network/src/main/java/QL/core/network/model/` from `@JsonClass(generateAdapter = true)` / `@Json(name = …)` to `@Serializable` / `@SerialName(…)`, keeping field names, nullability and defaults. Configure the `Json` instance with `ignoreUnknownKeys = true`, `explicitNulls = false` (so an absent nullable field decodes as `null`, as Moshi does) and `coerceInputValues = true`. Switch the `decode` helper in the DTO test to that `Json`; do not edit any assertion
- [X] T038 Change `IgdbApiService` (`core/network/src/main/java/QL/core/network/IgdbApiService.kt`) from Retrofit's `@POST(…) (@Body body: RequestBody)` to an interface whose five methods take the Apicalypse text, `suspend fun searchGames(query: String): List<IgdbGame>` and so on; add `IgdbApiServiceImpl.kt` (internal) over `HttpClient`, posting `text/plain` to `https://api.igdb.com/v4/games|popularity_primitives|platforms|release_dates`. Convert `IgdbAuthService.kt` to a Ktor `submitForm` / query POST to `https://id.twitch.tv/oauth2/token` returning `IgdbAuthResponse`, on a **separate** `HttpClient` with no auth plugin (it must not call itself)
- [X] T039 Replace `IgdbHttpErrorInterceptor.kt` with a Ktor `HttpResponseValidator` (file `IgdbHttpResponseValidator.kt`) that throws `IgdbHttpException(code, message)` for every non-2xx response, so Ktor's own `ResponseException` never leaves `:core:network`. Change `IgdbHttpException` (`IgdbHttpException.kt`) to extend `kotlinx.io.IOException` and update its KDoc: it is no longer thrown from an interceptor chain
- [X] T040 Build the client in `core/network/src/main/java/QL/core/network/di/NetworkKoin.kt`: `HttpClient(OkHttp)` with `ContentNegotiation { json(…) }`, the response validator, `defaultRequest` adding `Client-ID`, a request pipeline step adding `Authorization: Bearer <token>` from `IgdbAuthManager.getAccessToken()` (a suspend call, so no `runBlocking`; keep the "no token → send unauthorized, get a 401" behaviour documented in `IgdbAuthManager`), and `Logging` at `LogLevel.BODY` only when `BuildConfig.DEBUG` (headers carry the Bearer token, as the old KDoc warns). Keep `ElapsedRealtimeSource` bound to `SystemClock.elapsedRealtime()`: **do not** use `TimeSource.Monotonic`, which stops in deep sleep (research R5). Delete the Retrofit, Moshi, OkHttp-interceptor and logging-interceptor wiring
- [X] T041 Update `core/data/src/main/java/QL/core/data/repository/GameRepositoryImpl.kt`: replace the nine `queryText.toRequestBody("text/plain".toMediaTypeOrNull())` sites (lines ~196, 213, 317, 338, 376, 405, 466, 580, 607) by passing the query `String` straight to the service, and delete the `okhttp3` imports and `implementation(libs.okhttp)` from `core/data/build.gradle.kts`
- [X] T042 Update the four tests that mock the service with `okhttp3.RequestBody`: `GameRepositoryImplDeveloperGamesTest.kt`, `GameRepositoryImplDiscoverCacheTest.kt`, `GameRepositoryImplPopularGamesTest.kt` and `GameRepositoryImplRefreshReleaseDatesTest.kt` under `core/data/src/test/java/QL/core/data/repository/`. The edit is **only** the body type: `any<RequestBody>()` → `any<String>()`, `slot<RequestBody>()` → `slot<String>()`, and the `RequestBody.asText()` helpers deleted in favour of the captured string. Every assertion on the query text, call counts and results stays as it is (FR-011); review the diff to confirm no assertion changed
- [X] T043 Update `core/data/src/main/java/QL/core/data/repository/RepositoryErrorMapper.kt`: keep the `java.net` checks but move them into `internal fun Throwable.isConnectivityFailure(): Boolean` and `internal fun Throwable.isTimeoutFailure(): Boolean` in the same file, and add the Ktor timeout types from the new test to the timeout check. The existing `RepositoryErrorMapperTest` cases pass unchanged
- [ ] T044 Manual: parity rows 2, 3, 4 and 11 on the debug build; delete the app's data and confirm a fresh token is fetched; force an expired token (clear it via a debug breakpoint or wait out a short `expiresIn`) and confirm one refresh and a successful retry; airplane mode shows the offline error page. **In the same commit** rewrite `core/network/CLAUDE.md` for Ktor (client, response validator, `String` bodies, auth step), update `core/data/CLAUDE.md` lines ~40–41 (Retrofit's `HttpException` becomes Ktor's `ResponseException`), the root `CLAUDE.md` stack and data-flow lines, and the constitution (Principle II rationale, Platform stack line), bumping its version. Commit: `refactor(network): replace retrofit and moshi with ktor`
  - _Partly verified on the emulator: the offline error page shows. A live search, the token fetch and the expired-token refresh could not be run, because this network's TLS-intercepting proxy is not trusted by the emulator. Needs an unproxied network._

### B5 — Room on the bundled driver; DataStore at an explicit path (research R6)

- [X] T045 In `core/database/build.gradle.kts` add `androidx.sqlite:sqlite-bundled`. In `core/database/src/main/java/QL/core/database/di/DatabaseKoin.kt` add `.setDriver(BundledSQLiteDriver())` and `.setQueryCoroutineContext(Dispatchers.IO)`, and change the seed callback to `override fun onCreate(connection: SQLiteConnection)` using `connection.execSQL(…)` with the same two `INSERT` statements. Keep the database name, `version = 1` and `.fallbackToDestructiveMigration(true)`. Confirm `git diff core/database/schemas` is empty; if Room's output differs, commit the regenerated schema in the same commit
- [X] T046 In `core/data/src/main/java/QL/core/data/di/DataKoin.kt` replace the `preferencesDataStore("settings")` delegate with `PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }` (add `datastore-preferences-core` if needed), which resolves to the same `files/datastore/settings.preferences_pb`. The three preference-store tests (`AppearancePreferenceStoreImplTest.kt`, `OnboardingPreferenceStoreImplTest.kt`, `WishlistViewModePreferenceStoreImplTest.kt`) pass unchanged
- [X] T047 Manual: **Android update path** (`quickstart.md`). Install this build over the Phase 1 device without uninstalling. Onboarding must not reappear, dark appearance must hold, both lists with their games, statuses and custom cover must be present, and the reminder must still be scheduled. A loss here means a path or driver step is wrong: fix it, do not accept the reset. **In the same commit** update `core/database/CLAUDE.md` (the "legacy `SupportSQLiteOpenHelper` path" and "deliberately unused" paragraph: the bundled driver is now in use) and any constitution or `CLAUDE.md` line that describes the database setup. Commit: `refactor(database): move room to the bundled sqlite driver`
  - _Verified on the emulator only, by installing over the previous build: the datastore file is byte-identical, the row counts, list names and default-list pointer match, onboarding does not reappear, and a fresh database still seeds the default wishlist. That data has no custom cover, no dark appearance and no scheduled reminder, so those three were not exercised._

### B6 — Coil 3 and string cover paths (research R8)

- [X] T048 Replace `coil-compose` 2.x with Coil 3 (`coil3.compose`, `coil-network-ktor3`, plus `ktor-client-okhttp` for the fetcher) in `core/ui/build.gradle.kts`, `feature/search/build.gradle.kts` and `app/build.gradle.kts`; update every `coil.compose.AsyncImage` / `coil.request.ImageRequest` import (`grep -rn "import coil\." --include=*.kt core feature app`)
- [X] T049 Replace `java.io.File` with absolute-path `String` for covers in `feature/lists/src/main/java/QL/feature/lists/model/WishlistListUiModel.kt`, `feature/lists/.../components/WishlistRow.kt`, `feature/wishlist/.../WishlistScreen.kt`, `feature/wishlist/.../components/WishlistDetailHeader.kt` and `core/ui/.../component/WishlistFormSheet.kt` (line ~211, the `if (coverImage.startsWith("/")) File(coverImage) else coverImage` branch). Update `ListsUiMapperTest.kt` and `WishlistUiMapperTest.kt` only where they construct or compare a `File`. Manual: a list with a custom cover shows it in Lists, the wishlist header and the edit sheet. If Coil 3 does not resolve a bare absolute path, map it to `"file://$path"` in one place, `core/ui/.../component/gamecard/GameCoverImage.kt`. **In the same commit** change "Coil 2" to "Coil 3" in the root `CLAUDE.md` stack line and the constitution's Platform line. Commit: `refactor(ui): move to coil 3 and string cover paths`
  - _Verified on the emulator: Coil 3 resolves a bare absolute path with no `file://` mapping, so `GameCoverImage` is unchanged. A seeded cover shows in Lists, the wishlist header and the edit sheet. A cover freshly picked from the system picker (a content URI) was not exercised._

### B7 — Remove the Android-only glow (research R8)

- [X] T050 In `feature/game-detail/src/main/java/QL/feature/gamedetail/components/GameDetailActionPill.kt` replace `me.trishiraj.shadowglow.shadowGlow` (`pillGlow`, lines ~177–184) with Compose's `Modifier.dropShadow(…)` using the same colour, blur radius `GLOW_BLUR_RADIUS` and spread `GLOW_BLUR_SPREAD`; remove `shadowglow` from `feature/game-detail/build.gradle.kts` and from `gradle/libs.versions.toml`. Compare the pill against the Phase 1 screenshots in light and dark. **If the result is visibly different, stop and ask the owner** before accepting it (FR-001). Commit: `refactor(game-detail): replace shadowglow with a compose drop shadow`
  - _Compared on the emulator, pill region of the game-detail screen before and after: largest per-channel difference 7 (light) and 9 (dark), on 0 and 450 of 312,000 pixels. Not visibly different._

**Checkpoint (Phase 2)**: Android builds on Hilt-free, Retrofit-free, Coil-2-free code; every gate check and `KoinGraphTest` pass; the update-path check passed; parity rows 1–11 pass. The project is still Android-only and nothing is a KMP module yet

---

## Phase 3: User Story 1 - Android keeps working, on shared code (Priority: P1) 🎯 MVP

**Goal**: Every module except `:core:ai` becomes a KMP module that compiles its `commonMain` against a multiplatform target set, `:shared` takes over navigation, and the Android app runs on it with no behaviour change.

**Independent Test**: The parity checklist (SC-002), the Android update path, and a test count at or above the baseline (SC-001) — all without any iOS toolchain. This is the stopping point if iOS has to wait.

> Modules convert leaf-first. Each module is checked with its Android host tests and with
> `./gradlew :<module>:compileCommonMainKotlinMetadata` (macOS). On Windows, run
> `grep -rnE "^import (java|javax|android)\." <module>/src/commonMain`, which must print nothing.

### C0 — Build logic

- [X] T051 [US3] Add `build-logic/` as an included build: `build-logic/settings.gradle.kts` (shares `gradle/libs.versions.toml` as `libs`), `build-logic/convention/build.gradle.kts` (`kotlin-dsl`; `compileOnly` AGP, Kotlin and Compose Gradle plugins), and `pluginManagement { includeBuild("build-logic") }` in the root `settings.gradle.kts`
- [X] T052 [US3] Write `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt` registering **`questlog.kmp.library`**: applies `org.jetbrains.kotlin.multiplatform` and `com.android.kotlin.multiplatform.library`; `kotlin { android { namespace = "com.nikolasguillen.questlog.<path without hyphens, with dots>"; compileSdk = 37; minSdk = 29; withHostTest {} } ; iosArm64(); iosSimulatorArm64() }`; JVM target 11. Namespaces must equal today's (e.g. `:feature:game-detail` → `com.nikolasguillen.questlog.feature.gamedetail`). Add `kotlin.native.ignoreDisabledTargets=true` to `gradle.properties` so Windows builds skip the iOS targets
- [X] T053 [US3] Add `shared`, `build-logic` and `ios` to the `SCOPES` list in `.githooks/commit-msg` (the root `CLAUDE.md` says that list is authoritative)
- [X] T054 [US3] Amend the rules that this commit makes false: in the root `CLAUDE.md` replace "There is no `build-logic` or `buildSrc`. Every `build.gradle.kts` repeats …" with the convention-plugin rule (copy an existing module of the same kind and apply the matching `questlog.*` plugin); in `.specify/memory/constitution.md` update Additional Constraints / Platform and Principle V's "no `build-logic` or `buildSrc`" sentence, and bump the version per its Governance section; delete the "No convention plugins" entry from `docs/tech-debt.md`. Commit: `build: add build-logic with the kmp library convention`

### C1 — Convert the non-UI modules (research R2, R3; contracts/module-contracts.md)

- [X] T055 [US1] `:core:model`: apply `questlog.kmp.library` + the serialization plugin in `core/model/build.gradle.kts` (dependency `kotlinx-serialization-core` in `commonMain`), then `git mv core/model/src/main/java core/model/src/commonMain/kotlin`. Commit the move separately: `refactor(model): move sources to commonmain`. Check: `compileCommonMainKotlinMetadata`, the gate checks
- [ ] T056 [US1] `:core:navigation`: same conversion with JetBrains `navigation3-runtime` in `commonMain`; add, next to `Routes.kt` in `core/navigation/src/commonMain/kotlin/QL/core/navigation/`, a `SavedStateConfiguration` registering every `GameNavKey` subclass in a polymorphic `SerializersModule` (JetBrains Navigation 3 docs, "saving the back stack on non-Android targets"); note it in the `Routes.kt` KDoc as part of "adding a route"
- [ ] T057 [US1] Test the navigation configuration in `core/navigation/src/androidHostTest/kotlin/QL/core/navigation/RoutesSerializationTest.kt`: every route in `Routes.kt` (`SearchRoute`, `ListsRoute`, `RadarRoute`, `SettingsRoute`, `OwnedPlatformsRoute`, `ReleaseNotificationsRoute`, `OnboardingRoute`, `WishlistRoute(listId)`, `GameDetailRoute(gameId)`) round-trips through the configuration's serializers module, and a test fails when a new `GameNavKey` subclass is missing from it (use `GameNavKey::class.sealedSubclasses` to cross-check). Add `kotlin-reflect`, `kotlinx-serialization-json` and JUnit4 to `androidHostTest` in `core/navigation/build.gradle.kts`, and run `./gradlew :core:navigation:testAndroidHostTest` explicitly
- [ ] T058 [US1] `:core:common` conversion: apply the plugin; `git mv` sources to `commonMain` and the two Android-only classes (`AppVersionProviderImpl.kt`, `NetworkStatusProviderImpl.kt`) to `core/common/src/androidMain/kotlin/QL/core/common/`. Make `DateRendering.kt` an `internal expect fun renderLocalDate(date, pattern)` / `(date, style)` pair with `java.time` actuals in `androidMain` (`DateRendering.android.kt`). Add `expect val commonPlatformModule: Module` in `commonMain` (actual in `androidMain` binds the two `*Impl` classes); `commonModule` keeps only common bindings. T006 moves to `androidHostTest` and passes unchanged; run `./gradlew :core:common:testAndroidHostTest` explicitly and confirm it executes `DateUtilsTest` (the aggregate command is not settled until the next-but-one task)
- [ ] T059 [US1] `:core:domain` conversion: apply the plugin with `kotlinx-coroutines-core` and `kotlinx-datetime` in `commonMain` and MockK, JUnit4 and `kotlinx-coroutines-test` in `androidHostTest`; `git mv` sources, then `git mv core/domain/src/test/java core/domain/src/androidHostTest/kotlin` (14 test files, no edits); run `./gradlew :core:domain:testAndroidHostTest` explicitly and confirm all 14 files execute
- [ ] T060 [US1] Settle the test command: run `./gradlew test`, `./gradlew :core:domain:testAndroidHostTest` and `./gradlew allTests`, and record which of them reaches every converted module's host tests. If `./gradlew test` no longer does, pick the aggregate (or register a root `test` task depending on each module's `testAndroidHostTest`), compare the count with T001, and update the commands in the root `CLAUDE.md` (Commands section and the Windows note), `quickstart.md` and the constitution's Principle V in the same commit. Commit: `build: settle the unit test command for multiplatform modules`
- [ ] T061 [US1] `:core:network` conversion: apply the plugin with Ktor core, content-negotiation, serialization and logging in `commonMain`; `ktor-client-okhttp` in `androidMain`; replace the module's `buildConfigField` blocks and `java.util.Properties` code in `core/network/build.gradle.kts` with the `com.github.gmazzo.buildconfig` plugin generating `internal object IgdbCredentials` from `local.properties` into `commonMain`'s generated sources (never committed, FR-013); replace `BuildConfig.DEBUG` with a public `NetworkConfig(val logBodies: Boolean)` in `core/network/src/commonMain/kotlin/QL/core/network/NetworkConfig.kt`. Until the `:shared` entry point exists, `:app` binds `NetworkConfig(BuildConfig.DEBUG)` in a new `app/src/main/java/QL/di/InterimKoin.kt` (`val interimModule`), which the later `:app` slimming task deletes. Make `ElapsedRealtimeSource` bindings per platform: `expect val networkPlatformModule: Module`, actual in `androidMain` using `SystemClock.elapsedRealtime()`. Move `IgdbAuthManagerTest.kt` to `androidHostTest` unchanged. Check that `git status` shows no generated or secret file
- [ ] T062 [US1] `:core:database` conversion: apply the plugin and the `androidx.room` Gradle plugin (`room { schemaDirectory("$projectDir/schemas") }`, replacing the `ksp { arg("room.schemaLocation", …) }` block), KSP on `kspAndroid`, `kspIosArm64` and `kspIosSimulatorArm64`; in `commonMain` declare `expect object QuestLogDatabaseConstructor : RoomDatabaseConstructor<QuestLogDatabase>` and annotate `QuestLogDatabase` with `@ConstructedBy(QuestLogDatabaseConstructor::class)`; keep `version = 1`. Create `core/database/src/commonMain/kotlin/QL/core/database/DefaultWishlistSeed.kt` (`data class DefaultWishlistSeed(val name: String, val description: String)`), and have the seed callback use it instead of `Context.getString` (move `default_wishlist_name` and `default_wishlist_description` from `core/database/src/main/res/values/strings.xml`, which is deleted, to `app/src/main/res/values/strings.xml`, and bind `DefaultWishlistSeed` from them in `InterimKoin.kt` so every commit keeps a complete Koin graph; they move on to `:shared` in T086). Provide `expect fun databaseBuilder(): RoomDatabase.Builder<QuestLogDatabase>` with the Android actual returning `Room.databaseBuilder(androidContext(), QuestLogDatabase::class.java, QuestLogDatabase.DATABASE_NAME)` for the same file path as today. `git diff core/database/schemas` must be empty. Verify that the `@Transaction` functions on the DAOs in `dao/GameDao.kt`, `ListDao.kt`, `PlatformDao.kt` and `DiscoverCacheDao.kt` generate for all three targets with `./gradlew :core:database:kspCommonMainKotlinMetadata` (and, on macOS, `kspKotlinIosSimulatorArm64`)
- [ ] T063 [US1] `:core:data` conversion: apply the plugin; `commonMain` gets the repository, the mappers (except `TranslationMapper.kt`), the three preference stores, `RepositoryErrorMapper.kt` and `DataKoin.kt`; `androidMain` gets `notification/ReleaseNotifierImpl.kt`, `scheduler/ReleaseRefreshSchedulerImpl.kt`, `scheduler/ReleaseNotificationSchedulerImpl.kt`, `worker/*`, `translation/*` (including `TranslationMapper.kt`), `local/WishlistCoverImageStorageImpl.kt`, and `src/main/res/values/strings.xml` → `core/data/src/androidMain/res/values/strings.xml`; the `:core:ai` dependency becomes `androidMain` only. Create `expect val dataPlatformModule: Module` with the Android actual holding the Android bindings and workers that were in `DataKoin.kt`. Turn the `java.net` checks in `RepositoryErrorMapper.kt` into `internal expect fun Throwable.isConnectivityFailure(): Boolean` / `isTimeoutFailure()` with `androidMain` actuals; create the DataStore through `PreferenceDataStoreFactory.createWithPath` fed by an `expect fun settingsDataStorePath(): Path` (Android actual: the same `files/datastore/settings.preferences_pb`). Move the 19 test files to `androidHostTest` with `git mv`, no edits
- [ ] T064 [US1] Run the gate checks, `KoinGraphTest` (add the new platform modules and `interimModule` to its list), parity rows 1–11 and the Android update path. Commit the C1 series with subjects like `refactor(<module>): convert to a multiplatform module`; each module is its own commit and passes the gate checks
- [ ] T065 [US3] Windows check: on the Windows machine run `.\gradlew.bat :app:assembleDebug` and `.\gradlew.bat :core:domain:testAndroidHostTest`, and confirm Gradle configures every module with `kotlin.native.ignoreDisabledTargets=true`, including the `kspIosArm64` / `kspIosSimulatorArm64` configurations in `core/database/build.gradle.kts`. If configuration fails because disabled targets have no KSP configuration, guard the iOS target declarations in `KmpLibraryConventionPlugin.kt` and those KSP lines with `HostManager.hostIsMac`. Record the result in `specs/010-kmp-migration/baseline.md`

### D — Convert the UI modules to Compose Multiplatform (research R8)

- [ ] T066 [US3] Write **`questlog.kmp.compose`** and **`questlog.kmp.feature`** in `build-logic/convention/src/main/kotlin/` (contracts/module-contracts.md § Convention plugins): Compose compiler and CMP plugins, `androidResources { enable = true }`, resources class naming, and for features the six allowed `:core:*` `commonMain` dependencies, the JetBrains lifecycle artifacts, and the `androidHostTest` dependencies. Commit: `build: add the compose and feature conventions`
- [ ] T067 [US1] `:core:designsystem` conversion: apply `questlog.kmp.compose`; `git mv` sources to `commonMain`; extract the `WindowCompat`/`Activity`/`LocalView` block of `theme/QuestLogTheme.kt` into `internal expect fun SystemBarsAppearance(darkTheme: Boolean)` with the Android actual in `androidMain` (`SystemBarsAppearance.android.kt`); `QuestLogTheme` calls it. Check parity: status-bar icon colours in light and dark are unchanged
- [ ] T068 [US1] `:core:ui` — build and sources: apply `questlog.kmp.compose` with CMP foundation/material3, `org.jetbrains.compose.material:material-icons-core`/`-extended` `1.7.3`, Haze, Coil 3 and `compose.resources { publicResClass = true; packageOfResClass = "com.nikolasguillen.questlog.core.ui.resources" }` in `core/ui/build.gradle.kts`; `git mv` sources to `commonMain`, except the Android-only files in the next task
- [ ] T069 [US1] `:core:ui` — resources: `git mv core/ui/src/main/res/values/strings.xml core/ui/src/commonMain/composeResources/values/strings.xml` and `core/ui/src/main/res/drawable/*` (6 `ic_wishlist_*.xml` vectors and `placeholder.png`) to `composeResources/drawable/`; fix any resource syntax Compose resources reads differently (escaping, `plurals`, positional `%1$s` arguments); replace every `R.string.x` / `R.drawable.x` in the module with `Res.string.x` / `Res.drawable.x` and the `androidx.compose.ui.res.stringResource` / `painterResource` / `pluralStringResource` imports with `org.jetbrains.compose.resources.*`
- [ ] T070 [US1] `:core:ui` — `UiText`: in `core/ui/src/commonMain/kotlin/QL/core/ui/model/UiText.kt` replace `@StringRes Int` / `@PluralsRes Int` with `org.jetbrains.compose.resources.StringResource` / `PluralStringResource` keeping `equals`/`hashCode`/`toString`; keep `@Composable asString()`; replace `asString(context: Context)` with `suspend fun asString(): String` built on `getString` / `getPluralString`. Update only the resource references in the 9 test files that build a `UiText` (`grep -rln "UiText" core/*/src feature/*/src` under the test source sets) from `R.string.x` to `Res.string.x`; no assertion changes
- [ ] T071 [US1] `:core:ui` — Android-only UI behind `expect` (contracts/platform-contracts.md): `git mv` `util/NotificationPermission.kt` and `component/NotificationPermissionDeniedDialog.kt` to `core/ui/src/androidMain/kotlin/QL/core/ui/…`, with `expect` declarations in `commonMain` exposing exactly what their callers use; add `expect fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher` (Android actual: the `PickVisualMedia(ImageOnly)` launcher from `component/WishlistFormSheet.kt`, returning the content `Uri` string) and `expect fun rememberTextSharer(): TextSharer` (Android actual: the `ACTION_SEND` chooser from `feature/game-detail/.../GameDetailScreen.kt`, lines ~69–73). `WishlistFormSheet.kt` uses `rememberCoverImagePicker`, and no `commonMain` file may import `androidx.activity.*`
- [ ] T072 [US1] `:core:ui` — finish: use the alias rule for other modules (`import QL.core.ui.resources.Res as CoreUiRes`; the module's own `Res` imported bare); replace `android.content.Context` use in `UiText.kt` and `LocalContext` use elsewhere; move `core/ui/src/test` to `androidHostTest` (`PlatformPickerMapperTest.kt`, plus the `GameUiMapperTest.kt` and `HtmlUtilsTest.kt` added in Phase 2); run the grep guard and `compileCommonMainKotlinMetadata`
- [ ] T073 [P] [US1] `:feature:settings` conversion: apply `questlog.kmp.feature`; `git mv` `src/main/java` → `src/commonMain/kotlin`, `src/main/res` → `src/commonMain/composeResources`, `src/test/java` → `src/androidHostTest/kotlin`; swap `R` for `Res` with the `CoreUiRes` alias rule; use `UiText.asString()` (suspend) where a `Context` was used. No `commonMain` file imports `android.*`
- [ ] T074 [P] [US1] `:feature:search` conversion: same steps as settings; additionally replace `androidx.activity.compose.BackHandler` (`SearchScreen.kt`) with the common `BackHandler` from CMP 1.12.1 (confirm the artifact and package in the CMP docs), replace `LocalContext` + `asString(context)` (lines ~77, 86) with the suspend `asString()`, and `annotatedStringResource` (`components/SearchBars.kt`, line ~396) with a `Res`-based variant that parses with the parser from T013
- [ ] T075 [P] [US1] `:feature:radar` conversion: same steps; `RadarScreen.kt` `LocalContext` + `asString(context)` (lines ~70, 91) → suspend `asString()`
- [ ] T076 [P] [US1] `:feature:wishlist` conversion: same steps; `WishlistScreen.kt` (lines ~102, 111) and `components/SwipeToRevealRow.kt` (`LocalDensity`, `LocalHapticFeedback` are common — keep)
- [ ] T077 [P] [US1] `:feature:lists` conversion: same steps; `ListsScreen.kt` (lines ~57, 64)
- [ ] T078 [P] [US1] `:feature:game-detail` conversion: same steps; `GameDetailScreen.kt` uses `rememberTextSharer()` instead of `Intent`/`startActivity` (lines ~48, 69–73) and the suspend `asString()` for the snackbar (line ~90)
- [ ] T079 [P] [US1] `:feature:onboarding` conversion: same steps; replace `BackHandler` and `android.os.Build` in `OnboardingScreen.kt` (the notification-permission API-33 check moves behind the existing permission `expect`)
- [ ] T080 [US1] Run the gate checks, `compileCommonMainKotlinMetadata` for every UI module, and parity rows 1–10 in light and dark against the Phase 1 screenshots after **each** feature module, and again after all seven. Commit each module as `refactor(<scope>): convert to compose multiplatform`

### E — The `:shared` app root (research R2; contracts/module-contracts.md)

- [ ] T081 [US3] Write **`questlog.android.application`** in `build-logic/convention/src/main/kotlin/` (`com.android.application`, `compileSdk`/`minSdk`/`targetSdk`, Java 11, Compose compiler plugin) and apply it in `app/build.gradle.kts`
- [ ] T082 [US1] Create the `:shared` module: `shared/build.gradle.kts` applying `questlog.kmp.compose`, dependencies on every feature and every `:core:*` module except `:core:ai`, `koin-core`, `koin-compose`, `koin-compose-viewmodel`, JetBrains `navigation3-ui` and `lifecycle-viewmodel-navigation3`; add `include(":shared")` to `settings.gradle.kts`
- [ ] T083 [US1] Write `shared/src/commonMain/kotlin/QL/shared/RootViewModel.kt` exposing `appearanceMode: StateFlow<AppearanceMode>` and `onboardingCompleted: StateFlow<Boolean?>` (`null` until the stored flag is read) from `GetAppearanceModeUseCase` and `GetOnboardingCompletedUseCase`, with its `RootUiState`; test first in `shared/src/androidHostTest/kotlin/QL/shared/RootViewModelTest.kt` (initial `null`, then the stored value; appearance emissions pass through)
- [ ] T084 [US1] Move `app/src/main/java/QL/QuestLogNavDisplay.kt` and `QuestLogBottomBar.kt` to `shared/src/commonMain/kotlin/QL/shared/` (`git mv`, then fix imports) and extract the `Scaffold`/bottom-bar/back-stack body of `MainActivity.kt`'s `MainContent` into `shared/.../QuestLogRoot.kt`: parameters `pendingDeepLinkGameId: Int?`, `onDeepLinkConsumed: () -> Unit`, `displayCornerRadius: Dp`; it resolves `AppearanceMode` → dark/light **once**, via `RootViewModel`, and wraps `QuestLogTheme`; the back stack's first key comes from `RootViewModel.onboardingCompleted`; `rememberNavBackStack` takes the `SavedStateConfiguration` added next to `Routes.kt` in `:core:navigation`. Move the three bottom-bar labels (`search_nav_bar_item`, `radar_nav_bar_item`, `lists_nav_bar_item`) from `app/src/main/res/values/strings.xml` into `shared/src/commonMain/composeResources/values/strings.xml` and switch `QuestLogBottomBar.kt` to `Res.string.*` (`:app` keeps `app_name`)
- [ ] T085 [US1] Move `app/src/main/java/QL/di/ViewModelKoin.kt` to `shared/src/commonMain/kotlin/QL/shared/di/ViewModelModule.kt` and add `RootViewModel` to it; create `shared/.../di/SharedKoin.kt` listing `commonModule`, `domainModule`, `networkModule`, `databaseModule`, `dataModule`, `viewModelModule` and the `expect` platform modules, and exposing `fun initKoin(isDebugBuild: Boolean, platformModules: List<Module>)`, which also binds `NetworkConfig(logBodies = isDebugBuild)`; move `KoinGraphTest.kt` to `shared/src/androidHostTest/kotlin/QL/shared/KoinGraphTest.kt` and keep it passing
- [ ] T086 [US1] Provide `DefaultWishlistSeed` from `:shared`: move the two strings (`default_wishlist_name`, `default_wishlist_description`) into `shared/src/commonMain/composeResources/values/strings.xml` (`app/src/main/res` keeps the Android-only `app_name`, themes and icons), and bind `DefaultWishlistSeed` in `SharedKoin.kt` from `getString(Res.string.default_wishlist_name)` / `…_description`, resolved on first database open. `InterimKoin.kt` is removed in the next task
- [ ] T087 [US1] Slim `:app`: `MainActivity.kt` keeps `installSplashScreen` (held while `RootViewModel.onboardingCompleted` is `null`, plus the existing 500 ms minimum on a fresh launch), `enableEdgeToEdge`, the `questlog://game/{id}` parsing in `Intent.toGameDeepLinkId()` and `onNewIntent`, and the `RoundedCorner` probe (computed inside `setContent` from `LocalView`/`LocalDensity`), and calls `QuestLogRoot(…)`; `QuestLogApp.kt` calls `initKoin(isDebugBuild = BuildConfig.DEBUG, <Android platform modules>)` (with `androidContext` and `workManagerFactory`) and keeps `schedulePeriodicRefresh()`; delete `app/src/main/java/QL/di/InterimKoin.kt`. Reduce `app/build.gradle.kts` to `:shared`, `:core:navigation`, `:core:domain` plus the Android-only libraries it still uses; delete dependencies that moved. Keep the release signing config untouched
- [ ] T088 [US3] Amend the rules that this commit makes false: in the root `CLAUDE.md` ("`:app` is the only module that knows about navigation", the Where-things-live rows for `QuestLogNavDisplay`/`MainActivity`/`QuestLogBottomBar`, the module graph with `:shared`, the "18 modules" count) and in the constitution's Principle I and Principle IV ("resolved once in `MainActivity`" → `QuestLogRoot`), bumping its version per Governance. "Adding a route" is still two edits: a `NavKey` plus its `SavedStateConfiguration` entry in `Routes.kt`, and a branch in `:shared`'s `entryProvider`
- [ ] T089 [US3] Full Android verification: gate checks, `KoinGraphTest`, parity rows 1–11 in light and dark (row 9 is the reminder deep link), and the Android update path once more. Compare the test count with T001 (SC-001). Commit: `refactor: move navigation and the app root into the shared module`
- [ ] T090 [US3] Windows check, final: on the Windows machine run `.\gradlew.bat :app:assembleDebug`, the settled test command, and the `commonMain` import grep for every module; record the result in `specs/010-kmp-migration/baseline.md`

**Checkpoint (Phase 3)**: User Story 1 is complete. Android runs entirely on multiplatform modules, is releasable, and no iOS toolchain has been needed. Everything below is optional until iOS is wanted.

---

## Phase 4: User Story 4 - Platform capabilities behind contracts, with iOS implementations (Priority: P3)

**Goal**: Every capability in `contracts/platform-contracts.md` has an iOS implementation (or a deliberate exception), reached only through its contract.

**Independent Test**: Replace any one platform implementation with a stub and confirm shared code still compiles and its host tests still pass without editing shared code; then compile every module for `iosSimulatorArm64`.

> macOS with Xcode is required from here. Modules are enabled leaf-first, in the order of Phases C and D.

- [ ] T091 [US4] Prepare the Mac: install Xcode, accept its licence, run `./gradlew help` once so Kotlin/Native downloads, and confirm `./gradlew :core:model:compileKotlinIosSimulatorArm64` passes
- [ ] T092 [P] [US4] Write the common iOS-bound implementations in `commonMain` of `:core:data` (they need no platform API, so they are testable on the JVM): `scheduler/InProcessReleaseRefreshScheduler.kt` (`schedulePeriodicRefresh()` runs `RefreshReleaseDatesUseCase` in the app scope when `release_dates_last_refresh_epoch_ms` is missing or older than the 24 h constant shared with the WorkManager scheduler; `scheduleImmediateRefresh()` runs it now; never two at once; the key is written to the settings DataStore only after a successful run), `notification/NoOpReleaseNotificationScheduler.kt`, `notification/NoOpReleaseNotifier.kt`, `translation/UnsupportedGameDescriptionTranslator.kt` (status always `TranslationModelStatus.UNSUPPORTED`; translate/download return the failure the Android implementation returns for an unsupported device). Test first, in `core/data/src/androidHostTest/kotlin/QL/core/data/scheduler/InProcessReleaseRefreshSchedulerTest.kt` (due when missing, due after 24 h, not due at 23 h 59 m, immediate refresh ignores the timestamp, no overlap, a failed run does not advance the timestamp, cancellation is rethrown) and `…/translation/UnsupportedGameDescriptionTranslatorTest.kt`. Add the `release_dates_last_refresh_epoch_ms` key to the settings store
- [ ] T093 [P] [US4] `:core:common` iOS actuals in `core/common/src/iosMain/kotlin/QL/core/common/`: `DateRendering.ios.kt` (`NSDateFormatter`, `NSLocale.currentLocale`, `dateFormat = pattern` or `dateStyle` mapped from `DateStyle`), `AppVersionProviderImpl.ios.kt` (`NSBundle.mainBundle` `CFBundleShortVersionString`, empty on a miss), `NetworkStatusProviderImpl.ios.kt` (last `NWPathMonitor` path: `satisfied && !isExpensive && !isConstrained`) and `actual val commonPlatformModule`. Compile with `./gradlew :core:common:compileKotlinIosSimulatorArm64`
- [ ] T094 [P] [US4] `:core:navigation`, `:core:model`, `:core:domain`: compile for `iosSimulatorArm64` and fix any `commonMain` API that the iOS target rejects
- [ ] T095 [US4] `:core:network` iOS: `ktor-client-darwin` in `iosMain` dependencies; `actual val networkPlatformModule` binding `HttpClient(Darwin)` and an `ElapsedRealtimeSource` over `mach_continuous_time` (counts sleep, like `SystemClock.elapsedRealtime()`); compile
- [ ] T096 [US4] `:core:database` iOS: `actual fun databaseBuilder()` using `NSFileManager` Application Support (create the directory if missing) with the same database file name; compile; confirm `kspKotlinIosSimulatorArm64` generates `QuestLogDatabaseConstructor`'s actual
- [ ] T097 [US4] `:core:data` iOS: in `core/data/src/iosMain/kotlin/QL/core/data/`: `actual fun settingsDataStorePath()` (Application Support), `actual fun Throwable.isConnectivityFailure()` / `isTimeoutFailure()` (a `DarwinHttpRequestException` whose `NSError` domain is `NSURLErrorDomain` and whose code is one of: not connected, cannot find host, cannot connect to host, network connection lost, DNS lookup failed; timeout is `NSURLErrorTimedOut`), `local/WishlistCoverImageStorageImpl.ios.kt` (`persist(source)` reads the temporary file path with `UIImage(contentsOfFile:)`, downsizes to the Android max dimension, writes a `Uuid`-named JPEG/PNG into `Application Support/<same subdirectory name>`, deletes the temporary file; `delete(path)`), and `actual val dataPlatformModule` binding `InProcessReleaseRefreshScheduler` as `ReleaseRefreshScheduler`, the two no-ops, `UnsupportedGameDescriptionTranslator`, the cover storage and `StaticReleaseRemindersAvailability(false)`. Compile
- [ ] T098 [P] [US4] `:core:designsystem` and `:core:ui` iOS actuals: `SystemBarsAppearance` as a no-op (contrast is decided in the walkthrough, T105); `rememberCoverImagePicker` with `PHPickerViewController` (one image, images only, presented from the current top `UIViewController`, the picked image copied to a temporary file whose absolute path is `source`; cancel → `null`); `rememberTextSharer` with `UIActivityViewController`; inert `actual`s for the notification-permission surface (report "not granted", never composed because every call site is behind `ReleaseRemindersAvailability`). Compile both modules
- [ ] T099 [P] [US4] Compile the seven feature modules for `iosSimulatorArm64` and fix what the iOS target rejects (typically a stray `android.*` import that the metadata check missed, or a missing `actual`)
- [ ] T100 [US4] Verify the Phase-4 acceptance: temporarily bind a stub for one capability in a throwaway test (for example a fake `ReleaseRefreshScheduler`), confirm shared modules compile and their host tests pass without a platform implementation, then remove the throwaway; update the capability exception register in `contracts/platform-contracts.md` if any behaviour differs from what it says. Commit per module: `feat(<scope>): add ios implementations of platform capabilities`

**Checkpoint (Phase 4)**: User Story 4 is complete. Every module compiles for `iosArm64`/`iosSimulatorArm64` and Android is unchanged

---

## Phase 5: User Story 2 - Use QuestLog on iOS (Priority: P2)

**Goal**: A person with an iPhone installs QuestLog and can search, save, browse Radar and open details, in the same design as Android.

**Independent Test**: The iOS walkthrough in `quickstart.md` (rows 1–11) on an iOS 16+ simulator, timed for SC-003. Device install, signing and store distribution are out of scope (recorded in `docs/roadmap.md`).

- [ ] T101 [US2] In `shared/build.gradle.kts` configure the iOS framework: `binaries.framework { baseName = "QuestLogShared"; isStatic = true }` for `iosArm64` and `iosSimulatorArm64`; write `shared/src/iosMain/kotlin/QL/shared/MainViewController.kt` (`fun MainViewController(): UIViewController = ComposeUIViewController { … QuestLogRoot(displayCornerRadius = <fixed default>, …) }` after an idempotent `initKoin(isDebugBuild = Platform.isDebugBinary, iosPlatformModules)`, with the `ExperimentalNativeApi` opt-in) and call `ReleaseRefreshScheduler.schedulePeriodicRefresh()` once per launch there. Check `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- [ ] T102 [US2] Create `iosApp/` on macOS (Xcode: new iOS App, SwiftUI, deployment target **iOS 16.0**, bundle id `com.nikolasguillen.questlog`): `iosApp/iosApp.xcodeproj`, `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/ContentView.swift` (a `UIViewControllerRepresentable` hosting `MainViewControllerKt.MainViewController()`, `.ignoresSafeArea(.all)`), `iosApp/iosApp/Info.plist` (display name "Quest Log"; no photo-library or notification usage strings; `ITSAppUsesNonExemptEncryption` = `NO`), and a "Run Script" build phase that runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` before compilation. Link the `QuestLogShared` framework; no CocoaPods, no SwiftPM export
- [ ] T103 [P] [US2] Export the launch assets from `app/src/main/res` (the splash logo drawable and launcher icon sources) into `iosApp/iosApp/Assets.xcassets` (`AppIcon`, `SplashLogo`) and create `iosApp/iosApp/LaunchScreen.storyboard` showing the app logo on the same background colour as the Android splash theme (`Theme.QuestLog.Splash`)
- [ ] T104 [US2] Run the app on an iOS 16+ simulator and fix what stops it launching (framework search path, Compose resources bundling, Koin start, Room file location). Confirm `local.properties` credentials reach the iOS build (a search returns results) and that the generated credentials file is not tracked by git
- [ ] T105 [US2] Walk through `quickstart.md` § iOS walkthrough rows 1–11: welcome flow without a reminders page; first-game timing under 3 minutes (SC-003); persistence after killing the app (FR-010); Radar dates with the device time zone at UTC−8 and UTC+9 matching Android (FR-009); no translate action, no reminder control, no reminders/permission/translation Settings rows (SC-006); the share sheet; appearance follow/force light/force dark; the photo-picker cover; swipe-back; offline error page and retry; the 24 h launch refresh. Record each row's result in `specs/010-kmp-migration/baseline.md`
- [ ] T106 [US2] Decide the status-bar contrast from walkthrough row 7: if system text colour is unreadable on a themed surface, set `UIViewController.preferredStatusBarStyle` from `iosApp/` (not shared code) and update the exception register's last row in `contracts/platform-contracts.md`
- [ ] T107 [US2] Check the iOS performance goals from plan.md on a recent simulator or device: first interactive frame within 2 s of launch, and no visible jank scrolling the Discover shelves, the search grid and the wishlist; if a goal is missed, record it in `docs/tech-debt.md` as a risk rather than tuning inside this feature
- [ ] T108 [US2] Fix the issues the walkthrough found in shared code (not `iosApp/`) where the cause is shared, one commit per fix: `fix(<scope>): <subject>`; re-run the Android gate checks and parity rows after each. Commit the app shell: `feat(ios): add the ios app shell`

**Checkpoint (Phase 5)**: User Story 2 is complete. QuestLog runs on iOS with the capability exceptions in the register

---

## Phase 6: User Story 3 - Verifiable steps, and rules that stay true (Priority: P3)

**Goal**: Prove every step was independently verifiable, the module boundaries still hold, and every instruction file matches the code.

**Independent Test**: Check out any sampled commit of the branch: the Android debug build and the unit tests pass. Run the boundary audit below and see no violation.

The per-commit amendments in Phases 2–3 already corrected the library rules; these tasks finish what remains (final stack versions, module count, commands, and every section that describes the end state).

- [ ] T109 [US3] Boundary audit, with the output recorded in `specs/010-kmp-migration/baseline.md`: `grep -n "project(" feature/*/build.gradle.kts shared/build.gradle.kts` (each feature lists only `:core:common`, `:core:model`, `:core:domain`, `:core:ui`, `:core:navigation`, `:core:designsystem`); `grep -rn "core:ai" --include=*.kts .` (only `core/data/build.gradle.kts`, in `androidMain`); `grep -rnE "^import (android|androidx\.compose|androidx\.activity)" core/model/src` prints nothing; `grep -rn "navigation3" --include=*.kts . | grep -v core/navigation | grep -v shared` shows no feature or core module outside the allowed ones
- [ ] T110 [US3] Stale-reference audit: `grep -rniE "hilt|retrofit|moshi|okhttp|coil 2|src/main/java|CoreUiR|hiltViewModel|BuildConfig|buildSrc|shadowglow" CLAUDE.md */CLAUDE.md core/*/CLAUDE.md feature/CLAUDE.md AGENTS.md README.md docs .specify/memory/constitution.md` and fix every hit that is no longer true (several are legitimately still true: Room, DataStore, WorkManager, `BuildConfig` in `:app`)
- [ ] T111 [US3] Rewrite the root `CLAUDE.md` for the finished state: the stack line (Kotlin 2.4.20, Compose Multiplatform 1.12.1, Koin 4.2.2, Ktor 3, Coil 3, Room KMP, iOS 16+), the Commands section (the settled test command, `:shared:linkDebugFrameworkIosSimulatorArm64`, the Windows/macOS note), the module graph and count (19 modules + `build-logic` + `iosApp/`), "Sources live in `src/commonMain/kotlin`", the `<Module>Res` alias rule (replacing `<Module>R`), the Data-flow chain (Ktor), the Injection rules, and the Where-things-live table (`QuestLogRoot`, `QuestLogNavDisplay` in `:shared`); replace the "Kotlin Multiplatform migration" section with rules for working in a multiplatform codebase (where platform code may live, the contract-per-capability rule, the exception register, never importing `android.*`/`java.*` in `commonMain`)
- [ ] T112 [P] [US3] Update the six directory instruction files after reading each: `feature/CLAUDE.md`, `core/data/CLAUDE.md`, `core/network/CLAUDE.md`, `core/database/CLAUDE.md` (the driver-based API is now in use, remove the "is migrating" note), `core/ui/CLAUDE.md`, `core/designsystem/CLAUDE.md`. Delete rules about things that no longer exist; do not annotate them
- [ ] T113 [P] [US3] Final constitution sweep in `.specify/memory/constitution.md`: Platform (stack versions), Persistence (bundled driver; schema location), Injection (Koin; ViewModels registered in `:shared`), Principle II (rationale names `IgdbHttpException` against Ktor's `ResponseException`, not Retrofit), Principle V (the settled commands), the Kotlin Multiplatform paragraph (migration complete: state the standing rules, drop "decided to migrate"); bump the version per Governance and set Last Amended
- [ ] T114 [P] [US3] Update `docs/tech-debt.md`: remove entries this feature resolved (convention plugins already removed in T054), correct the test-coverage note for the new source-set and task names, add any risk the walkthrough recorded, and update "Last audited". Update `docs/roadmap.md`: delete "Decisions that would be expensive to reverse" bullets that are now done (`kotlinx-datetime`, the Room driver) and keep the iOS follow-ups
- [ ] T115 [P] [US3] Update `README.md` (Tech Stack, Architecture and Modularization) and `AGENTS.md` (the stack line) to the finished state, and add a short "Run on iOS" section (macOS + Xcode, `local.properties` credentials, open `iosApp/iosApp.xcodeproj`)
- [ ] T116 [US3] Sample the history: check out five commits spread across Phases 2–5 (use `git worktree add`), and confirm on each that `./gradlew :app:assembleDebug` and the settled test command pass (SC-005). Record the five hashes in `specs/010-kmp-migration/baseline.md`. Commit: `docs: align instruction files with the multiplatform codebase`

**Checkpoint (Phase 6)**: User Story 3 is complete and the documentation is truthful

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T117 Verify SC-001: run the settled test command and confirm the test count is at or above T001, with no test deleted or disabled; list the tests whose files changed beyond renames (`git diff -M --stat develop -- '*Test*.kt'`) and confirm the only content edits are the ones this file names (the `RequestBody` → `String` type change, the extra `ReleaseRemindersAvailability` constructor argument, the `Res` handles, `File` → `String` in two mapper tests)
- [ ] T118 Verify SC-002: repeat the Phase 1 parity walkthrough on the final Android build in light and dark and compare with the screenshots
- [ ] T119 Verify SC-004: run the two `find … | wc -l` commands in `quickstart.md` § Shared-code ratio and record the percentage; it must be at least 90%, and every file under `androidMain`/`iosMain` must implement a capability from `contracts/platform-contracts.md`
- [ ] T120 Verify SC-007: change one shared rule (for example the `24 h` refresh constant, or a bucket boundary in `ReleaseBucketResolver`) in a single edit, rebuild both apps, and confirm both reflect it; revert the change
- [ ] T121 Run `quickstart.md` end to end once more (both walkthroughs) on the final commit and remove any leftover scratch from `specs/010-kmp-migration/` that is not part of the spec set
- [ ] T122 Run `speckit-analyze` for a cross-artifact consistency pass over spec.md, plan.md and this file before declaring the feature done

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies
- **Phase 2 (Foundational)**: depends on Phase 1 and **blocks everything else**. Within it: B1 → B2 → B3 → B4 → B5 → B6 → B7 in that order (B2's seams are used by B3's modules; B4 and B5 edit the modules B3 created)
- **Phase 3 (US1)**: depends on Phase 2. C0 → C1 (model → navigation → common → domain → settle-tests → network → database → data) → D (designsystem → ui → features) → E
- **Phase 4 (US4)**: depends on Phase 3 and, for the Mac, on Xcode. Leaf-first, same order as Phases C and D
- **Phase 5 (US2)**: depends on Phase 4
- **Phase 6 (US3)**: the amendments in T054 and T088 belong to the Phase 3 commits that make the old rule false; the rest depends on Phases 3–5
- **Phase 7 (Polish)**: depends on everything above

### Within a phase

- A characterization test is written and passing before the code it pins is rewritten
- `git mv` commits precede content-change commits for the same files
- The Hilt cutover tasks form **one** commit
- Phase 3's feature conversions are independent of each other once `:core:ui` is converted

### Parallel opportunities

- Phase 2 B1: the two characterization tests and the three mapper edits touch different modules
- Phase 2 B2: the four ViewModel gates are independent
- Phase 2 B3: the five `*Koin.kt` files are independent of each other
- Phase 3 D: the seven feature conversions
- Phase 4: `:core:common` actuals, the iOS compile of the pure modules, and the UI-module actuals
- Phase 6: the documentation tasks marked [P]

### Parallel example: Phase 3 D features

```bash
# After :core:ui is converted, any order or in parallel (one commit per module):
Task: ":feature:settings conversion"
Task: ":feature:search conversion"
Task: ":feature:radar conversion"
Task: ":feature:wishlist conversion"
Task: ":feature:lists conversion"
Task: ":feature:game-detail conversion"
Task: ":feature:onboarding conversion"
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1: baseline and toolchain
2. Phase 2: platform-neutral code and library swaps (still Android-only)
3. Phase 3: convert everything, move navigation to `:shared`
4. **Stop and validate**: parity checklist, update path, test count. This is a releasable Android app on multiplatform modules, and it is a complete place to pause

### Incremental delivery

1. Phases 1–2: Android on Koin, Ktor, the bundled Room driver and Coil 3 (releasable)
2. Phase 3: Android on multiplatform modules (releasable, MVP)
3. Phase 4: iOS compiles; Android untouched
4. Phase 5: iOS runs; ship it with the exception register
5. Phase 6–7: documentation, sampling, final verification

### Notes

- [P] tasks touch different files and have no unfinished dependency
- A commit that does not pass the gate checks is not a commit (US3 / FR-011)
- Never edit anything under `**/build/generated/**`, and never commit `local.properties`
- If a library API named here differs in the pinned version, follow the library's own docs and record the difference in the commit body; do not change the design without asking
- Tasks that say "stop and ask the owner" must not be skipped
