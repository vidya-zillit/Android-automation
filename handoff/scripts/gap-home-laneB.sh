#!/bin/bash
# Lane B (two-device) per RULE 01:50: Pixel first via mkdir, then ONE line "home" in QUEUE and
# take only an emulator lock whose owner starts with "RESERVED:home". Release both on exit.
S=/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad
N="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c"
L="$N/locks"; OUT=$S/results-gap-home; DIR=$S/gap-home; mkdir -p "$OUT" "$DIR"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; export ADB_MDNS=0
PIXEL=34281FDH2005XC
LANE_B=(TwoUserChatTest.otherMembersMessageOffersNoEdit TwoUserChatTest.unreadBadgeAndNewMessagesDivider
        TwoUserChatTest.liveEditKeepsReplies TwoUserChatTest.readByUpdatesLive
        TwoUserChatTest.searchFindsCommentSender TwoUserChatTest.forwardForwardedCopyToMember
        TwoUserChatTest.otherUserMessageInteractions)
got=""
cleanup() {
  for d in $got; do
    [[ $d == emulator-* ]] && $ADB -s $d shell cmd connectivity airplane-mode disable >/dev/null 2>&1
    rm -rf "$L/$d"
  done
}
trap cleanup EXIT
own() { echo "home/call-sheet web-gap agent: $1 $(date)" > "$L/$2/owner"; }
# Next in line (or already reserved): take the Pixel and the reserved emulator in whichever order
# they come, so a reservation is never left to expire (10 min) while the Pixel is busy.
until [[ "$(head -1 "$N/QUEUE" 2>/dev/null)" == home ]] || grep -lq '^RESERVED:home' "$L"/emulator-*/owner 2>/dev/null; do sleep 15; done
grep -qx home "$N/QUEUE" 2>/dev/null || grep -lq '^RESERVED:home' "$L"/emulator-*/owner 2>/dev/null || echo "home" >> "$N/QUEUE"
HAVE_PIXEL=""; EMU=""
while [[ -z "$HAVE_PIXEL" || -z "$EMU" ]]; do
  if [[ -z "$HAVE_PIXEL" ]] && mkdir "$L/$PIXEL" 2>/dev/null; then
    HAVE_PIXEL=1; got="$got $PIXEL"; own "lane B (Pixel, waiting for RESERVED:home): ${LANE_B[*]}" $PIXEL; echo "pixel taken $(date)"
  fi
  if [[ -z "$EMU" ]]; then
    for f in "$L"/emulator-*/owner; do
      if [[ -f "$f" ]] && head -c 13 "$f" | grep -q '^RESERVED:home'; then
        EMU=$(basename "$(dirname "$f")"); got="$got $EMU"; own "lane B (emulator, waiting for the Pixel): ${LANE_B[*]}" $EMU; echo "emulator $EMU taken $(date)"; break
      fi
    done
  fi
  [[ -z "$HAVE_PIXEL" || -z "$EMU" ]] && sleep 5
done
own "lane B: ${LANE_B[*]}" $EMU; own "lane B: ${LANE_B[*]}" $PIXEL
echo "lane B on $EMU + $PIXEL $(date)"
rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude .claude /Users/vidyasagar/StudioProjects/Android-automation/ "$DIR/"
cd "$DIR"
export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ZILLIT_FLAVOR=dev
for C in "${LANE_B[@]}"; do
  tag=$(echo "$C" | tr '.' '_')
  ZILLIT_UDID=$EMU ZILLIT_SENDER_UDID=$EMU ZILLIT_RECEIVER_UDID=$PIXEL ./run-and-report.sh --only "$C" > "$OUT/$tag.log" 2>&1
  echo "$C exit=$? $(date)"
  cp build/test-results/test/TEST-*.xml "$OUT/" 2>/dev/null
done
echo "done $(date)"
