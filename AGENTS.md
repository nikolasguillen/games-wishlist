# AGENTS.md

`GamesWishlist` is a modular Android app (Kotlin, Jetpack Compose, Hilt, Room, Retrofit/Moshi,
AndroidX Navigation 3) for tracking a videogame wishlist, backed by the IGDB API.

## Instructions live in CLAUDE.md

Project guidance for automated agents is maintained in `CLAUDE.md` files, not in this one:

- **`CLAUDE.md`** (repository root) — build commands, module graph and dependency rules, data flow, and the
  project-wide coding rules. **Read this before changing any code.**
- Directory-scoped files with module-specific conventions and pitfalls:
  `feature/CLAUDE.md`, `core/data/CLAUDE.md`, `core/network/CLAUDE.md`, `core/database/CLAUDE.md`,
  `core/ui/CLAUDE.md`, `core/designsystem/CLAUDE.md`.
  Read the one covering the module you are editing.
- **`docs/tech-debt.md`** — known deviations from those rules. Do not "fix" them as a side effect of
  unrelated work, and do not imitate them.

Claude Code loads these automatically. Other agents should read them explicitly.
