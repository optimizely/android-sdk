#!/usr/bin/env bash
set -euo pipefail

WORKDIR=/tmp/workdir
mkdir -p "$WORKDIR"
wget -q "https://dl.google.com/android/repository/sdk-tools-linux-$SDK_TOOLS.zip" -O "$WORKDIR/android-sdk-tools.zip" && unzip -q "$WORKDIR/android-sdk-tools.zip" -d "$ANDROID_HOME" && rm "$WORKDIR/android-sdk-tools.zip"
mkdir -p ~/.android && touch ~/.android/repositories.cfg
(echo y; echo y; echo y; echo y; echo y; echo y) | sdkmanager --licenses > /dev/null
sdkmanager "emulator" "tools" "platform-tools" > /dev/null
sdkmanager "build-tools;$ANDROID_BUILD_TOOLS" "platforms;android-$ANDROID_API" > /dev/null
sdkmanager "system-images;android-$EMULATOR_API;google_apis;armeabi-v7a" > /dev/null
sdkmanager "extras;google;m2repository" "extras;android;m2repository" "add-ons;addon-google_apis-google-19" > /dev/null
sdkmanager --list | head -15
echo no | avdmanager create avd -n test -k "system-images;android-$EMULATOR_API;google_apis;armeabi-v7a"
"$ANDROID_HOME/emulator/emulator" -avd test -no-audio -no-window &
scripts/android-wait-for-emulator.sh
adb shell input keyevent 82
echo "$0 finished execution"
