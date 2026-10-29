# Decisions

What we decided and why. When a decision changes, edit its entry and note when and why. These are the draft for an
ADR folder (roadmap → portfolio polish).

## Data and persistence

- **Quest data comes from VMaNGOS, exported once.** Blizzard's API doesn't say which items are quest rewards. VMaNGOS
  has the quests and `patch` versioning (useful later). It's exported from a throwaway `mariadb:11.4` container
  (`--name mariadb`, `127.0.0.1:2137`, DB `mangos`). Flyway never reads MariaDB; it only runs our exported SQL against
  Postgres. How the export works and what it filters: `tools/vmangos-export/README.md`.
- **Separate tables, joined on read.** Items are insert-once in `ItemService`, so facts that don't come from Blizzard
  live in their own tables instead of columns that would need backfills.
- **`quest_rewards` has one row per (item, quest)**, with PK `(base_id, quest_id)` and no FK to `items`, because it's
  reference data.
- **Equip vs acquisition constraints.** Class restrictions on the *item* go on `items` (from Blizzard). Class and race
  requirements of the *quest* go in `quest_rewards`. A suggestion must satisfy both.
- **One owner per fact.** Export raw facts (`MinLevel`, not "effective level") and derive in the app.
- **Identity:** `ItemKey(baseId, suffixId)`. Quest rewards never roll suffixes.
- **Class identity:** Blizzard class ids (1 Warrior … 11 Druid), which are also the bit positions (`id - 1`) in the
  mangos bitmasks. Races are mapped by Blizzard race id too.

## Domain

- **Filters on item columns go in SQL; filters on sources go in the domain (2026-10-21).** `ItemQueryService` runs the
  item-column query, batch-loads the quest rows with one `IN` query and joins them in memory. `ItemPoolProvider` then
  applies source-based rules through `Item` (`isObtainableBy`), where an item is obtainable if it has no sources or
  *any* source allows the character.
- **Single copy (2026-10-23).** An item can be worn only once if it's Unique or any of its sources is one-time
  (`Source.isRepeatable()`: a quest is not, anything else is). No sources means repeatable. Implemented as
  `Item.isEquippableOnce`, used by `SlotPair.areSameOnceEquippables` in both pair finders.
- **Talents (2026-10-29).** Only the active talent group counts. "No talents" (below level 10, no points spent, or a
  failed fetch) is a value, `WowTalents.none()`, not an `Optional`. A tie between the top trees resolves to `_NONE`
  (deterministic; the set's order varies per JVM) until the user can choose. Tree names are normalized in the mapper (uppercase,
  no spaces), and `ClassSpec` uses the real in-game names (`FERALCOMBAT`).
- **Error handling (2026-10-29).** Legitimate states are values, not exceptions: no talents is `WowTalents.none()`,
  and a tie will be a result the user answers. Mappers validate and throw a domain exception on broken required data;
  the calling service picks the fallback (skip the item, the `_NONE` spec) and logs. Retries are only for transient
  failures (network, 5xx, 429) and live in the fetchers: corrupted data comes back the same on every call, and our
  cache would serve it again anyway. Exceptions meant for the user are translated at the web edge
  (`@RestControllerAdvice` + `ProblemDetail`), never thrown from the async engine, where nobody is listening.

## Assumptions

Assumptions the design depends on, each with how it's verified. A wrong one should fail a test, not surprise us.

| # | Assumption | Status | Evidence, cost if wrong |
|---|---|---|---|
| A1 | A character can complete at most one of the quests that reward the same item | ✅ accepted 2026-10-23 as a conservative default (unverified) | Can't be checked yet: the export lacks the quest giver's faction, `ExclusiveGroup` and quest starters (roadmap → VMaNGOS session). If it's wrong, we miss a suggestion but never make an impossible one. After the export: verify it, then guard it with a test on the seed |
| A2 | Classic Era has no unique-equipped, only Unique (one copy in bags and bank) | verified | Unique-equipped arrived in patch 2.3.0. Band of Accuria fixture: `"unique_equipped": "Unique"` |
| A3 | An item with a one-time source and a repeatable one counts as one copy | ✅ accepted 2026-10-23, exact today | No such item exists yet (only quest sources are exported). Errs on the safe side (missed suggestion). Revisit when drop, vendor or crafting sources are exported |
