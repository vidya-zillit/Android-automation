#!/bin/bash
DEV=$1; APK=/Users/vidyasagar/StudioProjects/Zillit-Android-devbuild/app/build/outputs/apk/develop/debug/app-develop-debug.apk
L="/Users/vidyasagar/Library/Application Support/Claude/side-session-notes/local_43b380ae-985d-4659-a61e-6ec09577973c/locks"
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH" ADB_MDNS=0
got=0; trap '[ $got = 1 ] && rm -rf "$L/$DEV"' EXIT
until mkdir "$L/$DEV" 2>/dev/null; do sleep 15; done; got=1
echo "coordinator: installing development build $(date)" > "$L/$DEV/owner"
echo "$DEV: installing $(date +%T)"
adb -s $DEV install -r -d "$APK" 2>&1 | tail -2
echo "$DEV: $(adb -s $DEV shell dumpsys package com.zillit.zillitapp.dev | grep -E 'versionName|lastUpdateTime' | tr -s ' ' | tr '\n' ' ')"
adb -s $DEV shell monkey -p com.zillit.zillitapp.dev -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
