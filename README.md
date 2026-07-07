# Games Wishlist 🎮

**Games Wishlist** is a modern and modular Android application designed to track your video game collection, manage your backlog, and discover new titles using the **IGDB** APIs.

The project is built following the highest Android development standards, with a **Clean Architecture** and unidirectional data flow (**UDF**).

---

## ✨ Features

- **Advanced Search**: Explore the IGDB database to find your favorite titles.
- **Backlog Management**: Save games to your personal Wishlist.
- **Customized Details**: Add notes, set priority, and update the completion status for each game.
- **Local Persistence**: All your data is saved locally for quick offline consultation.
- **Modern UI**: Interface built entirely with **Jetpack Compose**, featuring Material Design 3 and smooth animations.
- **Search History**: Quick access to your latest searches.

---

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Navigation**: [AndroidX Navigation 3](https://developer.android.com/guide/navigation)
- **Asynchrony**: Kotlin Coroutines & Flow
- **Images**: [Coil](https://coil-kt.github.io/coil/)

---

## 🏗️ Architecture and Modularization

The project follows a **multi-module** structure to ensure scalability and separation of concerns:

### Feature Modules
- `:feature:search`: Search and filter management.
- `:feature:game-detail`: Detailed view and personal metadata management (notes, status, priority).
- `:feature:wishlist` & `:feature:lists`: Organization of user collections.

### Core Modules
- `:core:domain`: Contains pure business logic and Use Cases.
- `:core:data`: Repository implementation and data mapping between network and database.
- `:core:network`: Client for external APIs (IGDB).
- `:core:database`: Local persistence management with Room.
- `:core:ui`: Reusable Compose components and UI models.
- `:core:designsystem`: Design tokens, themes, colors, and icons.
- `:core:model`: Shared domain models between modules.

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
- Android Studio Ladybug or higher.
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

---

## 📄 License

This project is distributed under the MIT license. See the `LICENSE` file for details.

---

**Disclaimer**: *This app uses the IGDB APIs but is not officially affiliated with or endorsed by IGDB/Twitch.*
