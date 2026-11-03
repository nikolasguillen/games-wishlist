---
name: backlog
description: Capture raw notes (bugs, feature ideas, tech debt, improvements) into QuestLog's docs/backlog.md, rewording each one into a clear, detailed, code-grounded entry first. Use whenever the user pastes or dictates loose to-do points, Google Keep notes, "things to fix", "ideas", or says "add to the backlog", "note this down", "I found a bug", "remember to…", or wants to list, triage or clean up the backlog — even if they never say "backlog" and even when the text is a messy one-liner or in another language.
---

# Backlog

The user keeps loose points (bugs, ideas, chores) and wants them in `docs/backlog.md` instead of Google Keep.
A raw note like "search crashes sometimes" is useless in three weeks, so the value of this skill is turning
each note into an entry that a future session, with no memory of today, can act on.

## Why a separate file

`docs/tech-debt.md` is only for deviations from `CLAUDE.md` and known technical risks, and agents are told
not to touch what is listed there. `docs/roadmap.md` is only for features whose shape was already decided.
The backlog is the inbox before either: unrefined, unscheduled, unprioritised. Do not write to those two
files from this skill. If an item clearly belongs in one, say so and let the user decide to move it.

## Workflow

For every batch of raw points the user gives you:

1. **Split** the input into individual items. One note may hold two unrelated things; make them two entries.
2. **Classify** each as `Bug`, `Feature`, `Tech debt` or `Improvement` (see the field templates below). If the
   category is a genuine coin flip, pick the likelier one and flag it in the draft rather than interrupting.
3. **Ground it in the code.** Spend a few quick lookups per item (the knowledge graph tools first, as the
   repo's `CLAUDE.md` says, then read the source) to find the module, screen or file it concerns. A pointer like
   `feature/search/SearchViewModel.kt` is the single most useful detail in an entry. Do not guess: if you cannot
   find it quickly, write "location unknown" instead of inventing a path. Never change code from this skill.
4. **Check for duplicates** in `docs/backlog.md`, `docs/tech-debt.md` and `docs/roadmap.md`. If the point is
   already covered, tell the user and propose merging or skipping instead of adding a second entry.
5. **Reword** into the entry format. Keep the user's intent and wording of facts; add structure and detail,
   never invented facts. Anything you inferred and the user did not say (a suspected cause, an assumed
   platform) goes in a field marked as a hypothesis, so it is not read as a confirmed fact.
6. **Show the draft and wait for approval.** Present all the entries together, with a short list of open
   questions at the end (missing repro steps, which platform, expected behaviour). Write nothing to the file
   until the user approves or edits. The user said wrong guesses should not end up in the file.
7. **Write** the approved entries into `docs/backlog.md` under the right section, newest last. Create the file
   from the skeleton below if it does not exist. Report what was added in one or two lines.

If a point is too vague to reword usefully (no hint of where or what), ask one targeted question for it in
the draft step instead of padding the entry with filler.

## Language and tone

Write entries in **English**, whatever language the user wrote in; the repo's `CLAUDE.md` requires all
documentation in English. Be concrete and short: a title that states the problem or the capability, and fields
that each carry information. Do not hardcode user-facing strings or restate project rules in an entry.

## Entry format

Each entry is an `###` heading under its section, followed by bullets. Omit a field that has nothing real in
it rather than writing "N/A".

**Bug**

```markdown
### <Symptom in one line, e.g. "Search shows stale results after clearing the query">
- **Added:** YYYY-MM-DD
- **Where:** <module / screen / file, or "location unknown">
- **Platform:** Android | iOS | Desktop | all | unknown
- **Steps:** <numbered or one-line repro, as far as known>
- **Expected / Actual:** <what should happen vs what happens>
- **Hypothesis:** <suspected cause, only if there is one; say it is unverified>
```

**Feature**

```markdown
### <The capability, e.g. "Filter Radar by platform">
- **Added:** YYYY-MM-DD
- **Why:** <the user problem or motivation>
- **Scope:** <rough shape: what it includes, and what it deliberately does not>
- **Where:** <modules likely touched>
- **Notes:** <related roadmap phase, dependencies, open design questions>
```

**Tech debt**

```markdown
### <What is wrong in the code, e.g. "GameRepositoryImpl mixes mapping and caching">
- **Added:** YYYY-MM-DD
- **Where:** <files / modules>
- **Problem:** <why it hurts: risk, duplication, cost>
- **Suggested fix:** <direction, if the user gave one or it is obvious>
```

**Improvement** (existing behaviour or UX that works but could be better)

```markdown
### <The change, e.g. "Show a skeleton while the game detail loads">
- **Added:** YYYY-MM-DD
- **Where:** <screen / module>
- **Now → Better:** <current behaviour and the desired one>
- **Why:** <what it improves>
```

Use today's date for `Added`. Look it up from the environment instead of guessing.

## File skeleton

If `docs/backlog.md` is missing, create it with this header and the four sections, in this order:

```markdown
# Backlog

Unrefined points: bugs, ideas, chores and improvements that are not scheduled. This is the inbox before
`docs/tech-debt.md` (deviations from `CLAUDE.md`) and `docs/roadmap.md` (features with an agreed shape).

**For AI agents:** these are notes, not tasks. Do not start on an item unless the user asked for it.

**When an item is done or dropped, delete it.** No "fixed in" note; `git log` is the history.

## Bugs

## Features

## Tech debt

## Improvements
```

## Other requests

- **"What's in the backlog?" / "show the bugs"**: read the file and summarise it, grouped by section. Do not
  edit anything.
- **"X is done" / "drop Y"**: delete that entry (after confirming which one if the match is ambiguous).
- **"Move X to the roadmap / tech debt"**: delete it here and tell the user which file and section it now
  needs; only write to that file if they ask.
- **A commit**: only if the user asks. The message is `docs(docs): …` per the repo's convention.
