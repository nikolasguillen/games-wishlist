# Blockers

Baseline `b982bf11`.

**None.** The rubric (`research.md` R9) makes a finding a blocker when it is High, or Medium and materially more
expensive to fix once other work builds on `develop`. No finding is High: the Android build, 535 JVM tests, the iOS
test, the iOS framework link, the Xcode build and the minified release build all succeed, and no dependency, purity,
capability or placement rule is violated. Neither Medium finding gets more expensive after the merge.

For the owner, though, two Medium findings deserve a decision before the merge button is pressed. They are not blockers
under the rubric:

| Finding | Why it still matters now | Fix | Size | Owner decision |
|---|---|---|---|---|
| F-001 documented compile/test commands no longer exist | Spec 010 FR-014 and `research.md:327` required the commands to be updated "in the same commit"; the branch is the one that made them stale. Fixing it on the branch keeps `develop` from inheriting wrong instructions | Edit `CLAUDE.md` (3 lines) and the constitution Principle V (PATCH amendment 1.3.3) | S | The constitution edit needs the owner's approval |
| F-002 Windows build unverified | The project is also developed on Windows; its configuration (convention plugins, `kotlin.native.ignoreDisabledTargets`, KSP per target) has never been run there | Run two commands on the Windows machine and record them | S (owner's time) | Merge first and check on Windows, or hold the merge for it |
