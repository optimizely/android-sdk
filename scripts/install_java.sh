#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

# this script removes any existing java that comes with a travis worker and installs our own


function cleanup_ {
  # remove all java versions that come with worker image
  #sudo apt-get -y remove default-jdk default-jre default-jdk-headless default-jre-headless ca-certificates-java openjdk-8-jdk openjdk-8-jdk-headless openjdk-8-jre openjdk-8-jre-headless
  # just because we are paranoid, lets make sure we get them all
  dpkg-query -f '${Package} ${Status}\n' -W "openjdk*" | grep 'ok installed' | awk '{print $1}' | xargs sudo apt-get -y remove
  # remove java custom installed by travis since $PATH looks for this first before /usr/lib/jvm
  sudo rm -rf /usr/local/lib/jvm
}

function install_ {
  TARBALL=jdk-8u211-linux-x64.tar.gz
  WORKDIR=/tmp/workdir
  mkdir -p $WORKDIR
  # fetch manually downloaded tarbal of oracle's jdk 8
  aws s3 cp --quiet "s3://optly-fs-travisci-artifacts/java/$TARBALL" "$WORKDIR/"
  # get java installer script
  wget -O $WORKDIR/install-java.sh https://raw.githubusercontent.com/juancarlostong/install-java/master/install-java.sh
  chmod u+x $WORKDIR/install-java.sh
  # install
  touch "$HOME/.bashrc"
  yes | sudo bash -eux $WORKDIR/install-java.sh -f $WORKDIR/$TARBALL
}

function verify_ {
  echo "set this in your .travis.yml global env:"
  echo "JAVA_HOME=$JAVA_HOME"
  which java
  java -version
  echo "$0 finished execution"
}

function main {
  cleanup_
  install_
  verify_
}
main
