#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Runs one standalone specification TCK runner against TomEE.
#
#   runner-standalone/run-standalone-suite.sh <id> [extra mvn args]
#
# Container-based runners default to ports 8080/8443/8005/1527. At branch
# start the script selects free ports for exactly what the chosen runner
# binds and passes them as the documented -D overrides
# (-Dtomee.http.port/-Dtomee.https.port/-Dtomee.shutdown.port for the TomEE
# container, -Dtck.derby.port for Derby, -Dtck.harness.log.port for the
# JavaTest harness log listener); a caller-supplied -D always wins. The
# security source reactor still assumes its fixed ports. Selection matters
# because the Arquillian adapter silently attaches to any server already on
# the port, so a foreign server on 8080 would make a runner look green.
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

# Select free ports for exactly what this runner binds and append them as -D
# overrides. A caller-supplied -D for the same knob always wins; its value
# still seeds the later picks' avoid lists so a selection can never return a
# port the caller reserved for another knob. Last match wins, like Maven.
SELECT_PORT="$ROOT_DIR/environment/ports/select-free-port.sh"
arg_value() { prefix=$1; shift; v=''; for a in "$@"; do case $a in "$prefix"*) v=${a#"$prefix"} ;; esac; done; printf '%s' "$v"; }

NEED_TOMEE=no NEED_DERBY=no NEED_HARNESS=no NEED_HTTP_ONLY=no HARNESS_PREF=2000
case "$ID" in
  concurrency|data|servlet|pages|rest|validation|websocket|cdi|cdi-ee) NEED_TOMEE=yes; NEED_DERBY=yes ;;
  persistence) NEED_DERBY=yes ;;
  transactions|faces-old) NEED_TOMEE=yes; NEED_HARNESS=yes ;;
  security-old) NEED_TOMEE=yes; NEED_DERBY=yes; NEED_HARNESS=yes; HARNESS_PREF=2100 ;;
  authentication|faces) NEED_HTTP_ONLY=yes ;;
  # security: the downloaded reactor pins 8080/8443/8005/33389; its pom
  # asserts them free instead (no selection possible).
  # annotations, di, el, jsonp, jsonb, debugging: no ports.
esac

# Ports no selection may hand out, whatever else is free. The rest runner's
# SeBootstrap tests boot their own embedded Jetty on the SeBootstrap default
# port the spec mandates (8080) and cannot be pointed elsewhere, so TomEE must
# not take it: inside the branch's private network namespace 8080 is otherwise
# free and the selector's preferred pick would collide with the test server.
RESERVED=
case "$ID" in
  rest) RESERVED=8080 ;;
esac

selected=no
if [ "$NEED_TOMEE" = yes ] || [ "$NEED_HTTP_ONLY" = yes ]; then
  HTTP=$(arg_value -Dtomee.http.port= "$@")
  if [ -z "$HTTP" ]; then
    HTTP=$(sh "$SELECT_PORT" 8080 $RESERVED); set -- "$@" "-Dtomee.http.port=$HTTP"; selected=yes
  fi
fi
if [ "$NEED_TOMEE" = yes ]; then
  HTTPS=$(arg_value -Dtomee.https.port= "$@")
  if [ -z "$HTTPS" ]; then
    HTTPS=$(sh "$SELECT_PORT" 8443 $RESERVED ${HTTP:-}); set -- "$@" "-Dtomee.https.port=$HTTPS"; selected=yes
  fi
  SHUTDOWN=$(arg_value -Dtomee.shutdown.port= "$@")
  if [ -z "$SHUTDOWN" ]; then
    SHUTDOWN=$(sh "$SELECT_PORT" 8005 $RESERVED ${HTTP:-} ${HTTPS:-}); set -- "$@" "-Dtomee.shutdown.port=$SHUTDOWN"; selected=yes
  fi
fi
if [ "$NEED_DERBY" = yes ]; then
  DERBY=$(arg_value -Dtck.derby.port= "$@")
  if [ -z "$DERBY" ]; then
    DERBY=$(sh "$SELECT_PORT" 1527 $RESERVED ${HTTP:-} ${HTTPS:-} ${SHUTDOWN:-}); set -- "$@" "-Dtck.derby.port=$DERBY"; selected=yes
  fi
fi
if [ "$NEED_HARNESS" = yes ]; then
  HARNESS=$(arg_value -Dtck.harness.log.port= "$@")
  if [ -z "$HARNESS" ]; then
    HARNESS=$(sh "$SELECT_PORT" "$HARNESS_PREF" $RESERVED ${HTTP:-} ${HTTPS:-} ${SHUTDOWN:-} ${DERBY:-}); set -- "$@" "-Dtck.harness.log.port=$HARNESS"; selected=yes
  fi
fi
if [ "$selected" = yes ]; then
  echo "run-standalone-suite: selected ports http=${HTTP:-} https=${HTTPS:-} shutdown=${SHUTDOWN:-} derby=${DERBY:-} harness=${HARNESS:-}" >&2
fi

exec "$ROOT_DIR/mvnw" -B -ntp \
  -pl "$MODULES" -am \
  -Dtck.standalone=true \
  "-Dtomee.classifier=$TOMEE_CLASSIFIER" \
  "$GOAL" "$@"
