# AGENTS.md

## Overview
- `GamesWishlist` is a modular Android app built with Kotlin, Jetpack Compose, Hilt, Room, Retrofit/Moshi, and AndroidX Navigation 3.
- Module split is intentional: `feature/*` owns screens + viewmodels, `core/domain` wraps repository calls in use cases, `core/data` is the single repository implementation, `core/network` talks to RAWG, `core/database` persists local state, and `core/model` holds shared models.
- Main app composition lives in `app/src/main/java/com/example/gameswishlist/MainActivity.kt`; feature modules do not own navigation graphs.

## Architecture and data flow
- Standard read path: Feature `ViewModel` -> domain use case -> `GameRepository` -> network/Room -> mapper -> `core/model`.
- Example chain for search: `feature/search/SearchViewModel.kt` -> `core/domain/usecase/SearchGamesUseCase.kt` -> `core/data/repository/GameRepositoryImpl.kt` -> `core/network/RawgApiService.kt`.
- Example chain for list contents: `feature/wishlist/WishlistViewModel.kt` -> `core/domain/usecase/list/GetGamesByListUseCase.kt` -> `GameRepositoryImpl.getGamesByList()` -> `core/database/dao/GameDao.kt`.
- `getGameDetail()` is local-first (`GameDao.getGameById`) and only hits RAWG if the game is absent locally.
- Search currently swallows repository exceptions and returns `emptyList()` in `GameRepositoryImpl.searchGames()`. If you want visible search errors, fix repository/domain behavior, not just UI.

## Navigation and UI composition
- Navigation uses serializable `NavKey`s in `core/navigation/src/main/java/com/example/gameswishlist/core/navigation/Routes.kt` and a single `NavDisplay` in `MainActivity.kt`.
- Add new destinations by extending `GameNavKey`, then wiring the screen into `MainActivity.kt`; there is no per-feature navigation module yet.
- Shared reusable Compose UI belongs in `core/ui` (example: `core/ui/component/GameCard.kt`). Theme tokens live in `core/designsystem`.
- Screens typically collect flows with `collectAsStateWithLifecycle()` and wrap content in `Scaffold(..., contentWindowInsets = WindowInsets.systemBars)`.

## Persistence and model conventions
- Room schema is in `core/database`: tables `games`, `wishlists`, and many-to-many join `game_list_cross_ref`.
- Game personal metadata is stored locally on `GameEntity`: `notes`, `priority`, `status`, `isWishlisted`.
- List-like fields (`platforms`, `genres`, `publishers`, `developers`) are stored as comma-separated strings in `GameEntity`; any model change here requires mapper updates in `core/data/mapper/GameMapper.kt`.
- Watch existing naming mismatches: model uses `metaCritic`, network/database use `metacritic`.
- `Priority` is an enum in `core/model/Priority.kt`; if you touch detail editing, keep mapper conversions (`toInt()`, `toPriority()`) aligned.

## Dependency injection and external services
- Hilt is used everywhere; app entry point is `GamesWishlistApp` with `@HiltAndroidApp`.
- Singleton bindings are in `core/network/di/NetworkModule.kt`, `core/database/di/DatabaseModule.kt`, and `core/data/di/DataModule.kt`.
- Network base URL is hardcoded to `https://api.rawg.io/api/` in `NetworkModule.kt`.
- HTTP logging is set to `BODY`, so avoid adding noisy polling/network loops without considering log volume.

## Build, test, and iteration workflow
- Use the Gradle wrapper from repo root.
- Useful commands:
  - `./gradlew :app:assembleDebug`
  - `./gradlew test`
  - `./gradlew :app:connectedDebugAndroidTest`
  - `./gradlew :feature:search:compileDebugKotlin` (replace module for targeted checks)
- Existing automated tests are minimal (`app/src/test/...ExampleUnitTest.kt`, `app/src/androidTest/...ExampleInstrumentedTest.kt`), so expect to add coverage in the module you touch.

## Project-specific agent guidance
- Prefer editing files under `src/main` and `src/test`; this repo contains checked-in/generated `build/` outputs that can pollute searches.
- Do not edit generated KSP/Room/Moshi files under `**/build/generated/**` or `**/build/kspCaches/**`.
- Keep feature modules UI-focused; business rules belong in `core/domain` or `core/data`, not directly in composables.
- When adding a new feature screen, mirror the current pattern: feature module with `Screen` + `@HiltViewModel`, domain use case if data access changes, route in `core/navigation`, host wiring in `MainActivity.kt`.

