# Verification

Baseline `b982bf11`, macOS 26.6.2, Xcode 27.0, Gradle 9.7.1. Logs are in the repo's git-ignored `build/audit/`.
Gradle reuses a task's result only when its inputs are unchanged, so an `UP-TO-DATE` task counts as evidence for the
current sources. Where a result was reused I say so, and where it was cheap I forced a real execution.

| # | Command | Status | Notes |
|---|---|---|---|
| T017 | `./gradlew :androidApp:assembleDebug` | **passed** | `BUILD SUCCESSFUL`, 374 tasks (17 executed, 357 up-to-date). Compile and packaging tasks were up to date: the APK was already built from identical inputs |
| T018 | `./gradlew test` | **passed** | `BUILD SUCCESSFUL`, 364 tasks (28 executed). 534 tests across 14 multiplatform modules, 0 failures, 0 errors, 0 skipped, plus the single template test in `:androidApp`. Details below |
| T019 | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | **passed** (reused) | `BUILD SUCCESSFUL`. `compileKotlinIosSimulatorArm64` and the framework link were `UP-TO-DATE` for every module: the framework had already been compiled and linked from identical inputs. Not force-rebuilt (a full Kotlin/Native rebuild); the identical inputs are the evidence |
| T020 | `./gradlew :core:network:iosSimulatorArm64Test` | **passed** | Executed with `--rerun --no-build-cache`: `PlatformTransportFailureTest`, 5 tests, 0 failures, on the simulator |
| T021 | `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build` | **passed** | `** BUILD SUCCEEDED **`, 0 `warning:` lines. The "Compile Kotlin Framework" script phase invoked Gradle (`embedAndSignAppleFrameworkForXcode` skipped, `linkDebugFramework…` up to date) |

## Extra check beyond the plan: minified release build

| Command | Status | Notes |
|---|---|---|
| `./gradlew :androidApp:assembleRelease --console=plain` | **passed** (compiled, not run) | `BUILD SUCCESSFUL in 1m 24s`, 568 tasks (37 executed). `minifyReleaseWithR8` executed (not cached), `androidApp-release.apk` 11.1 MB, `mapping.txt` produced, no R8 missing-class or warning lines. `androidApp/proguard-rules.pro` has no keep rules beyond source-file attributes, so the minified app depends entirely on the libraries' own consumer rules (Ktor, kotlinx.serialization, Room, Koin, Coil, Compose resources). **Whether the minified app runs was not checked**: a device and an emulator are attached, but installing would replace the debug app that is already on them, so it was left for the owner (see the report) |

Why it was added: the migration replaced Hilt/Retrofit/Moshi with Koin/Ktor/kotlinx.serialization and the release
variant has `isMinifyEnabled = true`; none of the five planned commands builds that variant.

## T018 detail: tests by module

How each module's result was produced: **fresh** = executed in this audit's run, **forced** = result directory
removed and re-executed with `--no-build-cache`, so Gradle had to run it again.

| Module | Tests | Failures | Origin |
|---|---|---|---|
| `:core:common` | 10 | 0 | forced |
| `:core:domain` | 127 | 0 | forced |
| `:core:data` | 122 | 0 | forced |
| `:core:network` | 31 | 0 | forced |
| `:core:navigation` | 4 | 0 | forced |
| `:core:ui` | 24 | 0 | fresh |
| `:feature:search` | 35 | 0 | fresh |
| `:feature:radar` | 9 | 0 | fresh |
| `:feature:game-detail` | 39 | 0 | fresh |
| `:feature:lists` | 11 | 0 | fresh |
| `:feature:wishlist` | 55 | 0 | fresh |
| `:feature:settings` | 33 | 0 | fresh |
| `:feature:onboarding` | 28 | 0 | fresh |
| `:shared` | 6 | 0 | fresh (`KoinGraphTest` "every dependency in the graph is declared", `RootViewModelTest`) |
| `:androidApp` | 1 | 0 | fresh (template `ExampleUnitTest`, known in `docs/tech-debt.md`) |
| `:core:designsystem`, `:core:model`, `:core:database` | — | — | `NO-SOURCE` (known: DAOs untested, in `docs/tech-debt.md`) |
| `:core:ai` | — | — | no test source set (known, in `docs/tech-debt.md`) |

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-VER-01 | pass | T017 |
| R-VER-02 | pass | T018: 0 failures, 0 errors, 0 skipped; `KoinGraphTest` executed and passed. No test deleted or weakened, compared with `develop`: 0 deleted test files (`git diff --name-status -M develop..HEAD -- '*Test.kt'`), test files 47 → 58, `@Test` annotations 455 → 539, assertion-like lines (`assert\|verify\|coVerify\|expect`) 869 → 1136. The 010 baseline's floor (SC-001) is 456 tests; 534 JVM + 1 template + 5 iOS now execute. This is a proxy count, not a diff of every assertion |
| R-VER-03 | pass | T019 (reused up-to-date result, see above) |
| R-VER-04 | pass | T020 |
| R-VER-05 | pass | T021 |

## Not verifiable here

| Item | Why not | Where it is recorded |
|---|---|---|
| Windows configuration and build (`.\gradlew.bat :androidApp:assembleDebug`, `:core:domain:testAndroidHostTest`, `kotlin.native.ignoreDisabledTargets`) | This machine is a Mac. `CLAUDE.md` says the project is also developed on Windows | 010 `tasks.md` T065, T090 (open); `baseline.md` line 154: "still need the owner's Windows machine" |
| Pre-migration parity screenshots and the data-continuity check on a device | They need a device and the pre-migration build | 010 T002 (partial), T003 (open) |
| iOS first-run timing by a person; iOS offline-search row | A person with a stopwatch; a simulator shares the Mac's network | 010 T124, T126 (open); `baseline.md` row 10 "Not exercised"; `docs/tech-debt.md` |
| iOS on a physical device and iOS 16.x | No device, oldest runtime is iOS 17.0 | `docs/tech-debt.md` "The iOS 16 floor is untested" |

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-VER-1 | Spec 010 still has 6 open tasks, all manual checks | `specs/010-kmp-migration/tasks.md`: 127 tasks, 121 done, 6 open (T002/T003 device parity and data continuity, T065/T090 Windows builds, T124 first-run timing, T126 iOS offline). The open set is recorded in the 010 `baseline.md`, so it is known. Two of the task descriptions still say `:app:assembleDebug` (the module was renamed `:androidApp`). Severity medium for the Windows pair (a stated development platform with no recorded result), low for the rest. Classification known / follow-up |

Notes that are not candidates:
- The reused/up-to-date results above are the only qualification on a "pass".
- No manual simulator walkthrough was done (out of scope per research R10). `specs/010-kmp-migration/baseline.md`
  holds the migration's own.
