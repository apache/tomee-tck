#!/bin/sh

set -eu

overlay_dir=$1
derby_dir=$2

case "$overlay_dir" in
  */target/tomee-overlay) ;;
  *) echo "Refusing to remove unexpected overlay path: $overlay_dir" >&2; exit 1 ;;
esac

case "$derby_dir" in
  */target/derby) ;;
  *) echo "Refusing to remove unexpected Derby path: $derby_dir" >&2; exit 1 ;;
esac

rm -rf "$overlay_dir" "$derby_dir"
