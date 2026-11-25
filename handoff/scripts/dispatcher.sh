#!/bin/bash
# dispatcher.sh — fair hand-out of emulators. Agents waiting for an emulator append their name
# (one line) to locks/QUEUE. Whenever an emulator lock is free, the dispatcher takes it, writes
# owner "RESERVED:<name>" and pops that name. Agents accept a lock whose owner starts with
# "RESERVED:<their name>" (overwrite owner, run, release as usual). If a reservation is not
# picked up within 10 min, it is released.
L="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/locks"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; export ADB_MDNS=0
Q="$L/../QUEUE"; touch "$Q"
while true; do
  # expire stale reservations
  for o in "$L"/emulator-*/owner; do
    [ -f "$o" ] || continue
    if grep -q '^RESERVED:' "$o" && [ $(( $(date +%s) - $(stat -f %m "$o") )) -gt 600 ]; then
      echo "$(date) expired $(cat "$o") on $(basename "$(dirname "$o")")"; rm -rf "$(dirname "$o")"
    fi
  done
  head=$(grep -m1 -v '^\s*$' "$Q")
  if [ -n "$head" ]; then
    for d in $($ADB devices | awk '/^emulator-[0-9]+\tdevice/{print $1}'); do
      if mkdir "$L/$d" 2>/dev/null; then
        echo "RESERVED:$head by dispatcher $(date)" > "$L/$d/owner"
        awk -v h="$head" 'BEGIN{d=0} {if(!d && $0==h){d=1; next} print}' "$Q" > "$Q.tmp" && mv "$Q.tmp" "$Q"
        echo "$(date) $d -> $head"; break
      fi
    done
  fi
  sleep 1
done
