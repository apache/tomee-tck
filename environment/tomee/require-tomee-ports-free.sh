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
#   require-tomee-ports-free.sh <http-port> [https-port] [shutdown-port]

set -eu

busy=0

# A connect that succeeds means something already listens on the port. Prefer
# nc, fall back to bash's /dev/tcp; both are present on the CI agents.
port_in_use() {
  port=$1
  if command -v nc >/dev/null 2>&1; then
    nc -z localhost "$port" >/dev/null 2>&1
  else
    (exec 3<>"/dev/tcp/localhost/$port") 2>/dev/null && exec 3>&- 3<&-
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

check_port "HTTP" "${1:?usage: require-tomee-ports-free.sh <http-port> [https-port] [shutdown-port]}"
check_port "HTTPS" "${2:-}"
check_port "shutdown" "${3:-}"

if [ "$busy" -ne 0 ]; then
  exit 1
fi
