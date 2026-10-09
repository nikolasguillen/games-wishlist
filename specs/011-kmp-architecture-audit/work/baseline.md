# Baseline

- Baseline SHA (010-kmp-migration): `b982bf11`
- Audit branch HEAD: `33c74324` on `011-kmp-architecture-audit`
- Date: 2026-10-09

## Environment
```
ProductName:		macOS
ProductVersion:		26.6.2
BuildVersion:		25G83
Xcode 27.0
Build version 27A266a
Gradle 9.7.1
Kotlin:        2.4.0
Launcher JVM:  17.0.19 (Amazon.com Inc. 17.0.19+10-LTS)
Daemon JVM:    Compatible with Java 21, any vendor, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)
```

## git diff --stat 010-kmp-migration..HEAD
```
 .../checklists/requirements.md                     |  39 ++++
 .../contracts/audit-report.md                      |  65 ++++++
 specs/011-kmp-architecture-audit/data-model.md     |  93 +++++++++
 specs/011-kmp-architecture-audit/plan.md           | 146 +++++++++++++
 specs/011-kmp-architecture-audit/quickstart.md     |  74 +++++++
 specs/011-kmp-architecture-audit/research.md       | 218 ++++++++++++++++++++
 specs/011-kmp-architecture-audit/spec.md           | 228 +++++++++++++++++++++
 specs/011-kmp-architecture-audit/tasks.md          | 209 +++++++++++++++++++
 8 files changed, 1072 insertions(+)
```

## git status --short
```
?? specs/011-kmp-architecture-audit/work/
```
