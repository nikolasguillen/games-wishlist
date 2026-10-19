# Data Model: Two-Pane Search and Game Detail

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-18

The feature adds no persisted data, no domain model and no UI state. Its only "data" is the shape of the back stack
and the window width, both in `:shared` / `:core:navigation`.

## GameDetailRoute (changed, `:core:navigation`)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `gameId` | `Int` | — | The game to show (unchanged). |
| `fromSearch` | `Boolean` | `false` | The detail was opened from the Search flow: the results, the Discover feed, a "see all" shelf, a genre browse, or a similar game inside another `fromSearch` detail. Only these details take part in two-pane mode. |

- `@Serializable`, still registered once in `GameNavSavedStateConfiguration`. A saved stack from before the change
  decodes with `fromSearch = false`.
- Created with `fromSearch = true` only by `:shared`'s Search entry and by a `fromSearch` detail's similar-game click.
  Radar, Wishlist and the reminder deep link keep `GameDetailRoute(id)`.

## Search-detail run (derived, `:shared`)

The trailing run of `GameDetailRoute(fromSearch = true)` keys that sits **directly** on `SearchRoute`.

```text
[SearchRoute, GD(1,s), GD(2,s)]          → run = indices 1..2
[SearchRoute, GD(1,s), GD(9)]            → no run (the top is a deep-linked detail)
[SearchRoute, RadarRoute, GD(4)]         → no run
[SearchRoute]                            → no run
```

**Invariant**: a `fromSearch` detail only ever exists inside such a run. It is created only on top of `SearchRoute`
or another `fromSearch` detail, and every tab switch pops to the root.

## Layout state (derived, `:shared`)

`twoPaneEnabled` = window width ≥ 840dp (`WIDTH_DP_EXPANDED_LOWER_BOUND`).

| `twoPaneEnabled` | run exists | What is shown | Rail | Detail navigation button |
|---|---|---|---|---|
| false | — | Today's single pane: the last entry | Visible only on top-level routes, as today | Shown |
| true | no | Today's single pane. Search at full width if it is the top. | As today | Shown (non-Search details) |
| true | yes | **Two panes**: Search on the left, the run's last detail on the right | Visible, Search selected | Close (X) |

## Transitions

```text
                      tap a game in Search (wide)
  Search full width ───────────────────────────────▶ Two panes [Search | D]
        ▲                                                 │  │
        │ back (wide): remove the whole run               │  │ tap another game / similar game (wide):
        └─────────────────────────────────────────────────┘  │   replace the run with the new detail
                                                             ▼
                                                     Two panes [Search | D']

  Two panes ── window < 840dp ──▶ D full-screen with back button (stack untouched)
  D full-screen (fromSearch) ── window ≥ 840dp ──▶ Two panes [Search | D]
  Below 840dp: every open is a push and every back pops one entry, exactly as today.
```
