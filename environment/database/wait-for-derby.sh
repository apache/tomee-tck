#!/bin/sh

set -eu

java_command=$1
classpath=$2
attempt=0

until "$java_command" -cp "$classpath" org.apache.derby.drda.NetworkServerControl ping >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    echo "Derby did not become ready on localhost:1527 within 30 seconds" >&2
    exit 1
  fi
  sleep 1
done

echo "Derby is ready on localhost:1527"
