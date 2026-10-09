# Data Model: KMP Architecture Audit Before Merge

The audit produces records, not code. These are the records, their fields, and the rules that make a report valid.
They map one-to-one onto the spec's Key Entities. The report layout is in
[contracts/audit-report.md](contracts/audit-report.md).

## Rule

A documented architectural constraint that gets checked.

| Field | Content |
|---|---|
| `id` | `R-<AREA>-NN`, where AREA is one of `DEP` (dependency edges), `PURE` (shared-code purity), `CAP` (capability contracts), `PLACE` (where things live), `BUILD` (build configuration), `LEFT` (leftovers), `DUP` (duplication), `DOC` (documentation), `VER` (verification) |
| `statement` | The rule in one sentence |
| `source` | File and section it comes from (research R1) |
| `outcome` | `pass` · `violated` · `not verifiable` |
| `evidence` | What was examined: the command run, the files read, the grep used |
| `findings` | IDs of the findings it produced (empty when `pass`) |

**Validation**:
- Every rule has an outcome and evidence (SC-001).
- `violated` implies at least one finding.
- `not verifiable` carries a reason.

## Finding

One architectural issue.

| Field | Content |
|---|---|
| `id` | `F-NNN`, numbered when the report is assembled. Candidates in `work/*.md` carry a provisional `C-<AREA>-n` ID until then |
| `title` | Short, specific |
| `location` | Module plus file path(s) and line(s), or a build file or document |
| `rule` | The rule ID it breaks, or `intent:` plus the 010 document it contradicts |
| `impact` | What goes wrong if it is left as is |
| `severity` | `high` · `medium` · `low` (research R9) |
| `classification` | `blocker` · `follow-up` · `known` · `intentional` |
| `evidence` | Enough for a reader to reproduce it (FR-015) |
| `commit` | The SHA it was checked against (research R2) |
| `fix` | Recommended fix. **Required** for blockers |
| `fix_size` | `S` · `M` · `L`. **Required** for blockers |
| `owner_decision` | The question the owner has to answer, if the fix is hard to undo or has options (FR-011) |
| `known_ref` | The tech-debt or roadmap entry, or the 010 document and section, when `known` or `intentional` |

**Validation**:
- `blocker` requires `fix` and `fix_size` (SC-002).
- `known` and `intentional` require `known_ref`.
- `high` implies `blocker`, unless the finding is `known` and the migration did not make it worse.

**Lifecycle**:

```text
lead ──check──► candidate ──evidence──► confirmed ──rubric──► blocker | follow-up | known | intentional
                    │
                    └──► dismissed (logged in the lead table with its evidence; never silently dropped)
```

## Capability mapping

One row per git-tracked platform source file (research R5).

| Field | Content |
|---|---|
| `file` | Path under `src/androidMain` or `src/iosMain` |
| `platform` | `android` · `ios` |
| `maps_to` | Contract name, compatibility seam, platform-shell row, or `*PlatformModule` |
| `counterpart` | The other platform's file for the same contract, or `none (documented)` |
| `status` | `mapped` · `unmapped` (→ finding) · `counterpart missing` (→ finding) |

## Verification result

| Field | Content |
|---|---|
| `command` | Exactly as run |
| `status` | `passed` · `failed` · `not run` |
| `reason` | Required when `not run` |
| `notes` | Failing tests or tasks; the finding ID when `failed` |

**Validation**:
- `failed` produces a `high` finding (spec User Story 4, scenario 3).
- Nothing is `passed` that was not run (SC-005).

## Verdict

| Field | Content |
|---|---|
| `decision` | `merge` · `do not merge yet` |
| `blockers` | Finding IDs. Must be empty if and only if the decision is `merge` |
| `follow_ups` | Finding IDs |
| `unverified` | Rules or commands that are `not verifiable` or `not run` |
| `commit` | Baseline SHA |

The verdict is derived from the findings, never chosen separately (FR-013).
