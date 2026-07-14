#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# shellcheck disable=SC1091
. "$SCRIPT_DIR/versions.env"

if [ -n "${1:-}" ] && [ "${1:-}" != "--metadata-only" ]; then
  echo "usage: $0 [--metadata-only]" >&2
  exit 2
fi

hash_file() {
  algorithm=$1
  file=$2
  if command -v "sha${algorithm}sum" >/dev/null 2>&1; then
    "sha${algorithm}sum" "$file" | awk '{print $1}'
  else
    shasum -a "$algorithm" "$file" | awk '{print $1}'
  fi
}

assert_hash() {
  algorithm=$1
  expected=$2
  file=$3
  actual=$(hash_file "$algorithm" "$file")
  if [ "$actual" != "$expected" ]; then
    echo "checksum mismatch: $file" >&2
    echo "expected: $expected" >&2
    echo "actual:   $actual" >&2
    exit 1
  fi
  echo "verified SHA-$algorithm: $file"
}

tmp_dir=$(mktemp -d "${TMPDIR:-/tmp}/tomee-tck-inputs.XXXXXX")
trap 'rm -rf "$tmp_dir"' EXIT HUP INT TERM

curl -fsSL "$JAKARTA_TCK_BOM_URL" -o "$tmp_dir/artifacts-bom.pom"
assert_hash 256 "$JAKARTA_TCK_BOM_SHA256" "$tmp_dir/artifacts-bom.pom"

derby_base="https://repo1.maven.org/maven2/org/apache/derby"
curl -fsSL "$derby_base/derbyclient/$DERBY_VERSION/derbyclient-$DERBY_VERSION.jar" -o "$tmp_dir/derbyclient.jar"
curl -fsSL "$derby_base/derbynet/$DERBY_VERSION/derbynet-$DERBY_VERSION.jar" -o "$tmp_dir/derbynet.jar"
curl -fsSL "$derby_base/derbyshared/$DERBY_VERSION/derbyshared-$DERBY_VERSION.jar" -o "$tmp_dir/derbyshared.jar"
curl -fsSL "$derby_base/derbytools/$DERBY_VERSION/derbytools-$DERBY_VERSION.jar" -o "$tmp_dir/derbytools.jar"
assert_hash 256 "$DERBY_CLIENT_SHA256" "$tmp_dir/derbyclient.jar"
assert_hash 256 "$DERBY_NET_SHA256" "$tmp_dir/derbynet.jar"
assert_hash 256 "$DERBY_SHARED_SHA256" "$tmp_dir/derbyshared.jar"
assert_hash 256 "$DERBY_TOOLS_SHA256" "$tmp_dir/derbytools.jar"
