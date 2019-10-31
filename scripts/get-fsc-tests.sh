#!/bin/bash

# This script fetches Full stack compatibility suite and copies all the feature files and datafiles to the given paths.

# inputs:
# FEATURES_PATH - destination path to copy feature files (required)
# DATAFILES_PATH - destination path to use for datafiles (required)
# TAG_FILTER_EXPRESSION - Gherkin filter
FEATURE_FILES_PATH="${FEATURES_PATH:-}"
DATAFILES_PATH="${DATAFILES_PATH:-}"
TAG_FILTER_EXPRESSION=""
usage() { echo "Usage: $0 -h [-t <string>] [-f <string>] [-d <string>]" 1>&2; }

show_example() { cat <<EOF
Example: $0 -f /usr/tests/integration/features -d /usr/support/fsc-datafiles -t "@FEATURE_ROLLOUT && ~@INPUT_FILTER"
EOF
}

while getopts ":t:f:d:h" o; do
  case "${o}" in    
    f)
      FEATURE_FILES_PATH=${OPTARG}
      ;;
    d)
      DATAFILES_PATH=${OPTARG}
      ;;
    t)
      TAG_FILTER_EXPRESSION=${OPTARG}
      ;;
    h)
      usage
      echo
      show_example
      exit 1
      ;;
    *)
      usage
      exit 1
      ;;
  esac

done
shift $((OPTIND-1))


if [ -z "$FEATURE_FILES_PATH" ]; then
  echo
  echo "-f is a required argument"
  echo
  show_example
  exit 1
fi

if [ -z "$DATAFILES_PATH" ]; then
  echo
  echo "-d is a required argument"
  echo
  show_example
  exit 1
fi

set -ex
FSC_FEATURE_FILES_PATH="$FSC_PATH/features/"
rm -rf $FEATURE_FILES_PATH
mkdir -p $FEATURE_FILES_PATH
cp -r $FSC_FEATURE_FILES_PATH/* $FEATURE_FILES_PATH

FSC_DATAFILES_DIR="$FSC_PATH/features/support/datafiles"
mkdir -p $DATAFILES_PATH
cp -r $FSC_DATAFILES_DIR/*.json "$DATAFILES_PATH"
echo "Ready for testing."
