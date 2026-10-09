# R-DOC — Other documents

Baseline `b982bf11`. Covers `AGENTS.md`, `README.md`, `.specify/memory/constitution.md`, `docs/tech-debt.md`,
`docs/roadmap.md` and `specs/010-kmp-migration/contracts/*.md`. Each concrete claim was checked against the source.

## Claims confirmed

| Document | Claim checked | Result |
|---|---|---|
| `AGENTS.md` | Points at the root `CLAUDE.md`, the six directory files, `docs/tech-debt.md` | all exist |
| `README.md` | Module list, "every module except `:core:ai` and `:androidApp` is multiplatform", `:core:ai` reachable only from `:core:data`, the iOS run steps and the `xcodebuild` line, JDK 21, SDK 37, Koin/Ktor/Room/Coil/Navigation 3 | matches (the `xcodebuild` line was run: `BUILD SUCCEEDED`). The "Core Modules" list omits `:core:common` and `:core:navigation`; it did so on `develop` as well, so it is not a migration regression (not reported) |
| Constitution | 19 modules; Principle I edges; three-edit route registration; `KoinGraphTest` must stay green; `version = 1` with `fallbackToDestructiveMigration(true)` and a single schema; `SavedStateHandle` unused; secrets generated into `IgdbCredentials`; "Android must build and pass its tests" | matches (`dep.md`, `place.md`, `verification.md`) |
| `docs/roadmap.md` | iOS follow-ups: reminders, translation, distribution; `:core:ai` stays Android-only; `InProcessReleaseRefreshScheduler` once per 24 h at launch; `UnsupportedGameDescriptionTranslator` | matches `cap-contracts.md` |
| `docs/tech-debt.md` | Room migrations deferred (version 1, destructive fallback); release signed with the debug key (`signingConfig = signingConfigs.getByName("debug")`); no CI (`.github/` absent); `WishlistFormSheet` keeps `72.dp` and `48.dp` (`WishlistFormSheet.kt:203,215,264`); `ExampleUnitTest`/`ExampleInstrumentedTest` untouched templates; `:core:ai` has no test source set; iOS has one test class; no DAO tests (`:core:database` `NO-SOURCE`); Material 3 split with a pin and the `SearchBarScrollBehaviorCompat` seam | matches |
| 010 `platform-contracts.md` | All interface signatures (`ReleaseRefreshScheduler`, `ReleaseNotificationScheduler`, `ReleaseNotifier`, `ReleaseRemindersAvailability`, `WishlistCoverImageStorage`, `AppVersionProvider`, `NetworkStatusProvider`, `rememberCoverImagePicker`, `rememberTextSharer`, `renderLocalDate`) and the capability exception register | matches the code |
| 010 `module-contracts.md` | Convention plugin table, source layout table, allowed external dependencies per layer | matches |

## Candidates

| ID | Title | Detail |
|---|---|---|
| C-DOC-6 | `docs/tech-debt.md` "Cleanup pass in progress" is stale and its ordering contradicts the migration | `docs/tech-debt.md:16-27`: "An ordered pass over this list is underway on `develop`, one fix per commit", with only "Release signing" left, then "Convention plugins, CI and the test-coverage gaps are deliberately last … since a multiplatform move would rewrite the build logic anyway." The multiplatform move and the convention plugins have now happened, so the sentence describes the future of something that is done. The remaining content is a single on-hold item that the "Technical risks" section already covers (`Release is signed with the debug key`). Delete the section, as the file's own rule says ("When an item is fixed, delete it"). Severity low, follow-up, S |
| C-DOC-7 | `docs/tech-debt.md` test-coverage statements are out of date | `docs/tech-debt.md` "Test coverage gaps": "in `:core:ui` only `PlatformPickerMapper` is covered — the other mappers are not". `core/ui` now has `GameUiMapperTest`, `PlatformPickerMapperTest` and `HtmlUtilsTest` (24 tests): the migration added the first and the last. "In `:core:domain` only the `usecase/discover/` and `radar/` use cases are covered; the search, list, detail and translation ones are not": `GetListsUseCaseTest`, `DeleteListUseCaseTest`, `GetWishlistDetailUseCaseTest` (list), `SetGameStatusUseCaseTest` (detail) and three notification tests exist, and the list tests were already there on `develop`. The claim also names no `notification/` or `release/` coverage. Severity low, follow-up, S |
| C-DOC-8 | Constitution Principle V repeats the dead command and lists the wrong test source sets | `.specify/memory/constitution.md:98` `./gradlew :<module>:compileDebugKotlin` (see C-DOC-1); `:103-105` "The existing test source sets — `core/data`, `core/domain`, `core/network`, `core/ui`, `feature/{…}` and `androidApp` — MUST stay green" omits `core/common` (`DateUtilsTest`), `core/navigation` (`RoutesSerializationTest`) and `:shared` (`KoinGraphTest`, `RootViewModelTest`), which the same document elsewhere requires to stay green (`KoinGraphTest`), and lists `androidApp` whose only test is the untouched template. Severity low (the command part is C-DOC-1), follow-up, S |
| C-DOC-9 | Constitution lists the splash screen among capabilities that must be a shared contract; the 010 contracts class it as platform shell | See C-CAP-2 in `cap-contracts.md`. Severity low, follow-up, S |

(The other drift in migration documents is C-DEP-1 and C-PLACE-2, in `module-contracts.md`, and C-CAP-1 and the
`kotlin.uuid` point of C-DUP-1, in `platform-contracts.md`.)

## Rule outcomes

| Rule | Outcome | Evidence |
|---|---|---|
| R-DOC-02 | **violated, minor** | C-DOC-6 … C-DOC-9 above, plus the four migration-contract points |
| R-DOC-03 | **violated, minor** | C-DOC-2 (root `CLAUDE.md`) and C-DOC-6 (`tech-debt.md`) keep text about a state that ended; FR-014 of spec 010 required deleting such text in the same commit |
| R-DOC-04 | **pass** | Every "Technical risk" and "Infrastructure" entry still describes something that exists, apart from the coverage sentences in C-DOC-7 |
