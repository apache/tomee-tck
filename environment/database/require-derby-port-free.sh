#!/bin/sh

set -eu

java_command=$1
classpath=$2

if "$java_command" -cp "$classpath" org.apache.derby.drda.NetworkServerControl ping >/dev/null 2>&1; then
  echo "Refusing to start: a Derby server already answers on localhost:1527" >&2
  exit 1
fi

echo "Derby port localhost:1527 is available"
