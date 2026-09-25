# Quickstart: validating the anticipated-lane change

Three levels, cheapest first. Prerequisites for all of them: this repo, `local.properties` with
`IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` (already required for the app to build at all).

## 1. Re-check the IGDB assumption still holds

The whole feature rests on IGDB still marking pending dates with `release_dates.date_format = 7` (see
`contracts/upcoming-lane-release-filter.md`). This is an external dependency, not app state, so it is
worth a 30-second re-check before trusting the rest of this guide, especially if time has passed since
2026-09-25.

```bash
CLIENT_ID=$(grep IGDB_CLIENT_ID local.properties | cut -d'=' -f2 | tr -d '[:space:]')
CLIENT_SECRET=$(grep IGDB_CLIENT_SECRET local.properties | cut -d'=' -f2 | tr -d '[:space:]')
TOKEN=$(curl -s -X POST "https://id.twitch.tv/oauth2/token" \
  -d "client_id=${CLIENT_ID}" -d "client_secret=${CLIENT_SECRET}" -d "grant_type=client_credentials" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl -s -X POST "https://api.igdb.com/v4/games" \
  -H "Client-ID: ${CLIENT_ID}" -H "Authorization: Bearer ${TOKEN}" \
  -d 'fields name, hypes; where id = (81249,28029) & (first_release_date != null | release_dates.date_format = 7);'
```

**Expected outcome**: exactly one result — The Elder Scrolls VI (id `81249`). If Half-Life 3 (`28029`)
also comes back, IGDB has started marking it as pending and nothing needs to change here. If *neither*
comes back, IGDB has changed how it represents TBD dates and the contract needs re-deriving before
proceeding.

## 2. Unit tests

```bash
./gradlew :core:data:testDebugUnitTest --console=plain -q
```

**Expected outcome**: all tests in `GameRepositoryImplPopularGamesTest` pass, including the new cases for
this feature (an undated-but-TBD game admitted, an undated-no-signal game excluded, the popular-lane query
string unchanged). See `contracts/upcoming-lane-release-filter.md`'s truth table for what each case
asserts.

On Windows, use `.\gradlew.bat` instead.

## 3. Manual check in the running app

1. Build and run: `./gradlew :app:assembleDebug` (or run from Android Studio).
2. Open the Search tab — Discover loads by default.
3. Look at the "Most anticipated" shelf and the hero card above it.

**Expected outcome**: previously-invisible high-anticipation undated titles now appear (The Elder Scrolls
VI is a reliable one to check for, given its hype score). Any undated game's card — including the hero, if
it lands there — shows the existing "unknown release date" text rather than a blank space or a fabricated
date. The "Popular this month" shelf is visually unchanged.

**Not covered by this guide**: a regression check that the *set* of games in "Popular this month" is
byte-for-byte identical before/after — that is what the unchanged-query-string assertion in the unit tests
already guarantees (SC-004), and re-deriving it by hand against a live, constantly-reranking feed would be
unreliable.
