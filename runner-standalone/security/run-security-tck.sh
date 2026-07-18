#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Jakarta Security 4.0 TCK against TomEE.
#
# The Security TCK is distributed as a source Maven reactor (~85 app modules,
# JUnit 4 + Arquillian + HtmlUnit), not as consumable test jars, so it cannot
# be a plain module of this build. This script follows the approach of the
# apache/tomee tck/security-standalone runner: download the distribution,
# activate a TomEE Arquillian profile, and run the reactor.
#
# Requirements: JDK 17+, network access. Fixed ports 8080/8443/8005.

set -eu

TCK_VERSION=${SECURITY_TCK_VERSION:-4.0.1}
TCK_ZIP="jakarta-security-tck-${TCK_VERSION}.zip"
TCK_URL="https://download.eclipse.org/jakartaee/security/4.0/${TCK_ZIP}"
TCK_SHA256=${SECURITY_TCK_SHA256:-}
TOMEE_CLASSIFIER=${TOMEE_CLASSIFIER:-plume}

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
WORK_DIR="$SCRIPT_DIR/target"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

if [ ! -f "$TCK_ZIP" ]; then
  echo "Downloading $TCK_URL"
  curl -fLO "$TCK_URL"
fi
if [ -n "$TCK_SHA256" ]; then
  echo "$TCK_SHA256  $TCK_ZIP" | shasum -a 256 -c -
fi
rm -rf security-tck && mkdir security-tck
unzip -q "$TCK_ZIP" -d security-tck

echo "The Jakarta Security TCK reactor is unpacked under:"
find security-tck -maxdepth 2 -name pom.xml | head -3
cat <<'EOF'

Next steps (not yet automated):
 1. The shipped tck/pom.xml contains a legacy 'tomee' profile using
    org.apache.tomee:arquillian-tomee-remote against the plus classifier.
    Update it for TomEE 11 (classifier, ports, protocol
    arquillian-protocol-servlet-jakarta) following
    apache/tomee tck/security-standalone/pom.xml, which also regenerates the
    expired OpenID self-signed certificates and skips security-signaturetest.
 2. Run: mvn -f security-tck/*/tck/pom.xml install -Ptomee-remote
EOF
exit 2
