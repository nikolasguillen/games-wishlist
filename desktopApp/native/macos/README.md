# macOS notifications library

`QuestLogNotifications.swift` is the C ABI that the desktop app calls for release reminders on macOS (specs/020-desktop-app,
research R9). It is built into `libQuestLogNotifications.dylib` by the `buildMacNotifications` Gradle task, which runs
only on macOS and needs the Xcode command line tools (`swiftc`):

```bash
./gradlew :desktopApp:buildMacNotifications
```

The library lands in `desktopApp/build/native-resources/macos-arm64/` (Apple silicon), the resources root of the
Compose application, so `run`, `runDistributable` and `packageDmg` all pick it up. It is never committed.

`UNUserNotificationCenter` works only inside an app bundle: from `:desktopApp:run` the library reports "unsupported" and
the app shows notifications as blocked. Use `:desktopApp:runDistributable` to try reminders on a Mac.
