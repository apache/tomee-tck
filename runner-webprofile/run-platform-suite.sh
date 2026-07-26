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

# The TomEE distribution under test. The default manifest and exclusions
# describe the EclipseLink-based Plume ZIP; another TOMEE_CLASSIFIER selects
# that distribution together with its platform-suite-<classifier>.tsv counts
# and exclusions/<classifier> overrides, when those exist.
TOMEE_CLASSIFIER=${TOMEE_CLASSIFIER:-plume}
case "$TOMEE_CLASSIFIER" in
  webprofile|microprofile|plus|plume) ;;
  *)
    echo "Unknown TOMEE_CLASSIFIER '$TOMEE_CLASSIFIER'; supported: webprofile, microprofile, plus, plume" >&2
    exit 2 ;;
esac

# TOMEE_VERSION points the suite at a specific TomEE build -- typically one
# environment/tomee/build-tomee.sh produced from a tag or branch. Unset, the
# pom's default snapshot version applies.
MANIFEST="$SCRIPT_DIR/platform-suite.tsv"
# A classifier-specific manifest carries only the rows that differ; merge it
# over the base manifest by partition id.
OVERRIDES="$SCRIPT_DIR/platform-suite-$TOMEE_CLASSIFIER.tsv"
if [ -f "$OVERRIDES" ]; then
  MERGED=$(mktemp "${TMPDIR:-/tmp}/platform-suite.XXXXXX")
  trap 'rm -f "$MERGED"' EXIT HUP INT TERM
  awk -F'\t' '
    NR == FNR { if ($0 !~ /^(#|$)/) override[$1] = $0; next }
    { print ($1 in override ? override[$1] : $0) }
  ' "$OVERRIDES" "$MANIFEST" > "$MERGED"
  MANIFEST="$MERGED"
fi

if [ "$PROTOCOL" != "servlet" ] && [ "$PROTOCOL" != "javatest" ]; then
  echo "Unknown protocol '$PROTOCOL'; supported protocols: servlet, javatest" >&2
  exit 2
fi

# Select free ports once at branch start; all partitions in the loop run
# sequentially on one node so a single selection covers them. Each preferred
# port avoids the ones already chosen. The validate-phase guards inside each
# mvnw run then assert these chosen ports immediately before servers start.
SELECT_PORT="$ROOT_DIR/environment/ports/select-free-port.sh"
TOMEE_HTTP_PORT=$(sh "$SELECT_PORT" 8080)
TOMEE_HTTPS_PORT=$(sh "$SELECT_PORT" 8443 "$TOMEE_HTTP_PORT")
TOMEE_SHUTDOWN_PORT=$(sh "$SELECT_PORT" 8005 "$TOMEE_HTTP_PORT" "$TOMEE_HTTPS_PORT")
TCK_DERBY_PORT=$(sh "$SELECT_PORT" 1527 "$TOMEE_HTTP_PORT" "$TOMEE_HTTPS_PORT" "$TOMEE_SHUTDOWN_PORT")
TCK_HARNESS_LOG_PORT=$(sh "$SELECT_PORT" 2000 "$TOMEE_HTTP_PORT" "$TOMEE_HTTPS_PORT" "$TOMEE_SHUTDOWN_PORT" "$TCK_DERBY_PORT")
echo "Using ports: http=$TOMEE_HTTP_PORT https=$TOMEE_HTTPS_PORT shutdown=$TOMEE_SHUTDOWN_PORT derby=$TCK_DERBY_PORT harness-log=$TCK_HARNESS_LOG_PORT"

# Build the shared modules once so the per-partition runs can skip -am and
# not rebuild the unchanged reactor every iteration.
"$ROOT_DIR/mvnw" -B -ntp -pl tomee-porting -am install </dev/null

TAB=$(printf '\t')
while IFS="$TAB" read -r partition artifact protocol groups source_classes expected_classes test_pattern; do
  case "$partition" in
    ''|'#'*) continue ;;
  esac
  [ "$protocol" = "$PROTOCOL" ] || continue
  [ -z "$ONLY_PARTITION" ] || [ "$partition" = "$ONLY_PARTITION" ] || continue

  exclusions_file="$SCRIPT_DIR/exclusions/$partition.txt"
  if [ -f "$SCRIPT_DIR/exclusions/$TOMEE_CLASSIFIER/$partition.txt" ]; then
    exclusions_file="$SCRIPT_DIR/exclusions/$TOMEE_CLASSIFIER/$partition.txt"
  fi

  echo "Running $partition: $artifact ($groups; classifier: $TOMEE_CLASSIFIER; source: $source_classes, expected after exclusions: $expected_classes)"
  report_dir="$SCRIPT_DIR/run/target/failsafe-reports/$partition"
  rm -rf "$report_dir"
  "$ROOT_DIR/mvnw" \
    -pl runner-webprofile/run \
    ${TOMEE_VERSION:+"-Dtomee.version=$TOMEE_VERSION"} \
    "-Dtomee.classifier=$TOMEE_CLASSIFIER" \
    "-Dtck.artifact=$artifact" \
    "-Dtck.partition=$partition" \
    "-Dtck.protocol=$protocol" \
    "-Dtck.groups=$groups" \
    "-Dtck.test=$test_pattern" \
    "-Dtck.exclusions.file=$exclusions_file" \
    "-Dtomee.http.port=$TOMEE_HTTP_PORT" \
    "-Dtomee.https.port=$TOMEE_HTTPS_PORT" \
    "-Dtomee.shutdown.port=$TOMEE_SHUTDOWN_PORT" \
    "-Dtck.derby.port=$TCK_DERBY_PORT" \
    "-Dtck.harness.log.port=$TCK_HARNESS_LOG_PORT" \
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
