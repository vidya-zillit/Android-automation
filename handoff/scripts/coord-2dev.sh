#!/bin/bash
# coord-2dev.sh — coordinator lane for two-device suites whose agents have finished.
# Pixel lock first (mkdir), then an emulator from the QUEUE (name "coord"), run each spec, release.
S=/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad
N="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c"; L="$N/locks"
P=34281FDH2005XC; DIR=$S/coord-2dev; OUT=$S/results-coord-2dev; mkdir -p "$DIR" "$OUT"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; export ADB_MDNS=0
EMU=""; gotP=0
cleanup() { pkill -f "gradle-worker.*coord-2dev" 2>/dev/null; [ -n "$EMU" ] && rm -rf "${L:?}/${EMU:?}"; [ $gotP = 1 ] && rm -rf "${L:?}/${P:?}"; }
trap cleanup EXIT
until mkdir "$L/$P" 2>/dev/null; do sleep 15; done; gotP=1
echo "coordinator 2-device lane (Pixel, waiting for an emulator) $(date)" > "$L/$P/owner"; echo "PIXEL $(date)"
echo coord >> "$N/QUEUE"
while [ -z "$EMU" ]; do for o in "$L"/emulator-*/owner; do grep -q '^RESERVED:coord' "$o" 2>/dev/null && EMU=$(basename "$(dirname "$o")") && break; done; [ -z "$EMU" ] && sleep 3; done
echo "coordinator 2-device lane: $* $(date)" > "$L/$EMU/owner"; echo "GOT $EMU $(date)"
rsync -a --delete --exclude build --exclude .gradle --exclude .git --exclude .idea --exclude .claude /Users/vidyasagar/StudioProjects/Android-automation/ "$DIR/"
cd "$DIR"; export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ZILLIT_FLAVOR=dev
export ZILLIT_UDID=$EMU ZILLIT_SENDER_UDID=$EMU ZILLIT_RECEIVER_UDID=$P
for C in "$@"; do tag=$(echo "$C" | tr '.*' '__'); ./run-and-report.sh --only "$C" > "$OUT/$tag.log" 2>&1; echo "$C exit=$? $(date)"; cp build/test-results/test/TEST-*.xml "$OUT/" 2>/dev/null; done
echo "DONE $(date)"
