#!/usr/bin/env bash
#
# Checks the packaged jar the way someone given the file would use it.
#
# There is one failure mode that unit tests cannot see: Lucene resolves its codecs through
# META-INF/services, and building an uber-jar means merging those files from every
# dependency. Overwrite them instead of merging and everything still compiles, every test
# still passes, and opening an index throws at runtime in front of a user. So this opens the
# real jar and asks it to do the thing that would break.
set -euo pipefail

cd "$(dirname "$0")/.."
JAR=backend/target/leading-tone-runner.jar
PORT=${VERIFY_PORT:-8099}
DATA=$(mktemp -d)
# Wait for it to actually exit before clearing up: the database is still being written
# while the process is shutting down, and removing the directory under it fails.
cleanup() {
    if [ -n "${APP:-}" ]; then
        kill "$APP" 2>/dev/null || true
        wait "$APP" 2>/dev/null || true
    fi
    rm -rf "$DATA"
}
trap cleanup EXIT

[ -f "$JAR" ] || { echo "no jar: run 'make package' first"; exit 1; }

echo "==> Lucene service files survived the merge"
SERVICES=$(unzip -l "$JAR" | grep -c 'META-INF/services/org.apache.lucene' || true)
echo "    $SERVICES Lucene service files in the jar"
[ "$SERVICES" -ge 5 ] || { echo "    too few: the uber-jar overwrote them instead of merging"; exit 1; }

echo "==> the jar starts on an empty data directory"
MUSIC_DATA_DIR="$DATA" MUSIC_HTTP_PORT="$PORT" java -jar "$JAR" >"$DATA/log" 2>&1 &
APP=$!
for _ in $(seq 1 60); do
    curl -fsS "http://localhost:$PORT/q/health" >/dev/null 2>&1 && break
    kill -0 "$APP" 2>/dev/null || { echo "    it exited:"; tail -20 "$DATA/log"; exit 1; }
    sleep 1
done
curl -fsS "http://localhost:$PORT/q/health" >/dev/null || { echo "    never became healthy"; tail -20 "$DATA/log"; exit 1; }

echo "==> it serves the interface and the API"
curl -fsS "http://localhost:$PORT/" | grep -q "Leading Tone" || { echo "    the frontend is not bundled"; exit 1; }
curl -fsS "http://localhost:$PORT/api/knowledge/status" >/dev/null || { echo "    the knowledge API is down"; exit 1; }

echo "==> the search index opens and answers from inside the jar"
SEARCH=$(curl -fsS "http://localhost:$PORT/api/knowledge/search?q=dominant&mode=hybrid")
echo "$SEARCH" | grep -q '"results"' || { echo "    hybrid search failed: $SEARCH"; exit 1; }

echo
echo "    the packaged jar is sound"
