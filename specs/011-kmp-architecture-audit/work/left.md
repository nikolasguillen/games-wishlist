# R-LEFT — Migration leftovers

Baseline `b982bf11`. **Tracked files only** (research R6): `git ls-files`. Untracked and ignored local state is listed
separately at the end and is never a finding, because it cannot be merged.

## Rule outcomes (tracked)

| Rule | Outcome | Evidence |
|---|---|---|
| R-LEFT-01 | **pass** | `git ls-files` shows no `src/main/` file in any `core/`, `feature/` or `shared/` module except `core/ai` (an Android library, by design). `androidApp/src/main` is the Android shell |
| R-LEFT-02 | **pass** | Android `res/` is tracked only in `androidApp/src/main/res` (21 files: launcher, splash, themes, colours, backup rules) and `core/data/src/androidMain/res` (1 file, the notification strings, documented). Every other module's strings and drawables are under `src/commonMain/composeResources` (core/ui 8, feature/* 1–2 each, shared 1) |
| R-LEFT-03 | **violated, minor** | 12 entries in `gradle/libs.versions.toml` are referenced by no build file or convention plugin (below). All 12 were in use on `develop`, so the migration orphaned them. The removed libraries themselves (Hilt, Dagger, Retrofit, Moshi, kapt, OkHttp logging, Coil 2, `shadowglow`) appear nowhere in the catalog or build files |
| R-LEFT-04 | **pass** | No hand-written `android {}` block repeating convention-owned settings in a KMP module (see `build.md`) |
| R-LEFT-05 | **pass** (known) | Two template files remain, both already listed in `docs/tech-debt.md`: `androidApp/src/test/.../ExampleUnitTest.kt`, `androidApp/src/androidTest/.../ExampleInstrumentedTest.kt` |
| R-LEFT-06 | **pass** | The only tracked `src/test` is `androidApp/src/test` (the template above). Every KMP module's tests are under `src/androidHostTest` (and `core/network` `src/iosTest`) |

## C-LEFT-1: 12 unused version-catalog entries

Method: Python scan of every tracked `*.gradle.kts`, `*ConventionPlugin.kt` and `ProjectExtensions.kt` for the exact
`libs.<alias>` accessor and the convention plugins' `lib("<alias>")` calls (an accessor that is only a prefix of a longer
one, such as `libs.ktor.client.okhttp` for `okhttp`, does not count). Compared with `develop` the same way.

| Entry | Used on `develop` | Used now |
|---|---|---|
| `androidx-lifecycle-viewmodel-compose` | yes | no (the JetBrains `jetbrains-lifecycle-viewmodel-compose` replaced it) |
| `androidx-lifecycle-runtime-compose` | yes | no (replaced by the JetBrains one) |
| `androidx-lifecycle-viewmodel-navigation3` | yes | no (replaced by `jetbrains-lifecycle-viewmodel-navigation3`) |
| `androidx-navigation3-ui` | yes | no (replaced by `jetbrains-navigation3-ui`) |
| `androidx-compose-material-icons-core`, `-extended` | yes | no (replaced by `jetbrains-material-icons-*`) |
| `androidx-compose-ui-graphics`, `androidx-compose-ui-tooling-preview` | yes | no |
| `androidx-compose-adaptive`, `-adaptive-layout`, `-adaptive-navigation3` | yes (declared in `:app`) | no. Already unused by any source on `develop`; `specs/010-kmp-migration/research.md:353` said "mention, don't fix", and the migration then dropped the dependency lines from `:androidApp` |
| `okhttp` (`com.squareup.okhttp3:okhttp`) | yes | no (Ktor's OkHttp engine is used instead) |

Once those are deleted, the version entries only they used become orphans too (the tool found 0 unreferenced versions
now, because the dead libraries still reference them).

Impact: none on the build or the app. It leaves a catalog that suggests the project uses both the AndroidX and the
JetBrains lifecycle/navigation artifacts, which is the exact confusion the migration set out to remove. Severity low,
follow-up, size S (one commit deleting the lines and their orphaned versions).

## Local state (not tracked, not a finding)

| Item | State |
|---|---|
| `core/model/src/main/`, `core/navigation/src/main/` | empty directories (Git does not track them) |
| `core/database/schemas/com.nikolasguillen.questlog.core.database.GamesWishlistDatabase/` | empty directory; the rule in `core/database/CLAUDE.md` says "exactly one directory in `schemas/`", and Git tracks exactly one (`…QuestLogDatabase/1.json`). Delete the empty one locally |
| `androidApp/src/main/res/mipmap-anydpi`, `androidApp/src/test/java/.../di`, `core/data/src/commonMain/kotlin/.../data/worker` | empty directories |
| `build/`, `captures/`, `.gradle/`, `.kotlin/`, `.idea/`, `.agent/`, `.code-review-graph/`, `local.properties` | git-ignored |

## Candidates

| ID | Title |
|---|---|
| C-LEFT-1 | 12 unused entries in `gradle/libs.versions.toml` orphaned by the migration (low, follow-up, S) |
