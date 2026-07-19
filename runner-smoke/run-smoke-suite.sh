#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Selects free HTTP and shutdown ports and runs the smoke suite on them. The
# stock plume distribution ships the TLS and AJP connectors commented out, so
# the smoke run binds only these two host ports.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
SELECT_PORT="$ROOT_DIR/environment/ports/select-free-port.sh"
TOMEE_HTTP_PORT=$(sh "$SELECT_PORT" 8080)
TOMEE_SHUTDOWN_PORT=$(sh "$SELECT_PORT" 8005 "$TOMEE_HTTP_PORT")
echo "Using ports: http=$TOMEE_HTTP_PORT shutdown=$TOMEE_SHUTDOWN_PORT"

# Final assertion on the chosen ports: the Arquillian adapter otherwise
# attaches to whatever already answers on the HTTP port. The empty second arg
# skips the HTTPS check via the guard's [ -n "$port" ] handling.
sh "$ROOT_DIR/environment/tomee/require-tomee-ports-free.sh" "$TOMEE_HTTP_PORT" "" "$TOMEE_SHUTDOWN_PORT"

exec "$ROOT_DIR/mvnw" -B -ntp -pl runner-smoke -am verify \
  "-Dtomee.http.port=$TOMEE_HTTP_PORT" "-Dtomee.shutdown.port=$TOMEE_SHUTDOWN_PORT" "$@"
