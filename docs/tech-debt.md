# Technical debt

Known places where the codebase deviates from the rules in `CLAUDE.md`, plus technical risks worth
tracking. This file exists so those rules can stay strict without an agent either "fixing" legacy code
unasked or copying a deviation as if it were the convention.

**For AI agents:** do not fix anything listed here as a side effect of unrelated work, and do not treat
these patterns as the house style. Mention the relevant item if it blocks you, then move on.

**When an item is fixed, delete it.** No "fixed in", no note explaining what changed, no leftover row —
`git log` is the history, this file is only the present. It is meant to shrink until it is empty, and then
to be deleted.

Last audited: 2026-08-16.

## Cleanup pass in progress

An ordered pass over this list is underway on `develop`, one fix per commit; `git log` is the record of
what has already been done. Entries are deleted from this file as they are fixed, so whatever is still
written below is still true. Agreed order for the rest:

1. **Release signing** — on hold: blocked on the owner generating a keystore, and not being chased in the
   meantime.

Convention plugins, CI and the test-coverage gaps are deliberately last — see the KMP section in the root
`CLAUDE.md`, since a multiplatform move would rewrite the build logic anyway.

## Technical risks

- **Room migrations are deferred until release**: the database is deliberately pinned to `version = 1`
  with `.fallbackToDestructiveMigration(true)`, so every entity change wipes the device. That is the
  owner's decision while the app is unpublished — **it is not a bug to fix, and the version must not be
  bumped**. Before the first release: freeze the schema, decide where `Migration` objects live, and
  replace the blanket fallback. `exportSchema` is already on and `schemas/1.json` is checked in, which is
  the starting point.
- **Release is signed with the debug key**: `app/build.gradle.kts` still uses
  `signingConfigs.getByName("debug")`, so the APK cannot be distributed. Needs a real keystore read from a
  git-ignored `keystore.properties`.

## Infrastructure

- **No convention plugins**: no `build-logic`, no `buildSrc`. `compileSdk = 37`, `minSdk = 29` and Java 11
  are repeated by hand in all 15 module build files.
- **No CI** (`.github/` does not exist) and **no static analysis** (no detekt, ktlint, spotless,
  `.editorconfig`, or `lint {}` block).
- **Test coverage gaps**: no tests at all for `:feature:wishlist`, `:core:database` DAOs, or `:core:ui`
  mappers. In `:core:domain` only the `usecase/discover/` use cases are covered; the search, list and
  detail ones are not. In `:core:network` only `IgdbAuthManager` is covered —
  `IgdbHttpErrorInterceptor` and the API service are not. No Compose UI tests — `ui-test-junit4` is
  wired into `:app` but only the template `ExampleInstrumentedTest` exists. `app/src/test/ExampleUnitTest.kt`
  is also an untouched template.
