# Games Wishlist 🎮

**Games Wishlist** is a modern and modular Kotlin Multiplatform application (Android and iOS) designed to track your video game collection, manage your backlog, and discover new titles using the **IGDB** APIs.

The project is built following the highest Android development standards, with a **Clean Architecture** and unidirectional data flow (**UDF**).

---

## ✨ Features

- **Advanced Search**: Explore the IGDB database to find your favorite titles.
- **Backlog Management**: Save games to your personal Wishlist.
- **Customized Details**: Add notes, set priority, and update the completion status for each game.
- **Local Persistence**: All your data is saved locally for quick offline consultation.
- **Modern UI**: Interface built entirely with **Compose Multiplatform**, shared by both platforms, featuring Material Design 3 and smooth animations.
- **Search History**: Quick access to your latest searches.

---

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) Multiplatform (Android, iOS)
- **UI**: [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) with Material 3
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Networking**: [Ktor](https://ktor.io/) client & kotlinx.serialization
- **Database**: [Room](https://developer.android.com/kotlin/multiplatform/room) with the bundled SQLite driver
- **Navigation**: [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- **Asynchrony**: Kotlin Coroutines & Flow
- **Images**: [Coil 3](https://coil-kt.github.io/coil/)
- **Background work (Android)**: WorkManager

---

## 🏗️ Architecture and Modularization

The project follows a **multi-module** structure to ensure scalability and separation of concerns. Every module
except `:core:ai` and `:app` is multiplatform; platform code lives in `androidMain` and `iosMain` behind the
contracts shared code defines.

### App Modules
- `:app`: The Android entry point (activity, `Application`, manifest).
- `:shared`: Navigation, the app root, Koin assembly and the iOS framework `iosApp/` embeds.
- `iosApp/`: The Xcode project that hosts the shared UI on iOS.

### Feature Modules
- `:feature:search`: Search and filter management.
- `:feature:game-detail`: Detailed view and personal metadata management (notes, status, priority).
- `:feature:wishlist` & `:feature:lists`: Organization of user collections.
- `:feature:radar`, `:feature:settings` & `:feature:onboarding`: Release timeline, preferences and the welcome flow.

### Core Modules
- `:core:domain`: Contains pure business logic and Use Cases.
- `:core:data`: Repository implementation and data mapping between network and database.
- `:core:network`: Client for external APIs (IGDB).
- `:core:database`: Local persistence management with Room.
- `:core:ui`: Reusable Compose components and UI models.
- `:core:designsystem`: Design tokens, themes, colors, and icons.
- `:core:model`: Shared domain models between modules.
- `:core:ai`: Android-only on-device translation (ML Kit GenAI), reachable only from `:core:data`.

---

## 📐 Development Principles

- **Unidirectional Data Flow (UDF)**: Each screen is managed by a unique `UiState` exposed by the ViewModel via `StateFlow`.
- **Clean Architecture**: Clear separation between data (Data), business logic (Domain), and presentation (UI).
- **Mappers**: Data transformation between layers (Network -> Domain -> Database) to avoid coupling.
- **Localization Ready**: Use of `UiText` to manage strings context-agnostically, facilitating localization.
- **Type Safety**: Navigation based on serializable classes for safe parameter passing between screens.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio with the Android SDK 37 (JDK 21 is used for the build).
- An API Key from [IGDB (Twitch Developers)](https://api-docs.igdb.com/).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/nikolasguillen/games-wishlist
   ```
2. Enter your IGDB credentials in the `local.properties` file:
   ```properties
   IGDB_CLIENT_ID=your_client_id
   IGDB_CLIENT_SECRET=your_client_secret
   ```
3. Sync the project with Gradle and run the app on an emulator or physical device.

### Run on iOS

Needs macOS with Xcode. iOS runs without release reminders and on-device translation for now (see `docs/roadmap.md`).

1. Put the IGDB credentials in `local.properties` as above; the Gradle build bakes them into the framework.
2. Open `iosApp/iosApp.xcodeproj` in Xcode, pick an iPhone simulator and run. The "Compile Kotlin Framework" build
   phase builds the shared module with Gradle, so the first build takes a few minutes.
3. From a terminal: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build`.

Installing on a physical device (signing, TestFlight) is not set up yet.

---

## 📄 License

This project is distributed under the MIT license. See the `LICENSE` file for details.

---

**Disclaimer**: *This app uses the IGDB APIs but is not officially affiliated with or endorsed by IGDB/Twitch.*
