#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Refuses to start when any TomEE port is already bound. The Arquillian
# adapter (and a plain socket wait) silently attaches to whatever already
# answers on the port, so a foreign server left on 8080 makes a runner look
# green without ever exercising the TomEE under test. Mirrors
# environment/database/require-derby-port-free.sh for the container ports.
#
#   require-tomee-ports-free.sh <http-port> [https-port] [shutdown-port] [additional-port ...]

set -eu

busy=0

# Resolve a TCP probe once, up front, and fail closed if none is usable. This
# script runs under dash on the CI agents, where /dev/tcp is not wired, so the
# /dev/tcp fallback must run through an explicit bash rather than inline -- an
# inline redirect under dash silently succeeds and makes every port look free.
# With neither nc nor bash available the guard cannot tell a bound port from a
# free one; approving an unverifiable port would let the Arquillian adapter
# attach to a foreign server already on it, so refuse instead. Kept identical
# to environment/ports/select-free-port.sh so selection and this final
# assertion agree on what "free" means.
NC=$(command -v nc 2>/dev/null || true)
BASH=$(command -v bash 2>/dev/null || true)
if [ -z "$NC" ] && [ -z "$BASH" ]; then
  echo "Refusing to start: no working TCP probe available (need nc, or bash with /dev/tcp)" >&2
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

check_port() {
  name=$1
  port=$2
  [ -n "$port" ] || return 0
  if port_in_use "$port"; then
    echo "Refusing to start: the $name port localhost:$port is already bound" >&2
    busy=1
  else
    echo "TomEE $name port localhost:$port is available"
  fi
}

check_port "HTTP" "${1:?usage: require-tomee-ports-free.sh <http-port> [https-port] [shutdown-port] [additional-port ...]}"
check_port "HTTPS" "${2:-}"
check_port "shutdown" "${3:-}"

if [ "$#" -ge 4 ]; then
  shift 3
  for extra in "$@"; do
    check_port "additional" "$extra"
  done
fi

if [ "$busy" -ne 0 ]; then
  exit 1
fi
