# Quickstart: Validating Cloud Backup and Restore

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

How to prove the feature works end to end. Contracts are in [contracts/](contracts/) and types in
[data-model.md](data-model.md); they are not repeated here.

## Prerequisites (one-off, owner)

**Firebase console** (project already created):
1. **Authentication → Sign-in method**: enable **Google**. Enable **Apple**, with the Services ID, Apple Team ID,
   key ID and private key from step 3. Leave **Settings → User account linking** on *one account per email
   address* (the default).
2. **Project settings → Your apps**: on the Android app, add the debug **SHA-1 and SHA-256** (and the release or
   Play App Signing ones before shipping). Then **download both config files again**: they only carry the OAuth
   client ids once Google sign-in is enabled.
   - `androidApp/google-services.json`
   - `iosApp/iosApp/GoogleService-Info.plist`, which must contain `CLIENT_ID` and `REVERSED_CLIENT_ID`
3. **Apple Developer** (paid membership): the App ID `com.nikolasguillen.questlog` with *Sign in with Apple*; a
   **Services ID** with the return URL `https://<project-id>.firebaseapp.com/__/auth/handler`; a **key** with Sign in
   with Apple (download the `.p8` once). Optionally register `noreply@<project-id>.firebaseapp.com` for the private
   email relay.
4. **Firestore Database**: create it in production mode in a region near the users. Paste
   `firebase/firestore.rules` into **Rules** and publish.

**Xcode**: `firebase-ios-sdk` resolved at 11.x (research R2) with `FirebaseAuth` and `FirebaseFirestore`;
`GoogleSignIn-iOS` 10.0.0 with `GoogleSignIn`; the Sign in with Apple capability; the `REVERSED_CLIENT_ID` URL type.

Two devices or a device plus a simulator, ideally one Android and one iOS. Test accounts: a Gmail Google account,
an Apple ID with the **same** Gmail address, and an Apple ID that hides its email.

## 1. Spike gate (before any feature code, research R13)

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -q
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build build
```

Run both apps. The debug log line from the spike must show Firebase reachable (current user `null`, manifest read
answered or denied by the rules) on **both** platforms. If iOS fails to link or crashes on the first Firebase call,
stop: the plan switches to native SDKs (research R1) before anything else is built.

## 2. Unit suites

```bash
./gradlew :core:data:testAndroidHostTest --console=plain -q      # mapper round trip, chunker, repositories, error mapping
./gradlew :core:cloud:testAndroidHostTest --console=plain -q     # Firebase code classification
./gradlew :core:domain:testAndroidHostTest --console=plain -q    # DeleteAccountUseCase order
./gradlew :feature:settings:testAndroidHostTest --console=plain -q   # AccountBackupViewModel
./gradlew :shared:testAndroidHostTest --console=plain -q         # KoinGraphTest with the new modules
./gradlew test                                                   # everything stays green
```

## 3. Static checks

- `:feature:*` build files do not mention `:core:cloud` (Principle I). Only `:core:data` and `:shared` do.
- No `dev.gitlive`, `com.google.firebase` or `androidx.credentials` import outside `:core:cloud`.
- No Firebase type in `:core:data`'s `RepositoryErrorMapper.kt`.
- `git ls-files | grep -iE 'google-services|GoogleService-Info'` prints nothing.
- No new `dp` literal, and every string is in `strings.xml`.

## 4. Hands-on (Android, then iOS)

| # | Steps | Expected | Spec |
|---|---|---|---|
| 1 | Fresh install, open Settings | "Account & backup" row, subtitle "Not signed in"; the app works fully | FR-001, FR-009 |
| 2 | Open it | Explainer of what is stored; Google and Apple buttons; no backup/restore actions | US1-1, FR-031 |
| 3 | Continue with Google, then cancel the sheet | Still signed out, no error message | FR-027 |
| 4 | Continue with Google, complete | Address and "Google" shown; "No backup yet" | US1, FR-009 |
| 5 | Save 3+ lists (one with a custom cover), 20+ games with statuses/priorities/notes, 2 owned platforms; Back up now | Busy state; then "Last backup" with date and time and "N lists, M games" | US1-2, FR-016 |
| 6 | Back up again after changing a game | Time moves forward; Firestore shows one manifest, and only the new snapshot's parts after the sweep | US1-3, FR-013 |
| 7 | Tap Back up now twice quickly | Only one backup runs | US1-5, FR-026 |
| 8 | Airplane mode, Back up now | "No connection" message; the manifest in the console is unchanged | Edge: no network, FR-014 |
| 9 | Second device (other platform), fresh install, sign in with **Apple using the same Gmail** | Same account: same last-backup time | US3-1, FR-004 |
| 10 | Restore on the empty device | No dialog; lists, cover, games, statuses, priorities, notes, default wishlist and owned platforms match device 1 | US2-1, US2-4, SC-003 |
| 11 | On device 1, add a list; Restore | Dialog says local data will be replaced; Cancel changes nothing; Confirm brings back the backed-up state | US2-2, US2-3, FR-019 |
| 12 | Start a restore of a large backup and kill the app mid-way (`adb shell am force-stop com.nikolasguillen.questlog`, or swipe away on iOS) | On relaunch the data is entirely old or entirely restored | US2-5, FR-020, SC-004 |
| 13 | Search history and Recently viewed before and after a restore | Unchanged | FR-022 |
| 14 | Sign in with the Apple ID that hides its email | A separate, empty account | US3-3 |
| 15 | From the Google account, Link Apple with the hidden-email Apple ID | Both methods listed; signing in with that Apple ID now shows the backup | US3-2, FR-005 |
| 16 | Try to link a Google account that already has its own backup account | "Already linked to another account" message; nothing changes | US3-4, FR-006 |
| 17 | Sign out | Signed-out state; local data unchanged; signing back in shows the backup | US4-1, FR-007 |
| 18 | Delete account, then cancel | Still signed in; backup unchanged | US4-4 |
| 19 | Delete account, confirm, complete the fresh sign-in | Signed out; local data unchanged; the console shows no `users/{uid}` and no parts; for an Apple account, Settings → Apple ID → Sign in with Apple no longer lists the app; signing in again shows "No backup yet" | US4-2, US4-3, FR-008, SC-006 |
| 20 | Device 1 empty (fresh install after delete), signed in, Back up now | "Replace your backup with an empty one?" dialog | FR-015 |
| 21 | With two test accounts, try to read the other's `users/{uid}` (Rules Playground in the console) | Denied | FR-029, SC-007 |
| 22 | Restore a manifest edited in the console to `formatVersion: 99` | "Update the app" message; nothing changes | FR-024 |
| 23 | Delete one part document in the console, then Restore | "Backup can't be read" message; nothing changes | Edge: unreadable |

Timing for SC-001 and SC-002 (500 saved games) is measured once on a real device on mobile data and recorded in the
PR description.

## 5. iOS (Mac only)

Everything in §4 is repeated on iOS. Items that only a device can show, such as the Apple ID settings check in row
19 and the kill test in row 12, are recorded as **not run** until they are run on a physical iPhone.
