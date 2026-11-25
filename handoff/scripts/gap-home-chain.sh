#!/bin/bash
# gap-home-chain.sh — Home/Call Sheet web-gap agent.
# 1) Take an emulator: a free one, or one whose lock owner starts with "RESERVED:home" (coordinator).
# 2) Run lane A (single-device suites) on it.
# 3) Keep the emulator, wait for the Pixel lock, run lane B (two-device) on emulator + Pixel.
# 4) Release both (and switch airplane mode off on the emulator) on exit.
S=/private/tmp/claude-501/-Users-vidyasagar-StudioProjects-Android-automation/b1614600-558f-4f70-91b2-a2665802f9c5/scratchpad
L="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/locks"
OUT=$S/results-gap-home; DIR=$S/gap-home; mkdir -p "$OUT" "$DIR"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; export ADB_MDNS=0
PIXEL=34281FDH2005XC
LANE_A=(HomeWebGapTest CallSheetTest HomeChatTest ChatOptionsTest)
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

EMU=""
while [[ -z "$EMU" ]]; do
  for d in $($ADB devices | awk '/^emulator-[0-9]+\tdevice/{print $1}'); do
    if mkdir "$L/$d" 2>/dev/null; then EMU=$d; break; fi
    if [[ -f "$L/$d/owner" ]] && head -c 13 "$L/$d/owner" | grep -q '^RESERVED:home'; then EMU=$d; break; fi
  done
  [[ -z "$EMU" ]] && sleep 10
done
got="$EMU"; own "lane A: ${LANE_A[*]}" $EMU
echo "lane A on $EMU $(date)"
rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude .claude /Users/vidyasagar/StudioProjects/Android-automation/ "$DIR/"
cd "$DIR"
export ANDROID_HOME=$HOME/Library/Android/sdk PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ZILLIT_FLAVOR=dev
for C in "${LANE_A[@]}"; do
  ZILLIT_UDID=$EMU ./run-and-report.sh --only "$C" > "$OUT/$C.log" 2>&1; echo "$C exit=$? $(date)"
  cp build/test-results/test/TEST-*.xml "$OUT/" 2>/dev/null
done
own "lane B (waiting for the Pixel): ${LANE_B[*]}" $EMU
echo "lane A done; waiting for the Pixel $(date)"
until mkdir "$L/$PIXEL" 2>/dev/null; do sleep 10; done
got="$got $PIXEL"; own "lane B: ${LANE_B[*]}" $PIXEL; own "lane B: ${LANE_B[*]}" $EMU
echo "lane B on $EMU + $PIXEL $(date)"
rsync -a --delete --exclude build --exclude .gradle --exclude node_modules --exclude .git --exclude .idea --exclude .claude /Users/vidyasagar/StudioProjects/Android-automation/ "$DIR/"
for C in "${LANE_B[@]}"; do
  tag=$(echo "$C" | tr '.' '_')
  ZILLIT_UDID=$EMU ZILLIT_SENDER_UDID=$EMU ZILLIT_RECEIVER_UDID=$PIXEL ./run-and-report.sh --only "$C" > "$OUT/$tag.log" 2>&1
  echo "$C exit=$? $(date)"
  cp build/test-results/test/TEST-*.xml "$OUT/" 2>/dev/null
done
echo "done $(date)"
