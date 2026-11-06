#!/usr/bin/env bash
# Records Blizzard item responses as test fixtures, requested exactly like the app does
# (namespace static-classic1x-eu, locale en_GB).
#
# Usage: BLIZZARD_CLIENT_ID=... BLIZZARD_CLIENT_SECRET=... tools/blizzard-fetch/fetch_items.sh [reference.json]
#   reference.json defaults to src/test/resources/benchmark/combat-rogue-preraid.json; every id in its
#   "best", "alternatives" and "extraCompetitors" lists is fetched into src/test/resources/blizzard/item/benchmark/<id>.json.
# Already recorded items are skipped, so it can be rerun safely. The token is never printed.
set -euo pipefail

# paths resolve from the repository root, so the script works from any directory
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REFERENCE="${1:-$ROOT/src/test/resources/benchmark/combat-rogue-preraid.json}"
OUT_DIR="$ROOT/src/test/resources/blizzard/item/benchmark"
API="https://eu.api.blizzard.com"

: "${BLIZZARD_CLIENT_ID:?set BLIZZARD_CLIENT_ID (the same one the app uses)}"
: "${BLIZZARD_CLIENT_SECRET:?set BLIZZARD_CLIENT_SECRET (the same one the app uses)}"

# whitespace is stripped first because grep matches line by line and a formatter may spread an id list over lines
ids=$(tr -d '[:space:]' < "$REFERENCE" | grep -oE '"(best|alternatives|extraCompetitors)":\[[^]]*\]' \
  | grep -oE '[0-9]+' | sort -un)
[ -n "$ids" ] || { echo "no item ids found in $REFERENCE" >&2; exit 1; }

token=$(curl -sf -u "$BLIZZARD_CLIENT_ID:$BLIZZARD_CLIENT_SECRET" -d grant_type=client_credentials \
  https://oauth.battle.net/token | grep -oE '"access_token" *: *"[^"]+"' | sed -E 's/.*"([^"]+)"$/\1/')
[ -n "$token" ] || { echo "could not get a client-credentials token" >&2; exit 1; }

mkdir -p "$OUT_DIR"
fetched=0; skipped=0; failed=0
for id in $ids; do
  file="$OUT_DIR/$id.json"
  if [ -s "$file" ]; then skipped=$((skipped + 1)); continue; fi
  if curl -sf -H "Authorization: Bearer $token" -H "Battlenet-Namespace: static-classic1x-eu" \
      "$API/data/wow/item/$id?locale=en_GB" -o "$file"; then
    fetched=$((fetched + 1))
  else
    rm -f "$file"; failed=$((failed + 1)); echo "failed: item $id" >&2
  fi
done

echo "fetched $fetched, skipped $skipped, failed $failed (into $OUT_DIR)"
[ "$failed" -eq 0 ]
