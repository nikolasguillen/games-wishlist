# Data Model: Multiplatform Migration

This feature changes **where** data lives and how it is reached, not what is stored.

- **No entity changes**: no new column, no new table.
- **No database version change.**
- **No user-visible data shape changes.**

Below are the persisted stores (and what must stay identical about them), the handful of new in-memory
types, and the shapes that change because a platform type cannot cross into common code.

## Persisted stores — must be unchanged on Android

| Store | Today | After | Invariant |
|---|---|---|---|
| Room database | `Room.databaseBuilder(context, …, "<name>")`, framework SQLite, `version = 1`, `fallbackToDestructiveMigration(true)` | `commonMain` database, `BundledSQLiteDriver`, same `version = 1`, same fallback | **Same file name and location on Android** (`context.getDatabasePath(<name>)`), so an update keeps saved games and lists. The exported schema in `core/database/schemas/` stays byte-identical, or is re-committed in the same commit if Room's output changes |
| Default-list seed | `RoomDatabase.Callback.onCreate(SupportSQLiteDatabase)` inserts the default list using `context.getString(R.string.default_wishlist_*)` | `onCreate(SQLiteConnection)` inserts the same rows using a `DefaultWishlistSeed` (below) | Same SQL, same rows, same `default_wishlist` pointer row (`id = 0`) |
| Settings `DataStore<Preferences>` | `preferencesDataStore("<name>")` on `Context` | `PreferenceDataStoreFactory.createWithPath { <path> }` | **Android path resolves to the same `files/datastore/<name>.preferences_pb`**, so the appearance mode, onboarding-seen flag and wishlist view mode survive the update. Existing keys and their types are unchanged |
| Wishlist cover images | Files in the app's internal files dir, written by `WishlistCoverImageStorage` | Same directory on Android; Application Support on iOS | Stored paths in the database remain valid on Android after the update |

**New key (iOS only, in the same settings store)**: `release_dates_last_refresh_epoch_ms: Long`. It is
written by the iOS `ReleaseRefreshScheduler` after a successful refresh and read at launch to decide
whether a refresh is due (research R9). Android never reads or writes it.

## New types

### `DefaultWishlistSeed` (`:core:database`, `commonMain`)

| Field | Type | Rule |
|---|---|---|
| `name` | `String` | Non-blank. Resolved from the shared string resource `default_wishlist_name` at the app root |
| `description` | `String` | Resolved from `default_wishlist_description` |

- **Why it exists**: `:core:database` can no longer call `Context.getString`. The two strings move to the shared resources, and the app root (`:shared`) supplies the resolved values through Koin.
- **When it is resolved**: lazily, the first time the database is opened. That happens on a background dispatcher, so resolving the suspend resource lookup there blocks no UI thread.

### `ReleaseRemindersAvailability` (`:core:domain`, contract)

A single read-only capability flag.

| Member | Type | Android | iOS |
|---|---|---|---|
| `isAvailable` | `Boolean` | `true` | `false` |

The flag is constant for the process lifetime, and only ViewModels read it (Principle III). It removes
these entry points when `false`:

| Screen | What disappears when `false` |
|---|---|
| Settings | Release-reminders row and notification-permission row |
| Release notifications screen | Unreachable, since its only entry is the Settings row |
| Game detail | Per-game reminder control and the notifications banner |
| Onboarding | The reminders page (the flow ends after the platforms step) |
| Radar | The permission-request effect is never emitted |

## Shapes that change because a platform type cannot cross into common code

| Type | Today | After | Notes |
|---|---|---|---|
| `UiText.StringResource` | `@StringRes resId: Int` + args | `res: org.jetbrains.compose.resources.StringResource` + args | `equals`/`hashCode` keep comparing resource + args |
| `UiText.PluralResource` | `@PluralsRes resId: Int`, `quantity`, args | `res: PluralStringResource`, `quantity`, args | |
| `UiText.asString(context: Context)` | non-suspend, `Context`-based | `suspend fun asString(): String` (backed by `getString`/`getPluralString`) | Used by the 5 screens that resolve snackbar text inside `LaunchedEffect`; removes their `LocalContext` |
| UI models holding a cover image (`WishlistListUiModel` and the wishlist header) | `java.io.File?` | `String?` (absolute path) | Coil 3 loads a path string on both platforms |
| `WishlistCoverImageStorage.persist(sourceUri: String)` | content `Uri` string from the Android photo picker | `persist(source: PickedImage)` | `PickedImage` is the picker's output (contracts); the storage decodes it per platform |
| `AppVersionProvider`, `NetworkStatusProvider` | concrete `@Singleton` classes taking `Context` | interfaces in `commonMain`, one implementation per platform | Same members (`versionName`, `isUnmeteredNetworkAvailable`); `SettingsViewModelTest` mocks keep working |
| `DateUtils` style parameter | `java.time.format.FormatStyle` | `DateStyle` enum (`SHORT`, `MEDIUM`, `LONG`, `FULL`) in `:core:common` | Same default (`MEDIUM`) |
| Moshi DTOs in `:core:network` | `@JsonClass(generateAdapter = true)`, `@Json(name = …)` | `@Serializable`, `@SerialName(…)` | Field names, nullability and defaults unchanged; `ignoreUnknownKeys = true` |

## Capability exception register (FR-008)

The register of what each platform does differently, as the spec requires. It is kept in
`contracts/platform-contracts.md`, next to the contracts it refers to, and it must be updated in the
same commit as any change to a platform's behaviour.

## Module and source-set map

Where each module's code lives after the migration. **C** = `commonMain`, **A** = `androidMain`,
**I** = `iosMain`.

| Module | Plugin | C | A | I | Tests |
|---|---|---|---|---|---|
| `:core:model` | kmp.library | all | — | — | — |
| `:core:common` | kmp.library | `DateUtils`, `AlgorithmUtils`, contracts for version/network | date actual, `AppVersionProviderImpl`, `NetworkStatusProviderImpl` | same three on iOS | — |
| `:core:domain` | kmp.library | all (contracts + use cases + Koin module) | — | — | androidHostTest (14 files, moved) |
| `:core:navigation` | kmp.library | `Routes.kt`, `SavedStateConfiguration` | — | — | — |
| `:core:network` | kmp.library | Ktor client, DTOs, `IgdbApiService`, auth, validator, generated credentials | OkHttp engine | Darwin engine | androidHostTest (1, moved) |
| `:core:database` | kmp.library + Room | entities, DAOs, database, constructor, seed callback | builder factory (path) | builder factory (path) | — |
| `:core:data` | kmp.library | repository, mappers, error mapper, preference stores, Koin module | WorkManager workers and schedulers, notifier, ML Kit translator, cover storage, connectivity check | in-process refresh scheduler, no-op reminder implementations, `UNSUPPORTED` translator, cover storage, connectivity check | androidHostTest (19, moved) |
| `:core:ai` | android.library (unchanged) | — | all | — | — |
| `:core:designsystem` | kmp.compose | theme, tokens, typography | `SystemBarsAppearance` actual | `SystemBarsAppearance` actual (no-op) | — |
| `:core:ui` | kmp.compose | components, modifiers, mappers, `UiText`, resources | notification-permission UI, picker and sharer actuals | picker and sharer actuals | androidHostTest (1, moved) |
| `feature/*` (7) | kmp.feature | all (screens, ViewModels, mappers, models, resources) | — | — | androidHostTest (12, moved) |
| `:shared` (new) | kmp.compose | `QuestLogRoot`, `QuestLogNavDisplay`, bottom bar, Koin assembly, `ViewModelModule` | Android Koin platform module | `MainViewController()`, iOS Koin platform module | androidHostTest: Koin `verify()` |
| `:app` | android.application | — | `QuestLogApp`, `MainActivity`, manifest, splash, icons | — | `app/src/test` (template, unchanged) |
| `iosApp/` (new, Xcode) | — | — | — | Swift entry point, launch screen, Info.plist, icons | — |
