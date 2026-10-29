# Findings

Facts we learned about the systems we depend on, with the evidence. When one surprises us again, record it here, and
for the Blizzard API add the response to `src/test/resources/blizzard/`.

## Blizzard API

- Unset fields are either omitted or sent as `0` (`required_level`). Both mean "not set".
- Inventory type codes follow the client's `INVTYPE_*` constants (`TWOHWEAPON`, `HAND`, `ROBE`). This is fixed, and
  recorded fixtures live in `src/test/resources/blizzard/item/`.
- `preview_item.requirements.playable_classes.links[]` gives `{id, name}`. `name` is localized (`?locale=en_GB`), so
  map by `id`.
- Secondary stats come as equip spells (`preview_item.spells[].spell.id`), not `stats`.
- The API is shared across game versions, so field names are retail's: Classic Era sends Vanilla's **Unique** in
  `unique_equipped`.
- Character names must be lowercase in profile URLs; a capitalized name gets a 404.
- `_links.self` never shows the locale. A response requested without `locale` has every name as a dict of 12 locales,
  which is the tell.

### Talents (2026-10-29; recordings in `src/test/resources/blizzard/specs/`, all `en_GB`)

- Shape: `_links`, `character`, `specialization_groups[]` (`is_active`) → `specializations[]`
  (`specialization_name`, `spent_points`, `talents[]`). There's no top-level `specializations` (that's the retail
  shape).
- **Dual spec: up to two groups, and the active one isn't always first** (`hunter_with_beast_mastery.json`: the
  inactive Marksmanship 34 group comes before the active Beast Mastery 34 group). Map the group with `is_active`,
  never `[0]`.
- **Trees with 0 points are left out** (`spent_points_above_10.json`: Combat 32, Assassination 12, no Subtlety), so a
  character can list one, two or three trees.
- **No points spent** (`unspent_points_above_10.json`): one active group with **no `specializations` key at all**
  (absent, not `[]`).
- Names are plain `en_GB` strings with no spec ids. "Beast Mastery" and "Feral Combat" are the real in-game tree names.
- Talent entries carry `talent.id`, `talent_rank` and `spell_tooltip` (`power_cost` is sometimes `null`). We don't
  use them.
- **Level < 10: 404** (`below_10.json`, a new character). The character endpoint
  `/profile/wow/character/{realm}/{name}` 404s too, so the API doesn't serve the character at all. Most likely
  characters below 10 aren't served (not proven: it could be a very long propagation delay).
- Sources: `@blizzard-api/classic-wow` typings, the HA-WOW integration's Classic talent parser.

## VMaNGOS DB

- `item_template` and `quest_template` are both keyed `(entry, patch)`, so take the latest row per entry. All columns
  are `NOT NULL DEFAULT 0`, where 0 = not set.
- Quest rewards aren't marked on items; the link lives only in `quest_template`.
- Quest-reward gear: 1633 BoP, 16 BoE (tradeable, so **not** single-copy), 2 quest items, 3 none.
- 113 class-quest rewards have no class restriction on the item itself (e.g. Primitive Hatchet, quest
  `RequiredClasses 1245`).
- Quest rewards usually have no level requirement on the item; the gate is the quest's `MinLevel` (Whirlwind Axe:
  item `required_level` 0, quest 1792 min level 30).
- Crafted items are a source too (e.g. Glacial Vest via spell 28207).
- Equip spells in `spell_template`: `effect1 = 6` (apply aura), `effectApplyAuraName1` (99 attack power, 54 hit, 42
  proc) and value `effectBasePoints1 + effectDieSides1`.
- **`RequiredRaces = 0` doesn't mean every race.** 1547 of 1737 seed rows have it. Faction usually comes from the quest
  giver: quest 7491 (Onyxia's head → Dragonslayer's Signet) is Horde-only (Overlord Runthak) with mask 0, and its
  Alliance mirror 7496 has mask 0 too. So `QuestSource.allows(race)` lets most faction-only rewards through to the
  other faction.
- **Same item, several quests, overlapping masks:** 113 of 151 multi-quest items. Samples are faction mirrors via the
  giver (Dragonslayer's Signet), alternative quests (Blade of Cunning: three Alliance quests, all mask 77, Unique) and
  duplicate entries (Outlaw Sabre: quests 909 and 6922, both "Baron Aquanis", both Horde, one-hand, not Unique). Open:
  can one Horde character complete both? Most likely an old and a new version of one quest. A1 covers it either way.
- Seed vs README drift: the seed has 1737 rows and 151 multi-quest items, but the README says 1738 and 169. Recount at
  the next export.

## Codebase

- Three bugs so far came from invented test data, and a fourth (every character resolving to `CLASS_NONE`) from a DTO
  written without a real response. API-level builders must use real API codes, and DTOs need a recorded response
  behind them (both in `CLAUDE.md`).
