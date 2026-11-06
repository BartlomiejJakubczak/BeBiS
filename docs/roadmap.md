# BeBiS roadmap

Open work only, most urgent first. Every item states its **problem** first, in game terms, so it makes sense without
the conversation it came from. Finished items are deleted; `git log` keeps the record. Reference material lives next
door:

- [decisions.md](decisions.md): what we decided and why, and the assumptions the design rests on
- [findings.md](findings.md): facts about the Blizzard API, VMaNGOS and our own codebase
- [ideas.md](ideas.md): parked ideas and features that aren't planned yet

**Recently done:** accuracy benchmark v0 ([benchmark.md](benchmark.md), Combat Rogue: 5 hits, 1 tie, 5 misses),
quest rewards gated by their quest's min level, talents read from the real Classic Era response (every character used
to resolve to `CLASS_NONE`), `d56b9f1` slot ownership moved into the orchestrators.

## Next (order agreed 2026-10-26)

### 1. One VMaNGOS export session: factions, exclusive quests, equip spells

**Problem:** two data gaps.
- **Faction leaks:** 1547 of the 1737 seeded quest rewards have `RequiredRaces = 0` ("any race"), even when only one
  faction can talk to the quest giver. So Horde-only rewards are offered to Alliance characters (Dragonslayer's Signet,
  quest 7491 from Overlord Runthak).
- **Equip spells aren't scored:** most secondary stats in Classic (hit, crit, attack power) come as equip spells
  (`preview_item.spells[]`), not as `stats`, so those items are undervalued.

**Idea:** in one sitting, export the quest giver's faction and `ExclusiveGroup` for `quest_rewards`, plus a
`spell_id → (StatType, value)` table for flat-stat auras (findings.md → VMaNGOS). Then verify assumption A1 and
recount the seed: the README says 1738 rows and 169 multi-quest items, but the seed has 1737 and 151.

**Where:** `tools/vmangos-export`, a new migration, `QuestSource.allows(race)`.

### 2. Score equip spells, then re-run the benchmark

**Problem:** the same undervaluation as in 1, now fixable with the exported table. The benchmark shows its size: both
Devilsaur pieces score ~1 (armor only) and every neck scores 0, though all of them carry attack power and crit.

**Idea:** map flat-stat equip spells to stats when items are mapped, then re-run the benchmark, add a row to
benchmark.md's score history and update `KNOWN_HITS`.

### 3. Re-plan

Upgrade results aren't consumed by anything yet (no endpoint, no UI), so decide what comes after accuracy.

## Later (not ordered)

- **Spec choice for ties and dual spec.** A tied character (e.g. 15/15) has no clear spec, and with dual spec a
  player may want gear for the inactive group or a secondary tree. Resolve the spec during `selectCharacter` as a
  sealed result (resolved, tied with candidates, no talents), let the user pick when it's tied, and pass the chosen
  spec to the upgrade search. It has to happen before the `@Async` engine runs: nobody can be asked from in there.
- **404s count as Blizzard outages.** A 404 means "no such resource" (e.g. characters below level 10), but the fetchers
  retry it, and the circuit breaker, shared by the item and spec fetchers, counts it as a failure. A few could block
  item fetching for 20 s. `EquipmentService.syncEquipment` has no fallback, so a 404 there fails `selectCharacter`.
  Don't retry or count 404s, and decide what selecting a level 1–9 character should do.
- **Small engine cleanups.** `ItemScoringContext` is only used in `engine.upgrade`, so it could be package-private
  there. The three orchestrators each build `ItemPoolProvider.Query` from `CharacterInfo`; one factory would keep that
  in one place.
- **Equipment package cleanup.** `EquipmentRepository` declares `Long` as its id (the real one is
  `WowCharacterEntity.CompositeKey`) and nothing uses it: delete it. `Equipment` is mutable (`@Setter`, and
  `getEquipment()` hands out its map), so make it immutable (`Map.copyOf`). Names: the field `equipment` inside
  `Equipment`, and `EquippedItem.playerEnchs`.
- **"Unique-equipped" is the API's word, not the game's.** Classic only has Unique (decisions.md → A2), so rename
  `uniqueEquipped` in the domain. Open: does the column follow (V5)?
- **Test infrastructure.** The base-class tree bundles independent concerns (transactions, Blizzard mocks, cache
  clearing, which containers run), and the resilience tests fit no branch of it. Replace it with composition:
  `@ImportTestcontainers` holders for Postgres and Redis, composed annotations for the Blizzard mocks (class-level
  `@MockitoBean(types = …)`, Spring 6.2+) and cache clearing, and plain `@Transactional` where a rollback is wanted.
- **Observable async.** A failing `void @Async` orchestrator is only logged. Return `CompletableFuture` and combine
  them in `UpgradeFinderService` (`allOf`), so callers see failures and tests can `join()` instead of polling. Do it
  with the stat-caps feature (ideas.md), which needs the service to collect all results anyway.
- **Portfolio polish** (what reviewers notice): a README with an architecture overview (per feature the write and read
  path and who may call whom, as a Mermaid diagram), CI (GitHub Actions running `./mvnw verify`), a Dockerfile, OpenAPI
  docs, ADRs (decisions.md is the draft) and ArchUnit rules (e.g. `engine` must not depend on `item.jpa`, only `item`
  may use `ItemMapper`).

## Known limitations (accepted for now)

- Two rewards from one quest's choices can both be suggested (choice groups aren't modelled).
- Completed quests are unknown, so a reward from a quest the character already did can still be suggested.
- A quest reward counts as one copy however many quests reward it (A1), even with a repeatable source too (A3).
- A failed talents fetch looks the same as "no talents": both get class-generic weights.
