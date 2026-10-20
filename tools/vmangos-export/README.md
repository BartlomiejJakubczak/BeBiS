# VMaNGOS export

One-off scripts that derive reference data for BeBiS from the [VMaNGOS](https://github.com/vmangos/core) world database,
a community reconstruction of WoW Classic (1.12) game data. Blizzard's API doesn't expose where an item comes from, so
facts like "this item is a one-time quest reward" are exported from here and shipped as Flyway seed migrations. The app
never connects to MariaDB.

## Running

1. Download the world DB snapshot from the VMaNGOS release `db_latest` and unzip it. Only `mangos.sql` is needed.
2. Load it into a throwaway MariaDB:
   ```bash
   docker run -d --name mariadb -e MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=1 -e MARIADB_DATABASE=mangos \
     -p 127.0.0.1:2137:3306 -v /path/to/dump:/docker-entrypoint-initdb.d:ro mariadb:11.4
   ```
3. Generate the seed:
   ```bash
   docker exec -i mariadb mariadb -N -r -u root mangos < quest_rewards_export.sql > V4__Seed_quest_rewards.sql
   ```
   `-N` drops the column header and `-r` keeps the `\n` in the generated header comment as real line breaks.

## Scripts

| Script | Output | Notes |
|---|---|---|
| `quest_rewards_export.sql` | `quest_rewards` rows, one per (item, quest) | BoP gear from non-repeatable quests. Latest patch row of each quest and item. |

## Data notes

Checked against the snapshot from 2026-09-28:

- **Rewarded by more than one quest:** 169 gear items. They are mostly Alliance/Horde mirror quests or duplicated quest
  entries, so a character still gets one copy. All quests are kept; the app combines them.
- **BoE quest rewards:** 16 items, e.g. Knightly Longsword. They can be traded, so they're excluded.
- **"Create item" spells:** these also give quest rewards to players (e.g. Drakefire Amulet), so they are not a sign
  of a crafted item. They're deliberately not used as a filter.
