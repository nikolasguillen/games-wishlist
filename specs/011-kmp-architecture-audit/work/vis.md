# Visibility of implementation classes

An extra check beyond the planned areas (not in `tasks.md`), added because the constitution's "Development Workflow"
says "Default to `internal` for anything local to a module" and multiplatform conversions often force types public.
Baseline `b982bf11`. Method: Python scan of every `commonMain`/`androidMain`/`iosMain` source for top-level
`class`/`object`/`interface` declarations without `internal`/`private`, then a word search for each name in files of
*other* modules (tests count as the module that owns them).

## Result

| Module | Public top-level types | Never referenced from another module |
|---|---|---|
| `:core:data` | 18 | **17** |
| `:core:database` | 41 | 8 (entities and relation types, exposed through public DAOs) |
| `:core:network` | 17 | 5 (DTOs nested in `IgdbGame`, so required public) |
| `:core:domain` | 59 | 2 |
| `:core:ui`, `:core:common`, `:core:designsystem`, `:core:navigation` | 17 / 4 / 3 / 10 | 6 / 1 / 1 / 1 |

Only `:core:data` stands out. Its 17 public types are the implementations (`GameRepositoryImpl`, three preference
stores, both schedulers and workers, `ReleaseNotifierImpl`, `WishlistCoverImageStorageImpl` ×2, the iOS no-ops
`NoOpReleaseNotificationScheduler`, `NoOpReleaseNotifier`, `StaticReleaseRemindersAvailability`,
`UnsupportedGameDescriptionTranslator`, `InProcessReleaseRefreshScheduler`) and the `WishlistCoverImageStorage`
contract. Each is bound by that module's own Koin module and reached through a `:core:domain` interface, so nothing
outside needs the concrete type. In `:core:network` the types that must be public are public and the three that are
only used by the module (`ElapsedRealtimeSource`, `IgdbAuthManager`, `IgdbAuthService`) are already `internal`.

Compared with `develop`: the same scan finds 12 public top-level types in `:core:data` and **0 `internal`**, with all
10 implementation-like classes public. So the pattern existed before the migration; the migration added 7 more
implementation classes following it. `docs/tech-debt.md` does not list it.

## Candidate

| ID | Title | Detail |
|---|---|---|
| C-VIS-1 | `:core:data` exposes its implementation classes as public although nothing outside the module uses them | 17 of 18 public types (list above). Nothing forces them public: the bindings are `singleOf(::X)` inside the module's own `dataModule`/`dataPlatformModule`, and `internal` classes can be used in a public `val` initializer. Impact: another module could depend on a concrete class and bypass the `:core:domain` contracts, which is the layering the rules protect; the compiler would not stop it. Pre-existing (0 `internal` on `develop`), extended by the migration. Fix: make them `internal` (module tests in `androidHostTest` can still see them). Severity low, follow-up, size M (touching about 17 files; Koin `verify()` and `KoinGraphTest` should cover the wiring). Not a regression, so it is a candidate for `docs/tech-debt.md` if the owner prefers to defer |
