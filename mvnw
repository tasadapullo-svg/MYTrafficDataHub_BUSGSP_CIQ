#!/bin/sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Could not find $WRAPPER_PROPERTIES." >&2
  exit 1
fi

DISTRIBUTION_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPERTIES" | sed 's/^distributionUrl=//')

if [ -z "$DISTRIBUTION_URL" ]; then
  echo "distributionUrl is missing in $WRAPPER_PROPERTIES." >&2
  exit 1
fi

DIST_FILE=$(basename "$DISTRIBUTION_URL")
DIST_NAME=${DIST_FILE%-bin.zip}
CACHE_DIR="${HOME}/.m2/wrapper/dists/${DIST_NAME}"
ZIP_PATH="${CACHE_DIR}/${DIST_FILE}"
EXTRACT_DIR="${CACHE_DIR}/${DIST_NAME}"
MVN_CMD="${EXTRACT_DIR}/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
  mkdir -p "$CACHE_DIR"
  if [ ! -f "$ZIP_PATH" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DISTRIBUTION_URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$DISTRIBUTION_URL" -O "$ZIP_PATH"
    else
      echo "curl or wget is required to download Maven." >&2
      exit 1
    fi
  fi
  if [ ! -d "$EXTRACT_DIR" ]; then
    unzip -q -o "$ZIP_PATH" -d "$CACHE_DIR"
  fi
fi

exec "$MVN_CMD" "$@"

