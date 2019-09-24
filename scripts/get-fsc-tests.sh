#!/bin/bash
# This script fetches Full stack compatibility suite and copy feature files in to fsc-app assets directory
# and copy datafiles from Full stack compatibility suite in to raw datafiles folder of fsc-app module
set -e
FSC_PATH=tmp/fsc-repo
rm -rf $FSC_PATH
mkdir -p $FSC_PATH
 
pushd $FSC_PATH && git init && git fetch --depth=1 https://$CI_USER_TOKEN@github.com/optimizely/fullstack-sdk-compatibility-suite ${FSC_BRANCH:-master} && git checkout FETCH_HEAD && popd
mkdir -p ./fsc-app/src/main/res/raw
cp -r ./$FSC_PATH/features/support/datafiles/*.json ./fsc-app/src/main/res/raw/
mkdir -p ./fsc-app/src/androidTest/assets/features
cp -r ./$FSC_PATH/features/* ./fsc-app/src/androidTest/assets/features/
echo "Ready for testing."
