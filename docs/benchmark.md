# Accuracy benchmark

Measures whether the engine ranks items the way an outside reference does. Correctness (does the engine obey the
game's rules?) is covered by the regular tests; this answers a different question: are its rankings any good? Options
and blind spots: [ideas.md](ideas.md) → Measuring accuracy.

## Method

- **Closed world, one pool per slot:** the reference's best item for the slot plus its listed competitors, nothing
  else. It asks "does the engine order plausible contenders correctly?", not "does it avoid all garbage?".
- **Classification:** the best item's score against its **best rival's** (the highest-scoring other candidate). Higher
  is a **hit**, equal a **tie**, lower a **miss**. Item ids never decide: `ScoredItemPool` breaks ties by `baseId`, so
  "is the best item ranked first?" would turn some ties into hits (covered by `SlotResultTest`).
- **Ratchet, not pass/fail:** `KNOWN_HITS` lists the slots that hit today. The test fails if one of them stops
  hitting; when a slot starts hitting, it's added by hand. A set rather than a count, so a regression in one slot
  can't hide behind an improvement in another.
- **What runs:** recorded item JSON → `ItemMapper` → `ItemEntityFactory` → domain `Item` → `ItemScorer` with the real
  weight registries, all in memory. No Spring, DB, async or pool query: those are plumbing covered by ITs, and they
  would make a miss ambiguous.

## Reference

`src/test/resources/benchmark/combat-rogue-preraid.json`: Combat Rogue, Human, level 60, isolated slots only. The file
records its sources:

- **Best per slot:** Icy Veins' Classic Era rogue pre-raid list (phase 4 picks); its phase 1–3 picks that phase 4
  replaced are the `alternatives`.
- **Extra competitors:** undisputed "Optional" items from Wowhead's pre-raid guide (Season of Mastery, so used for
  competitors only, never for the best item).

Items come from `src/test/resources/blizzard/item/benchmark/<id>.json`, recorded with
`tools/blizzard-fetch/fetch_items.sh` (namespace `static-classic1x-eu`, locale `en_GB`, as the app requests them).

Out of scope for now: rings, trinkets and hands (they need the pair finders), random-suffix items, set bonuses.

## Running

```bash
./mvnw test -Dtest=BenchmarkTest
```

It prints one row per slot (both scores and the margin) and a summary. A small positive margin marks a fragile hit.

**Adding a spec:** a new reference file, its items recorded with the fetch script. While there's one file the baseline
is a constant in the test; with more, it moves into each reference file and the test is parameterized over them.

## Score history

| Date       | Change                    | Hits | Ties | Misses | Notes                                                                                                  |
|------------|---------------------------|------|------|--------|--------------------------------------------------------------------------------------------------------|
| 2026-11-06 | v0 baseline (Combat Rogue) | 5    | 1    | 5      | Equip spells aren't scored: Devilsaur Gauntlets and Leggings score ~1 (armor only), every neck scores 0 |
