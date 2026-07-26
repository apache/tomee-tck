#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Builds Apache TomEE from source at a given git ref and installs the
# artifacts the harness consumes -- every apache-tomee distribution ZIP and
# the arquillian-tomee-remote adapter -- into a Maven local repository.
#
#   build-tomee.sh <git-ref> [repository-directory]
#
# <git-ref> is any tag, branch, or commit of apache/tomee (override the clone
# source with TOMEE_REPO_URL). The repository directory defaults to the
# Maven local repository the calling environment already uses; pass an
# explicit path to populate a repository slice for stashing.
#
# The script echoes the built version to stdout as a single
# TOMEE_VERSION=<version> line and writes the same line to
# <repository-directory>/../tomee-build.env, so callers can feed the version
# back into the runners with -Dtomee.version=... . All other output goes to
# stderr, so a caller can capture the version with a plain command
# substitution.
#
# apache/tomee ships no Maven wrapper, so the build runs through this
# harness's wrapper (pointed at the TomEE reactor with -f). Tests are skipped:
# the harness validates the build through the TCK suites, not through TomEE's
# own unit tests.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)

REF=${1:-}
if [ -z "$REF" ]; then
  echo "usage: $0 <git-ref> [repository-directory]" >&2
  exit 2
fi

TOMEE_REPO_URL=${TOMEE_REPO_URL:-https://github.com/apache/tomee.git}
# Where the built artifacts land. Everything downstream resolves against this
# same directory, so the distribution ZIP, the adapter and the transitive
# TomEE modules stay consistent with one another.
LOCAL_REPO=${2:-}
if [ -z "$LOCAL_REPO" ]; then
  LOCAL_REPO="${HOME:-.}/.m2/repository"
fi
case "$LOCAL_REPO" in
  /*) ;;
  *) LOCAL_REPO=$(CDPATH= cd -- "$(dirname -- "$LOCAL_REPO")" && pwd)/$(basename -- "$LOCAL_REPO") ;;
esac

# The clone lives outside the harness tree so a `deleteDir()` of the runner
# workspace or a stray `mvn clean` cannot take it with it, and so repeated
# local runs can reuse it.
SRC_DIR=${TOMEE_SRC_DIR:-$ROOT_DIR/target/tomee-src}

echo "build-tomee: ref=$REF url=$TOMEE_REPO_URL src=$SRC_DIR repo=$LOCAL_REPO" >&2

# Fetch just the requested ref rather than the full history: apache/tomee is
# large and CI clones it from scratch every build. A ref that is already
# checked out from a previous local run is re-fetched so a moving branch
# tip is picked up rather than silently reused.
if [ ! -d "$SRC_DIR/.git" ]; then
  mkdir -p "$(dirname -- "$SRC_DIR")"
  git init -q "$SRC_DIR"
  git -C "$SRC_DIR" remote add origin "$TOMEE_REPO_URL"
fi
git -C "$SRC_DIR" remote set-url origin "$TOMEE_REPO_URL"
git -C "$SRC_DIR" fetch --depth 1 --tags origin "$REF" >&2
git -C "$SRC_DIR" -c advice.detachedHead=false checkout -q --force FETCH_HEAD
# A ref switch leaves the previous ref's build output behind; TomEE's reactor
# would happily package stale modules over it.
git -C "$SRC_DIR" clean -qxdf

echo "build-tomee: checked out $(git -C "$SRC_DIR" rev-parse HEAD)" >&2

# The version to hand the runners. Read it from the reactor rather than
# deriving it from the ref: a tag named tomee-11.0.0 builds version 11.0.0,
# and a branch builds whatever -SNAPSHOT it carries.
TOMEE_VERSION=$("$ROOT_DIR/mvnw" -B -ntp -q -f "$SRC_DIR/pom.xml" \
  -Dexec.executable=echo -Dexec.args='${project.version}' \
  --non-recursive org.codehaus.mojo:exec-maven-plugin:3.5.0:exec \
  -Dmaven.repo.local="$LOCAL_REPO" </dev/null | tail -n 1 | tr -d '[:space:]')

if [ -z "$TOMEE_VERSION" ]; then
  echo "build-tomee: ERROR: could not determine the TomEE version at $REF" >&2
  exit 1
fi
echo "build-tomee: building TomEE $TOMEE_VERSION" >&2

# Build and install the reactor. The harness pulls more than the distribution
# ZIPs -- arquillian-tomee-remote and the TomEE modules it depends on -- and
# those must all come from this same build, so a narrow -pl list would leave
# the rest resolving against whatever stale snapshot is in the repository.
# (jakartaee-api, which the signature tests check against, is not part of this
# reactor: it is released from apache/tomee-jakartaee-api and pinned
# separately by the runners that need it.)
#
# TomEE's own 'quick' profile is the narrowest one that still carries
# everything the harness consumes (assembly, arquillian, tomee); it drops the
# examples, itests and TomEE's own tck modules, none of which this harness
# resolves and all of which cost build time. Naming it deactivates the root
# pom's activeByDefault 'main' profile, which is the point; the distribution
# module's own 'all' profile lives in a different pom and stays active, so all
# four classifiers are still assembled.
#
# Only test *execution* is skipped, never test compilation: the distribution
# module generates its BOMs by running org.apache.tomee.bootstrap.GenerateBoms
# through exec-maven-plugin with classpathScope=test, so -Dmaven.test.skip
# leaves that class uncompiled and the build dies on ClassNotFoundException.
"$ROOT_DIR/mvnw" -B -ntp -f "$SRC_DIR/pom.xml" -Pquick \
  -DskipTests -Dsurefire.skip=true -DfailIfNoTests=false \
  -Dmaven.javadoc.skip=true -Dsource.skip=true \
  -Drat.skip=true -Dcheckstyle.skip=true -Dlicense.skip=true \
  -Dmaven.repo.local="$LOCAL_REPO" \
  install </dev/null >&2

# Fail loudly here rather than letting each of ~25 downstream branches fail
# with an opaque "could not resolve" halfway through its suite.
missing=
for classifier in webprofile microprofile plus plume; do
  zip="$LOCAL_REPO/org/apache/tomee/apache-tomee/$TOMEE_VERSION/apache-tomee-$TOMEE_VERSION-$classifier.zip"
  if [ -f "$zip" ]; then
    echo "build-tomee: installed $zip" >&2
  else
    missing="$missing $classifier"
  fi
done
adapter="$LOCAL_REPO/org/apache/tomee/arquillian-tomee-remote/$TOMEE_VERSION/arquillian-tomee-remote-$TOMEE_VERSION.jar"
if [ -f "$adapter" ]; then
  echo "build-tomee: installed $adapter" >&2
else
  missing="$missing arquillian-tomee-remote"
fi
if [ -n "$missing" ]; then
  echo "build-tomee: ERROR: the build did not install:$missing" >&2
  exit 1
fi

echo "TOMEE_VERSION=$TOMEE_VERSION" > "$(dirname -- "$LOCAL_REPO")/tomee-build.env"
echo "TOMEE_VERSION=$TOMEE_VERSION"
