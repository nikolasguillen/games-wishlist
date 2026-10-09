# Quickstart: Running and Validating the Audit

How to run this audit's checks and confirm the report meets the spec. The method behind each step is in
[research.md](research.md), and the report layout is in [contracts/audit-report.md](contracts/audit-report.md).

## Prerequisites

- A Mac with Xcode and an iOS simulator runtime. Without one, the iOS rows are recorded as **not run**; they are
  not skipped silently.
- `local.properties` with the IGDB credentials (the build generates `IgdbCredentials` from them).
- A clean working tree on `011-kmp-architecture-audit`. Record the baseline:

```bash
git rev-parse --short 010-kmp-migration        # baseline SHA for every finding (research R2)
git diff --stat 010-kmp-migration..HEAD        # must touch only specs/011-kmp-architecture-audit/
```

## 1. Verification commands (research R10)

Run each one and record its status in the report's Verification table:

```bash
./gradlew :androidApp:assembleDebug
./gradlew test                                            # includes KoinGraphTest and every androidHostTest
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q  # also the authority for commonMain purity (R4)
./gradlew :core:network:iosSimulatorArm64Test --console=plain -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

**Expected**: all of them pass. A failure becomes a High finding and a blocker.

## 2. Structural checks (locators)

These find candidates. Each candidate is then confirmed in source before it becomes a finding.

```bash
# R3: declared edges, from module files and convention plugins
grep -oE 'project\(":[a-z:-]+"\)' */build.gradle.kts */*/build.gradle.kts build-logic/convention/src/main/kotlin/*.kt
./gradlew :androidApp:dependencies --configuration debugCompileClasspath | grep 'project :'   # confirm when in doubt

# R4: platform imports in shared code (androidx.* is checked per artifact, not by pattern)
grep -rnE '^import (android|java|javax)\.' $(git ls-files '*/src/commonMain/*.kt')
grep -rnE '^import androidx\.' $(git ls-files '*/src/commonMain/*.kt') | sort -u -t: -k3

# R5: platform sources to map against platform-contracts.md
git ls-files '*/src/androidMain/*.kt' '*/src/iosMain/*.kt'
grep -rnE '\b(expect|actual) ' $(git ls-files '*.kt')

# R6: tracked leftovers
git ls-files '*/src/main/*' | grep -vE '^(androidApp|core/ai|build-logic)/'
```

**Expected**: the first `grep` under R4 returns nothing, and every file listed under R5 maps to a row.

## 3. Validating the report

The report is complete when all of these hold:

| Check | Spec reference |
|---|---|
| The verdict is at the top, and its blocker list matches the Blockers section exactly | FR-013, SC-003 |
| Every rule in the ledger has an outcome and evidence; every `violated` row links a finding | SC-001 |
| Every blocker has a fix and a size; every finding has every FR-010 field | FR-010, FR-011, SC-002 |
| Every verification row is `passed`, `failed` or `not run` with a reason | FR-009, SC-005 |
| Every lead in research.md appears either as a finding or in "Leads dismissed" | FR-012 |
| `git status` shows changes only under `specs/011-kmp-architecture-audit/` | FR-014, SC-006 |

## 4. After the owner's decision (out of this plan)

The owner picks which blockers to fix. Those fixes are separate commits that follow the project's commit
convention. Each one keeps `./gradlew :androidApp:assembleDebug` and `./gradlew test` green, and the documentation
fixes delete stale rules rather than annotating them. Re-running sections 1–2 afterwards must report zero blockers
(SC-004).
