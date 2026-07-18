#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0.

# Jakarta Authentication 3.1 TCK against TomEE.
#
# The Authentication TCK is distributed as a source Maven reactor (17
# modules, JUnit 4 + Arquillian + HtmlUnit), not as consumable test jars.
# Its pom ships tomcat-ci-managed/tomcat-remote profiles that are the
# closest template for TomEE, which implements Jakarta Authentication on
# top of Tomcat's native support.
#
# Requirements: JDK 17+, network access. Fixed ports 8080/8443/8005.

set -eu

TCK_VERSION=${AUTHENTICATION_TCK_VERSION:-3.1.2}
TCK_ZIP="jakarta-authentication-tck-${TCK_VERSION}.zip"
TCK_URL="https://download.eclipse.org/jakartaee/authentication/3.1/${TCK_ZIP}"
TCK_SHA256=${AUTHENTICATION_TCK_SHA256:-}

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
rm -rf authentication-tck && mkdir authentication-tck
unzip -q "$TCK_ZIP" -d authentication-tck

echo "The Jakarta Authentication TCK reactor is unpacked under:"
find authentication-tck -maxdepth 2 -name pom.xml | head -3
cat <<'EOF'

Next steps (not yet automated):
 1. Add a tomee-remote profile modeled on the shipped tomcat-remote profile,
    using org.apache.tomee:arquillian-tomee-remote and this repository's
    environment/tomee/conf users. The spi module additionally needs a shared
    log.file.location system property on server and client plus the
    logical.hostname.servlet value.
 2. Run: mvn -f authentication-tck/*/tck/pom.xml verify -Ptomee-remote \
      -DskipEJB -DskipJACC   # EJB/JACC modules are outside the Web Profile
EOF
exit 2
