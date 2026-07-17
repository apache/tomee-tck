#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
PROTOCOL=${1:-servlet}
ONLY_PARTITION=${2:-}

# The TomEE distribution under test. The default Web Profile manifest and
# exclusions describe the webprofile ZIP (OpenJPA); TOMEE_CLASSIFIER=plume
# selects the EclipseLink-based distribution together with its own
# platform-suite-plume.tsv counts and exclusions/plume overrides.
TOMEE_CLASSIFIER=${TOMEE_CLASSIFIER:-webprofile}
MANIFEST="$SCRIPT_DIR/platform-suite.tsv"
if [ "$TOMEE_CLASSIFIER" != "webprofile" ]; then
  if [ ! -f "$SCRIPT_DIR/platform-suite-$TOMEE_CLASSIFIER.tsv" ]; then
    echo "No manifest platform-suite-$TOMEE_CLASSIFIER.tsv for TOMEE_CLASSIFIER=$TOMEE_CLASSIFIER" >&2
    exit 2
  fi
  MANIFEST="$SCRIPT_DIR/platform-suite-$TOMEE_CLASSIFIER.tsv"
fi

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
  [ -z "$ONLY_PARTITION" ] || [ "$partition" = "$ONLY_PARTITION" ] || continue

  exclusions_file="$SCRIPT_DIR/exclusions/$partition.txt"
  if [ "$TOMEE_CLASSIFIER" != "webprofile" ] && [ -f "$SCRIPT_DIR/exclusions/$TOMEE_CLASSIFIER/$partition.txt" ]; then
    exclusions_file="$SCRIPT_DIR/exclusions/$TOMEE_CLASSIFIER/$partition.txt"
  fi

  echo "Running $partition: $artifact ($groups; classifier: $TOMEE_CLASSIFIER; source: $source_classes, expected after exclusions: $expected_classes)"
  report_dir="$SCRIPT_DIR/run/target/failsafe-reports/$partition"
  rm -rf "$report_dir"
  "$ROOT_DIR/mvnw" \
    -pl runner-webprofile/run -am \
    "-Dtomee.classifier=$TOMEE_CLASSIFIER" \
    "-Dtck.artifact=$artifact" \
    "-Dtck.partition=$partition" \
    "-Dtck.protocol=$protocol" \
    "-Dtck.groups=$groups" \
    "-Dtck.test=$test_pattern" \
    "-Dtck.exclusions.file=$exclusions_file" \
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
