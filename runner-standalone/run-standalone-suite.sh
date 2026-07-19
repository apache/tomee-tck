#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Runs one standalone specification TCK runner against TomEE.
#
#   runner-standalone/run-standalone-suite.sh <id> [extra mvn args]
#
# Container-based runners default to ports 8080/8443/8005/1527; run one at
# a time. -Dtck.derby.port overrides the Derby port everywhere, and the
# container-based runners honor -Dtomee.http.port/-Dtomee.https.port/
# -Dtomee.shutdown.port for side-by-side runs (the security, authentication,
# and faces source reactors still assume the fixed ports). Use the overrides
# whenever anything else may hold 8080: the Arquillian adapter silently
# attaches to any server already on the port.
# TOMEE_CLASSIFIER selects the distribution (default: plume).
# The reviewed exclusion list in runner-standalone/exclusions/<id>.txt is
# applied by default; append -Dtck.exclusions.file=... to override (see
# exclusions/none.txt for full baseline runs).

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ID=${1:-}
TOMEE_CLASSIFIER=${TOMEE_CLASSIFIER:-plume}

usage() {
  cat >&2 <<'EOF'
Usage: run-standalone-suite.sh <id> [extra mvn args]
Runners: annotations, concurrency, data, di, el, cdi, cdi-ee, servlet,
         pages, rest, validation, websocket, jsonp, jsonb, debugging,
         persistence, transactions, security, security-old, authentication,
         faces, faces-old
See runner-standalone/README.md for per-TCK status.
EOF
  exit 2
}

# A runner id maps to its module directory, preceded by its <id>-install
# twin when one exists. The source-reactor runners manage their own TomEE
# and need the full Maven lifecycle up to verify for their invoker/JavaTest
# runs; everything else stops at test.
GOAL=test
case "$ID" in
  ''|*-install|tck-common|exclusions|*/*|.*) usage ;;
  security|security-old|authentication|faces|faces-old|transactions)
    GOAL=verify ;;
esac
[ -f "$SCRIPT_DIR/$ID/pom.xml" ] || usage

MODULES="runner-standalone/$ID"
if [ -f "$SCRIPT_DIR/$ID-install/pom.xml" ]; then
  MODULES="runner-standalone/$ID-install,$MODULES"
fi

shift
exec "$ROOT_DIR/mvnw" -B -ntp \
  -pl "$MODULES" -am \
  -Dtck.standalone=true \
  "-Dtomee.classifier=$TOMEE_CLASSIFIER" \
  "$GOAL" "$@"
