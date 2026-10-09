# Consolidation and classification

Baseline `b982bf11`. Applies the rubric of `research.md` R9 to every candidate in `work/*.md`.

Rubric, restated: **High** = breaks the Android or iOS build or tests, or violates a NON-NEGOTIABLE rule (constitution
Principle I, `commonMain` purity). **Medium** = violates another documented rule, or creates structure that later work
would build on. **Low** = hygiene or documentation drift that misleads but structures nothing. **Blocker** = any High,
plus any Medium that would be materially more expensive to fix once other branches build on `develop`.
`known`/`intentional` override the classification when the migration did not make the issue worse.

## Candidates → findings

| Candidates | Finding | Severity | Classification | Reasoning |
|---|---|---|---|---|
| C-DOC-1, C-DOC-8 (command part) | F-001 | medium | follow-up | A documented rule (R-DOC-01) and FR-014 of spec 010 are broken, and the dead commands mislead every contributor. The cost of fixing is the same before or after the merge (one docs edit), so it is not a blocker by the rubric. Suggested timing: with the merge, because 010 promised it |
| C-VER-1 | F-002 | medium | known | Open items already recorded in 010 `tasks.md` and `baseline.md`. Medium because of the Windows pair |
| C-DUP-1 | F-003 | low | follow-up | Three constants written twice; agrees today |
| C-VIS-1 | F-004 | low | follow-up | Pre-existing (0 `internal` on `develop`); extended by 7 classes |
| C-LEFT-1 | F-005 | low | follow-up | Dead catalog entries, no build effect |
| C-DOC-6, C-DOC-7 | F-006 | low | follow-up | `docs/tech-debt.md` stale |
| C-DOC-2, C-DOC-3 | F-007 | low | follow-up | Root `CLAUDE.md` |
| C-DOC-4, C-DOC-5 | F-008 | low | follow-up | Directory `CLAUDE.md` files |
| C-DOC-8 (rest), C-DOC-9 = C-CAP-2 | F-009 | low | follow-up | Constitution; amendment needs the owner |
| C-DEP-1, C-PLACE-2, C-CAP-1, `kotlin.uuid` point of C-DUP-1 | F-010 | low | follow-up | 010 contracts out of step with code |
| C-BUILD-1, C-BUILD-2 | F-011 | low | follow-up | Build-file leftovers |
| C-PLACE-1 | F-012 | low | follow-up (optional) | Pre-existing trade-off |

Merged (seen from two angles): C-DOC-8's command part and C-DOC-1; C-DOC-9 and C-CAP-2; the `kotlin.uuid` contract
drift (found in `dup.md`) with the 010 contract drift of F-010.

## Dismissed or recorded as known / intentional (no finding)

| Item | Disposition | Source |
|---|---|---|
| L1 `:androidApp → :core:domain` | intentional | `module-contracts.md` ("Edges that change"); documentation point kept in F-007 |
| L3 `koin-core` in `:core:domain` | intentional | `module-contracts.md` "External dependencies allowed per layer" |
| L4 `androidx` imports in `QuestLogNavDisplay.kt` | dismissed | `pure.md` |
| L6 empty `src/main` directories and the empty second schema directory | local hygiene, not merged | `left.md` |
| L7 iOS tests outside the root `test` task; iOS Koin graph not covered by a JVM test | known | `docs/tech-debt.md` "Test coverage gaps" |
| `iosX64` target absent | intentional | `specs/010-kmp-migration/research.md:339` |
| Android `compileSdk`/`namespace` in `:core:ai` and `:androidApp` | intentional | `module-contracts.md` "Unchanged" |
| `runBlockingCompat` has identical `actual`s | intentional seam | `platform-contracts.md` "Compatibility seams" |
| iOS reminders, translation, background refresh, deep link absent | intentional | `platform-contracts.md` "Capability exception register", `docs/roadmap.md` |
| Material 3 version split, `SearchBarScrollBehaviorCompat` | known | `docs/tech-debt.md` |
| Template tests in `:androidApp` | known | `docs/tech-debt.md` |
| Room `version = 1` with destructive fallback; schema byte-identical to `develop` | intentional | `docs/tech-debt.md`, `core/database/CLAUDE.md` |
| Release signed with the debug key | known | `docs/tech-debt.md` |
| `android.disallowKotlinSourceSets=false` experimental flag | observation (pre-existing, undocumented) | `build.md` |
| Gradle metadata view shows `:core:network` implementation dependencies under dependents | observation, no boundary crossed in source | `dep.md` |

## Result

0 High findings. 0 Medium findings that get costlier after the merge. **0 blockers.** 2 medium (F-001, F-002), 10 low.
