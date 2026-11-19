#!/usr/bin/env bash
# Screenshots of the demo on the test phone: tools/capture.sh ru|en → docs/screenshots/<lang>/*.png
# Builds -Pdemo -Pshots (the pet stands still), installs, starts each screen by intent and takes the screen.
set -eu
lang=${1:-ru}
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
./gradlew assembleDebug -Pdemo -Pshots -Pdemolang=$lang -q
bash tools/install.sh app/build/outputs/apk/debug/app-debug.apk | tail -1
pkg=app.caregotchi.demo
adb shell pm clear $pkg >/dev/null
adb shell am start -n $pkg/app.caregotchi.MainActivity >/dev/null; sleep 14
out=docs/screenshots/$lang; mkdir -p $out
day=$(( $(date +%s) / 86400 - 1 ))
shot() { adb shell am force-stop $pkg; adb shell am start -n $pkg/app.caregotchi.MainActivity --es open "$2" -f 0x24000000 >/dev/null; sleep ${3:-6}
  f=$(adb shell dumpsys window | grep -m1 mCurrentFocus); case "$f" in *$pkg*) ;; *) echo "not in front: $f"; exit 1;; esac
  adb exec-out screencap -p > $out/$1.png; echo $1; }
shot 1-home tab:2
shot 2-sill tab:0
shot 3-diary tab:1
shot 4-nest tab:3
shot 5-shop tab:4
shot 6-day "day:$day"
shot 7-sport sheet:move
shot 8-bad bad
shot 9-settings settings
