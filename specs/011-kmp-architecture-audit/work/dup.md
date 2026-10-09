# R-DUP — Duplicated platform logic

Baseline `b982bf11`. Method (research R7): for each `androidMain`/`iosMain` pair, decide whether it contains logic that
touches no platform API and is written twice (flag), or only wraps a platform API (legitimate). Pairs from
`cap-mapping.md`.

## Pair-by-pair

| Pair | Verdict | Why |
|---|---|---|
| `AppVersionProviderImpl` | legitimate | `PackageManager` vs `NSBundle`; the only shared idea ("a miss degrades to empty") is one `orEmpty()` |
| `NetworkStatusProviderImpl` | legitimate | `ConnectivityManager` vs `NWPathMonitor`; each encodes a different platform definition of "unmetered" |
| `DateRendering` | legitimate | `java.time` vs `NSDateFormatter`. The `DateStyle` enum and `renderLocalDate` signatures are in `commonMain`; each side only maps the enum to its own style type |
| `ReleaseRefreshSchedulerImpl` / `InProcessReleaseRefreshScheduler` | legitimate, constant shared | Both use `RELEASE_DATES_REFRESH_INTERVAL` (`24.hours`, one definition in `commonMain`) as `PC` requires. The iOS scheduler is itself in `commonMain` |
| `RepositoryErrorMapper` actuals | legitimate | Android names the `java.net` types; iOS tests for the network module's two types. `toRepositoryError()` itself is shared |
| `PlatformTransportFailure` | legitimate | Android returns `null`; iOS maps `NSURLError` codes. The table exists only once |
| `SystemBarsAppearance` | legitimate | `WindowCompat` vs no-op |
| `FullScreenDialogProperties` | legitimate | Two different `DialogProperties` options (`decorFitsSystemWindows` vs `usePlatformInsets`) |
| `SearchBarScrollBehaviorCompat` | legitimate | Two different Material 3 API shapes (androidx `scrollState` vs JetBrains alpha properties). Listed as a known risk in `docs/tech-debt.md` |
| `CoverImagePicker`, `TextSharer`, `NotificationPermission*` | legitimate | Platform pickers and share sheets; iOS permission `actual`s are inert and documented |
| `RunBlocking` | legitimate, identical by necessity | `actual fun <T> runBlockingCompat(block) = runBlocking(block = block)` is the same one-liner on both platforms because `runBlocking` is not visible from `commonMain`. An intermediate source set for the two targets would remove the `expect`, at the cost of a new source set; the contract file records the seam |
| **`WishlistCoverImageStorageImpl`** | **duplicated pure logic** | See below |
| Koin `*PlatformModule`s | legitimate | Each binds its own platform implementations |

## C-DUP-1: `WishlistCoverImageStorageImpl` repeats its policy constants on each platform

`platform-contracts.md` ("`WishlistCoverImageStorage`") requires the iOS version to scale "to the same max dimension",
use the "same subdirectory name" and the same file naming as Android. Those values exist as private constants in each
file, with no shared definition in `commonMain`:

| Value | Android (`core/data/src/androidMain/.../local/WishlistCoverImageStorageImpl.kt`) | iOS (`core/data/src/iosMain/.../local/WishlistCoverImageStorageImpl.kt`) |
|---|---|---|
| Covers subdirectory | `COVERS_DIR_NAME = "wishlist_covers"` (companion, line 92) | `private const val COVERS_DIR_NAME = "wishlist_covers"` (line 24) |
| Longest side | `MAX_DIMENSION_PX = 1440` (line 98, `Int`) | `private const val MAX_DIMENSION_PX = 1440.0` (line 28, `Double`) |
| JPEG quality | `COMPRESSION_QUALITY = 85` (line 100) | `private const val JPEG_QUALITY = 0.85` (line 29) |
| Alpha → PNG, else JPEG | `if (bitmap.hasAlpha()) PNG else JPEG` (line 36) | `if (hasAlpha) PNG else JPEG` (line 48) |
| File name | `"${UUID.randomUUID()}.${extension}"` (`java.util.UUID`) | `"$coversDir/${NSUUID().UUIDString}.…"` |

- **Impact**: today the values agree. If one side is tuned (a larger cap, a different directory), the other keeps the
  old value and covers saved on the two platforms differ in size or location, with no compile error and no test, since
  neither implementation has one. Because the directory name decides where `delete(path)` can find a file, a divergence
  would also make cleanup miss files.
- **Contract drift**: the contract says the file name uses `kotlin.uuid.Uuid`, but both implementations use their
  platform's own UUID type.
- **Fix** (size S): one `internal` constants holder (directory name, max dimension, quality percentage, alpha rule) in
  `core/data/src/commonMain/.../local/`, read by both implementations; optionally `kotlin.uuid.Uuid.random()` for the
  name in `commonMain`. No new module or edge.
- **Severity**: low (medium for drift risk, but three constants in one pair). **Classification**: follow-up.

## Rule outcome

| Rule | Outcome | Evidence |
|---|---|---|
| R-DUP-01 | **violated, minor** | 1 of the 13 pair groups above (C-DUP-1). The other 12 are platform-API wrappers or identical by necessity |
| R-CAP-08 | pass | See `cap-contracts.md` |
