#!/bin/bash

# Originally written by Ralf Kistner <ralf@embarkmobile.com>, but placed in the public domain

set +e

bootanim=""
failcounter=0
#timeout_in_sec=360 # 6 minutes
timeout_in_sec=900 # 15 minutes
echo "Waiting for emulator to start"
until [[ "$bootanim" =~ "stopped" ]]; do
  bootanim=`adb -e shell getprop init.svc.bootanim 2>&1 &`
#echo bootanim=\`$bootanim\`
  if [[ "$bootanim" =~ "device not found" || "$bootanim" =~ "device offline"
    || "$bootanim" =~ "running" || "$bootanim" =~  "error: no emulators found" ]]; then
    let "failcounter += 5"
    echo -n "."
    if [[ $failcounter -gt timeout_in_sec ]]; then
      echo "Timeout ($timeout_in_sec seconds) reached; failed to start emulator"
      exit 1
    fi
  else
    if [[ ! "$bootanim" =~ "stopped" ]]; then
      echo "unexpected behavior from (adb -e shell getprop init.svc.bootanim): $bootanim"
      exit 1
    fi
  fi
  sleep 5
done

echo "Emulator is ready (took $failcounter seconds)"
