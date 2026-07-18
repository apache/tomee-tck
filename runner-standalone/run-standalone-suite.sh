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
# rest runner also honors -Dtomee.http.port/-Dtomee.https.port/
# -Dtomee.shutdown.port for side-by-side runs.
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
Runners: annotations, concurrency, data, di, cdi, cdi-ee, servlet, pages,
         rest, validation, websocket, security, authentication, faces
See runner-standalone/README.md for per-TCK status.
EOF
  exit 2
}

case "$ID" in
  '') usage ;;
  annotations)
    MODULES="runner-standalone/annotations-install,runner-standalone/annotations" ;;
  di)
    MODULES="runner-standalone/di-install,runner-standalone/di" ;;
  servlet)
    MODULES="runner-standalone/servlet-install,runner-standalone/servlet" ;;
  pages)
    MODULES="runner-standalone/pages-install,runner-standalone/pages" ;;
  rest)
    MODULES="runner-standalone/rest-install,runner-standalone/rest" ;;
  validation)
    MODULES="runner-standalone/validation-install,runner-standalone/validation" ;;
  websocket)
    MODULES="runner-standalone/websocket-install,runner-standalone/websocket" ;;
  concurrency|data|cdi|cdi-ee)
    MODULES="runner-standalone/$ID" ;;
  security|authentication|faces)
    # These TCK reactors manage their own TomEE; they need the full Maven
    # lifecycle up to verify for their invoker runs.
    shift
    exec "$ROOT_DIR/mvnw" -B -ntp \
      -pl "runner-standalone/$ID" -am \
      -Dtck.standalone=true \
      verify "$@" ;;
  *) usage ;;
esac

shift
exec "$ROOT_DIR/mvnw" -B -ntp \
  -pl "$MODULES" -am \
  -Dtck.standalone=true \
  "-Dtomee.classifier=$TOMEE_CLASSIFIER" \
  test "$@"
