#!/bin/bash
# run2q.sh <Class>… — two-device queued run: Pixel via mkdir first, then an emulator from the queue: append "drive" to QUEUE, take the lock the dispatcher
# reserves for drive (owner "RESERVED:drive…"), run each class with --only, release.
D=/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad/drive
N="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c"
L="$N/locks"; MAIN=/Users/vidyasagar/StudioProjects/Android-automation; OUT=$D/results; mkdir -p "$OUT"
DEV=""; got=0; PIX=34281FDH2005XC; gotP=0
cleanup() {
  if [ "$gotP" = 1 ]; then
    for s in $(curl -s 127.0.0.1:4723/sessions | python3 -I -c "import sys,json;[print(x['id']) for x in json.load(sys.stdin)['value'] if x['capabilities'].get('udid')=='$PIX']" 2>/dev/null); do
      curl -s -X DELETE 127.0.0.1:4723/session/$s >/dev/null; done
    rm -rf "$L/$PIX"
  fi
  [ "$got" = 1 ] || return
  for s in $(curl -s 127.0.0.1:4723/sessions | python3 -I -c "import sys,json;[print(x['id']) for x in json.load(sys.stdin)['value'] if x['capabilities'].get('udid')=='$DEV']" 2>/dev/null); do
    curl -s -X DELETE 127.0.0.1:4723/session/$s >/dev/null; done
  rm -rf "$L/$DEV"
}
trap cleanup EXIT
until mkdir "$L/$PIX" 2>/dev/null; do sleep 10; done; gotP=1
echo "drive agent (two-device, B=Pixel, waiting for a queued emulator): $* $(date)" > "$L/$PIX/owner"
echo drive >> "$N/QUEUE"
while [ -z "$DEV" ]; do
  for d in "$L"/emulator-*; do
    [ -f "$d/owner" ] || continue
    if head -c 14 "$d/owner" | grep -q "^RESERVED:drive"; then
      DEV=$(basename "$d"); echo "drive agent: $* $(date)" > "$d/owner"; got=1; break
    fi
  done
  [ -z "$DEV" ] && sleep 5
done
echo "devices A=$DEV B=$PIX $(date)"
echo "drive agent (two-device A=$DEV B=$PIX): $* $(date)" > "$L/$PIX/owner"
R=$D/repo-2dev; mkdir -p "$R"
rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude .claude "$MAIN/" "$R/"
[ -e "$R/node_modules" ] || ln -s $MAIN/node_modules "$R/node_modules"
cd "$R"
export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ADB_MDNS=0 ZILLIT_FLAVOR=dev ZILLIT_UDID=$DEV ZILLIT_APP_PACKAGE=com.zillit.zillitapp.dev
for C in "$@"; do
  if [ "$C" = DriveSharedWithMeTest ]; then U=$PIX; else U=$DEV; fi
  ZILLIT_UDID=$U ZILLIT_SENDER_UDID=$DEV ZILLIT_RECEIVER_UDID=$PIX ./run-and-report.sh --only "$C" > "$OUT/${C%%.*}.log" 2>&1; echo "$C exit=$? $(date)"
  cp build/test-results/test/TEST-*"${C%%.*}".xml "$OUT/" 2>/dev/null
  mkdir -p "$OUT/shots-${C%%.*}"; cp -r build/screenshots/* "$OUT/shots-${C%%.*}/" 2>/dev/null
done
