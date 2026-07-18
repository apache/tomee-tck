#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Jakarta Faces 4.1 TCK against TomEE.
#
# The Faces TCK is distributed as a source Maven reactor with two very
# different halves: the modern JUnit 4 + Arquillian + HtmlUnit modules
# (faces22/23/40/41, ~319 tests) and the legacy JavaTest old-tck
# (~5,500 tests) that is wired to GlassFish asadmin deployment and still
# needs a real TomEE port. The shipped tomcat-remote/tomcat-ci-managed
# profiles are the template for the modern half; TomEE Plume ships Mojarra,
# matching the reference implementation the TCK was developed against.

set -eu

TCK_VERSION=${FACES_TCK_VERSION:-4.1.2}
TCK_ZIP="jakarta-faces-tck-${TCK_VERSION}.zip"
TCK_URL="https://download.eclipse.org/jakartaee/faces/4.1/${TCK_ZIP}"
TCK_SHA256=${FACES_TCK_SHA256:-}

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WORK_DIR="$SCRIPT_DIR/target"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

if [ ! -f "$TCK_ZIP" ]; then
  echo "Downloading $TCK_URL"
  curl -fLO "$TCK_ZIP"
fi
if [ -n "$TCK_SHA256" ]; then
  echo "$TCK_SHA256  $TCK_ZIP" | shasum -a 256 -c -
fi
rm -rf faces-tck && mkdir faces-tck
unzip -q "$TCK_ZIP" -d faces-tck

echo "The Jakarta Faces TCK reactor is unpacked under:"
find faces-tck -maxdepth 2 -name pom.xml | head -3
cat <<'EOF'

Next steps (not yet automated):
 1. Modern modules: add a tomee-remote profile (template: the shipped
    tomcat-remote profile) using org.apache.tomee:arquillian-tomee-remote
    and run mvn verify on the faces41/... modules.
 2. old-tck: the legacy JavaTest harness deploys through GlassFish asadmin
    and reads old-tck/source/install/jsf/bin/ts.jte; porting it to TomEE is
    an open work item comparable to the old platform-TCK harness.
 3. faces-signaturetest: run against TomEE's jakarta.faces API jar.
EOF
exit 2
