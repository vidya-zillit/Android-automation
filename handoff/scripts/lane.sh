#!/bin/bash
# lane.sh <name> <device> <Class> [<Class> …] — own project copy, waits for the device lock,
# runs each class with run-and-report --only, keeps every result XML, releases the lock.
NAME=$1; DEV=$2; shift 2
S=/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad
L="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/locks"
MAIN=/Users/vidyasagar/StudioProjects/Android-automation
DIR=$S/lane-$NAME; OUT=$S/results-$NAME; mkdir -p "$DIR" "$OUT"
rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude .claude "$MAIN/" "$DIR/"
got=0; trap '[ $got = 1 ] && rm -rf "$L/$DEV"' EXIT
until mkdir "$L/$DEV" 2>/dev/null; do sleep 20; done; got=1
echo "coordinator: fixed-tests lane $NAME $(date)" > "$L/$DEV/owner"
echo "lane $NAME on $DEV from $(date)"
cd "$DIR"
export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ADB_MDNS=0 ZILLIT_FLAVOR=dev ZILLIT_UDID=$DEV
for C in "$@"; do
  ./run-and-report.sh --only "$C" > "$OUT/$C.log" 2>&1; echo "$C exit=$?"
  cp build/test-results/test/TEST-*"$C".xml "$OUT/" 2>/dev/null
done
echo "lane $NAME done $(date)"
