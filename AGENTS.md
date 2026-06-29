# AGENTS.md

## Overview
- `GamesWishlist` is a modular Android app built with Kotlin, Jetpack Compose, Hilt, Room, Retrofit/Moshi, and AndroidX Navigation 3.
- Module split is intentional: `feature/*` owns screens + viewmodels, `core/domain` wraps repository calls in use cases, `core/data` is the single repository implementation, `core/network` talks to IGDB (formerly RAWG), `core/database` persists local state, and `core/model` holds shared models.
- Main app composition lives in `app/src/main/java/com/example/gameswishlist/MainActivity.kt`; feature modules do not own navigation graphs.
- This repository does not include other AI instruction files (for example: `.github/copilot-instructions.md`, `CLAUDE.md`, or `.cursor/rules/`). Use this `AGENTS.md` as the single source of guidance for automated agents.

## Architecture and data flow
- Standard read path: Feature `ViewModel` -> domain use case -> `GameRepository` -> network/Room -> mapper -> `core/model`.
- Example chain for search: `feature/search/SearchViewModel.kt` -> `core/domain/usecase/search/SearchGamesUseCase.kt` -> `core/data/repository/GameRepositoryImpl.kt` -> `core/network/IgdbApiService.kt`.
- `SearchGamesUseCase` is a "Composite Use Case" that returns `SearchResult` (containing both games and derived filters).
- UI Models use the `-UiModel` suffix (e.g., `GameFilterUiModel`, `GameItemUiModel`) to separate domain models from Compose-specific needs.
- **ViewModel Structure**: Always follow the Unidirectional Data Flow (UDF) pattern as implemented in `SearchViewModel.kt` and `GameDetailViewModel.kt`:
    - Use a single `uiState: StateFlow<FeatureUiState>` where `FeatureUiState` is a data class.
    - Use a `sealed interface FeatureContentState` inside the UI state for main content lifecycle (e.g., `Loading`, `Success`, `Error`).
    - Expose a single `onEvent(event: FeatureUiEvent)` function to handle all UI interactions.
    - Initialize data collection (like lists or history) in the `init` block updating the state via `_uiState.update { ... }`.
- Use `AppResult.map` for clean data transformations across layers.

## Navigation and UI composition
- Navigation uses serializable `NavKey`s in `core/navigation/.../Routes.kt` and a single `NavDisplay` in `MainActivity.kt`.
- Shared reusable Compose UI belongs in `core/ui`. Theme tokens live in `core/designsystem`.
- **UI Best Practices**:
    - Use `sealed interface` for UI states and filter types to leverage exhaustive `when` and smart casting.
    - Prefer `alpha(0f)` over removing elements (like selection icons) to avoid layout shifts.
    - Use `animateContentSize()` for smooth transitions if elements must change size.
    - **Modularization**: Extract sub-composables into separate files whenever they represent a logical unit or to prevent main feature files from becoming too large (avoid "God UI" files).
    - Collect flows with `collectAsStateWithLifecycle()`.
    - **Previews**: Always provide a `@Preview` for every new Composable to allow rapid UI iteration and testing. For complex states, provide multiple previews (Loading, Success, Empty, etc.).

## Persistence and Model Conventions
- Room schema in `core/database` is normalized using Many-to-Many relationships for Platforms, Genres, and Involved Companies.
- **Key entities**: `GameEntity`, `PlatformEntity`, `GenreEntity`, `CompanyEntity`, and their respective `CrossRef` tables.
- Use `GameWithAllDetails` POJO to fetch a complete game model in a single transaction.
- Game personal metadata (notes, priority, status, isWishlisted) is stored on `GameEntity`.
- Mapping between Network -> Domain -> Database is managed in `core/data/mapper/GameMapper.kt`.

## Project Rules (AI Guidance)
- **Naming**: Always append `UiModel` to models used in the UI layer.
- **UI Spacing**: When setting a dimension in `dp`, always check if a matching value exists in `MaterialTheme.spacing` first to maintain design consistency.
- **State Management**: Use `sealed interface` for `SearchContentState` or similar UI state patterns.
- **Dialogs**: For any alert dialog, always use the project-specific `CustomAlertDialog` composable from `core:ui` instead of the standard `AlertDialog`.
- **Constants**: Store shared UI constants (like `MAX_PLATFORM_NAME_LENGTH`) in `core/ui/util/Constants.kt`.
- **Filtering**: Perform filtering and logic in the `ViewModel` or `UseCase` using IDs (not names) for robustness.
- **Business Logic**: Keep logic (like the "12 characters label rule") in Mappers or UseCases, not in the Composables.
- **Persistence**: When saving a game, use the `GameDao.saveGame` transactional method to ensure all related entities (Platforms, Genres, etc.) are persisted.
- **Game Types**: We use the IGDB `game_type` field (formerly `category`) to classify games. By default, we exclude "noisy" types like Bundles, Packs, Forks, and Updates.
- **Documentation & Language**: 
    - Always use **English** for comments, KDoc, and any form of internal documentation.
    - **Models**: Mandatory class-level KDoc using `@property` tags for all models in `core:model` (Domain) and `core:network` (Network). Avoid inline comments for fields.
    - **Localization**: NEVER hardcode user-facing strings in models or logic. Use Android String Resources (`strings.xml`). Domain models should only contain raw data or Enums; UI-specific display labels must be handled in the UI layer (e.g., using `UiText` or Mappers).
    - **Business Logic**: Document Use Cases and complex algorithms (like sorting or filtering) explaining the *rationale* and parameters.

## Build, test, and iteration workflow
- Use the Gradle wrapper: `./gradlew :app:assembleDebug`.
- On Windows (PowerShell), prefer the bundled batch wrapper: `.
gradlew.bat :app:assembleDebug` or `.
gradlew :app:assembleDebug` (PowerShell will resolve the .bat wrapper).
- Feature modules should remain UI-focused; business rules belong in `core/domain` or `core/data`.
- Avoid editing generated files under `**/build/generated/**`.
