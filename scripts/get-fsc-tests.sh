#!/bin/bash

set -e
FSC_PATH=tmp/fsc-repo
rm -rf $FSC_PATH
mkdir -p $FSC_PATH
test -n "$FSC_PATH" && if [ "$FSC_PATH" != "git" ]; then
  exit 0
fi
 
pushd $FSC_PATH && git init && git fetch --depth=1 https://$CI_USER_TOKEN@github.com/optimizely/fullstack-sdk-compatibility-suite ${FSC_BRANCH:-master} && git checkout FETCH_HEAD && popd
cp ./$FSC_PATH/features/support/datafiles ./test-app/src/main/res/raw
cp ./$FSC_PATH/features/ ./test-app/src/androidTest/assets/features

echo "Ready for testing."
