#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Selects a free TCP port for a runner to bind. Echoes the preferred port when
# it is free, otherwise scans upward for the first free port not in the avoid
# list. The probe is kept identical to
# environment/tomee/require-tomee-ports-free.sh so that selection and the final
# refuse-if-busy assertion agree on what "free" means. Only the chosen port
# reaches stdout; all diagnostics go to stderr so callers can capture it with
# a simple command substitution.
#
#   select-free-port.sh <preferred-port> [port-to-avoid ...]

set -eu

# Resolve a TCP probe once, up front, and fail closed if none is usable. This
# script runs under dash on the CI agents, where /dev/tcp is not wired, so the
# /dev/tcp fallback must run through an explicit bash rather than inline -- an
# inline redirect under dash silently succeeds and makes every port look free.
# With neither nc nor bash available the script cannot tell a bound port from a
# free one; picking or approving an unverifiable port would let the Arquillian
# adapter attach to a foreign server already on it, so refuse instead.
NC=$(command -v nc 2>/dev/null || true)
BASH=$(command -v bash 2>/dev/null || true)
if [ -z "$NC" ] && [ -z "$BASH" ]; then
  echo "no working TCP probe available (need nc, or bash with /dev/tcp); refusing to select an unverifiable port" >&2
  exit 1
fi

# A connect that succeeds means something already listens on the port.
port_in_use() {
  port=$1
  if [ -n "$NC" ]; then
    "$NC" -z localhost "$port" >/dev/null 2>&1
  else
    "$BASH" -c 'exec 3<>"/dev/tcp/localhost/$1" && exec 3>&- 3<&-' _ "$port" >/dev/null 2>&1
  fi
}

preferred=${1:?usage: select-free-port.sh <preferred-port> [port-to-avoid ...]}
shift

# Ports this selection must not reuse (e.g. this branch's other picks). Ports
# are space-safe tokens, so a single captured copy iterates cleanly.
avoid="$*"

attempts=0
port=$preferred
while [ "$attempts" -lt 500 ]; do
  skip=0
  for a in $avoid; do
    if [ "$a" = "$port" ]; then
      skip=1
      break
    fi
  done

  if [ "$skip" -eq 0 ] && ! port_in_use "$port"; then
    if [ "$port" != "$preferred" ]; then
      echo "port $preferred is busy or reserved; selected $port instead" >&2
    fi
    echo "$port"
    exit 0
  fi

  port=$((port + 1))
  attempts=$((attempts + 1))
done

echo "no free TCP port found scanning upward from $preferred" >&2
exit 1
