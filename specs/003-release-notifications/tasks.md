---

description: "Task list for Release Notifications (Phase 3)"
---

# Tasks: Release Notifications

**Input**: Design documents from `/specs/003-release-notifications/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/domain-ports.md](./contracts/domain-ports.md),
[contracts/ui-contracts.md](./contracts/ui-contracts.md), [quickstart.md](./quickstart.md)

**Tests**: Included. `core/domain/CLAUDE.md`'s existing suites and the constitution's Principle V both
expect new use-case/mapper/ViewModel logic to land with a test in the same module's `src/test`, and the
`docs/tech-debt.md` gap list explicitly does *not* cover the modules this feature touches
(`:core:domain`, `:core:data`, `:feature:radar`, `:feature:settings`) — so their existing coverage bar
applies here, not an exemption from it.

**Organization**: Grouped by user story from spec.md (US1, US2 — both P1; US3 — P2), after a Setup phase
and a Foundational phase that every story depends on. Task numbering is execution order; a task's
description states what it builds on when that isn't simply "the previous task" — the full dependency
graph is also spelled out in the Dependencies & Execution Order section at the end.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on another unfinished task)
- **[Story]**: US1, US2, or US3 — omitted for Setup, Foundational, and Polish tasks
- Every task names its exact file path(s)

## Path Conventions

Modular Android app (see root `CLAUDE.md`). `.../` stands for `com/nikolasguillen/questlog/` under
`src/main/java` (or `src/test/java` for tests).

---

## Phase 1: Setup

**Purpose**: Dependency and manifest groundwork the rest of the feature builds on. No behavior yet.

- [X] T001 [P] Add `libs.androidx.activity.compose` and `libs.androidx.core.ktx` to
  `core/ui/build.gradle.kts` (`implementation(...)`, matching the versions already pinned in
  `gradle/libs.versions.toml` — `activityCompose = "1.13.0"`, `coreKtx = "1.19.0"`). Needed for the
  permission-request launcher and `NotificationManagerCompat`.
- [X] T002 [P] In `app/src/main/AndroidManifest.xml`: add
  `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` alongside the existing
  `INTERNET` and `ACCESS_NETWORK_STATE` permissions; add `android:launchMode="singleTop"` to the
  `MainActivity` `<activity>` element; add a second `<intent-filter>` on that same element with
  `<action android:name="android.intent.action.VIEW" />`,
  `<category android:name="android.intent.category.DEFAULT" />`,
  `<category android:name="android.intent.category.BROWSABLE" />` and
  `<data android:scheme="questlog" android:host="game" />` (per
  [contracts/ui-contracts.md](./contracts/ui-contracts.md)'s Deep link section).

**Checkpoint**: Build still compiles; no new symbols referenced yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The persistence, domain ports, and shared UI plumbing every user story needs. No user-visible
behavior on its own — the opt-in has nowhere to be triggered from until Phase 3+.

**⚠️ CRITICAL**: Complete this phase before starting any user story phase.

### Data layer

- [X] T003 [P] Create `core/database/src/main/java/.../core/database/entity/ReleaseNotificationEntity.kt`:
  `@Entity(tableName = "release_notifications") data class ReleaseNotificationEntity(@PrimaryKey val gameId: Int, val enabledAt: Long, val notifiedForDate: Long? = null)`
  — presence of a row means opted in, per [data-model.md](./data-model.md). No foreign key, matching
  every other entity in this module.
- [X] T004 Using the entity from T003, create
  `core/database/src/main/java/.../core/database/dao/ReleaseNotificationDao.kt`:
  ```kotlin
  @Dao
  interface ReleaseNotificationDao {
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun upsert(entity: ReleaseNotificationEntity)

      @Query("DELETE FROM release_notifications WHERE gameId = :gameId")
      suspend fun delete(gameId: Int)

      @Query("SELECT gameId FROM release_notifications ORDER BY enabledAt DESC")
      fun observeGameIds(): Flow<List<Int>>

      @Query("SELECT * FROM release_notifications WHERE gameId = :gameId")
      suspend fun get(gameId: Int): ReleaseNotificationEntity?

      @Query("UPDATE release_notifications SET notifiedForDate = :date WHERE gameId = :gameId")
      suspend fun markNotified(gameId: Int, date: Long)
  }
  ```
- [X] T005 Register `ReleaseNotificationEntity::class` in the `entities = [...]` list and
  `abstract fun releaseNotificationDao(): ReleaseNotificationDao` in
  `core/database/src/main/java/.../core/database/QuestLogDatabase.kt`. Leave `version = 1` unchanged
  (per `core/database/CLAUDE.md` — the app is unpublished, no `Migration`, no version bump).
- [X] T006 Regenerate the exported schema by building `:core:database`
  (`./gradlew :core:database:assembleDebug` — KSP writes the schema as a build side effect) and commit
  the updated `core/database/schemas/com.nikolasguillen.questlog.core.database.QuestLogDatabase/1.json`
  in the same commit as T003–T005. Confirm there is still exactly one directory under `schemas/`.

### Domain ports and repository contract

- [X] T007 [P] Create
  `core/domain/src/main/java/.../core/domain/notification/ReleaseNotificationScheduler.kt`:
  ```kotlin
  interface ReleaseNotificationScheduler {
      fun schedule(gameId: Int, at: Instant)
      fun cancel(gameId: Int)
  }
  ```
  (`kotlin.time.Instant`, matching `GetRadarTimelineUseCase`). Contract only — no `androidx.work` import
  here, per [contracts/domain-ports.md](./contracts/domain-ports.md).
- [X] T008 [P] Create `core/domain/src/main/java/.../core/domain/notification/ReleaseNotifier.kt`:
  ```kotlin
  interface ReleaseNotifier {
      fun notifyReleased(gameId: Int, gameName: String)
      fun canDeliver(): Boolean
  }
  ```
  Contract only — no Android notification API import here.
- [X] T009 [P] Add to `core/domain/src/main/java/.../core/domain/repository/GameRepository.kt` (all five
  are DB-only, so all return a bare `Flow`/`Unit`, never `AppResult`, per the constitution's Principle
  II):
  ```kotlin
  fun getReleaseNotificationGameIds(): Flow<Set<Int>>
  fun getGamesWithReleaseNotifications(): Flow<List<Game>>
  suspend fun setReleaseNotificationEnabled(gameId: Int, enabled: Boolean)
  suspend fun markReleaseNotificationDelivered(gameId: Int, releaseDate: Long)
  suspend fun getReleaseNotificationDeliveredDate(gameId: Int): Long?
  ```
  with KDoc matching [contracts/domain-ports.md](./contracts/domain-ports.md). This is an interface edit
  only — it compiles standalone and does not require T004 to exist yet.
- [X] T010 With the DAO from T004 and the interface from T009 both in place, implement the five new
  `GameRepository` methods in
  `core/data/src/main/java/.../core/data/repository/GameRepositoryImpl.kt`, injecting
  `ReleaseNotificationDao`:
  - `getReleaseNotificationGameIds()` → `releaseNotificationDao.observeGameIds().map { it.toSet() }`
  - `getGamesWithReleaseNotifications()` → `combine(gameDao.getSavedGames(), releaseNotificationDao.observeGameIds()) { saved, orderedIds -> val byId = saved.associateBy { it.game.id }; orderedIds.mapNotNull { byId[it]?.toGame() } }`
    — iterates the DAO's ordered id list (most recently opted-in first), not `saved`'s own order.
  - `setReleaseNotificationEnabled(gameId, enabled)` → upsert with `enabledAt = <now>` when `true`,
    `delete(gameId)` when `false`
  - `markReleaseNotificationDelivered(gameId, releaseDate)` → `releaseNotificationDao.markNotified(...)`
  - `getReleaseNotificationDeliveredDate(gameId)` → `releaseNotificationDao.get(gameId)?.notifiedForDate`

### Shared date resolution (Radar/notification parity — FR-006)

- [X] T011 Create `core/domain/src/main/java/.../core/domain/radar/ReleaseDateResolver.kt` and move the
  private `Game.resolveReleaseDates(ownedPlatformIds: Set<Int>): List<ReleaseDate>` function out of
  `core/domain/src/main/java/.../core/domain/radar/GetRadarTimelineUseCase.kt` into it, unchanged, as a
  top-level function visible to both files. Update `GetRadarTimelineUseCase.kt` to import and call it.
  Also add, in the same new file:
  ```kotlin
  fun resolveNotificationInstant(
      releaseDateEpochSeconds: Long?,
      precision: DatePrecision,
      now: Instant,
      timeZone: TimeZone = TimeZone.currentSystemDefault()
  ): Instant?
  ```
  returning 09:00 local on the release calendar day; `now` when that has passed but the day is still
  today; `null` when the day is past or `precision != DatePrecision.EXACT_DATE` (see
  [research.md](./research.md) §2 and §3 for the exact rule).
- [X] T012 [P] After T011, run `./gradlew :core:domain:testDebugUnitTest --console=plain -q` and confirm
  `core/domain/src/test/java/.../core/domain/radar/GetRadarTimelineUseCaseTest.kt` still passes
  unmodified — it is the regression guard for the extraction in T011.
- [X] T013 [P] After T011, add
  `core/domain/src/test/java/.../core/domain/radar/ReleaseDateResolverTest.kt` covering
  `resolveNotificationInstant` for: `DatePrecision.EXACT_DATE` today before 09:00 local, `EXACT_DATE`
  today after 09:00 local (must return `now`), `EXACT_DATE` yesterday (must return `null`), `EXACT_DATE`
  far future, and each of `YEAR_MONTH` / `QUARTER` / `YEAR_ONLY` / `TBD` (must all return `null`
  regardless of date). Include one case in a non-default `TimeZone` to prove the parameter is honored.

### Reconciler and delivery use cases

- [X] T014 Using the repository interface from T009, create
  `core/domain/src/main/java/.../core/domain/usecase/notification/SyncReleaseNotificationsUseCase.kt`:
  ```kotlin
  class SyncReleaseNotificationsUseCase @Inject constructor(
      private val repository: GameRepository,
      private val scheduler: ReleaseNotificationScheduler
  ) {
      suspend operator fun invoke(gameId: Int? = null) { /* see below */ }
  }
  ```
  Body: read `optedInIds = repository.getReleaseNotificationGameIds().first()`, narrowed to `{gameId}`
  when non-null; read `savedGames = repository.getSavedGames().first()` and
  `ownedPlatformIds = repository.getOwnedPlatformIds().first()`. For each id in scope:
  - if no game in `savedGames` has that id → `repository.setReleaseNotificationEnabled(id, false)` and
    `scheduler.cancel(id)` (prunes an opt-in that left the saved set — FR-005's observable half);
  - else resolve `earliestDate = game.resolveReleaseDates(ownedPlatformIds).minByOrNull { it.date ?: Long.MAX_VALUE }`;
    compute `instant = resolveNotificationInstant(earliestDate?.date, earliestDate?.precision ?: DatePrecision.TBD, Clock.System.now())`;
    read `notifiedForDate = repository.getReleaseNotificationDeliveredDate(id)`;
    if `instant == null` or (`earliestDate?.date == notifiedForDate`) → `scheduler.cancel(id)`;
    else → `scheduler.schedule(id, instant)`.
  This is the eligibility predicate from [data-model.md](./data-model.md) applied per game; every enqueue
  is idempotent (`REPLACE`), so this function is safe to call repeatedly. Uses `ReleaseDateResolver` from
  T011.
- [X] T015 [P] Alongside T014, create
  `core/domain/src/test/java/.../core/domain/usecase/notification/SyncReleaseNotificationsUseCaseTest.kt`
  (MockK on `GameRepository` and `ReleaseNotificationScheduler`) covering: schedules an eligible
  future-exact-date game; cancels + does not error on a `YEAR_MONTH`/`QUARTER`/`YEAR_ONLY`/`TBD` game;
  cancels on a past date; re-arms (schedules again) when the resolved date differs from
  `notifiedForDate`; does *not* reschedule when the resolved date equals `notifiedForDate`; prunes
  (disables + cancels) an opted-in game absent from `getSavedGames()`; picks the earliest of several
  owned-platform dates for one game; narrows to one game when `gameId` is passed.
- [X] T016 Using the repository interface from T009, create
  `core/domain/src/main/java/.../core/domain/usecase/notification/DeliverReleaseNotificationUseCase.kt`:
  ```kotlin
  class DeliverReleaseNotificationUseCase @Inject constructor(
      private val repository: GameRepository,
      private val notifier: ReleaseNotifier
  ) {
      suspend operator fun invoke(gameId: Int): Boolean {
          val game = repository.getSavedGames().first().find { it.id == gameId } ?: return false
          val optedIn = repository.getReleaseNotificationGameIds().first().contains(gameId)
          if (!optedIn) return false
          val ownedPlatformIds = repository.getOwnedPlatformIds().first()
          val earliestDate = game.resolveReleaseDates(ownedPlatformIds).minByOrNull { it.date ?: Long.MAX_VALUE }
              ?: return false
          if (earliestDate.precision != DatePrecision.EXACT_DATE) return false
          val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
          val releaseDay = Instant.fromEpochSeconds(earliestDate.date ?: return false)
              .toLocalDateTime(TimeZone.currentSystemDefault()).date
          if (releaseDay > today) return false
          if (repository.getReleaseNotificationDeliveredDate(gameId) == earliestDate.date) return false
          notifier.notifyReleased(gameId, game.name)
          repository.markReleaseNotificationDelivered(gameId, earliestDate.date)
          return true
      }
  }
  ```
  Re-applies the full eligibility check at fire time (this is what makes lazy reconciliation safe — see
  [research.md](./research.md) §5). Uses `ReleaseDateResolver` from T011.
- [X] T017 [P] Alongside T016, create
  `core/domain/src/test/java/.../core/domain/usecase/notification/DeliverReleaseNotificationUseCaseTest.kt`
  covering: posts and marks delivered for an eligible game; returns `false` and does not post when the
  game is not saved, not opted in, the date is imprecise, the release day is in the future, or
  `notifiedForDate` already equals the resolved date.
- [X] T018 [P] Using the repository interface from T009 and `SyncReleaseNotificationsUseCase` from T014,
  create
  `core/domain/src/main/java/.../core/domain/usecase/notification/SetReleaseNotificationEnabledUseCase.kt`:
  ```kotlin
  class SetReleaseNotificationEnabledUseCase @Inject constructor(
      private val repository: GameRepository,
      private val syncReleaseNotificationsUseCase: SyncReleaseNotificationsUseCase
  ) {
      suspend operator fun invoke(gameId: Int, enabled: Boolean) {
          repository.setReleaseNotificationEnabled(gameId, enabled)
          syncReleaseNotificationsUseCase(gameId)
      }
  }
  ```
- [X] T019 [P] Alongside T018, create
  `core/domain/src/test/java/.../core/domain/usecase/notification/SetReleaseNotificationEnabledUseCaseTest.kt`
  covering: `enabled = true` writes the opt-in then reconciles that game id; `enabled = false` clears the
  opt-in then reconciles (cancelling) that game id.
- [X] T020 [P] Using the repository interface from T009, create
  `core/domain/src/main/java/.../core/domain/usecase/notification/GetReleaseNotificationGameIdsUseCase.kt`:
  `operator fun invoke(): Flow<Set<Int>> = repository.getReleaseNotificationGameIds()`.
- [X] T021 [P] Using the repository interface from T009, create
  `core/domain/src/main/java/.../core/domain/usecase/notification/GetGamesWithReleaseNotificationsUseCase.kt`:
  `operator fun invoke(): Flow<List<Game>> = repository.getGamesWithReleaseNotifications()`.

### Android implementations of the two ports

- [X] T022 [P] Implementing the port from T007, create
  `core/data/src/main/java/.../core/data/scheduler/ReleaseNotificationSchedulerImpl.kt`:
  `schedule(gameId, at)` computes
  `initialDelay = (at - Clock.System.now()).coerceAtLeast(Duration.ZERO)` and enqueues a
  `OneTimeWorkRequestBuilder<ReleaseNotificationWorker>().setInitialDelay(...).setInputData(workDataOf("gameId" to gameId))`
  as `enqueueUniqueWork("release_notification_$gameId", ExistingWorkPolicy.REPLACE, request)`. No
  network constraint (fire-time content comes from Room, per the Constraints in plan.md). `cancel(gameId)`
  calls `WorkManager.getInstance(context).cancelUniqueWork("release_notification_$gameId")`. Follow the
  `// KEEP:`-comment style of `ReleaseRefreshSchedulerImpl.kt` to document *why* the policy is `REPLACE`
  here (idempotent rescheduling) versus `KEEP` there (collapsing redundant enqueues).
- [X] T023 [P] Create `core/data/src/main/res/values/strings.xml` with the notification channel
  name/description and the reminder title/body (e.g. `release_notification_channel_name`,
  `release_notification_channel_description`, `release_notification_title_format` taking the game name,
  `release_notification_body`). New file — this module has no `res/` yet; `:core:database` owning one for
  the default wishlist's strings is the precedent (see [research.md](./research.md) §8).
- [X] T024 Implementing the port from T008, using the strings from T023, create
  `core/data/src/main/java/.../core/data/notification/ReleaseNotifierImpl.kt`:
  - `canDeliver()` → `NotificationManagerCompat.from(context).areNotificationsEnabled()`.
  - `notifyReleased(gameId, gameName)` → lazily calls
    `NotificationManagerCompat.createNotificationChannel` (or the channel constant is created once and
    reused) with the T023 channel strings, builds a `PendingIntent` around
    `Intent(Intent.ACTION_VIEW, Uri.parse("questlog://game/$gameId"))` with
    `FLAG_IMMUTABLE`, and posts a `NotificationCompat.Builder` using the T023 title/body strings and
    `gameId` as the notification id (so a second reminder for the same game replaces rather than stacks).
- [X] T025 [P] Bind both new ports in
  `core/data/src/main/java/.../core/data/di/DataModule.kt`:
  `@Binds ReleaseNotificationScheduler`, `@Binds ReleaseNotifier` (following the existing
  `@Binds @Singleton` pattern used for `GameRepository`/`ReleaseRefreshScheduler`).
- [X] T026 With `DeliverReleaseNotificationUseCase` (T016) and `ReleaseNotifierImpl` (T024) in place,
  create `core/data/src/main/java/.../core/data/worker/ReleaseNotificationWorker.kt`:
  ```kotlin
  @HiltWorker
  class ReleaseNotificationWorker @AssistedInject constructor(
      @Assisted context: Context,
      @Assisted params: WorkerParameters,
      private val deliverReleaseNotificationUseCase: DeliverReleaseNotificationUseCase
  ) : CoroutineWorker(context, params) {
      override suspend fun doWork(): Result {
          val gameId = inputData.getInt("gameId", -1)
          if (gameId == -1) return Result.failure()
          deliverReleaseNotificationUseCase(gameId)
          return Result.success()
      }
  }
  ```
  Always `Result.success()` after a valid call — per
  [contracts/domain-ports.md](./contracts/domain-ports.md), a stale schedule declining to post is correct
  behavior, not a failure to retry.
- [X] T027 Using `SyncReleaseNotificationsUseCase` from T014, edit
  `core/data/src/main/java/.../core/data/worker/ReleaseDatesRefreshWorker.kt`: after
  `refreshReleaseDatesUseCase()` returns `AppResult.Success`, call `syncReleaseNotificationsUseCase()`
  (no `gameId` — reconcile every opt-in) before returning `Result.success()`. Inject
  `SyncReleaseNotificationsUseCase` alongside the existing `RefreshReleaseDatesUseCase`. This is the call
  site that satisfies FR-007 (rescheduling on a refreshed date) — it must run here because the periodic
  refresh normally executes with the app's process dead.
- [X] T028 [P] After T027, update
  `core/data/src/test/java/.../core/data/worker/ReleaseDatesRefreshWorkerTest.kt` (create it if it does
  not already exist — check first) to verify `SyncReleaseNotificationsUseCase` is invoked exactly once
  after a successful refresh and not invoked after a failed one.

### Shared UI plumbing (needed by all three feature-module stories)

- [X] T029 [P] After T001, create
  `core/ui/src/main/java/.../core/ui/util/NotificationPermission.kt`:
  ```kotlin
  @Immutable
  data class NotificationPermissionState(
      val canDeliver: Boolean,
      val isPermanentlyDenied: Boolean,
      val request: () -> Unit
  )

  @Composable
  fun rememberNotificationPermissionState(): NotificationPermissionState
  ```
  `canDeliver` reads `NotificationManagerCompat.from(context).areNotificationsEnabled()`, re-evaluated on
  `Lifecycle.Event.ON_RESUME` via a `DisposableEffect` + `LocalLifecycleOwner`. On API 33+, `request()`
  uses `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for
  `Manifest.permission.POST_NOTIFICATIONS`; `isPermanentlyDenied` is true once
  `shouldShowRequestPermissionRationale` returns `false` after a prior denial. Below API 33, `request()`
  launches `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)` directly (per
  [contracts/ui-contracts.md](./contracts/ui-contracts.md)).
- [X] T030 [P] After T001, create
  `core/ui/src/main/java/.../core/ui/component/NotificationPermissionDeniedDialog.kt`: a
  `CustomAlertDialog` (never Material's `AlertDialog`, per `core/ui/CLAUDE.md`) explaining reminders will
  not arrive until notifications are enabled, with a confirm action that launches
  `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. Give it a `@Preview` wrapped in `QuestLogTheme { }`, per
  this module's convention.
- [X] T031 [P] Add to `core/ui`'s `strings.xml`: the denied-dialog title/body/action text and the shared
  bell content descriptions ("Turn on release reminder" / "Turn off release reminder").

**Checkpoint**: `./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest --console=plain -q`
green; `./gradlew :core:ui:compileDebugKotlin --console=plain -q` green. No feature module or `:app` has
been touched yet — user story work can now begin.

---

## Phase 3: User Story 1 - Opt in to a release reminder (Priority: P1) 🎯 MVP

**Goal**: A saved game can be opted into a release reminder from Radar or its detail screen, the two
surfaces agree on the state, and the reminder actually fires and opens the game on tap.

**Independent Test**: Save a game with a future exact release date; toggle "Notify me" on from Radar;
confirm the detail screen shows the same "on" state; force the scheduled work to run
(`adb shell cmd jobscheduler run -f ...`, per [quickstart.md](./quickstart.md) Scenario 2) and confirm a
notification arrives naming the game and opens its detail screen on tap.

### Radar surface

- [X] T032 [P] [US1] Create `feature/radar/src/main/java/.../feature/radar/model/RadarUiEvent.kt`:
  `internal sealed interface RadarUiEvent { data class ToggleReleaseNotification(val gameId: Int) : RadarUiEvent }`.
- [X] T033 [P] [US1] Create `feature/radar/src/main/java/.../feature/radar/model/RadarUiEffect.kt`:
  `internal sealed interface RadarUiEffect { data object RequestNotificationPermission : RadarUiEffect }`.
- [X] T034 [US1] Add `val isNotificationEnabled: Boolean = false` to
  `feature/radar/src/main/java/.../feature/radar/model/RadarEntryUiModel.kt` (and to its `getDummy()`
  companion factory).
- [X] T035 [US1] Building on T034's new field, edit
  `feature/radar/src/main/java/.../feature/radar/mapper/RadarUiMapper.kt`: thread a
  `notificationEnabledGameIds: Set<Int>` parameter through `List<RadarTimelineSection>.toUiModel(...)`,
  `RadarTimelineSection.toUiModel(...)` and `RadarEntry.toUiModel(...)`, setting
  `isNotificationEnabled = game.id in notificationEnabledGameIds` on each entry.
- [X] T036 [US1] Using the event/effect types from T032/T033 and the mapper change from T035, edit
  `feature/radar/src/main/java/.../feature/radar/RadarViewModel.kt`: inject
  `GetReleaseNotificationGameIdsUseCase` and `SetReleaseNotificationEnabledUseCase`; fold the ids `Flow`
  into the existing `combine` alongside `getRadarTimelineUseCase()`'s sections, passing the set into the
  T035 mapper call; add `_uiEffect = Channel<RadarUiEffect>(Channel.BUFFERED)` +
  `internal val uiEffect = _uiEffect.receiveAsFlow()`; add
  `internal fun onEvent(event: RadarUiEvent)` handling `ToggleReleaseNotification` by launching
  `viewModelScope.launch { val wasEnabled = <current known state for gameId>; setReleaseNotificationEnabledUseCase(gameId, !wasEnabled); if (!wasEnabled) _uiEffect.send(RadarUiEffect.RequestNotificationPermission) }`.
- [X] T037 [US1] Using the field from T034, edit
  `feature/radar/src/main/java/.../feature/radar/components/RadarGameRow.kt`: add an
  `onToggleNotification: () -> Unit` parameter and, in the trailing content after the existing
  `PlatformTile`, a bell `IconButton` (filled `Icons.Filled.Notifications` when
  `entry.isNotificationEnabled`, outlined `Icons.Outlined.NotificationsNone` otherwise) with a
  `contentDescription` from the T031 shared strings, sized for a 48dp touch target. Omit the bell entirely
  when the row's bucket is `RECENTLY_RELEASED` (per
  [contracts/ui-contracts.md](./contracts/ui-contracts.md) — pass a `showNotificationToggle: Boolean`
  computed by the caller from the section's `bucket`). Update this file's three existing `@Preview`s to
  pass a no-op lambda.
- [X] T038 [US1] With T036 and T037 done, edit
  `feature/radar/src/main/java/.../feature/radar/RadarScreen.kt`: call
  `rememberNotificationPermissionState()` (T029); pass `onToggleNotification` from `RadarGameRow` through
  to `viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(entry.id))`; collect `uiEffect` in the
  existing `LaunchedEffect { lifecycle.repeatOnLifecycle(STARTED) { ... } }` shape, calling
  `permissionState.request()` on `RequestNotificationPermission` when `!permissionState.canDeliver`; show
  `NotificationPermissionDeniedDialog` (T030) when the request comes back still unable to deliver.
- [X] T039 [P] [US1] Alongside T036, add to
  `feature/radar/src/test/java/.../feature/radar/RadarViewModelTest.kt`: `isNotificationEnabled` reaches
  the right `RadarEntryUiModel`s (including *both* rows of a multi-platform game sharing one game id);
  `ToggleReleaseNotification` calls `SetReleaseNotificationEnabledUseCase` with the toggled value; an
  enabling toggle emits `RadarUiEffect.RequestNotificationPermission`, a disabling one does not. Mock the
  use cases, not the repository, per `feature/CLAUDE.md`.

### Game-detail surface

- [X] T040 [P] [US1] Add `data object ToggleReleaseNotification : GameDetailUiEvent` to
  `feature/game-detail/src/main/java/.../feature/gamedetail/model/GameDetailUiEvent.kt`.
- [X] T041 [P] [US1] Add `data object RequestNotificationPermission : GameDetailUiEffect` to
  `feature/game-detail/src/main/java/.../feature/gamedetail/model/GameDetailUiEffect.kt`.
- [X] T042 [US1] Add `val isNotificationEnabled: Boolean = false` and
  `val isNotificationAvailable: Boolean = false` to top level (not nested in `AvailabilityUiModel`) of
  `feature/game-detail/src/main/java/.../feature/gamedetail/model/GameDetailUiModel.kt` (and its
  `getDummy()`).
- [X] T043 [US1] Using the fields from T042, edit
  `feature/game-detail/src/main/java/.../feature/gamedetail/mapper/GameDetailUiMapper.kt`: thread an
  `isNotificationEnabled: Boolean` parameter into the `Game.toUiModel(...)` call chain, and compute
  `isNotificationAvailable` from the same "is this game saved" signal the mapper already has available
  (mirroring `GameDao.getSavedGames`'s predicate: in a list, or has a non-null `status`/`priority`) AND
  the game's earliest resolved release date not already being in the past.
- [X] T044 [US1] Using T040, T041 and T043, edit
  `feature/game-detail/src/main/java/.../feature/gamedetail/GameDetailViewModel.kt`: inject
  `GetReleaseNotificationGameIdsUseCase` and `SetReleaseNotificationEnabledUseCase`; fold the opted-in-id
  membership check for this screen's single `gameId` into the existing state pipeline; handle
  `ToggleReleaseNotification` in `onEvent` by calling `SetReleaseNotificationEnabledUseCase` and sending
  `GameDetailUiEffect.RequestNotificationPermission` through the existing `_uiEffect` channel when turning
  on, following the same shape as `RadarViewModel` (T036).
- [X] T045 [US1] Using the fields from T042, edit
  `feature/game-detail/src/main/java/.../feature/gamedetail/components/GameDetailSuccessContent.kt`: pass
  an `actions = { alpha -> ... }` lambda to the existing `ImmersiveDetailLayout(...)` call, rendering the
  bell `IconButton` only `if (game.isNotificationAvailable)`, calling
  `onEvent(GameDetailUiEvent.ToggleReleaseNotification)`, with the icon/tint driven by
  `game.isNotificationEnabled` and `alpha` exactly as `navigationIcon`'s existing
  `topBarAlpha`-driven white→onSurface transition does (per
  [contracts/ui-contracts.md](./contracts/ui-contracts.md)'s Game detail section for the precise
  snippet).
- [X] T046 [US1] With T044 done, edit
  `feature/game-detail/src/main/java/.../feature/gamedetail/GameDetailScreen.kt`: call
  `rememberNotificationPermissionState()`; handle `GameDetailUiEffect.RequestNotificationPermission` in
  the existing effect-collecting `LaunchedEffect`, requesting permission and showing
  `NotificationPermissionDeniedDialog` on denial — mirroring T038.
- [X] T047 [P] [US1] Alongside T044, add to
  `feature/game-detail/src/test/java/.../feature/gamedetail/GameDetailViewModelTest.kt`: toggling calls
  `SetReleaseNotificationEnabledUseCase` with the flipped value; the effect is emitted only when turning
  on; `isNotificationEnabled`/`isNotificationAvailable` surface correctly in `uiState`.

### Deep link and delivery wiring

- [X] T048 [US1] After T002's manifest change, edit `app/src/main/java/.../MainActivity.kt`: read
  `intent?.data` in `onCreate` (and add an `override fun onNewIntent(intent: Intent)` calling
  `setIntent(intent)` then the same handling) — when the URI's scheme is `questlog` and host is `game`,
  parse the last path segment (or the appropriate URI part per the `questlog://game/<gameId>` shape) as
  `gameId` and push `GameDetailRoute(gameId)` onto `backStack` above the `SearchRoute` root, guarded by
  `if (backStack.lastOrNull() != nextRoute)` as everywhere else in this file.
- [ ] T049 [US1] With Phase 2's data/domain/data-layer tasks (T005, T007–T010, T022, T024–T026) and this
  phase's T032–T048 all done, build and manually run Scenario 1 and Scenario 2 from
  [quickstart.md](./quickstart.md) end to end on a device/emulator: opt in from Radar, confirm parity on
  detail, force-run the scheduled work, confirm the notification arrives and tapping it opens the right
  game (cold start via `onCreate` and warm via `onNewIntent`).

**Checkpoint**: User Story 1 is fully functional and independently testable. This is the MVP slice.

---

## Phase 4: User Story 2 - Notification follows a shifted release date (Priority: P1)

**Goal**: When Radar's periodic refresh moves an opted-in game's date, the reminder silently follows —
never firing on the stale date, never going silent on the new one.

**Independent Test**: Opt in to a game with date D1 (Scenario 3 in
[quickstart.md](./quickstart.md)); update its stored release date to D2 by editing the row directly, then
trigger the refresh worker; confirm `dumpsys jobscheduler` shows the reminder now scheduled for D2 and
nothing fires at D1. Separately, move a date into the past and confirm no reminder remains scheduled.

**Note on dependencies**: Every task this story needs (`SyncReleaseNotificationsUseCase`, the
`ReleaseDatesRefreshWorker` edit, the scheduler's `REPLACE` policy) was built in Phase 2 as shared
infrastructure — T014, T015, T022, T027 and T028 already deliver and test this story's core logic. This
phase is therefore verification plus the one piece of user-facing surface the story specifically needs:
proof, not new production code.

- [X] T050 [US2] In `core/data/src/test/java/.../core/data/worker/ReleaseDatesRefreshWorkerTest.kt` (from
  T028), add a case if not already covered: given `RefreshReleaseDatesUseCase` succeeds,
  `SyncReleaseNotificationsUseCase` is invoked with no `gameId` argument (i.e. reconciles every opt-in,
  not just one).
- [X] T051 [US2] In `SyncReleaseNotificationsUseCaseTest` (T015), add a case if not already covered that
  explicitly asserts the **re-arm** behavior end to end: an opt-in already `notifiedForDate`-stamped for
  D1, whose game now resolves to a different future date D2, is rescheduled for D2 — not left cancelled.
- [ ] T052 [US2] With the full Phase 2 data/domain layer and T027 in place, manually run Scenario 3 from
  [quickstart.md](./quickstart.md) on a device/emulator: verify the scheduled delay changes after a
  simulated date shift and that moving a date into the past clears the schedule entirely (FR-008).

**Checkpoint**: User Stories 1 and 2 both work independently. Rescheduling is proven, not just implied by
shared infrastructure.

---

## Phase 5: User Story 3 - Manage notifications from Settings (Priority: P2)

**Goal**: A "Notifications" group in Settings shows how many games have reminders on, links to a
management list where each can be turned off, and surfaces the system permission state with a way to fix
it.

**Independent Test**: Opt in to two or three games (via US1's toggles); open Settings; confirm the group's
count and that the management screen lists all of them with a working per-row toggle; separately, revoke
the notification permission and confirm a permission row appears with a path to system settings, and that
it disappears once permission is restored.

### Route and navigation

- [X] T053 [US3] Add `@Serializable data object ReleaseNotificationsRoute : GameNavKey` to
  `core/navigation/src/main/java/.../core/navigation/Routes.kt`.
- [X] T054 [US3] Using the route from T053, add a branch for `ReleaseNotificationsRoute` in the
  `entryProvider` in `app/src/main/java/.../QuestLogNavDisplay.kt`, using
  `hiltViewModel<ReleaseNotificationsViewModel>()` (no arguments — standard Hilt, not assisted) and
  rendering `ReleaseNotificationsScreen` with `onBackClick = { backStack.removeLastOrNull() }`, following
  the `OwnedPlatformsRoute` branch's shape.

### Management sub-screen

- [X] T055 [P] [US3] Create
  `feature/settings/src/main/java/.../feature/settings/model/ReleaseNotificationUiModel.kt`:
  `@Immutable internal data class ReleaseNotificationUiModel(val gameId: Int, val coverImage: String?, val title: String, val dateLabel: UiText)`
  — `dateLabel` is the formatted exact date, or a "no date yet" `UiText` for an opt-in whose game still
  resolves to a coarser precision (making FR-013 visible, per
  [contracts/ui-contracts.md](./contracts/ui-contracts.md)).
- [X] T056 [P] [US3] Create
  `feature/settings/src/main/java/.../feature/settings/model/ReleaseNotificationsContentState.kt`:
  `@Immutable internal sealed interface ReleaseNotificationsContentState { data object Loading; data object Empty; data class Success(val games: List<ReleaseNotificationUiModel>) }`.
- [X] T057 [P] [US3] Create
  `feature/settings/src/main/java/.../feature/settings/model/ReleaseNotificationsUiState.kt`:
  `@Immutable internal data class ReleaseNotificationsUiState(val contentState: ReleaseNotificationsContentState = ReleaseNotificationsContentState.Loading)`.
- [X] T058 [US3] Using the model from T055, create
  `feature/settings/src/main/java/.../feature/settings/mapper/ReleaseNotificationsUiMapper.kt`: an
  `internal fun List<Game>.toReleaseNotificationUiModels(ownedPlatformIds: Set<Int>): List<ReleaseNotificationUiModel>`
  reusing `resolveReleaseDates`/date-formatting the same way `RadarUiMapper` does, but with only two
  outcomes for `dateLabel`: the formatted exact date, or the "no date yet" string.
- [X] T059 [US3] Using T056, T057 and T058, create
  `feature/settings/src/main/java/.../feature/settings/ReleaseNotificationsViewModel.kt`
  (`@HiltViewModel`, no assisted factory): injects `GetGamesWithReleaseNotificationsUseCase`,
  `GetSelectedPlatformIdsUseCase`, `SetReleaseNotificationEnabledUseCase`; `.stateIn(viewModelScope,
  WhileSubscribed(5000), ...)` over the `combine` of the two use-case flows mapped through T058, empty
  list → `ReleaseNotificationsContentState.Empty`; exposes `internal fun onEvent` handling a
  `ToggleOff(gameId: Int)` event that calls `SetReleaseNotificationEnabledUseCase(gameId, false)`.
- [X] T060 [P] [US3] Create
  `feature/settings/src/main/java/.../feature/settings/model/ReleaseNotificationsUiEvent.kt`:
  `internal sealed interface ReleaseNotificationsUiEvent { data class ToggleOff(val gameId: Int) : ReleaseNotificationsUiEvent }`.
- [X] T061 [US3] Using T059 and T060, create
  `feature/settings/src/main/java/.../feature/settings/ReleaseNotificationsScreen.kt`: public
  `ReleaseNotificationsScreen(viewModel, onBackClick, modifier)` + `internal` stateless
  `ReleaseNotificationsContent`, following `OwnedPlatformsScreen.kt`'s split. Renders `Loading` via
  `LoadingPage`, `Empty` via `EmptyPage`, `Success` as a scrollable list of `GameListRow`s (cover, title,
  `dateLabel` as `trailingContent`) each with a trailing toggle wired to
  `onEvent(ReleaseNotificationsUiEvent.ToggleOff(gameId))`. Include `@Preview`s for all three states.
- [X] T062 [P] [US3] Alongside T059, create
  `feature/settings/src/test/java/.../feature/settings/ReleaseNotificationsViewModelTest.kt`: list renders
  from the combined use-case flows; a game with an imprecise date shows the "no date yet" label; toggling
  a row off calls `SetReleaseNotificationEnabledUseCase(gameId, false)`; an empty opt-in set produces
  `Empty`.

### Settings hub group

- [X] T063 [P] [US3] Create
  `feature/settings/src/main/java/.../feature/settings/model/NotificationPermissionRowState.kt`:
  `internal sealed interface NotificationPermissionRowState { data object Granted; data object Blocked }`
  — mirrors the existing `TranslationModelRowState` shape in this module.
- [X] T064 [US3] Using T063, add to
  `feature/settings/src/main/java/.../feature/settings/model/SettingsUiState.kt`:
  `val releaseNotificationCount: Int = 0` and
  `val notificationPermission: NotificationPermissionRowState = NotificationPermissionRowState.Granted`.
- [X] T065 [P] [US3] Add to
  `feature/settings/src/main/java/.../feature/settings/model/SettingsUiEvent.kt`:
  `data class NotificationPermissionChanged(val canDeliver: Boolean, val isPermanentlyDenied: Boolean) : SettingsUiEvent`.
- [X] T066 [US3] Using T064 and T065, edit
  `feature/settings/src/main/java/.../feature/settings/SettingsViewModel.kt`: inject
  `GetReleaseNotificationGameIdsUseCase`; fold its `.map { it.size }` into `_uiState.update { ... }`
  (locally-driven pipeline shape, per `feature/CLAUDE.md`, since this screen already uses
  `MutableStateFlow` + `update`); handle `NotificationPermissionChanged` in `onEvent` by updating
  `notificationPermission` to `Blocked` when `!canDeliver`, `Granted` otherwise.
- [X] T067 [US3] With T066 done, edit
  `feature/settings/src/main/java/.../feature/settings/SettingsScreen.kt`: call
  `rememberNotificationPermissionState()` (T029); dispatch `SettingsUiEvent.NotificationPermissionChanged`
  from it (e.g. in a `LaunchedEffect(permissionState.canDeliver, permissionState.isPermanentlyDenied)`);
  add a third `SettingsGroup(title = stringResource(R.string.settings_group_notifications))` between the
  existing two groups, containing: a `SettingsRow` navigating to `onReleaseNotificationsClick` with
  subtitle `"$releaseNotificationCount games"` (or a "None yet" string when zero) — mirroring
  `settings_owned_platforms`'s row — and, **only when** `state.notificationPermission is Blocked`, a
  second `SettingsRow` stating reminders won't arrive, `onClick` launching
  `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. Add `onReleaseNotificationsClick: () -> Unit` as a new
  parameter on `SettingsScreen`/`SettingsContent`, wired from `QuestLogNavDisplay.kt` (T054's call site)
  the same way `onOwnedPlatformsClick` already is.
- [X] T068 [P] [US3] Alongside T066, add to
  `feature/settings/src/test/java/.../feature/settings/SettingsViewModelTest.kt`: the opted-in count
  reaches `uiState.releaseNotificationCount`; `NotificationPermissionChanged(canDeliver = false, ...)`
  sets `Blocked`; `canDeliver = true` sets `Granted`.
- [X] T069 [P] [US3] Add to `feature/settings`'s `strings.xml`: the new group title, the management row's
  title/subtitle-count/"none yet" strings, the permission-blocked row's text, and the sub-screen's title
  and empty-state string.

**Checkpoint**: All three user stories are independently functional. Full feature ready for
[quickstart.md](./quickstart.md) Scenario 4 and Scenario 5.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Whole-feature verification that spans stories, plus the edge cases spec.md calls out that no
single story task above fully exercises end to end.

- [ ] T070 [P] Run all of [quickstart.md](./quickstart.md)'s Scenario 5 edge-case table by hand: opt in to
  a `TBD`/`QUARTER` game (no schedule, "no date yet" in Settings), let it resolve to an exact date via a
  real or simulated refresh (a reminder appears with no further user action), unsave an opted-in game (it
  leaves the Settings list at once and nothing is ever delivered for it), deny permission at first opt-in
  (denial dialog + blocked row), and confirm no permission dialog appears on an API 29–32 device while
  reminders still deliver there.
- [X] T071 Run `./gradlew :app:assembleDebug` (full build — this feature spans modules and touches DI
  wiring, per root `CLAUDE.md`'s guidance on when a single-module `compileDebugKotlin` is not enough).
- [X] T072 Run `./gradlew test` and confirm every existing suite plus every test added in this feature
  (T012, T013, T015, T017, T019, T028, T039, T047, T050, T051, T062, T068) is green.
- [X] T073 Re-read `core/domain/src/main/java/.../core/domain/radar/GetRadarTimelineUseCase.kt` and
  `core/domain/src/main/java/.../core/domain/radar/ReleaseDateResolver.kt` side by side to confirm the
  timeline and the reminder still resolve dates through the one shared function — the structural guarantee
  behind FR-006 — and that nothing reintroduced a second copy while implementing US1–US3.
- [X] T074 Update `docs/roadmap.md`: delete the "Phase 3 — Release notifications" section now that it has
  shipped (per the root `CLAUDE.md`'s rule that the roadmap describes only what is *not* built yet), and
  remove the "Notifications belong to Phase 3" clause from the "Settings holds only what has a backend"
  section since it is no longer a deferred item.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. Blocks every user story. Internally: T003→T004→T005→T006
  (data layer, strictly sequential on the same files); T007/T008 (ports) are independent of the data
  layer and of each other; T009 is an interface edit that compiles standalone, but T010 (its
  implementation) needs both T004 (the DAO) and T009 (the interface) to exist first; T011→T012/T013;
  T014 needs T009 (interface) and T011 (resolver) — T010 (the repository's actual implementation) is
  needed to *run* T014 but not to compile a test against the mocked interface, so T015 can proceed as soon
  as T014 exists; T022/T024 depend on T007/T008 respectively (and T024 also needs T023's strings); T026
  depends on T016 and T024; T027 depends on T014; T029–T031 depend only on T001 and are independent of the
  data/domain work.
- **User Stories (Phase 3+)**: all depend on Phase 2 being complete. US1 and US2 are both P1; US1 is the
  larger, user-facing slice and is listed first, but US2's production code was already delivered as
  shared infrastructure in Phase 2 (T014, T022, T027) — US2's phase is verification-weighted by design,
  not because it is less important. US3 (P2) depends only on Phase 2, not on US1 or US2, though it is
  more useful once US1 has produced some opt-ins to manage.
- **Polish (Phase 6)**: depends on all three user stories.

### Parallel opportunities

- Phase 1: T001 and T002 (different files).
- Phase 2: T003 must land before T004; T007 and T008 can run in parallel (different files, no shared
  dependency); T009 can be written any time (pure interface edit); once T009 exists, T018/T020/T021 can be
  written in parallel (three different new files, all depending only on the repository interface);
  T015/T017/T019 (tests) can be written in parallel with each other and with T028; T029/T030/T031 in
  parallel with the entire data/domain track once T001 lands.
- Phase 3 (US1): the Radar sub-track (T032–T039) and the game-detail sub-track (T040–T047) touch entirely
  different modules and can proceed in parallel; T048 (deep link) is independent of both until T049's
  end-to-end check needs everything.
- Phase 5 (US3): T055/T056/T057/T063/T065 (new model files) in parallel; T053 independent of the model
  files.

---

## Implementation Strategy

### MVP first

1. Phase 1 (Setup) → Phase 2 (Foundational) — no user-visible behavior yet, but this is most of the
   feature's actual complexity (persistence, eligibility, scheduling, permission plumbing).
2. Phase 3 (US1) → stop and validate with quickstart.md Scenarios 1 and 2. This alone is a demoable,
   shippable slice: opt in, see it stick, get notified.

### Incremental delivery

3. Phase 4 (US2) → validate with Scenario 3. Mostly proving what Phase 2 already built.
4. Phase 5 (US3) → validate with Scenario 4. Adds manageability and permission recovery on top of a
   feature that already works without it.
5. Phase 6 (Polish) → Scenario 5's edge-case sweep, full build, full test run, roadmap cleanup.

### Why Foundational is unusually large here

Unlike a typical CRUD feature, this one's hard part — the eligibility predicate, the idempotent
reschedule-on-`REPLACE` design, and the fire-time re-verification that makes lazy reconciliation safe — is
infrastructure shared by all three user stories, not story-specific logic. Front-loading it in Phase 2
means US1, US2 and US3 in Phases 3–5 are almost entirely UI wiring around already-tested domain logic,
which is also why US2 (Phase 4) is so thin: its behavior was the point of `SyncReleaseNotificationsUseCase`
from the start.
