# R-PLACE — Where things live

Baseline `b982bf11`. All commands run from the repository root over git-tracked files.

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-PLACE-01 | **pass** | `NavDisplay`, `entryProvider`, `NavBackStack` and `backStack.` appear in no file outside `shared/`. The single `entryProvider` is in `shared/.../QuestLogNavDisplay.kt` with 9 branches (`SearchRoute`, `RadarRoute`, `ListsRoute`, `SettingsRoute`, `OwnedPlatformsRoute`, `ReleaseNotificationsRoute`, `WishlistRoute`, `GameDetailRoute`, `OnboardingRoute`) |
| R-PLACE-02 | **pass** | `koinViewModel`, `koinInject`, `KoinPlatform`, `by inject()` appear only in `shared` and `androidApp`, plus the `get<>()` calls inside `core/database` and `core/network` Koin definitions. No feature module has a nav or DI file |
| R-PLACE-03 | **pass** | `Routes.kt` has 9 `GameNavKey` routes; `GameNavSavedStateConfiguration.kt` has 9 `subclass(...)` lines (same names); `RoutesSerializationTest` exists in `core/navigation/src/androidHostTest`; the entry provider has 9 branches. The 010 `module-contracts.md` says adding a route is two edits with the serializer registered "in the same file", which is stale (C-PLACE-2) |
| R-PLACE-04 | **pass** | 10 ViewModel classes in `commonMain` (`GameDetail`, `Lists`, `Onboarding`, `OwnedPlatforms`, `Radar`, `ReleaseNotifications`, `Root`, `Search`, `Settings`, `Wishlist`), 10 `viewModelOf(::…)` lines in `shared/.../di/ViewModelModule.kt`, same names. `initKoin` and `allModules` are in `shared/.../di/SharedKoin.kt` and list the four `expect` platform modules |
| R-PLACE-05 | **pass** | 0 `SavedStateHandle` references |
| R-PLACE-06 | **pass** | `shared/src/androidHostTest/.../di/KoinGraphTest.kt` ("every dependency in the graph is declared") executed and passed (`verification.md`); `:shared` is a multiplatform module, so the root `test` task depends on its `testAndroidHostTest`. It verifies the Android graph (`Context`, `WorkerParameters` as extra types); the iOS `actual` modules are not covered by a JVM test, which `docs/tech-debt.md` already records |
| R-PLACE-07 | **pass** | `installSplashScreen`, `enableEdgeToEdge`, `setKeepOnScreenCondition`, `onNewIntent` appear only under `androidApp/`. `androidApp` holds `MainActivity`, `QuestLogApp`, the manifest and resources; no route, no screen. The only `questlog://` literal outside `androidApp` is the producer in `ReleaseNotifierImpl` (C-PLACE-1) |
| R-PLACE-08 | **pass** | `shared/src/iosMain/.../MainViewController.kt`; `iosApp/` contains only the Xcode project, `iOSApp.swift`, `ContentView.swift` (one `UIViewControllerRepresentable` calling `MainViewControllerKt.MainViewController()`), the launch screen and assets |
| R-PLACE-09 | **pass** | Every cross-module `Res` import is aliased: 24 files in `feature/*` import `core.ui.resources.Res as CoreUiRes`, and none imports it unaliased. Each module's own `Res` is imported bare (`core.ui` 21 files, `feature.*` 52 files, `shared` 2) |
| R-PLACE-10 | **pass** | One implementation: `class GameRepositoryImpl(…) : GameRepository` in `core/data/.../repository/GameRepositoryImpl.kt:174`; the interface is in `core/domain/repository/` |
| R-PLACE-11 | **pass** | `AppResult.kt`, `RepositoryError.kt` in `core/model`; `UiText.kt` in `core/ui/model/`; `RepositoryError.toUiText()` is defined once (`core/ui/.../mapper/ErrorMapper.kt:12`); the only other mentions of `RepositoryError` cases are the constructions in `core/data`'s `RepositoryErrorMapper.kt` |
| R-PLACE-12 | **pass** | `IgdbApiService` returns bare `List<…>` for all 5 methods; no non-internal declaration in `core/network/src/commonMain` mentions `HttpClient`, `HttpResponse`, `HttpRequest`, `HttpStatusCode` or `HttpClientEngine` |
| R-PLACE-13 | **pass** (tracked) | One tracked schema: `core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json`. A second, **empty and untracked** directory `…GamesWishlistDatabase/` exists locally (leftover from the old database class name); it is not in the merge. Local hygiene, in `left.md` |
| R-PLACE-14 | **pass** | No `IgdbCredentials` file or `local.properties` is tracked; `README.md:86-87` shows `your_client_id`/`your_client_secret` placeholders; the generated object is `internal` and comes from `core/network/build.gradle.kts` |
| R-PLACE-15 | **pass** | `ElapsedRealtimeSource` (`fun interface`), `IgdbAuthManager` and `IgdbAuthService` are `internal` |

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-PLACE-1 | The `questlog://game/<id>` deep-link format is spelled out in three places with no shared constant | `androidApp/src/main/AndroidManifest.xml:36-37` (scheme, host), `androidApp/.../MainActivity.kt:94` (parser), `core/data/src/androidMain/.../ReleaseNotifierImpl.kt:28` (builder, in another module). All Android-only, and the format has not changed in the migration; the producer and consumer cannot share a constant without a new edge, so this is a trade-off rather than a defect. Severity low, follow-up, and arguably not worth acting on |
| C-PLACE-2 | `module-contracts.md` describes the route registration as two edits with the serializer "in the same file" | `specs/010-kmp-migration/contracts/module-contracts.md` (Navigation ownership): "a `NavKey` in `core/navigation/Routes.kt`, also registered in its `SavedStateConfiguration` in the same file". The code, the root and `feature/` `CLAUDE.md` and the constitution say three edits, with the `subclass(...)` line in `GameNavSavedStateConfiguration.kt`. Documentation drift in a migration spec. Severity low, follow-up |
