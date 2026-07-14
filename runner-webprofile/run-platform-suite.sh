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

if [ "$PROTOCOL" != "servlet" ] && [ "$PROTOCOL" != "javatest" ]; then
  echo "Unknown protocol '$PROTOCOL'; supported protocols: servlet, javatest" >&2
  exit 2
fi

TAB=$(printf '\t')
while IFS="$TAB" read -r partition artifact protocol groups source_classes expected_classes test_pattern; do
  case "$partition" in
    ''|'#'*) continue ;;
  esac
  [ "$protocol" = "$PROTOCOL" ] || continue

  echo "Running $partition: $artifact ($groups; source: $source_classes, expected after exclusions: $expected_classes)"
  report_dir="$SCRIPT_DIR/run/target/failsafe-reports/$partition"
  rm -rf "$report_dir"
  "$ROOT_DIR/mvnw" \
    -pl runner-webprofile/run -am \
    "-Dtck.artifact=$artifact" \
    "-Dtck.partition=$partition" \
    "-Dtck.protocol=$protocol" \
    "-Dtck.groups=$groups" \
    "-Dtck.test=$test_pattern" \
    verify </dev/null

  actual_classes=0
  if [ -d "$report_dir" ]; then
    actual_classes=$(find "$report_dir" -type f -name 'TEST-*.xml' | wc -l | tr -d ' ')
  fi
  if [ "$actual_classes" != "$expected_classes" ]; then
    echo "Suite scope mismatch for $partition: expected $expected_classes classes, ran $actual_classes" >&2
    exit 1
  fi
done < "$MANIFEST"
