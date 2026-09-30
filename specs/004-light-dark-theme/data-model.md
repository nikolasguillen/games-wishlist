# Data Model: Light Appearance & Theme Preference

**Feature**: `004-light-dark-theme` | **Date**: 2026-09-30

## `AppearanceMode` (new, `core/model`)

The user's chosen theme mode. Corresponds to the spec's **Appearance Preference** entity.

| Property | Type | Notes |
|---|---|---|
| `id` | `Int` | Stable identifier written to DataStore. Never renumber existing entries — same discipline as `GameStatus.id`. |

| Constant | `id` | Meaning |
|---|---|---|
| `SYSTEM` | `0` | Follow the device's system-level light/dark setting. Default for every install (spec Clarifications). |
| `LIGHT` | `1` | Always render the app's light surfaces. |
| `DARK` | `2` | Always render the app's dark surfaces (today's only look). |

```kotlin
enum class AppearanceMode(val id: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        /** Falls back to [SYSTEM] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): AppearanceMode = entries.find { it.id == id } ?: SYSTEM
    }
}
```

No relationships to other domain entities — it is a single, standalone, device-local value. No lifecycle /
state machine: it is read, and it is overwritten; there is no transition it can be in that isn't just "the
current value."

## Persistence shape

- **Store**: Jetpack DataStore Preferences (see research.md §1), one file, one key.
- **Key**: `intPreferencesKey("appearance_mode")`, value = `AppearanceMode.id`.
- **Read**: absent key (first launch, or a DataStore file created before this feature existed) resolves to
  `AppearanceMode.SYSTEM` via `AppearanceMode.fromId(storedIdOrDefault)`.
- **Write**: whole-value overwrite on every selection change — no partial updates, no history.

## Derived (not persisted) value: resolved dark/light boolean

Not an entity of its own — computed at the point `QuestLogTheme` is invoked, from `AppearanceMode` plus the
live system signal:

| `AppearanceMode` | Resolved `darkTheme` |
|---|---|
| `LIGHT` | `false` |
| `DARK` | `true` |
| `SYSTEM` | `isSystemInDarkTheme()` (Compose's live system signal; defaults to `false` where the OS gives no dark-mode signal at all) |

This resolution is recomputed on every recomposition that can change either input — a new `AppearanceMode`
emission from the store's `Flow`, or a system configuration change — so it is always current, never cached
past a single recomposition. See research.md §3 for why this lives at the `MainActivity` call site rather
than inside `QuestLogTheme`.

## Validation rules

- `AppearanceMode.fromId` must never throw — an unrecognized persisted id (e.g. written by a future app
  version this one doesn't understand yet) silently falls back to `SYSTEM`, not a crash.
- Exactly one `AppearanceMode` is active at any time; there is no multi-select, no per-screen override.
