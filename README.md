# Games Wishlist 🎮

**Games Wishlist** è un'applicazione Android moderna e modulare progettata per tracciare la propria collezione di videogiochi, gestire il proprio backlog e scoprire nuovi titoli sfruttando le API di **IGDB**.

Il progetto è costruito seguendo i più alti standard di sviluppo Android, con un'architettura **Clean** e un flusso di dati unidirezionale (**UDF**).

---

## ✨ Caratteristiche

- **Ricerca Avanzata**: Esplora il database IGDB per trovare i tuoi titoli preferiti.
- **Gestione Backlog**: Salva i giochi nella tua Wishlist personale.
- **Dettagli Personalizzati**: Aggiungi note, imposta la priorità e aggiorna lo stato di completamento per ogni gioco.
- **Persistenza Locale**: Tutti i tuoi dati sono salvati localmente per una consultazione offline rapida.
- **UI Moderna**: Interfaccia interamente in **Jetpack Compose** con supporto a Material Design 3 e animazioni fluide.
- **Cronologia di Ricerca**: Accesso rapido alle tue ultime ricerche effettuate.

---

## 🛠️ Tech Stack

- **Linguaggio**: [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Navigazione**: [AndroidX Navigation 3](https://developer.android.com/guide/navigation)
- **Asincronia**: Kotlin Coroutines & Flow
- **Immagini**: [Coil](https://coil-kt.github.io/coil/)

---

## 🏗️ Architettura e Modularizzazione

Il progetto segue una struttura **multi-modulo** per garantire scalabilità e separazione delle responsabilità:

### Moduli Feature
- `:feature:search`: Gestione della ricerca e dei filtri.
- `:feature:game-detail`: Visualizzazione dettagliata e gestione dei metadati personali (note, stato, priorità).
- `:feature:wishlist` & `:feature:lists`: Organizzazione delle collezioni dell'utente.

### Moduli Core
- `:core:domain`: Contiene la logica di business pura e gli Use Case.
- `:core:data`: Implementazione dei repository e mappatura dei dati tra rete e database.
- `:core:network`: Client per le API esterne (IGDB).
- `:core:database`: Gestione della persistenza locale con Room.
- `:core:ui`: Componenti Compose riutilizzabili e modelli UI.
- `:core:designsystem`: Token di design, temi, colori e icone.
- `:core:model`: Modelli di dominio condivisi tra i moduli.

---

## 📐 Principi di Sviluppo

- **Unidirectional Data Flow (UDF)**: Ogni schermata è gestita da un `UiState` unico esposto dal ViewModel tramite `StateFlow`.
- **Clean Architecture**: Netta separazione tra i dati (Data), la logica di business (Domain) e la presentazione (UI).
- **Mappers**: Trasformazione dei dati tra i vari livelli (Network -> Domain -> Database) per evitare accoppiamento.
- **Localization Ready**: Utilizzo di `UiText` per gestire le stringhe in modo agnostico dal contesto, facilitando la localizzazione.
- **Type Safety**: Navigazione basata su classi serializzabili per un passaggio di parametri sicuro tra le schermate.

---

## 🚀 Come iniziare

### Prerequisiti
- Android Studio Ladybug o superiore.
- Una API Key di [IGDB (Twitch Developers)](https://api-docs.igdb.com/).

### Installazione
1. Clona il repository:
   ```bash
   git clone https://github.com/tuo-username/games-wishlist.git
   ```
2. Inserisci le tue credenziali IGDB nel file `local.properties`:
   ```properties
   IGDB_CLIENT_ID=tua_client_id
   IGDB_CLIENT_SECRET=tuo_client_secret
   ```
3. Sincronizza il progetto con Gradle ed esegui l'app su un emulatore o dispositivo fisico.

---

## 📄 Licenza

Questo progetto è distribuito sotto la licenza MIT. Vedi il file `LICENSE` per dettagli.

---

**Disclaimer**: *Questa app utilizza le API di IGDB ma non è ufficialmente affiliata o approvata da IGDB/Twitch.*
