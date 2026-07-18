#!/bin/sh

set -eu

java_command=$1
classpath=$2
port=${3:-1527}

if "$java_command" -cp "$classpath" org.apache.derby.drda.NetworkServerControl ping -p "$port" >/dev/null 2>&1; then
  echo "Refusing to start: a Derby server already answers on localhost:$port" >&2
  exit 1
fi

echo "Derby port localhost:$port is available"
