# KMP Architecture Audit: 010-kmp-migration

**Baseline**: `b982bf11` on `010-kmp-migration` (audited from `011-kmp-architecture-audit`, which differs only by `specs/011-kmp-architecture-audit/`) · **Audited**: 2026-10-09 · **Environment**: macOS 26.6.2, Xcode 27.0, Gradle 9.7.1

## Verdict

**Merge.** No structural blocker was found: the Android build, all 535 JVM tests, the iOS framework link, the iOS test and the Xcode build pass, the dependency boundaries and shared-code purity hold, and every platform source file maps to a documented capability contract.

Blockers (must be fixed before merging): **none.**

Recommended to land together with the merge (not blocking): one documentation commit covering **F-001** (the documented compile and test commands no longer exist), **F-006** to **F-010** (stale statements in `tech-debt.md`, the instruction files, the constitution and the 010 contracts). Spec 010's own FR-014 requires the documentation to be updated in the same commit as the change that made it stale, and each fix is small. F-011 and F-005 are quick build-file cleanups that fit the same pass.

Owner decisions needed:
- **F-002**: merge first and run the two Windows commands afterwards, or hold the merge for them. The project is also developed on Windows and its build has not run there.
- **F-001 and F-009**: the constitution edits need your approval (PATCH amendment 1.3.3).
- **F-004**: make `:core:data`'s implementation classes `internal` (about 17 files), or record it in `docs/tech-debt.md`.

Not verified:
- Whether the **minified release app runs**. It compiles and packages (R8 ran, 11.1 MB), but `androidApp/proguard-rules.pro` has no keep rules beyond source-file attributes, so it depends on the libraries' consumer rules. A device and an emulator are attached, but installing would replace the debug app on them, so it was left for you.
- **Windows** configuration and build; device parity screenshots; iOS first-run timing; iOS offline search; an iOS 16.x runtime or a physical iOS device (spec 010 T002, T003, T065, T090, T124, T126; `docs/tech-debt.md`).
- The iOS **compile and link** were `UP-TO-DATE` (built from identical inputs); they were not force-rebuilt.
- Behaviour of the iOS app and the iOS Koin graph (no JVM test covers it).

Counts: 71 rules checked, 64 pass, 7 violated (6 minor, 1 with a medium finding); 12 findings (0 blockers, 2 medium, 10 low); 48 of 48 platform files mapped.

## Resolution

Decisions taken by the owner after reading this report, and what was done on `011-kmp-architecture-audit` (the findings below stay as observed at the baseline):

| Finding | Decision | Outcome |
|---|---|---|
| F-001, F-006, F-007, F-008, F-009, F-010 | Add the documentation fixes | Fixed in `cf903e24` (`CLAUDE.md`, constitution 1.3.3, `tech-debt.md`, `core/ui` and `core/designsystem` `CLAUDE.md`, the 010 contracts and the two open Windows task descriptions) |
| F-005, F-011 | Recommended cleanups for the same pass | Fixed in `b74e499b` (12 catalog entries and 8 orphaned versions removed; `:shared` icons dependency; stale build comments) |
| F-004 | Make `:core:data`'s implementation classes `internal` | Fixed in `24bf3d53` (18 declarations in 18 files). Re-verified: `./gradlew test`, `:androidApp:assembleDebug`, `:androidApp:assembleRelease`, the iOS framework link, `:core:network:iosSimulatorArm64Test` and the Xcode simulator build all pass |
| F-002 | Merge first, check Windows afterwards | Open by decision: run `.\gradlew.bat :androidApp:assembleDebug` and `:core:domain:testAndroidHostTest` on the Windows machine and record them in `specs/010-kmp-migration/baseline.md` |
| Minified release app | Smoke-tested by the owner | OK (the "Not verified" item above is closed) |
| F-003, F-012 | Not decided | Left as follow-ups |

Merge order: `010-kmp-migration` first, then `011-kmp-architecture-audit` (it is 010's tip plus the spec commits, the owner's two settings fixes and the commits above, so it merges without conflicts).

## Verification

| Command | Status | Notes |
|---|---|---|
| `./gradlew :androidApp:assembleDebug` | passed | 374 tasks, 17 executed; the APK was already built from identical inputs |
| `./gradlew test` | passed | 535 tests (534 in 14 multiplatform modules + 1 template), 0 failures, 0 errors, 0 skipped. 9 multiplatform modules executed in the run; the 5 modules whose result Gradle reused were re-executed with the result directories removed and `--no-build-cache`; `KoinGraphTest` passed |
| `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | passed (reused) | compile and link `UP-TO-DATE` for every module, i.e. built from identical inputs; not force-rebuilt |
| `./gradlew :core:network:iosSimulatorArm64Test` | passed | run fresh with `--rerun --no-build-cache`: `PlatformTransportFailureTest`, 5 tests |
| `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build` | passed | `** BUILD SUCCEEDED **`, 0 warnings |
| `./gradlew :androidApp:assembleRelease` (extra, not in the plan) | passed (compiled, not run) | R8 ran (1 m 24 s), 11.1 MB APK, no missing-class warnings. The minified app was **not launched** |

Logs are in the repository's git-ignored `build/audit/`. Gradle reuses a task result only when its inputs are unchanged; where that happened it is marked, and where re-execution was cheap it was forced. Per-module test counts are in `work/verification.md`.

## Findings

### Blockers

None. See `work/blockers.md` for why the two medium findings are not blockers under the rubric.

### Follow-ups

#### F-001: The single-module compile and test commands in `CLAUDE.md` and the constitution name tasks that no longer exist

- **Location**: `CLAUDE.md:33`, `:36`, `:48`; `.specify/memory/constitution.md:98`
- **Rule**: R-DOC-01. Intent: spec 010 FR-014 and `research.md:327` ("update the commands in `CLAUDE.md` and the constitution in the same commit")
- **Severity**: medium · **Classification**: follow-up · Suggested timing: with the merge
- **Impact**: The "fast single-module check" that the root file tells every contributor and agent to use fails with a Gradle "task not found" error, and Principle V of the constitution ("a plan MUST NEVER depend on a gate that does not exist") gives plans a wrong command.
- **Evidence**: `./gradlew :feature:search:compileDebugKotlin --dry-run` and `./gradlew :core:data:testDebugUnitTest --dry-run` fail ("What went wrong"). `./gradlew :feature:search:compileAndroidMain --dry-run` and `:core:data:testAndroidHostTest --dry-run` succeed. `compileDebugKotlin` exists only on `:androidApp` and `:core:ai` (`./gradlew :feature:search:tasks --all`).
- **Fix** (S): In `CLAUDE.md`, replace the two examples with `./gradlew :feature:search:compileAndroidMain --console=plain -q` and `./gradlew :core:data:testAndroidHostTest --console=plain -q`, relabel them "(multiplatform module)", and reword line 48 to "`compileAndroidMain` for a multiplatform module, `compileDebugKotlin` only for `:androidApp` and `:core:ai`". Make the same change in constitution Principle V, as a PATCH amendment (1.3.3), in the same commit.
- **Owner decision**: The constitution change needs the owner's approval (Governance: amendments).
- **Checked against**: `b982bf11` · **Candidates**: C-DOC-1, C-DOC-8

#### F-002: Spec 010 still has six open manual tasks, and the Windows build has no recorded result

- **Location**: `specs/010-kmp-migration/tasks.md` T002, T003, T065, T090, T124, T126; `specs/010-kmp-migration/baseline.md:154`
- **Rule**: Intent: 010 verification (US3, SC-001 parity). Not a rule violation: the open set is recorded
- **Severity**: medium (Windows pair), low (the rest) · **Classification**: known
- **Impact**: The project is also developed on Windows (`CLAUDE.md` "Commands"), but the convention plugins, per-target KSP in `:core:database` and `kotlin.native.ignoreDisabledTargets` have only run on a Mac. If configuration fails on Windows, it is discovered after the merge. Device parity screenshots (T002 partial, T003), the iOS first-run timing (T124) and the iOS offline row (T126) are also unrecorded.
- **Evidence**: `grep -cE "^- \[ \] T" specs/010-kmp-migration/tasks.md` = 6 of 127; `baseline.md:154`: "Windows checks (T065, T090) still need the owner's Windows machine"; `baseline.md` row 10 "Not exercised". Two task descriptions still say `:app:assembleDebug` (module renamed `:androidApp`).
- **Fix** (S (the owner's time)): On the Windows machine run `.\gradlew.bat :androidApp:assembleDebug` and `.\gradlew.bat :core:domain:testAndroidHostTest`, record both in `baseline.md`, and correct the `:app:` text in T065 and T090.
- **Owner decision**: Merge first and check on Windows afterwards, or hold the merge until the Windows check is done.
- **Checked against**: `b982bf11` · **Candidates**: C-VER-1

#### F-003: `WishlistCoverImageStorageImpl` repeats its policy constants on each platform

- **Location**: `core/data/src/androidMain/.../local/WishlistCoverImageStorageImpl.kt:92,98,100`; `core/data/src/iosMain/.../local/WishlistCoverImageStorageImpl.kt:24,28,29`
- **Rule**: R-DUP-01. Intent: `platform-contracts.md` ("same max dimension", "same subdirectory name", file name via `kotlin.uuid.Uuid`)
- **Severity**: low · **Classification**: follow-up
- **Impact**: The values agree today (`"wishlist_covers"`, 1440 px, JPEG quality 85/0.85, PNG when the image has alpha). If one side is changed, covers saved on the two platforms differ in size or location with no compile error and no test (neither implementation has a test), and `delete(path)` could miss files.
- **Evidence**: dup.md side-by-side table. Both files define their own `COVERS_DIR_NAME`, `MAX_DIMENSION_PX` (Int vs Double) and quality constants; `grep -rn "COVERS_DIR_NAME\|MAX_DIMENSION_PX" core/data/src/commonMain` finds nothing. The contract says `kotlin.uuid.Uuid`; the code uses `java.util.UUID` and `NSUUID`.
- **Fix** (S): One `internal` constants holder in `core/data/src/commonMain/.../local/` read by both implementations; optionally `kotlin.uuid.Uuid.random()` for the name. No new module or edge.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-DUP-1

#### F-004: `:core:data` exposes its implementation classes as public although nothing outside the module uses them

- **Location**: `core/data/src/{commonMain,androidMain,iosMain}`: 17 of the 18 public top-level types (`GameRepositoryImpl`, the three preference stores, both schedulers and workers, `ReleaseNotifierImpl`, `WishlistCoverImageStorageImpl` ×2, the iOS no-ops, `InProcessReleaseRefreshScheduler`, `UnsupportedGameDescriptionTranslator`, `WishlistCoverImageStorage`)
- **Rule**: R-PLACE-16: constitution "Development Workflow" ("Default to `internal` for anything local to a module")
- **Severity**: low · **Classification**: follow-up
- **Impact**: Another module could depend on a concrete class and bypass the `:core:domain` contracts, which is the layering the rules protect, and the compiler would not stop it. Pre-existing (0 `internal` types in `:core:data` on `develop`, all 10 implementation-like classes public); the migration added 7 more in the same style. Not listed in `docs/tech-debt.md`.
- **Evidence**: vis.md (Python scan of every `commonMain`/`androidMain`/`iosMain` file, run on both trees). Nothing forces them public: the bindings are `singleOf(::X)` inside the module's own Koin modules.
- **Fix** (M (about 17 files; `KoinGraphTest` covers the wiring)): Make them `internal`; `androidHostTest` in the same module still sees them. Alternatively add the pattern to `docs/tech-debt.md` and defer.
- **Owner decision**: Fix now or record as known debt.
- **Checked against**: `b982bf11` · **Candidates**: C-VIS-1

#### F-005: 12 entries in the version catalog are used by no build file

- **Location**: `gradle/libs.versions.toml`: `androidx-lifecycle-viewmodel-compose`, `androidx-lifecycle-runtime-compose`, `androidx-lifecycle-viewmodel-navigation3`, `androidx-navigation3-ui`, `androidx-compose-material-icons-core`, `androidx-compose-material-icons-extended`, `androidx-compose-ui-graphics`, `androidx-compose-ui-tooling-preview`, `androidx-compose-adaptive`, `-adaptive-layout`, `-adaptive-navigation3`, `okhttp`
- **Rule**: R-LEFT-03. Intent: 010 `plan.md` "Removed" list; `research.md:353` (adaptive: "mention, don't fix")
- **Severity**: low · **Classification**: follow-up
- **Impact**: No build or app effect. The catalog suggests the project uses both the AndroidX and the JetBrains lifecycle/navigation artifacts, which the migration set out to remove. All 12 were used on `develop`; the migration orphaned them. Their `[versions]` entries become orphans once the libraries go.
- **Evidence**: left.md: Python scan of every tracked `*.gradle.kts`, convention plugin and `ProjectExtensions.kt` for the exact `libs.<alias>` accessor and `lib("<alias>")` calls, on both trees.
- **Fix** (S): Delete the 12 library lines and any version only they referenced, in one commit; confirm with `./gradlew :androidApp:assembleDebug` and `./gradlew test`.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-LEFT-1

#### F-006: `docs/tech-debt.md` still describes a cleanup pass on `develop` and out-of-date test coverage

- **Location**: `docs/tech-debt.md:16-27` ("Cleanup pass in progress", "Convention plugins, CI and the test-coverage gaps are deliberately last"); "Test coverage gaps" paragraph
- **Rule**: R-DOC-02, R-DOC-03. Intent: spec 010 FR-014; the file's own rule ("When an item is fixed, delete it")
- **Severity**: low · **Classification**: follow-up
- **Impact**: The convention plugins the section calls "last" now exist. The remaining item (release signing) is already under "Technical risks". "In `:core:ui` only `PlatformPickerMapper` is covered" is wrong (`GameUiMapperTest`, `HtmlUtilsTest` exist, two of them added by the migration); "In `:core:domain` … list … not covered" was already wrong on `develop` (`GetListsUseCaseTest`, `DeleteListUseCaseTest`, `GetWishlistDetailUseCaseTest`).
- **Evidence**: doc-other.md C-DOC-6, C-DOC-7; `git ls-files "core/ui/src/*Test/*"`; develop test list compared.
- **Fix** (S): Delete the "Cleanup pass" section; rewrite the coverage sentence from the current test files.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-DOC-6, C-DOC-7

#### F-007: Root `CLAUDE.md` describes the migration as in progress and omits the `:androidApp → :core:domain` edge

- **Location**: `CLAUDE.md:89` ("The migration is converting modules one by one"); "Module graph" section (`:androidApp → :shared → everything`)
- **Rule**: R-DOC-01, R-DOC-03
- **Severity**: low · **Classification**: follow-up
- **Impact**: Misleads about the state of the project and hides one real, intentional edge. All 17 convertible modules are converted; `:shared` is "everything" except `:core:ai`.
- **Evidence**: doc-claude.md; `androidApp/build.gradle.kts:35-36`; resolved `debugCompileClasspath` of `:androidApp` = `:shared`, `:core:domain`.
- **Fix** (S): Delete the sentence; add `:core:domain` to the graph line or a note.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-DOC-2, C-DOC-3

#### F-008: Two directory `CLAUDE.md` files carry stale statements

- **Location**: `core/ui/CLAUDE.md` "Build notes" ("differs from the flag used by `core/network`"); `core/designsystem/CLAUDE.md:3` ("seven files under `theme/`. No composables beyond `QuestLogTheme`")
- **Rule**: R-DOC-01
- **Severity**: low · **Classification**: follow-up
- **Impact**: `core/network` no longer sets a compiler flag; `theme/` holds 8 distinct files (10 with the two actuals) including the `internal expect` composable `SystemBarsAppearance`.
- **Evidence**: doc-claude.md; `core/network/build.gradle.kts` has no `freeCompilerArgs`; `git ls-files "core/designsystem/src/*/theme/*"`.
- **Fix** (S): Drop the comparison; update the count and the "no composables" claim.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-DOC-4, C-DOC-5

#### F-009: The constitution disagrees with the code and the 010 contracts on the splash screen, and lists the wrong test source sets

- **Location**: `.specify/memory/constitution.md` "Kotlin Multiplatform" (splash among the capabilities that need a shared contract); Principle V lines 103-105
- **Rule**: R-DOC-02. Precedence rule: the source and `CLAUDE.md` win, the constitution is corrected
- **Severity**: low · **Classification**: follow-up
- **Impact**: The code follows `platform-contracts.md`, which classes the splash as platform shell. Principle V names `core/data`, `core/domain`, `core/network`, `core/ui`, the features and `androidApp` as the test sets that must stay green, but omits `core/common`, `core/navigation` and `:shared` (which holds `KoinGraphTest`), and `androidApp` only has the template test.
- **Evidence**: cap-contracts.md C-CAP-2; doc-other.md C-DOC-8; test files per module.
- **Fix** (S): Fold into the same PATCH amendment as F-001.
- **Owner decision**: The owner approves constitution amendments.
- **Checked against**: `b982bf11` · **Candidates**: C-CAP-2, C-DOC-8, C-DOC-9

#### F-010: The 010 contract documents disagree with the code in four places

- **Location**: `specs/010-kmp-migration/contracts/module-contracts.md:26` (`:androidApp → :core:navigation`) and "Navigation ownership" (route registration "in the same file", two edits); `contracts/platform-contracts.md` (no row for the Room builder and the network engine/clock platform modules; file name "via `kotlin.uuid.Uuid`")
- **Rule**: R-DOC-02; root `CLAUDE.md` names `platform-contracts.md` as the authority for what lives in `androidMain`/`iosMain`
- **Severity**: low · **Classification**: follow-up
- **Impact**: The code is right in every case; the documents are the stale side. The missing rows matter most, because the rule "every platform source implements a capability in `platform-contracts.md`" cannot be checked literally for four files.
- **Evidence**: dep.md C-DEP-1, place.md C-PLACE-2, cap-mapping.md C-CAP-1 (rows 17, 20, 36, 39), dup.md C-DUP-1.
- **Fix** (S): Add the two `*PlatformModule` rows to `platform-contracts.md` (or reference the two `CLAUDE.md` files from it); correct the three other statements, or mark the documents as historical.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-DEP-1, C-PLACE-2, C-CAP-1

#### F-011: Build-file leftovers: a redundant dependency and stale comments

- **Location**: `shared/build.gradle.kts:44` (`jetbrains.material.icons.extended`); `gradle.properties:12` ("14 modules"); root `build.gradle.kts` ("while modules are converted one by one", "the Android modules not yet converted")
- **Rule**: R-BUILD-09 (`core/ui/CLAUDE.md`: "do not re-declare them downstream")
- **Severity**: low · **Classification**: follow-up
- **Impact**: Harmless, but the comments describe a build in transition and the module count is wrong (19, not 14).
- **Evidence**: build.md.
- **Fix** (S): Remove the dependency line (`:shared` gets the icons through `:core:ui`'s `api(...)`); update the three comments.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-BUILD-1, C-BUILD-2

#### F-012: The `questlog://game/<id>` deep-link format is written in three places with no shared constant

- **Location**: `androidApp/src/main/AndroidManifest.xml:36-37`; `androidApp/.../MainActivity.kt:94`; `core/data/src/androidMain/.../ReleaseNotifierImpl.kt:28`
- **Rule**: No rule broken. Pre-existing; the format did not change in the migration
- **Severity**: low · **Classification**: follow-up (optional)
- **Impact**: A change to the scheme or host in one place silently breaks reminder taps. Sharing a constant between `:androidApp` and `:core:data` would need a new edge or a new home, so this is a trade-off rather than a defect.
- **Evidence**: place.md C-PLACE-1.
- **Fix** (S): Leave as is, or add a comment at each site pointing at the others.
- **Owner decision**: none
- **Checked against**: `b982bf11` · **Candidates**: C-PLACE-1

### Known and intentional

Known (already recorded, not made worse by the migration):
- iOS tests are outside the root `test` task, and the iOS Koin graph is not covered by a JVM test (`docs/tech-debt.md` "Test coverage gaps").
- Material 3 is split (androidx `1.5.0-beta01` on Android, JetBrains `1.12.0-alpha03` on iOS) with the `SearchBarScrollBehaviorCompat` seam (`docs/tech-debt.md`).
- Template tests `ExampleUnitTest` and `ExampleInstrumentedTest` in `:androidApp` (`docs/tech-debt.md`).
- Release is signed with the debug key (`docs/tech-debt.md`).
- Room stays at `version = 1` with `fallbackToDestructiveMigration(true)`; the schema JSON is byte-identical to `develop` (`docs/tech-debt.md`, `core/database/CLAUDE.md`).

Intentional (recorded in the migration's own documents):
- `iosX64` is not a target (`specs/010-kmp-migration/research.md:339`).
- `koin-core` in `:core:common` and `:core:domain` (`module-contracts.md`).
- `:androidApp → :core:domain` for the periodic refresh call (`module-contracts.md`).
- iOS has no reminders, translation, background refresh or deep link; refresh is in-process once per 24 h (`platform-contracts.md` "Capability exception register", `docs/roadmap.md`).
- `runBlockingCompat` has identical `actual`s because `runBlocking` is not visible from `commonMain` (`platform-contracts.md` "Compatibility seams").
- `:core:ai` and `:androidApp` keep Android-only build files (`module-contracts.md` "Unchanged").

Observations (not findings):
- `gradle.properties:22` `android.disallowKotlinSourceSets=false` makes Gradle warn on every build that the option is experimental. It predates the migration, has no comment, and may no longer be needed; whether it can be removed was not tested.
- In Gradle's multiplatform metadata view, `:core:network`'s implementation dependencies show up under dependents. No source crosses a boundary because of it (`dep.md`), but nothing but the compiler would stop one from doing so.
- Local, untracked clutter: six empty directories (including a second, empty schema directory `…GamesWishlistDatabase/`). Not part of the merge (`left.md`).

## Rule ledger

71 rules, from `work/rules.md` (sources in precedence order: root `CLAUDE.md` "RC", directory `CLAUDE.md` files "DC", constitution "CN", 010 `module-contracts.md` "MC", 010 `platform-contracts.md` "PC"). `R-PLACE-16` and `R-VER-06` were added during the audit.

| Rule | Statement | Source | Outcome | Evidence | Findings |
|---|---|---|---|---|---|
| R-DEP-01 | `feature/*` depends only on `:core:{common, model, domain, ui, navigation, designsystem}`; never on `:core:data`, `:core:network`, `:core:database` or `:core:ai` | RC "Module graph" (CN I, MC) | pass | dep.md: 7 features resolve to exactly the six allowed `:core:*` edges | - |
| R-DEP-02 | `:core:ai` is reachable only from `:core:data`, and only from its `androidMain` | RC (CN I, MC "Edges that change") | pass | dep.md: declared only in `core/data` `androidMain`; compile edge only on `:core:data` | - |
| R-DEP-03 | `:core:ai` depends on nothing but the ML Kit SDK and coroutines — not even `:core:model` | RC | pass | core/ai/build.gradle.kts: ML Kit + coroutines only | - |
| R-DEP-04 | `:shared` depends on every module except `:core:ai` | RC | pass | shared/build.gradle.kts: 9 core + 7 features, no `:core:ai` | - |
| R-DEP-05 | `:androidApp` depends on `:shared`, `:core:navigation` (deep link → `GameDetailRoute`) and `:core:domain` (scheduler call), and nothing else internal | MC "Edges that change" (RC graph shows `:shared` only) | pass | Build: `:shared`, `:core:domain`; `module-contracts.md` also lists `:core:navigation`, which is not needed (docs only) | F-010 |
| R-DEP-06 | `:core:data` depends on `:core:{common, model, domain, network, database}` | MC | pass | declared and resolved edges match `module-contracts.md` | - |
| R-DEP-07 | `:core:ui` depends on `:core:{common, model, designsystem}`; `:core:network`/`:core:database` on `:core:{common, model}`; `:core:domain` on `:core:{common, model}`; `:core:model` and `:core:navigation` on nothing internal | MC | pass | dep.md edge matrix | - |
| R-DEP-08 | No feature module and no UI module (`:core:ui`, `:core:designsystem`) depends on Koin; ViewModels are registered in `:shared` | DC feature/ (CN "Injection", MC) | pass | no `org.koin` import or classpath entry in features, `core/ui`, `core/designsystem` | - |
| R-DEP-09 | `:core:data` does not depend on Ktor (transport failures are translated in `:core:network`) | DC core/network, core/data | pass | 0 Ktor artifacts on `:core:data`; 0 `io.ktor` imports outside network/shared/androidApp | - |
| R-DEP-10 | A new module or edge is called out explicitly; every module is registered in `settings.gradle.kts` | RC, CN | pass | 19 `include`s, 19 build files | - |
| R-PURE-01 | `commonMain` never imports `android.*` or `java.*` (dates: `kotlinx-datetime`; IO: `kotlinx-io`) | RC "KMP" (CN) | pass | pure.md: 0 `android.`/`java.`/`javax.` imports in 414 `commonMain` files; iOS compile up to date | - |
| R-PURE-02 | A common file never imports `androidx.activity.*` (or another Android-only artifact) | DC core/ui | pass | pure.md: all `androidx.*` packages are multiplatform artifacts | - |
| R-PURE-03 | `:core:model` has no Android and no Compose dependency (only `kotlinx-serialization-core`) | RC (CN, MC) | pass | core/model: `kotlinx-serialization-core` only, `commonMain` only | - |
| R-PURE-04 | Feature modules contain no Android-only calls (`Intent`, `Context`, permissions, pickers); they sit behind an `expect` in `:core:ui` | DC feature/ | pass | pure.md: 0 Android-only identifiers in feature `commonMain` | - |
| R-PURE-05 | `:core:data` `commonMain` names no platform type; platform exception types appear only in `RepositoryErrorMapper.android.kt` | DC core/data | pass | pure.md: platform exception types only in `RepositoryErrorMapper.android.kt` | - |
| R-PURE-06 | The Room module uses no `SupportSQLiteOpenHelper` types (`SupportSQLiteDatabase`, `openHelper`, `SupportSQLiteQuery`) | DC core/database | pass | pure.md: 0 `SupportSQLite*` / `@RawQuery` | - |
| R-PURE-07 | New date code uses `kotlinx-datetime`, not `java.time` | CN "KMP" | pass | pure.md: `java.time` only in the Android `actual` | - |
| R-CAP-01 | Every file in `androidMain` or `iosMain` implements a capability in `platform-contracts.md`, or is a listed compatibility seam, platform-shell item or `*PlatformModule` | RC "KMP" | pass | cap-mapping.md: 48/48 files map to a row; the DB builder and network engine modules are covered by `CLAUDE.md` files rather than `platform-contracts.md` | F-010 |
| R-CAP-02 | A capability is a contract owned by shared code with one implementation per platform | RC (CN) | pass | cap-contracts.md | - |
| R-CAP-03 | `expect`/`actual` is for a single function or composable; a service with state is an interface in `commonMain`, bound per platform in that module's `*PlatformModule` | RC | pass | no `expect class`; stateful services are interfaces bound in `*PlatformModule`s | - |
| R-CAP-04 | Every `expect` has an `actual` for both Android and iOS | RC (PC) | pass | 17 names / 18 declarations hand-written on both platforms + Room-generated constructor | - |
| R-CAP-05 | No platform type (`Context`, `Uri`, `NSURL`, `UIViewController`) appears in a contract signature | PC "Rules" | pass | no platform type in any contract file | - |
| R-CAP-06 | Test doubles for a contract live in the consuming module's test source set, never in `commonMain` | PC "Rules" | pass | 0 fakes in any `commonMain` | - |
| R-CAP-07 | `:core:domain` never gains a platform source set | PC "Rules" | pass | `core/domain/src` = `commonMain` + `androidHostTest` | - |
| R-CAP-08 | The release-refresh interval is one constant shared by both implementations | PC `ReleaseRefreshScheduler` | pass | `RELEASE_DATES_REFRESH_INTERVAL` used by both schedulers | - |
| R-CAP-09 | The capability exception register matches what the code does on iOS (reminders absent, translation `UNSUPPORTED`, in-process refresh) | PC "Capability exception register" | pass | cap-contracts.md register-vs-code table, 6/6 rows match | - |
| R-CAP-10 | iOS `isConnectivityFailure`/`isTimeoutFailure` test only the network module's two exception types; the `NSError` translation lives in `:core:network` | PC, DC core/data | pass | `RepositoryErrorMapper.ios.kt` tests the two network types; `NSError` mapping in `:core:network` | - |
| R-CAP-11 | Platform-only capabilities (background refresh, notifications, on-device translation, splash) are reached through a contract owned by shared code | CN "KMP" | pass | cap-contracts.md; splash is a shell concern, constitution wording differs | F-009 |
| R-PLACE-01 | `:shared` is the only module that knows navigation: `NavDisplay`, the single `entryProvider`, back stack, bottom bar | RC (CN I, MC) | pass | place.md: no `NavDisplay`/`entryProvider`/`NavBackStack` outside `shared/`; 9 branches | - |
| R-PLACE-02 | Feature modules own no nav graph, no entry provider, no per-feature DI module; screens receive lambdas and never navigate | RC (CN I, DC feature/) | pass | place.md: Koin/nav calls only in `shared`, `androidApp`, module-internal `get<>()` | - |
| R-PLACE-03 | Adding a route = `NavKey` in `core/navigation/Routes.kt` + `subclass(...)` in `GameNavSavedStateConfiguration.kt` + branch in `QuestLogNavDisplay.kt` | CN I / DC feature/ (MC says two edits) | pass | place.md: 9 routes = 9 `subclass(...)` = 9 branches; `RoutesSerializationTest` exists | F-010 |
| R-PLACE-04 | Every ViewModel is registered with `viewModelOf` in `shared/.../di/ViewModelModule.kt`; `initKoin` lives in `shared/.../di/SharedKoin.kt` | RC "Where things live" | pass | place.md: 10 ViewModels = 10 `viewModelOf`; `initKoin` in `SharedKoin.kt` | - |
| R-PLACE-05 | `SavedStateHandle` is not used | DC feature/ (CN) | pass | 0 `SavedStateHandle` | - |
| R-PLACE-06 | `KoinGraphTest` exists in `:shared` and runs under the root `test` task | CN "Injection" | pass | `KoinGraphTest` executed and passed (Android graph only) | - |
| R-PLACE-07 | Splash, edge-to-edge and deep-link parsing live in `androidApp/.../MainActivity.kt`; `:androidApp` holds only the Activity, `Application` and manifest | RC | pass | splash/edge-to-edge/`onNewIntent` only under `androidApp/` | - |
| R-PLACE-08 | The iOS entry point is `MainViewController` in `shared/src/iosMain`, hosted by `iosApp/` | RC | pass | `MainViewController` in `shared/iosMain`; `iosApp/` is a thin host | - |
| R-PLACE-09 | A cross-module `Res` import is aliased `<Module>Res`; a module's own `Res` is imported bare | RC (DC feature/, core/ui) | pass | 24 aliased `CoreUiRes` files, 0 unaliased cross-module `Res` | - |
| R-PLACE-10 | Exactly one `GameRepository` implementation, in `:core:data`; the interface in `:core:domain` | RC (CN IV, DC core/data) | pass | one `GameRepositoryImpl : GameRepository` | - |
| R-PLACE-11 | `AppResult`/`RepositoryError` live in `:core:model`; `UiText` in `core/ui/model/`; `RepositoryError.toUiText()` is the single error-to-text boundary | RC, CN II | pass | `AppResult`, `RepositoryError` in `core/model`; one `toUiText()` | - |
| R-PLACE-12 | `:core:network` returns bare lists and throws; no result wrapper, no retries; HTTP-client types never leave it | DC core/network (CN II) | pass | bare `List<…>` returns; no Ktor type in a public declaration | - |
| R-PLACE-13 | Exactly one exported-schema directory under `core/database/schemas/` | DC core/database | pass | one tracked schema dir; an empty untracked second dir is local only | - |
| R-PLACE-14 | IGDB credentials are generated into `IgdbCredentials` under `build/`, never committed | RC (CN) | pass | no credentials or `local.properties` tracked; README placeholders only | - |
| R-PLACE-15 | `ElapsedRealtimeSource`, `IgdbAuthManager`, `IgdbAuthService` are `internal` | DC core/network | pass | the three types are `internal` | - |
| R-PLACE-16 | Anything local to a module defaults to `internal` (added during the audit; checked in `vis.md`) | CN "Development Workflow" | **violated (minor)** | vis.md: 17 of 18 public types in `:core:data` are never used outside it (pre-existing pattern) | F-004 |
| R-BUILD-01 | Build config lives in `build-logic/` convention plugins; a module applies one and declares only its own dependencies | RC (MC "Convention plugins") | pass | build.md: 18 modules apply one convention plugin; `:core:ai` by design | - |
| R-BUILD-02 | namespace, `compileSdk = 37`, `minSdk = 29`, Java 11 come from the plugin, never from a module | RC (MC) | pass | no `namespace`/SDK/Java in a KMP module build file | - |
| R-BUILD-03 | `:core:ai` keeps a hand-written Android-library build file; `:androidApp` applies `questlog.android.application` | RC (MC) | pass | `:core:ai` hand-written; `:androidApp` uses the application plugin | - |
| R-BUILD-04 | Features apply `questlog.kmp.feature`, which supplies the six allowed `:core:*` modules | DC feature/ (MC) | pass | 7 features apply `questlog.kmp.feature` | - |
| R-BUILD-05 | A root `test` task depends on every multiplatform module's `testAndroidHostTest` | RC "Commands" | pass | root `test` task; `./gradlew test` ran 14 KMP modules + 2 Android-only | - |
| R-BUILD-06 | Each KMP module targets Android plus `iosArm64` and `iosSimulatorArm64`; KSP runs once per target in `:core:database` | MC, DC core/database | pass | iOS targets from `KmpLibraryConventionPlugin`; KSP per target in `core/database` | - |
| R-BUILD-07 | `kotlin.native.ignoreDisabledTargets` is set so the project builds on Windows | RC "Commands" | pass | `gradle.properties:28` | - |
| R-BUILD-08 | Coil's network fetcher and per-platform Ktor engine are carried by `:shared`; image modules depend on `coil-compose` only | DC core/ui | pass | `shared` carries `coil3-network-ktor3` + both engines | - |
| R-BUILD-09 | `core/ui` exposes material-icons and haze via `api(...)`; downstream modules do not re-declare them | DC core/ui | **violated (minor)** | `shared/build.gradle.kts:44` re-declares icons already exposed by `core/ui` | F-011 |
| R-BUILD-10 | Versions in `CLAUDE.md`/constitution header match `gradle/libs.versions.toml` | RC (CN) | pass | doc-claude.md: header versions match `libs.versions.toml` | - |
| R-LEFT-01 | No tracked source folder that no module compiles (`<module>/src/main/java` in a KMP module) | MC "Source layout" | pass | left.md: no tracked `src/main` in KMP modules | - |
| R-LEFT-02 | Android `res/` strings and drawables moved to `composeResources/` (only `core/data` `androidMain/res` and `androidApp` keep Android resources) | MC "Source layout" | pass | left.md: Android `res/` only in `androidApp` and `core/data` `androidMain` | - |
| R-LEFT-03 | No unused entry in the version catalog from removed libraries (Hilt, Retrofit, Moshi, OkHttp logging, Coil 2, `shadowglow`) | 010 `plan.md` "Removed" | **violated (minor)** | left.md: 12 unused catalog entries, all used on `develop` | F-005 |
| R-LEFT-04 | No hand-written `android {}` block repeating convention-owned settings | MC | pass | no repeated convention-owned `android {}` settings | - |
| R-LEFT-05 | No template/example files except those already in `docs/tech-debt.md` | `docs/tech-debt.md` | pass | two template tests, both in `docs/tech-debt.md` | - |
| R-LEFT-06 | Tests moved to `androidHostTest` (no `src/test` left in a KMP module) | MC "Source layout" | pass | only `androidApp/src/test` (the template) outside `androidHostTest` | - |
| R-DUP-01 | Logic that touches no platform API is written once in shared code, not per platform | RC "KMP" (research R7) | **violated (minor)** | dup.md: cover-storage constants written twice (1 of 13 pair groups) | F-003 |
| R-DOC-01 | The instruction files (`CLAUDE.md` root + six directory files) state only what the source does | CN Governance | **violated** | doc-claude.md: dead compile/test commands (medium) and smaller stale statements | F-001, F-007, F-008 |
| R-DOC-02 | `AGENTS.md`, `README.md`, the constitution, `docs/tech-debt.md`, `docs/roadmap.md` and the 010 contracts match the source | CN Governance | **violated (minor)** | doc-other.md: constitution, tech-debt, 010 contracts | F-006, F-009, F-010 |
| R-DOC-03 | A resolved `docs/tech-debt.md` entry is deleted, not annotated; instruction files are instructions, not a changelog | RC "Directory-specific instructions" | **violated (minor)** | "converting modules one by one", "cleanup pass in progress" still present | F-006, F-007 |
| R-DOC-04 | The "Known deviations" in `docs/tech-debt.md` still exist (no entry for a thing already fixed) | RC | pass | tech-debt risks/infrastructure entries still exist (coverage sentences aside) | - |
| R-VER-01 | `./gradlew :androidApp:assembleDebug` succeeds | RC (CN V) | pass | verification.md T017 | - |
| R-VER-02 | `./gradlew test` passes, including `KoinGraphTest`; no test deleted or weakened | RC (CN) | pass | verification.md T018: 535 tests 0 failures; 0 test files deleted | - |
| R-VER-03 | The iOS framework links (`:shared:linkDebugFrameworkIosSimulatorArm64`) | RC | pass | verification.md T019 (up-to-date result from identical inputs) | - |
| R-VER-04 | The iOS-only tests (`:core:network:iosSimulatorArm64Test`) pass | RC | pass | verification.md T020: 5 tests, run fresh | - |
| R-VER-05 | The `xcodebuild` simulator build of `iosApp` succeeds | RC | pass | verification.md T021: `** BUILD SUCCEEDED **` | - |
| R-VER-06 | The minified release variant builds (added during the audit; the plan's five commands only build debug) | RC "Commands" (research R10 extra) | pass | verification.md: compiled and packaged; the minified app was **not run** | F-002 |

## Capability mapping

48 git-tracked `.kt` files under `*/src/androidMain` (27) and `*/src/iosMain` (21). "Maps to" is a row of `specs/010-kmp-migration/contracts/platform-contracts.md`. Detail and notes in `work/cap-mapping.md`.

| File | Platform | Maps to | Counterpart | Status |
|---|---|---|---|---|
| `core/common` · `AppVersionProviderImpl.kt` | android | `AppVersionProvider` | iOS `AppVersionProviderImpl.kt` | mapped |
| `core/common` · `NetworkStatusProviderImpl.kt` | android | `NetworkStatusProvider` | iOS `NetworkStatusProviderImpl.kt` | mapped |
| `core/common` · `DateRendering.android.kt` | android | "`DateUtils` platform rendering" | iOS `DateRendering.ios.kt` | mapped |
| `core/common` · `di/CommonPlatformModule.android.kt` | android | binds #1, #2 (Rule: bound in the platform module) | iOS `CommonPlatformModule.ios.kt` | mapped |
| `core/data` · `di/DataPlatformModule.android.kt` | android | binds #6–#16 (Rule: bound in the platform module) | iOS `DataPlatformModule.ios.kt` | mapped |
| `core/data` · `local/WishlistCoverImageStorageImpl.kt` | android | `WishlistCoverImageStorage` | iOS `WishlistCoverImageStorageImpl.kt` | mapped |
| `core/data` · `mapper/TranslationMapper.kt` | android | Compat seam: "`TranslationPromptBuilder`, `TranslationArtifactSanitizer`, `TranslationMapper` (Android only)" | iOS `UnsupportedGameDescriptionTranslator` (`commonMain`) | mapped |
| `core/data` · `notification/ReleaseNotifierImpl.kt` | android | `ReleaseNotifier` | iOS `NoOpReleaseNotifier` (`commonMain`) | mapped |
| `core/data` · `repository/RepositoryErrorMapper.android.kt` | android | "Connectivity classification" | iOS `RepositoryErrorMapper.ios.kt` | mapped |
| `core/data` · `scheduler/ReleaseNotificationSchedulerImpl.kt` | android | `ReleaseNotificationScheduler` | iOS `NoOpReleaseNotificationScheduler` (`commonMain`) | mapped |
| `core/data` · `scheduler/ReleaseRefreshSchedulerImpl.kt` | android | `ReleaseRefreshScheduler` | iOS `InProcessReleaseRefreshScheduler` (`commonMain`) | mapped |
| `core/data` · `translation/GameDescriptionTranslatorImpl.kt` | android | `GameDescriptionTranslator` | iOS `UnsupportedGameDescriptionTranslator` (`commonMain`) | mapped |
| `core/data` · `translation/TranslationArtifactSanitizer.kt` | android | Compat seam (Android-only translator part) | n/a, documented | mapped |
| `core/data` · `translation/TranslationPromptBuilder.kt` | android | Compat seam (Android-only translator part) | n/a, documented | mapped |
| `core/data` · `worker/ReleaseDatesRefreshWorker.kt` | android | `ReleaseRefreshScheduler` (WorkManager job it enqueues) | iOS `InProcessReleaseRefreshScheduler` | mapped |
| `core/data` · `worker/ReleaseNotificationWorker.kt` | android | `ReleaseNotificationScheduler` / `ReleaseNotifier` (WorkManager job) | iOS no-ops | mapped |
| `core/database` · `di/DatabasePlatformModule.android.kt` | android | PC "Platform shell" does not list it; `core/database/CLAUDE.md` + `module-contracts.md` ("`*PlatformModule`") cover it: the Room builder is per platform | iOS `DatabasePlatformModule.ios.kt` | mapped (see note 1) |
| `core/designsystem` · `theme/SystemBarsAppearance.android.kt` | android | `SystemBarsAppearance` | iOS `SystemBarsAppearance.ios.kt` (no-op, documented) | mapped |
| `core/network` · `PlatformTransportFailure.android.kt` | android | "Connectivity classification" | iOS `PlatformTransportFailure.ios.kt` | mapped |
| `core/network` · `di/NetworkPlatformModule.android.kt` | android | binds `ElapsedRealtimeSource` and the OkHttp engine (`core/network/CLAUDE.md`) | iOS `NetworkPlatformModule.ios.kt` | mapped (see note 1) |
| `core/ui` · `component/FullScreenDialogProperties.android.kt` | android | Compat seam: `fullScreenDialogProperties()` | iOS `…ios.kt` | mapped |
| `core/ui` · `component/NotificationPermissionDeniedDialog.android.kt` | android | "Notification permission UI" | iOS `…ios.kt` (inert) | mapped |
| `core/ui` · `util/CoverImagePicker.android.kt` | android | `rememberCoverImagePicker` | iOS `CoverImagePicker.ios.kt` | mapped |
| `core/ui` · `util/NotificationPermission.android.kt` | android | "Notification permission UI" | iOS `NotificationPermission.ios.kt` (inert) | mapped |
| `core/ui` · `util/TextSharer.android.kt` | android | `rememberTextSharer` | iOS `TextSharer.ios.kt` | mapped |
| `feature/search` · `components/SearchBarScrollBehaviorCompat.android.kt` | android | Compat seam: `SearchBarScrollBehaviorCompat` | iOS `…ios.kt` | mapped |
| `shared` · `RunBlocking.android.kt` | android | Compat seam: `runBlockingCompat` | iOS `RunBlocking.ios.kt` | mapped |
| `core/common` · `AppVersionProviderImpl.kt` | ios | `AppVersionProvider` | Android #1 | mapped |
| `core/common` · `NetworkStatusProviderImpl.kt` | ios | `NetworkStatusProvider` | Android #2 | mapped |
| `core/common` · `ApplicationSupport.kt` | ios | Compat seam: `applicationSupportDirectory()` (iOS only) | n/a, documented | mapped |
| `core/common` · `DateRendering.ios.kt` | ios | "`DateUtils` platform rendering" | Android #3 | mapped |
| `core/common` · `di/CommonPlatformModule.ios.kt` | ios | platform module | Android #4 | mapped |
| `core/data` · `di/DataPlatformModule.ios.kt` | ios | platform module | Android #5 | mapped |
| `core/data` · `local/WishlistCoverImageStorageImpl.kt` | ios | `WishlistCoverImageStorage` | Android #6 | mapped |
| `core/data` · `repository/RepositoryErrorMapper.ios.kt` | ios | "Connectivity classification" | Android #9 | mapped |
| `core/database` · `di/DatabasePlatformModule.ios.kt` | ios | platform module | Android #17 | mapped (note 1) |
| `core/designsystem` · `theme/SystemBarsAppearance.ios.kt` | ios | `SystemBarsAppearance` | Android #18 | mapped |
| `core/network` · `PlatformTransportFailure.ios.kt` | ios | "Connectivity classification" | Android #19 | mapped |
| `core/network` · `di/NetworkPlatformModule.ios.kt` | ios | platform module | Android #20 | mapped (note 1) |
| `core/ui` · `component/FullScreenDialogProperties.ios.kt` | ios | Compat seam | Android #21 | mapped |
| `core/ui` · `component/NotificationPermissionDeniedDialog.ios.kt` | ios | "Notification permission UI" | Android #22 | mapped |
| `core/ui` · `util/CoverImagePicker.ios.kt` | ios | `rememberCoverImagePicker` | Android #23 | mapped |
| `core/ui` · `util/NotificationPermission.ios.kt` | ios | "Notification permission UI" | Android #24 | mapped |
| `core/ui` · `util/TextSharer.ios.kt` | ios | `rememberTextSharer` | Android #25 | mapped |
| `core/ui` · `util/TopViewController.kt` | ios | Compat seam: `TopViewController` (iOS only) | n/a, documented | mapped |
| `feature/search` · `components/SearchBarScrollBehaviorCompat.ios.kt` | ios | Compat seam | Android #26 | mapped |
| `shared` · `MainViewController.kt` | ios | "Platform shell": iOS entry point | `:androidApp` `MainActivity` | mapped |
| `shared` · `RunBlocking.ios.kt` | ios | Compat seam: `runBlockingCompat` | Android #27 | mapped |

## Leads dismissed

| Lead | Evidence | Why dismissed or what it became |
|---|---|---|
| L1 `:androidApp → :core:domain` edge | Confirmed in `androidApp/build.gradle.kts:36` and resolved compile classpath (`:shared`, `:core:domain`). The call is intentional (the Android shell schedules the periodic refresh, as `MainViewController` does on iOS) and documented in `module-contracts.md` and `platform-contracts.md`. Only the root graph omits it (`dep.md`, `doc-claude.md`) | **Dismissed as architecture**; documentation point kept as C-DOC-3 |
| L2 `:androidApp → :core:navigation` | Not declared and not needed: `MainActivity` parses the intent into an `Int` (`pendingDeepLinkGameId`) and passes it to `QuestLogRoot`; the only `androidApp` imports of internal modules are `shared.*` and `core.domain.radar.ReleaseRefreshScheduler`. The code is right, `module-contracts.md:26` is stale | **Confirmed as a documentation finding**: C-DEP-1 |
| L3 `koin-core` in `:core:domain` | `module-contracts.md` ("External dependencies allowed per layer") explicitly allows `koin-core` for `:core:common` and `:core:domain`; the constitution forbids Koin only in feature modules, and no feature/UI module has it (`dep.md` R-DEP-08) | **Dismissed: intentional** (`module-contracts.md`, table row `:core:common`, `:core:domain`) |
| L4 `androidx` imports in `shared/.../QuestLogNavDisplay.kt` | Lines 3-18 import only `androidx.compose.*`, `androidx.lifecycle.viewmodel.navigation3.*` and `androidx.navigation3.*`, all multiplatform artifacts; no `android.*`, `androidx.activity`, `androidx.work` | **Dismissed** (`pure.md`) |
| L5 Stale `docs/tech-debt.md` sections | Both confirmed stale (`tech-debt.md:16-27`); the coverage sentences are also out of date | **Confirmed**: C-DOC-6, C-DOC-7 |
| L6 Untracked empty `src/main` dirs in `:core:model`, `:core:navigation` | Confirmed empty and untracked (0 files, 0 tracked); five more empty directories found, including a second schema directory | **Dismissed: local hygiene**, not mergeable state (`left.md`) |
| L7 iOS test not part of the root `test` task | Confirmed: root `test` depends on `testAndroidHostTest` only; the iOS test was run separately and passed (5 tests). `docs/tech-debt.md` "Test coverage gaps" says so | **Dismissed: known** |

## Reproducing this audit

Everything above is derived from the notes in `work/`: `baseline.md`, `rules.md`, `dep.md`, `pure.md`, `cap-mapping.md`,
`cap-contracts.md`, `place.md`, `build.md`, `left.md`, `dup.md`, `vis.md`, `doc-claude.md`, `doc-other.md`,
`verification.md`, `leads.md`, `classification.md`, `blockers.md`. The commands are in `quickstart.md` and in each note's
evidence column. On macOS, `git grep -E` does not support `\b`; the notes use plain `grep` or Python where a word
boundary matters.
