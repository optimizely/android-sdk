#!/usr/bin/env bash
set -euox pipefail

# requires Python 2 version 2.6.5+ or Python 3 version 3.3+

WORKDIR=/tmp/workdir
curl -s "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "$WORKDIR/awscli-bundle.zip"
unzip "$WORKDIR/awscli-bundle.zip" -d "$WORKDIR"
sudo "$WORKDIR/awscli-bundle/install" -i /usr/local/aws -b /usr/local/bin/aws
rm -rf "$WORKDIR/awscli-bundle"
rm "$WORKDIR/awscli-bundle.zip"
echo "$0 finished execution"
