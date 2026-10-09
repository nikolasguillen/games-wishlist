# Leads

Seeded from the "Leads carried into the audit" table in `research.md`. Resolved in T022. Baseline `b982bf11`.

| # | Lead | First observation | Resolution | Outcome |
|---|---|---|---|---|
| L1 | `:androidApp → :core:domain` edge | `QuestLogApp` imports `ReleaseRefreshScheduler`; listed in `module-contracts.md` but missing from the root CLAUDE.md graph | Confirmed in `androidApp/build.gradle.kts:36` and resolved compile classpath (`:shared`, `:core:domain`). The call is intentional (the Android shell schedules the periodic refresh, as `MainViewController` does on iOS) and documented in `module-contracts.md` and `platform-contracts.md`. Only the root graph omits it (`dep.md`, `doc-claude.md`) | **Dismissed as architecture**; documentation point kept as C-DOC-3 |
| L2 | `:androidApp → :core:navigation` | Listed in `module-contracts.md` for deep links, not declared in `androidApp/build.gradle.kts` | Not declared and not needed: `MainActivity` parses the intent into an `Int` (`pendingDeepLinkGameId`) and passes it to `QuestLogRoot`; the only `androidApp` imports of internal modules are `shared.*` and `core.domain.radar.ReleaseRefreshScheduler`. The code is right, `module-contracts.md:26` is stale | **Confirmed as a documentation finding**: C-DEP-1 |
| L3 | `koin-core` in `:core:domain` | `DomainKoin.kt` | `module-contracts.md` ("External dependencies allowed per layer") explicitly allows `koin-core` for `:core:common` and `:core:domain`; the constitution forbids Koin only in feature modules, and no feature/UI module has it (`dep.md` R-DEP-08) | **Dismissed: intentional** (`module-contracts.md`, table row `:core:common`, `:core:domain`) |
| L4 | `androidx` imports in `shared/.../QuestLogNavDisplay.kt` | Matched a naive pattern | Lines 3-18 import only `androidx.compose.*`, `androidx.lifecycle.viewmodel.navigation3.*` and `androidx.navigation3.*`, all multiplatform artifacts; no `android.*`, `androidx.activity`, `androidx.work` | **Dismissed** (`pure.md`) |
| L5 | Stale `docs/tech-debt.md` sections | "Cleanup pass in progress", "convention plugins deliberately last" | Both confirmed stale (`tech-debt.md:16-27`); the coverage sentences are also out of date | **Confirmed**: C-DOC-6, C-DOC-7 |
| L6 | Untracked empty `src/main` dirs in `:core:model`, `:core:navigation` | Local only | Confirmed empty and untracked (0 files, 0 tracked); five more empty directories found, including a second schema directory | **Dismissed: local hygiene**, not mergeable state (`left.md`) |
| L7 | iOS test not part of the root `test` task | Already in tech-debt | Confirmed: root `test` depends on `testAndroidHostTest` only; the iOS test was run separately and passed (5 tests). `docs/tech-debt.md` "Test coverage gaps" says so | **Dismissed: known** |

New leads found during the audit and resolved in the findings: the dead command names (C-DOC-1), the dead catalog
entries (C-LEFT-1), the cover-storage constants (C-DUP-1), the open spec 010 tasks (C-VER-1), and the unchecked
minified build (see `verification.md`, extra check).
