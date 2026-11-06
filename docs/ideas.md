# Ideas

Parked ideas and features that aren't planned yet. When one gets planned, move it to the roadmap with its problem
stated first.

## Measuring accuracy (options, 2026-10-26)

- Keep two questions apart. *Correctness*: does the engine obey the game's rules? Tests and properties answer that.
  *Accuracy*: are its rankings right? Only an outside reference can answer that.
- Correctness: property-based tests (jqwik), e.g. more of a weighted stat never lowers a score, shuffling the pool
  never changes the result, a suggestion is always equippable and obtainable, never two of an item that can be worn
  once.
- Accuracy, cheapest first:
  - **Closed-world benchmark** against published pre-raid BiS lists: per spec and slot, put the listed items in the
    pool and measure top-1 / top-3 hits. Report it as a score with a minimum that only goes up, not as pass/fail.
    (Built as v0: [benchmark.md](benchmark.md). It classifies hit / tie / miss against the best rival instead, since
    top-3 is meaningless in pools of three or four.)
  - **Approval snapshots** of real characters' results, so changes show up as reviewed diffs (catches regressions, not
    accuracy).
  - **Simulation reference**: compare stat weights or rankings with a community sim (Spearman rank correlation).
    Most work, highest fidelity.
- Known blind spots any benchmark will mostly measure: equip spells and procs aren't scored, weights are linear
  (no hit cap), no set bonuses, and the pool only holds items someone has synced (see catalog seeding).

## Item catalog seeding

Dungeon, raid and profession gear, so items nobody has synced yet can be suggested. A hybrid approach: the catalog from
mangos, stats from the Blizzard item endpoint.

## Stat caps (raised 2026-10-28, not designed yet)

Caps (e.g. the hit cap at 60) break the engine's core assumption that slots are independent: whether hit on a helm is
worth anything depends on every other piece. The likely shape: orchestrators produce per-slot candidates, and a
set-level step in `UpgradeFinderService` (it has `CharacterInfo`) checks the totals against the caps and swaps over-cap
pieces for the next best mix. The service will then call the orchestrators more than once, so they must stay stateless
and repeatable. Value above the cap drops to ~0, so weights stop being linear.

## Progression tiers ("level 60 problem", brainstormed 2026-10-20)

- **Idea:** place the character on a tier ladder, e.g. pre-raid → MC/Ony → BWL → AQ40 → Naxx (grouping is the user's
  call), and suggest upgrades from the next tier. Optionally, an LLM explains the result in plain language; the engine
  decides, the LLM only phrases it.
- **Patch ≠ difficulty.** Ashkandi (BWL) is patch 4 while Zin'rokh (ZG) is patch 5. Lionheart Helm (crafted) and every
  MC drop are patch 0. The tier must come from the item's **source** (instance → curated `ContentTierRegistry`), with
  `patch` at most a hint. "Introduced in" = `MIN(patch)` (Bonereaver's Edge has 0, 5 and 8).
- **Tier BiS doesn't need external lists:** run the existing engine on a pool restricted to tiers ≤ T, so it's
  consistent with our own stat weights. A character's tier is where their current gear score reaches roughly X% of
  that tier's BiS score.
- **Product decisions (user, 2026-10-20):**
  - **Auto mode** uses a curated difficulty ranking of instances. **Target mode** compares current gear against an
    instance the user picks.
  - Ladder variants follow the "nearest acquisition" principle, with absolute BiS always on top.
  - Crafted gear is mostly a pre-raid source. PvP gear is skipped for now.
  - Reputation gear is an alternative with high effort, gated by the faction's patch.
- **Shaping ideas:**
  - Give every source an *effort* score: dungeon < 20-man < 40-man raid; reputation scaled by rank; crafted by
    materials. The ladder variants are then the **Pareto front** of (score gain, effort), i.e. the best gain at each
    effort level.
  - Reputation gear is easy to detect: `npc_vendor` + `item_template.required_reputation_faction/rank`.
  - Ship in slices: v1 auto ladder, v2 target mode, v3 effort-based variants, v4 LLM explanations.
- **Data needed:** item → source instance, via `creature_loot_template` / `reference_loot_template` → creature →
  `creature.map`. This is the expensive part; design and export it when the feature starts, not before.
