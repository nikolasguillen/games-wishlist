# Contract: Audit Report

The single deliverable of this feature is `specs/011-kmp-architecture-audit/audit-report.md`. This file fixes its
layout, so the owner can reach the merge decision in one reading (SC-003) and a later audit can be diffed against
it. Field definitions are in [data-model.md](../data-model.md).

## Layout (in this order)

```markdown
# KMP Architecture Audit: 010-kmp-migration

**Baseline**: `<sha>` on `010-kmp-migration` · **Audited**: <date> · **Environment**: macOS <version>, Xcode <version>

## Verdict

**<Merge | Do not merge yet>.** <One sentence on why.>

Blockers (must be fixed before merging):
- F-00x: <title> (<fix size>)

Owner decisions needed: <list, or "none">

Not verified: <list, or "none">

## Verification

| Command | Status | Notes |
|---|---|---|

## Findings

### Blockers
#### F-00x: <title>
- **Location** · **Rule** · **Severity** · **Impact** · **Evidence** · **Fix** (size) · **Owner decision**

### Follow-ups
(same fields; Fix and size optional)

### Known and intentional
(one line each, with known_ref)

## Rule ledger

| Rule | Statement | Source | Outcome | Evidence | Findings |
|---|---|---|---|---|---|

## Capability mapping

| File | Platform | Maps to | Counterpart | Status |
|---|---|---|---|---|

## Leads dismissed

| Lead | Evidence | Why dismissed |
|---|---|---|
```

## Rules

- The verdict comes first and is consistent with the Blockers section: blockers are listed if and only if the
  verdict is "Do not merge yet".
- Every rule in the ledger has an outcome, and every `violated` row links to at least one finding.
- Every finding's evidence lets someone else reproduce it: the command, grep or file and line.
- No finding is phrased as already fixed. The audit changes nothing outside this spec directory (FR-014).
- Findings are written in English, like all project documentation.
