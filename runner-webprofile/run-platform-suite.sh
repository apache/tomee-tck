#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
MANIFEST="$SCRIPT_DIR/platform-suite.tsv"
PROTOCOL=${1:-servlet}

if [ "$PROTOCOL" != "servlet" ]; then
  echo "Protocol '$PROTOCOL' is not wired yet; supported protocol: servlet" >&2
  exit 2
fi

TAB=$(printf '\t')
while IFS="$TAB" read -r partition artifact protocol groups expected_classes; do
  case "$partition" in
    ''|'#'*) continue ;;
  esac
  [ "$protocol" = "$PROTOCOL" ] || continue

  echo "Running $partition: $artifact ($groups; expected source classes: $expected_classes)"
  "$ROOT_DIR/mvnw" \
    -pl runner-webprofile/run -am \
    "-Dtck.artifact=$artifact" \
    "-Dtck.partition=$partition" \
    "-Dtck.groups=$groups" \
    verify

  report_dir="$SCRIPT_DIR/run/target/failsafe-reports/$partition"
  actual_classes=$(find "$report_dir" -type f -name 'TEST-*.xml' | wc -l | tr -d ' ')
  if [ "$actual_classes" != "$expected_classes" ]; then
    echo "Suite scope mismatch for $partition: expected $expected_classes classes, ran $actual_classes" >&2
    exit 1
  fi
done < "$MANIFEST"
